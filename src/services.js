const DIFFICULTY_BY_IMAGE = {
  n: "N",
  h: "H",
  a: "A",
  l: "L",
  b: "B",
};

const firstValue = (record, keys) => {
  for (const key of keys) {
    const value = record?.[key];
    if (value !== undefined && value !== null && String(value).trim() !== "") return value;
  }
  return null;
};

const numberValue = (value) => {
  const match = String(value ?? "").replace(/,/g, "").match(/-?\d+(?:\.\d+)?/);
  return match ? Number(match[0]) : 0;
};

const normaliseDifficulty = (value) => {
  const raw = String(value ?? "").toUpperCase();
  if (/LEGG|LEG|☆|L/.test(raw)) return "L";
  if (/ANOTHER|SPA|DPA|A/.test(raw)) return "A";
  if (/HYPER|SPH|DPH|H/.test(raw)) return "H";
  return "N";
};

const normaliseMode = (value) => (/DP|DOUBLE|2P/.test(String(value ?? "").toUpperCase()) ? "DP" : "SP");

const clearType = (value) => {
  const raw = String(value ?? "").toUpperCase();
  if (/NO.?PLAY|未プレイ|NO PLAY|—|-/.test(raw)) return "NO PLAY";
  if (/FULL|FC|FULLCOMBO|EXH|EX HARD/.test(raw)) return "EXH-CLEAR";
  if (/HARD|H-CLEAR/.test(raw)) return "HARD";
  if (/EASY|E-CLEAR/.test(raw)) return "EASY";
  if (/CLEAR|CLEAR/.test(raw)) return "CLEAR";
  return raw || "NO PLAY";
};

const rankValue = (value) => {
  const raw = String(value ?? "").toUpperCase().replace(/\s/g, "");
  return /AAA|AA|A|B|C|D|E|F/.exec(raw)?.[0] ?? "—";
};

function isRawBjmScore(record) {
  return record && typeof record === "object"
    && firstValue(record, ["music_id", "musicId"]) !== null
    && firstValue(record, ["play_style", "playStyle"]) !== null
    && firstValue(record, ["note_id", "noteId"]) !== null;
}

function mapRawBjmScore(record) {
  if (!isRawBjmScore(record)) return null;
  const musicId = numberValue(firstValue(record, ["music_id", "musicId"]));
  const playStyle = numberValue(firstValue(record, ["play_style", "playStyle"]));
  const noteId = numberValue(firstValue(record, ["note_id", "noteId"]));
  if (!musicId || ![0, 1].includes(playStyle) || noteId < 0 || noteId > 4) return null;
  return {
    musicId,
    playStyle,
    noteId,
    clearFlag: numberValue(firstValue(record, ["clear_flag", "clearFlag"])),
    missCount: numberValue(firstValue(record, ["miss_count", "missCount"])),
    time: numberValue(firstValue(record, ["time", "playedAt"])),
    exScore: numberValue(firstValue(record, ["ex_score", "exScore"])),
    option1: numberValue(firstValue(record, ["option1"])),
    option2: numberValue(firstValue(record, ["option2"])),
    source: "bjm",
  };
}

export function parseTextageHtml(html) {
  const document = new DOMParser().parseFromString(html, "text/html");
  const rows = [...document.querySelectorAll("tr")].filter((row) => row.querySelector("td.tt0"));
  const charts = [];

  for (const row of rows) {
    const titleCell = row.querySelector("td.tt0");
    const title = titleCell?.textContent?.trim();
    if (!title) continue;

    const cells = [...row.querySelectorAll(":scope > td")];
    const titleIndex = cells.indexOf(titleCell);
    const version = document.querySelector("td.ctt")?.textContent?.replace(/VERSION\s*:\s*/i, "").split("[")[0].trim() || "Textage";
    const songId = titleCell.getAttribute("title") || title.toLowerCase().replace(/[^a-z0-9]+/g, "-");

    cells.forEach((cell, index) => {
      const image = cell.querySelector("img[src*=" + JSON.stringify("/lv/") + "]");
      const match = image?.getAttribute("src")?.match(/\/lv\/([a-z])([0-9]+)\.gif/i);
      if (!match || !Number(match[2])) return;

      const mode = index < titleIndex ? "SP" : "DP";
      const difficulty = DIFFICULTY_BY_IMAGE[match[1].toLowerCase()];
      if (!difficulty || difficulty === "B") return;

      const link = cell.querySelector("a[href]")?.getAttribute("href") || "";
      charts.push({
        id: `textage-${songId}-${mode.toLowerCase()}${difficulty.toLowerCase()}`,
        songId: `textage-${songId}`,
        title,
        artist: "",
        version,
        mode,
        difficulty,
        level: Number(match[2]),
        notes: null,
        score: 0,
        rank: "—",
        clearType: "NO PLAY",
        confirmed: false,
        source: "textage",
        textageUrl: link ? new URL(link, "https://textage.cc/score/").href : "https://textage.cc/score/",
        updatedAt: null,
      });
    });
  }

  return dedupeCharts(charts);
}

function collectRecordArrays(value, found = []) {
  if (Array.isArray(value)) {
    if (value.some((item) => item && typeof item === "object" && !Array.isArray(item))) found.push(value);
    value.forEach((item) => collectRecordArrays(item, found));
  } else if (value && typeof value === "object") {
    Object.values(value).forEach((item) => collectRecordArrays(item, found));
  }
  return found;
}

function mapScoreRecord(record) {
  const title = firstValue(record, ["title", "songTitle", "musicName", "name", "曲名", "楽曲名"]);
  if (!title) return null;
  const mode = normaliseMode(firstValue(record, ["mode", "playStyle", "style", "type", "プレイ"]));
  const difficulty = normaliseDifficulty(firstValue(record, ["difficulty", "chart", "譜面", "譜面難度"]));
  const scoreRaw = firstValue(record, ["score", "exScore", "exscore", "EXSCORE", "スコア"]);
  const levelRaw = firstValue(record, ["level", "difficultyLevel", "☆", "レベル"]);

  return {
    title: String(title).trim(),
    artist: String(firstValue(record, ["artist", "曲作者", "アーティスト"]) ?? "").trim(),
    version: String(firstValue(record, ["version", "作品", "シリーズ"]) ?? "BJM").trim(),
    mode,
    difficulty,
    level: numberValue(levelRaw),
    notes: numberValue(firstValue(record, ["notes", "noteCount", "ノーツ"])),
    score: numberValue(scoreRaw),
    rank: rankValue(firstValue(record, ["rank", "djLevel", "grade", "DJ LEVEL", "ランク"])),
    clearType: clearType(firstValue(record, ["clearType", "clear", "clearStatus", "status", "クリアタイプ"])),
    source: "bjm",
  };
}

export function parseBjmPayload(input) {
  const raw = typeof input === "string" ? input.trim() : input;
  if (!raw) return [];

  if (typeof raw === "string" && raw.startsWith("<")) {
    const document = new DOMParser().parseFromString(raw, "text/html");
    const rows = [...document.querySelectorAll("tr")];
    return rows.map((row) => {
      const cells = [...row.querySelectorAll("th,td")].map((cell) => cell.textContent?.trim()).filter(Boolean);
      if (cells.length < 2) return null;
      return mapScoreRecord({
        title: cells[0],
        difficulty: cells.find((cell) => /NORMAL|HYPER|ANOTHER|LEGGENDARIA|SP|DP/i.test(cell)) ?? "SP A",
        score: cells.find((cell) => /\d{3,}/.test(cell)),
        rank: cells.find((cell) => /AAA|AA|^[A-F]$/.test(cell)),
        clear: cells.find((cell) => /NO PLAY|CLEAR|HARD|EASY|EXH/i.test(cell)),
      });
    }).filter(Boolean);
  }

  try {
    const parsed = typeof raw === "string" ? JSON.parse(raw) : raw;
    const arrays = collectRecordArrays(parsed);
    const source = arrays.sort((a, b) => b.length - a.length)[0] || (Array.isArray(parsed) ? parsed : [parsed]);
    const rawScores = source.map(mapRawBjmScore).filter(Boolean);
    if (rawScores.length) return rawScores;
    return source.map(mapScoreRecord).filter(Boolean);
  } catch {
    return [];
  }
}

export function mergeBjmScores(charts, records) {
  if (!records.length) return { charts, matched: 0, imported: 0 };
  const next = charts.map((chart) => ({ ...chart }));
  let matched = 0;
  let imported = 0;

  for (const record of records) {
    const target = next.find((chart) => chart.mode === record.mode
      && chart.difficulty === record.difficulty
      && (record.level === 0 || chart.level === record.level)
      && similarTitle(chart.title, record.title));

    if (target) {
      Object.assign(target, {
        score: record.score || target.score,
        rank: record.rank || target.rank,
        clearType: record.clearType || target.clearType,
        notes: record.notes || target.notes,
        source: "bjm",
        updatedAt: new Date().toISOString(),
      });
      matched += 1;
      continue;
    }

    next.push({
      id: `bjm-${slug(record.title)}-${record.mode.toLowerCase()}${record.difficulty.toLowerCase()}-${record.level || "x"}`,
      songId: `bjm-${slug(record.title)}`,
      title: record.title,
      artist: record.artist,
      version: record.version,
      mode: record.mode,
      difficulty: record.difficulty,
      level: record.level || 0,
      notes: record.notes || null,
      score: record.score || 0,
      rank: record.rank || "—",
      clearType: record.clearType || "NO PLAY",
      confirmed: false,
      source: "bjm",
      updatedAt: new Date().toISOString(),
    });
    imported += 1;
  }

  return { charts: dedupeCharts(next), matched, imported };
}

export function mergeTextageCharts(current, imported) {
  if (!imported.length) return current;
  const next = current.map((chart) => ({ ...chart }));
  for (const incoming of imported) {
    const existing = next.find((chart) => chart.title === incoming.title
      && chart.mode === incoming.mode
      && chart.difficulty === incoming.difficulty
      && chart.level === incoming.level);
    if (existing) Object.assign(existing, { ...incoming, confirmed: existing.confirmed, score: existing.score, rank: existing.rank, clearType: existing.clearType });
    else next.push(incoming);
  }
  return dedupeCharts(next);
}

export function dedupeCharts(charts) {
  return [...new Map(charts.map((chart) => [chart.id, chart])).values()];
}

function similarTitle(left, right) {
  const normalise = (value) => String(value).toLowerCase().replace(/[\s()（）「」【】・!！?？☆★._-]/g, "");
  const a = normalise(left);
  const b = normalise(right);
  return a === b || a.includes(b) || b.includes(a);
}

function slug(value) {
  return String(value).toLowerCase().replace(/[^a-z0-9\u3040-\u30ff\u3400-\u9fff]+/g, "-").replace(/^-|-$/g, "") || "song";
}

export function makeBjmExportExample() {
  return JSON.stringify({
    scores: [
      { title: "A", mode: "SP", difficulty: "ANOTHER", level: 12, score: 3712, rank: "AA", clearType: "EASY" },
      { title: "quasar", mode: "SP", difficulty: "HYPER", level: 11, score: 3004, rank: "AA", clearType: "HARD" },
    ],
  }, null, 2);
}
