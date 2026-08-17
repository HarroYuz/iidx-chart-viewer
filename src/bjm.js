const BJM_ORIGIN = "https://u.bjmania.com";
const SCORE_PATH = "/api/WebUI/GetIidxScores";

const isObject = (value) => value !== null && typeof value === "object";

function nativeApi() {
  return globalThis.Capacitor?.Plugins?.BjmaniaApi
    || globalThis.CapacitorUtils?.Synapse?.BjmaniaApi
    || null;
}

function bytesToBase64(bytes) {
  let binary = "";
  for (let index = 0; index < bytes.length; index += 0x8000) {
    binary += String.fromCharCode(...bytes.subarray(index, index + 0x8000));
  }
  return btoa(binary);
}

function base64ToBytes(value) {
  const binary = atob(value || "");
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) bytes[index] = binary.charCodeAt(index);
  return bytes;
}

function readVarint(bytes, offset) {
  let value = 0n;
  let shift = 0n;
  let cursor = offset;
  while (cursor < bytes.length) {
    const current = bytes[cursor];
    value |= BigInt(current & 0x7f) << shift;
    cursor += 1;
    if ((current & 0x80) === 0) return { value, offset: cursor };
    shift += 7n;
    if (shift > 70n) throw new Error("BJM protobuf varint is too long");
  }
  throw new Error("BJM protobuf is truncated");
}

function toSafeNumber(value) {
  const number = Number(value);
  return Number.isSafeInteger(number) ? number : Number(value & 0x7fffffffffffffffn);
}

function skipProtoField(bytes, wireType, offset) {
  if (wireType === 0) return readVarint(bytes, offset).offset;
  if (wireType === 1) return offset + 8;
  if (wireType === 2) {
    const length = readVarint(bytes, offset);
    return length.offset + Number(length.value);
  }
  if (wireType === 5) return offset + 4;
  throw new Error(`Unsupported BJM protobuf wire type: ${wireType}`);
}

function unwrapGrpcWeb(bytes) {
  const payloads = [];
  let offset = 0;
  while (offset + 5 <= bytes.length) {
    const flags = bytes[offset];
    const length = new DataView(bytes.buffer, bytes.byteOffset + offset + 1, 4).getUint32(0, false);
    const end = offset + 5 + length;
    if (end > bytes.length) throw new Error("BJM gRPC-Web response is truncated");
    if ((flags & 0x80) === 0) payloads.push(bytes.slice(offset + 5, end));
    offset = end;
  }
  if (offset !== bytes.length) throw new Error("BJM gRPC-Web response has an invalid frame");
  if (payloads.length === 0) return new Uint8Array();
  if (payloads.length === 1) return payloads[0];

  const total = payloads.reduce((sum, payload) => sum + payload.length, 0);
  const merged = new Uint8Array(total);
  let cursor = 0;
  payloads.forEach((payload) => {
    merged.set(payload, cursor);
    cursor += payload.length;
  });
  return merged;
}

function decodeScore(bytes) {
  const score = {};
  let offset = 0;
  while (offset < bytes.length) {
    const tag = readVarint(bytes, offset);
    offset = tag.offset;
    const field = Number(tag.value >> 3n);
    const wireType = Number(tag.value & 7n);
    if (wireType === 0) {
      const result = readVarint(bytes, offset);
      offset = result.offset;
      const value = toSafeNumber(result.value);
      if (field === 1) score.musicId = value;
      if (field === 2) score.playStyle = value;
      if (field === 3) score.noteId = value;
      if (field === 4) score.clearFlag = value;
      if (field === 5) score.missCount = value;
      if (field === 6) score.time = value;
      if (field === 7) score.exScore = value;
      if (field === 10) score.option1 = value;
      if (field === 11) score.option2 = value;
      continue;
    }
    offset = skipProtoField(bytes, wireType, offset);
  }
  return score;
}

function decodeIidxScores(bytes) {
  const scores = [];
  let status = 0;
  let offset = 0;
  while (offset < bytes.length) {
    const tag = readVarint(bytes, offset);
    offset = tag.offset;
    const field = Number(tag.value >> 3n);
    const wireType = Number(tag.value & 7n);
    if (field === 1 && wireType === 2) {
      const length = readVarint(bytes, offset);
      offset = length.offset;
      const end = offset + Number(length.value);
      if (end > bytes.length) throw new Error("BJM score message is truncated");
      scores.push(decodeScore(bytes.slice(offset, end)));
      offset = end;
      continue;
    }
    if (field === 2 && wireType === 0) {
      const result = readVarint(bytes, offset);
      status = toSafeNumber(result.value);
      offset = result.offset;
      continue;
    }
    offset = skipProtoField(bytes, wireType, offset);
  }
  return { scores, status };
}

async function requestBjmJson(path) {
  const plugin = nativeApi();
  if (plugin?.authMe && path === "/api/auth/me") {
    const response = await plugin.authMe();
    return { status: response.status, data: response.data ?? null, native: true };
  }

  const response = await fetch(`${BJM_ORIGIN}${path}`, {
    method: "GET",
    headers: { Accept: "application/json, text/plain, */*" },
    credentials: "include",
    cache: "no-store",
  });
  const text = await response.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { /* BJM may return an HTML login page. */ }
  return { status: response.status, data, native: false };
}

async function requestBjmGrpc(path, request = new Uint8Array()) {
  const plugin = nativeApi();
  if (plugin?.grpcUnary) {
    const response = await plugin.grpcUnary({ path, requestBase64: bytesToBase64(request) });
    if (response.status < 200 || response.status >= 300) throw new Error(`BJM 请求失败 (${response.status})`);
    return unwrapGrpcWeb(base64ToBytes(response.responseBase64 || ""));
  }

  const response = await fetch(`${BJM_ORIGIN}${path}`, {
    method: "POST",
    headers: {
      Accept: "*/*",
      "Content-Type": "application/grpc-web+proto",
      "X-Grpc-Web": "1",
      "X-User-Agent": "grpc-web-javascript/0.1",
    },
    credentials: "include",
    cache: "no-store",
    body: request,
  });
  if (!response.ok) throw new Error(`BJM 请求失败 (${response.status})`);
  return unwrapGrpcWeb(new Uint8Array(await response.arrayBuffer()));
}

export async function getBjmSession() {
  const response = await requestBjmJson("/api/auth/me");
  if (response.status < 200 || response.status >= 300 || !isObject(response.data)) {
    return { authenticated: false, user: null };
  }
  const user = response.data;
  return {
    authenticated: true,
    user: {
      id: String(user.id ?? ""),
      name: String(user.name ?? ""),
      email: String(user.email ?? ""),
    },
  };
}

export async function fetchBjmIidxScores() {
  const session = await getBjmSession();
  if (!session.authenticated) throw new Error("BJM 登录态不可用，请先登录 BJMANIA");
  const payload = decodeIidxScores(await requestBjmGrpc(SCORE_PATH));
  return {
    ...payload,
    user: session.user,
    fetchedAt: new Date().toISOString(),
    source: "bjm",
  };
}

export function bjmScoreKey(score) {
  return `${score.musicId}:${score.playStyle}:${score.noteId}`;
}

export function summariseBjmScores(scores) {
  const summary = { total: scores.length, sp: 0, dp: 0, latest: null };
  for (const score of scores) {
    if (score.playStyle === 0) summary.sp += 1;
    if (score.playStyle === 1) summary.dp += 1;
    if (Number.isFinite(score.time) && (!summary.latest || score.time > summary.latest)) summary.latest = score.time;
  }
  return summary;
}

export const BJM_ENDPOINTS = Object.freeze({ auth: "/api/auth/me", scores: SCORE_PATH });
