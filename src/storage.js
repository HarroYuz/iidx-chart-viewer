const STORAGE_KEY = "iidx-chart-viewer-state-v2";
const STORAGE_VERSION = 2;

function hasStorage() {
  return typeof window !== "undefined" && window.localStorage;
}

function emptyState(charts) {
  return {
    version: STORAGE_VERSION,
    savedAt: null,
    charts,
    bjm: {
      user: null,
      scores: [],
      fetchedAt: null,
      status: "未同步",
    },
  };
}

function validChartList(value) {
  return Array.isArray(value) && value.length > 0 ? value : null;
}

export function loadStoredState(fallbackCharts) {
  if (!hasStorage()) return emptyState(fallbackCharts);
  try {
    const raw = JSON.parse(window.localStorage.getItem(STORAGE_KEY) || "null");
    if (raw?.version === STORAGE_VERSION && validChartList(raw.charts)) {
      return {
        ...emptyState(fallbackCharts),
        ...raw,
        bjm: { ...emptyState(fallbackCharts).bjm, ...(raw.bjm || {}) },
      };
    }

    // Migrate the first prototype, which only persisted the chart array.
    if (validChartList(raw?.charts)) {
      return { ...emptyState(fallbackCharts), charts: raw.charts };
    }
  } catch {
    // A malformed local snapshot should not prevent the app from opening.
  }
  return emptyState(fallbackCharts);
}

export function saveStoredState(state) {
  if (!hasStorage()) return false;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify({
      version: STORAGE_VERSION,
      savedAt: new Date().toISOString(),
      charts: state.charts,
      bjm: state.bjm,
    }));
    return true;
  } catch {
    return false;
  }
}

export function clearStoredState() {
  if (!hasStorage()) return;
  window.localStorage.removeItem(STORAGE_KEY);
}

export function storedStateSize(state) {
  return new Blob([JSON.stringify(state)]).size;
}

export { STORAGE_KEY, STORAGE_VERSION };
