import { cloneDemoCharts, DIFFICULTIES, MODES } from "./data.js";
import {
  makeBjmExportExample,
  mergeBjmScores,
  mergeTextageCharts,
  parseBjmPayload,
  parseTextageHtml,
} from "./services.js";
import { bjmScoreKey, fetchBjmIidxScores, summariseBjmScores } from "./bjm.js";
import { clearStoredState, loadStoredState, saveStoredState, storedStateSize } from "./storage.js";
import "./styles.css";

const app = document.querySelector("#app");
const initialState = loadStoredState(cloneDemoCharts());

const state = {
  view: "overview",
  charts: initialState.charts,
  bjm: initialState.bjm,
  filters: { query: "", mode: "SP", difficulty: "ALL", level: "ALL", status: "ALL", sort: "level-asc" },
  selectedChart: null,
  toast: null,
  sourceStatus: { textage: "未同步" },
  importTextage: "",
  importBjm: "",
};

function saveState() {
  saveStoredState(state);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function formatNumber(value) {
  return value ? Number(value).toLocaleString("zh-CN") : "—";
}

function getStats() {
  const total = state.charts.length;
  const confirmed = state.charts.filter((chart) => chart.confirmed).length;
  const played = state.bjm.scores.length || state.charts.filter((chart) => chart.clearType !== "NO PLAY").length;
  const level12 = state.charts.filter((chart) => chart.level === 12).length;
  const level12Done = state.charts.filter((chart) => chart.level === 12 && chart.confirmed).length;
  return { total, confirmed, played, level12, level12Done, percentage: total ? Math.round((confirmed / total) * 100) : 0 };
}

function applyBjmScoresToCharts(charts, scores) {
  const scoreMap = new Map(scores.map((score) => [bjmScoreKey(score), score]));
  return charts.map((chart) => {
    if (!Number.isFinite(chart.musicId)) return chart;
    const noteId = { B: 0, N: 1, H: 2, A: 3, L: 4 }[chart.difficulty];
    const playStyle = chart.mode === "DP" ? 1 : 0;
    const score = scoreMap.get(`${chart.musicId}:${playStyle}:${noteId}`);
    if (!score) return chart;
    return {
      ...chart,
      score: score.exScore,
      source: "bjm",
      updatedAt: new Date(score.time * 1000).toISOString(),
    };
  });
}

function bjmStatusText() {
  if (!state.bjm.user) return "未登录";
  const name = state.bjm.user.name || state.bjm.user.id || "已登录";
  return state.bjm.scores.length ? `${name} · ${state.bjm.scores.length} 条` : `${name} · 已登录`;
}

function visibleCharts() {
  const { query, mode, difficulty, level, status, sort } = state.filters;
  const normalizedQuery = query.trim().toLowerCase();
  const filtered = state.charts.filter((chart) => {
    const matchesQuery = !normalizedQuery || `${chart.title} ${chart.artist} ${chart.version}`.toLowerCase().includes(normalizedQuery);
    const matchesMode = mode === "ALL" || chart.mode === mode;
    const matchesDifficulty = difficulty === "ALL" || chart.difficulty === difficulty;
    const matchesLevel = level === "ALL" || chart.level === Number(level);
    const matchesStatus = status === "ALL"
      || (status === "TODO" && !chart.confirmed)
      || (status === "DONE" && chart.confirmed)
      || (status === "PLAYED" && chart.clearType !== "NO PLAY");
    return matchesQuery && matchesMode && matchesDifficulty && matchesLevel && matchesStatus;
  });
  return filtered.sort((a, b) => {
    if (sort === "title") return a.title.localeCompare(b.title, "zh-CN");
    if (sort === "score") return (b.score || 0) - (a.score || 0);
    if (sort === "todo") return Number(a.confirmed) - Number(b.confirmed) || b.level - a.level;
    return b.level - a.level || a.title.localeCompare(b.title, "zh-CN");
  });
}

function render() {
  const stats = getStats();
  app.innerHTML = `
    <div class="app-shell">
      ${renderHeader(stats)}
      <main class="page-content">
        ${state.view === "overview" ? renderOverview(stats) : ""}
        ${state.view === "charts" ? renderCharts() : ""}
        ${state.view === "import" ? renderImport() : ""}
        ${state.view === "settings" ? renderSettings() : ""}
      </main>
      ${renderBottomNav()}
      ${state.selectedChart ? renderChartDialog(state.selectedChart) : ""}
      ${state.toast ? `<div class="toast toast--${state.toast.kind}" role="status">${escapeHtml(state.toast.message)}</div>` : ""}
    </div>`;
  bindEvents();
}

function renderHeader(stats) {
  return `
    <header class="topbar">
      <div class="brand" data-action="nav-view" data-view="overview" role="button" tabindex="0">
        <div class="brand-mark"><span>7</span><span>IIDX</span></div>
        <div><p class="eyebrow">CHART CONFIRM</p><h1>铺面确认</h1></div>
      </div>
      <div class="topbar-actions">
        <span class="sync-dot" title="本地数据"><i></i> 本地</span>
        <button class="icon-button" data-action="nav-view" data-view="settings" aria-label="打开设置">⚙</button>
      </div>
    </header>
    <section class="status-strip">
      <div><span class="status-strip__label">CONFIRM PROGRESS</span><strong>${stats.confirmed}/${stats.total}</strong></div>
      <div class="status-progress"><span style="width:${stats.percentage}%"></span></div>
      <span class="status-strip__percent">${stats.percentage}%</span>
    </section>`;
}

function renderOverview(stats) {
  const next = state.charts.find((chart) => !chart.confirmed && chart.level === 12 && chart.mode === "SP") || state.charts.find((chart) => !chart.confirmed);
  const latest = state.charts.filter((chart) => chart.confirmed).sort((a, b) => String(b.updatedAt).localeCompare(String(a.updatedAt))).slice(0, 4);
  return `
    <section class="hero-card">
      <div class="hero-card__glow"></div>
      <div class="hero-copy">
        <p class="eyebrow eyebrow--bright">YOUR IIDX LIBRARY</p>
        <h2>把每一张铺面<br><em>确认下来。</em></h2>
        <p class="hero-copy__hint">从 Textage 获取铺面，合并 BJM 成绩，按自己的节奏完成确认。</p>
        <button class="primary-button" data-action="nav-view" data-view="charts">开始确认 <span>→</span></button>
      </div>
      <div class="progress-ring" style="--progress:${stats.percentage * 3.6}deg">
        <div><strong>${stats.percentage}<small>%</small></strong><span>已确认</span></div>
      </div>
      <div class="hero-orbit hero-orbit--one"></div><div class="hero-orbit hero-orbit--two"></div>
    </section>
    <section class="metric-grid">
      ${metricCard("铺面总数", formatNumber(stats.total), "ALL CHARTS", "purple")}
      ${metricCard("已有成绩", formatNumber(stats.played), "FROM BJM", "cyan")}
      ${metricCard("12 级进度", `${stats.level12Done}/${stats.level12}`, "SP + DP", "orange")}
    </section>
    <section class="section-heading"><div><p class="eyebrow">NEXT TARGET</p><h2>继续你的确认</h2></div><button class="text-button" data-action="nav-view" data-view="charts">查看全部 →</button></section>
    ${next ? `<article class="focus-card" data-action="open-chart" data-chart-id="${escapeHtml(next.id)}">
      <div class="focus-card__level"><span>LV</span><strong>${next.level || "?"}</strong></div>
      <div class="focus-card__main"><div class="tag-row"><span class="difficulty-badge difficulty-badge--${next.difficulty.toLowerCase()}">${next.mode} ${next.difficulty}</span><span class="muted-tag">${escapeHtml(next.version)}</span></div><h3>${escapeHtml(next.title)}</h3><p>${escapeHtml(next.artist || "未知艺术家")} · ${formatNumber(next.notes)} NOTES</p></div>
      <span class="arrow-button">↗</span>
    </article>` : emptyState("太棒了！当前筛选范围已经全部确认。")}
    <section class="section-heading section-heading--recent"><div><p class="eyebrow">ACTIVITY</p><h2>最近确认</h2></div><span class="section-count">${latest.length} 条</span></section>
    <section class="activity-list">${latest.length ? latest.map(renderActivity).join("") : emptyState("还没有确认记录，从下一张开始吧。")}</section>`;
}

function metricCard(label, value, hint, tone) {
  return `<article class="metric-card metric-card--${tone}"><span class="metric-card__icon">${tone === "purple" ? "◈" : tone === "cyan" ? "↗" : "✦"}</span><p>${label}</p><strong>${value}</strong><small>${hint}</small></article>`;
}

function renderActivity(chart) {
  return `<button class="activity-row" data-action="open-chart" data-chart-id="${escapeHtml(chart.id)}"><span class="activity-icon">✓</span><span class="activity-copy"><strong>${escapeHtml(chart.title)}</strong><small>${chart.mode} ${chart.difficulty} · ${escapeHtml(chart.clearType)}</small></span><span class="activity-score">${chart.score ? formatNumber(chart.score) : "—"}<small>${chart.rank}</small></span></button>`;
}

function renderCharts() {
  const charts = visibleCharts();
  const { query, mode, difficulty, level, status, sort } = state.filters;
  return `
    <section class="page-heading"><div><p class="eyebrow">CHART LIBRARY</p><h2>铺面列表</h2><p>选择一张铺面，确认你对它的理解。</p></div><button class="round-button" data-action="nav-view" data-view="import" aria-label="导入数据">＋</button></section>
    <section class="filter-panel">
      <label class="search-box"><span>⌕</span><input data-filter="query" value="${escapeHtml(query)}" placeholder="搜索曲名、艺术家或版本" aria-label="搜索曲目" /></label>
      <div class="filter-row filter-row--scroll">
        ${segmentedControl("mode", [{label:"全部",value:"ALL"},{label:"SP",value:"SP"},{label:"DP",value:"DP"}], mode)}
        ${segmentedControl("status", [{label:"全部",value:"ALL"},{label:"未确认",value:"TODO"},{label:"已确认",value:"DONE"}], status)}
      </div>
      <div class="filter-row">
        <select data-filter="difficulty" aria-label="难度">${option("全部难度", "ALL", difficulty)}${DIFFICULTIES.map((item) => option(item, item, difficulty)).join("")}</select>
        <select data-filter="level" aria-label="等级"><option value="ALL">全部等级</option>${Array.from({length:12}, (_, index) => option(`LEVEL ${index+1}`, index+1, level)).join("")}</select>
        <select data-filter="sort" aria-label="排序"><option value="level-asc" ${sort === "level-asc" ? "selected" : ""}>等级优先</option><option value="todo" ${sort === "todo" ? "selected" : ""}>待确认优先</option><option value="score" ${sort === "score" ? "selected" : ""}>分数优先</option><option value="title" ${sort === "title" ? "selected" : ""}>曲名排序</option></select>
      </div>
    </section>
    <div class="results-bar"><span><strong>${charts.length}</strong> 张铺面</span><span class="results-source">${state.sourceStatus.textage}</span></div>
    <section class="chart-list">${charts.length ? charts.map(renderChartCard).join("") : emptyState("没有找到符合条件的铺面。", true)}</section>`;
}

function segmentedControl(name, options, selected) {
  return `<div class="segmented-control" data-group="${name}">${options.map((item) => `<button class="${selected === item.value ? "is-active" : ""}" data-filter="${name}" data-value="${item.value}">${item.label}</button>`).join("")}</div>`;
}

function option(label, value, selected) {
  return `<option value="${value}" ${String(selected) === String(value) ? "selected" : ""}>${label}</option>`;
}

function renderChartCard(chart) {
  const scoreClass = chart.score ? "has-score" : "no-score";
  return `<article class="chart-card ${chart.confirmed ? "is-confirmed" : ""}" data-action="open-chart" data-chart-id="${escapeHtml(chart.id)}">
    <div class="chart-card__rail"><span class="level-label">LEVEL</span><strong>${chart.level || "?"}</strong><span class="mode-label">${chart.mode}</span></div>
    <div class="chart-card__body"><div class="tag-row"><span class="difficulty-badge difficulty-badge--${chart.difficulty.toLowerCase()}">${chart.difficulty === "L" ? "LEGGENDARIA" : chart.difficulty === "A" ? "ANOTHER" : chart.difficulty === "H" ? "HYPER" : "NORMAL"}</span><span class="muted-tag">${escapeHtml(chart.version)}</span></div><h3>${escapeHtml(chart.title)}</h3><p>${escapeHtml(chart.artist || "未知艺术家")}</p><div class="chart-card__meta"><span>${formatNumber(chart.notes)} notes</span><span class="score-pill ${scoreClass}">${chart.score ? `${formatNumber(chart.score)} · ${chart.rank}` : "NO PLAY"}</span></div></div>
    <div class="chart-card__state"><span class="clear-dot clear-dot--${chart.clearType.toLowerCase().replaceAll(" ", "-")}"></span><button class="confirm-button ${chart.confirmed ? "is-confirmed" : ""}" data-action="toggle-status" data-chart-id="${escapeHtml(chart.id)}" aria-label="${chart.confirmed ? "取消确认" : "确认铺面"}">${chart.confirmed ? "✓" : "○"}</button></div>
  </article>`;
}

function renderImport() {
  return `
    <section class="page-heading"><div><p class="eyebrow">DATA SOURCES</p><h2>导入成绩与铺面</h2><p>先用本地导入跑通流程，之后接入已登录的 BJM。</p></div><button class="text-button" data-action="nav-view" data-view="charts">返回列表</button></section>
    <section class="source-card source-card--textage"><div class="source-card__top"><div class="source-logo">TXT</div><div><p class="eyebrow">CHART SOURCE</p><h3>Textage 铺面</h3></div><span class="source-state">${state.sourceStatus.textage}</span></div><p>从 Textage 的选曲页 HTML 中解析 SP/DP 难度、等级与曲名。浏览器跨域受限时，可把页面另存为 HTML 后导入。</p><div class="source-card__actions"><button class="secondary-button" data-action="open-textage">打开 Textage</button><button class="secondary-button" data-action="pick-textage">导入 HTML</button><input type="file" accept=".html,.htm,text/html" id="textage-file" hidden /></div><textarea id="textage-input" class="data-textarea" placeholder="也可以把 Textage 页面源代码粘贴到这里…">${escapeHtml(state.importTextage)}</textarea><button class="primary-button primary-button--full" data-action="parse-textage">解析 Textage HTML <span>→</span></button></section>
    <section class="source-card source-card--bjm"><div class="source-card__top"><div class="source-logo source-logo--bjm">BJM</div><div><p class="eyebrow">SCORE SOURCE</p><h3>BJMANIA 成绩</h3></div><span class="source-state">${escapeHtml(bjmStatusText())}</span></div><p>登录态由浏览器或 Android WebView 的 Cookie 存储管理，成绩快照保存在本地。同步时使用 BJM 的完整成绩接口，并按谱面键更新本地记录。</p><div class="notice-box"><span>⌁</span><div><strong>${state.bjm.user ? "已发现 BJM 登录态" : "需要先登录 BJM"}</strong><p>${state.bjm.fetchedAt ? `上次同步：${new Date(state.bjm.fetchedAt).toLocaleString("zh-CN")}` : "初版会先打开 BJM 登录页；Android 版本沿用 GTDR 的原生 Cookie 会话。"}</p></div></div><div class="source-card__actions"><button class="secondary-button" data-action="open-bjm">打开 BJM</button><button class="primary-button" data-action="sync-bjm">${state.bjm.scores.length ? "更新成绩" : "同步成绩"} <span>↻</span></button><button class="secondary-button" data-action="pick-bjm">导入 JSON</button><input type="file" accept=".json,application/json" id="bjm-file" hidden /></div><textarea id="bjm-input" class="data-textarea" placeholder="也可以导入 BJM 原始成绩 JSON…">${escapeHtml(state.importBjm)}</textarea><div class="source-card__footer"><button class="text-button" data-action="fill-bjm-example">填入示例</button><button class="primary-button" data-action="parse-bjm">保存成绩快照 <span>→</span></button></div></section>`;
}

function renderSettings() {
  const size = storedStateSize(state);
  return `<section class="page-heading"><div><p class="eyebrow">APP SETTINGS</p><h2>设置</h2><p>成绩快照和铺面确认状态保存在本地；Cookie 由浏览器或 Android WebView 管理。</p></div></section><section class="settings-card"><div class="settings-row"><div><span class="settings-label">数据版本</span><small>IIDX Chart Viewer 初始版本</small></div><span class="settings-value">v0.1</span></div><div class="settings-row"><div><span class="settings-label">本地数据</span><small>${state.charts.length} 张铺面 · ${state.bjm.scores.length} 条 BJM 成绩 · ${formatNumber(size)} bytes</small></div><button class="danger-button" data-action="reset-data">重置</button></div><div class="settings-row settings-row--link"><div><span class="settings-label">数据来源</span><small>Textage / BJMANIA</small></div><span>↗</span></div></section><section class="about-card"><div class="about-mark">7<span>IIDX</span></div><h3>为每一次上机<br>留下清晰的进度。</h3><p>这是一个面向 IIDX 玩家个人使用的铺面确认工具，界面参考了提供的 GTDR APK 的信息密度与移动端交互。</p></section>`;
}

function renderBottomNav() {
  const items = [{view:"overview",label:"概览",icon:"◈"},{view:"charts",label:"铺面",icon:"⌘"},{view:"import",label:"数据",icon:"⇩"},{view:"settings",label:"设置",icon:"⚙"}];
  return `<nav class="bottom-nav" aria-label="主导航">${items.map((item) => `<button class="bottom-nav__item ${state.view === item.view ? "is-active" : ""}" data-action="nav-view" data-view="${item.view}"><span>${item.icon}</span><small>${item.label}</small></button>`).join("")}</nav>`;
}

function renderChartDialog(chart) {
  return `<div class="modal-backdrop" data-action="close-modal"><section class="chart-dialog" role="dialog" aria-modal="true" aria-label="铺面详情" data-dialog="true"><button class="modal-close" data-action="close-modal" aria-label="关闭">×</button><div class="dialog-hero"><span class="dialog-level">${chart.level || "?"}</span><div><p class="eyebrow">${chart.mode} ${chart.difficulty} · ${escapeHtml(chart.version)}</p><h2>${escapeHtml(chart.title)}</h2><p>${escapeHtml(chart.artist || "未知艺术家")}</p></div></div><div class="dialog-grid"><div><span>EX SCORE</span><strong>${formatNumber(chart.score)}</strong></div><div><span>DJ LEVEL</span><strong>${chart.rank}</strong></div><div><span>CLEAR TYPE</span><strong>${escapeHtml(chart.clearType)}</strong></div><div><span>NOTES</span><strong>${formatNumber(chart.notes)}</strong></div></div><div class="dialog-note"><span class="dialog-note__icon">✦</span><p>${chart.confirmed ? "这张铺面已经确认。保持手感，继续下一张。" : "确认前建议回看关键配置、节奏与个人失误点。"}</p></div><div class="dialog-actions"><button class="secondary-button" data-action="open-textage" data-chart-id="${escapeHtml(chart.id)}">查看 Textage</button><button class="primary-button" data-action="toggle-status" data-chart-id="${escapeHtml(chart.id)}">${chart.confirmed ? "取消确认" : "标记为已确认"} <span>→</span></button></div></section></div>`;
}

function emptyState(message, compact = false) {
  return `<div class="empty-state ${compact ? "empty-state--compact" : ""}"><span>◌</span><strong>${message}</strong>${compact ? "<button class=\"text-button\" data-action=\"reset-filters\">清除筛选</button>" : ""}</div>`;
}

function bindEvents() {
  document.querySelectorAll("[data-action]").forEach((element) => element.addEventListener("click", handleAction));
  document.querySelectorAll("[data-filter]").forEach((element) => {
    element.addEventListener("input", handleFilter);
    element.addEventListener("change", handleFilter);
  });
  const textageInput = document.querySelector("#textage-input");
  if (textageInput) textageInput.addEventListener("input", (event) => { state.importTextage = event.target.value; });
  const bjmInput = document.querySelector("#bjm-input");
  if (bjmInput) bjmInput.addEventListener("input", (event) => { state.importBjm = event.target.value; });
  document.querySelectorAll("[data-action='open-chart']").forEach((element) => element.addEventListener("keydown", (event) => { if (event.key === "Enter") handleAction(event); }));
}

function handleFilter(event) {
  const key = event.currentTarget.dataset.filter;
  const value = event.currentTarget.dataset.value ?? event.currentTarget.value;
  if (key) {
    state.filters[key] = value;
    if (key === "query") render();
    else render();
  }
}

async function handleAction(event) {
  const target = event.currentTarget;
  const action = target.dataset.action;
  if (action === "nav-view") {
    state.view = target.dataset.view;
    state.selectedChart = null;
    render();
  }
  if (action === "open-chart") {
    event.stopPropagation();
    state.selectedChart = state.charts.find((chart) => chart.id === target.dataset.chartId) || null;
    render();
  }
  if (action === "close-modal" && !target.closest("[data-dialog]")?.contains(event.target)) {
    state.selectedChart = null;
    render();
  }
  if (action === "toggle-status") {
    event.stopPropagation();
    const chart = state.charts.find((item) => item.id === target.dataset.chartId);
    if (chart) {
      chart.confirmed = !chart.confirmed;
      chart.updatedAt = chart.confirmed ? new Date().toISOString() : null;
      saveState();
      state.selectedChart = null;
      showToast(chart.confirmed ? "已确认这张铺面" : "已取消确认", "success");
    }
  }
  if (action === "reset-filters") {
    state.filters = { ...state.filters, query: "", difficulty: "ALL", level: "ALL", status: "ALL" };
    render();
  }
  if (action === "open-textage") {
    window.open(target.dataset.chartId ? state.charts.find((chart) => chart.id === target.dataset.chartId)?.textageUrl || "https://textage.cc/score/" : "https://textage.cc/score/", "_blank", "noopener,noreferrer");
  }
  if (action === "open-bjm") window.open("https://u.bjmania.com/panel/iidx/score", "_blank", "noopener,noreferrer");
  if (action === "sync-bjm") syncBjmScores();
  if (action === "pick-textage") document.querySelector("#textage-file")?.click();
  if (action === "pick-bjm") document.querySelector("#bjm-file")?.click();
  if (action === "fill-bjm-example") { state.importBjm = makeBjmExportExample(); render(); }
  if (action === "parse-textage") parseTextageInput();
  if (action === "parse-bjm") parseBjmInput();
  if (action === "reset-data") resetData();
}

async function parseTextageInput() {
  const input = document.querySelector("#textage-input")?.value.trim();
  if (!input) return showToast("请先粘贴或导入 Textage HTML", "error");
  const imported = parseTextageHtml(input);
  if (!imported.length) return showToast("没有识别到铺面，请确认是 Textage 选曲页 HTML", "error");
  state.charts = mergeTextageCharts(state.charts, imported);
  state.sourceStatus.textage = `已同步 ${imported.length} 张`;
  saveState();
  showToast(`已解析 ${imported.length} 张 Textage 铺面`, "success");
}

async function parseBjmInput() {
  const input = document.querySelector("#bjm-input")?.value.trim();
  if (!input) return showToast("请先粘贴或导入 BJM JSON / HTML", "error");
  const records = parseBjmPayload(input);
  if (!records.length) return showToast("没有识别到成绩记录，请检查导出格式", "error");
  if (records[0].musicId) {
    state.bjm = {
      ...state.bjm,
      scores: records,
      fetchedAt: new Date().toISOString(),
      status: `已导入 ${records.length} 条`,
    };
    state.charts = applyBjmScoresToCharts(state.charts, records);
    saveState();
    showToast(`已保存 ${records.length} 条 BJM 原始成绩`, "success");
    return;
  }
  const result = mergeBjmScores(state.charts, records);
  state.charts = result.charts;
  state.bjm = { ...state.bjm, status: `已导入 ${records.length} 条` };
  saveState();
  showToast(`已合并 ${result.matched} 条，新增 ${result.imported} 条`, "success");
}

async function syncBjmScores() {
  const button = document.querySelector("[data-action='sync-bjm']");
  if (button) {
    button.disabled = true;
    button.textContent = "同步中…";
  }
  try {
    const payload = await fetchBjmIidxScores();
    state.bjm = {
      user: payload.user,
      scores: payload.scores,
      fetchedAt: payload.fetchedAt,
      status: `已同步 ${payload.scores.length} 条`,
    };
    state.charts = applyBjmScoresToCharts(state.charts, payload.scores);
    saveState();
    const summary = summariseBjmScores(payload.scores);
    showToast(`BJM 已同步：SP ${summary.sp} 条，DP ${summary.dp} 条`, "success");
  } catch (error) {
    showToast(error instanceof Error ? error.message : "BJM 同步失败，请确认已经登录", "error");
  } finally {
    render();
  }
}

function showToast(message, kind = "success") {
  state.toast = { message, kind };
  render();
  window.setTimeout(() => { state.toast = null; render(); }, 2800);
}

function resetData() {
  if (!window.confirm("确定要清空本地确认记录并恢复示例数据吗？")) return;
  state.charts = cloneDemoCharts();
  state.bjm = { user: null, scores: [], fetchedAt: null, status: "未同步" };
  state.sourceStatus = { textage: "未同步" };
  clearStoredState();
  saveState();
  showToast("已恢复示例数据", "success");
}

document.addEventListener("change", async (event) => {
  const file = event.target.files?.[0];
  if (!file) return;
  const text = await file.text();
  if (event.target.id === "textage-file") { state.importTextage = text; render(); }
  if (event.target.id === "bjm-file") { state.importBjm = text; render(); }
  event.target.value = "";
});

render();
