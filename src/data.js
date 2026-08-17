const chart = (id, title, artist, version, mode, difficulty, level, notes, score, rank, clearType, confirmed = false) => ({
  id,
  songId: id.split("-")[0],
  title,
  artist,
  version,
  mode,
  difficulty,
  level,
  notes,
  score,
  rank,
  clearType,
  confirmed,
  source: "demo",
  updatedAt: confirmed ? "2026-08-16T20:40:00.000Z" : null,
});

export const DIFFICULTIES = ["N", "H", "A", "L"];
export const MODES = ["SP", "DP"];

export const DEMO_CHARTS = [
  chart("s1-spn", "A", "D.J.Amuro", "IIDX RED", "SP", "N", 6, 646, 1589, "A", "CLEAR", true),
  chart("s1-sph", "A", "D.J.Amuro", "IIDX RED", "SP", "H", 10, 1205, 2810, "AA", "HARD", true),
  chart("s1-spa", "A", "D.J.Amuro", "IIDX RED", "SP", "A", 12, 1881, 3712, "AA", "EASY", false),
  chart("s2-spn", "AA", "D.J.Amuro", "IIDX RED", "SP", "N", 7, 729, 1832, "A", "CLEAR", true),
  chart("s2-sph", "AA", "D.J.Amuro", "IIDX RED", "SP", "H", 10, 1128, 2790, "AA", "HARD", true),
  chart("s2-spa", "AA", "D.J.Amuro", "IIDX RED", "SP", "A", 12, 1739, 3610, "A", "CLEAR", false),
  chart("s3-spa", "皿の手", "Kobaryo", "PENDUAL", "SP", "A", 12, 1770, 0, "—", "NO PLAY", false),
  chart("s4-spa", "冥", "Amuro vs Killer", "HAPPY SKY", "SP", "A", 12, 2000, 2998, "A", "EASY", false),
  chart("s5-sph", "quasar", "OutPhase", "9th style", "SP", "H", 11, 1266, 3004, "AA", "HARD", true),
  chart("s5-spa", "quasar", "OutPhase", "9th style", "SP", "A", 12, 1527, 3410, "AA", "CLEAR", false),
  chart("s6-spa", "灼熱Beach Side Bunny", "DJ Mass MAD Izm*", "Resort Anthem", "SP", "A", 12, 1711, 0, "—", "NO PLAY", false),
  chart("s7-spa", "KAMAITACHI", "DJ TECHNORCH fw. GUHROOVY", "GOLD", "SP", "A", 11, 1400, 3102, "AA", "HARD", true),
  chart("s8-spn", "恋する☆宇宙戦争っ!!", "Prim", "Lincle", "SP", "N", 5, 602, 0, "—", "NO PLAY", false),
  chart("s8-sph", "恋する☆宇宙戦争っ!!", "Prim", "Lincle", "SP", "H", 9, 1002, 0, "—", "NO PLAY", false),
  chart("s9-spa", "PARANOiA ～HADES～", " α-Type-300", "SIRIUS", "SP", "A", 12, 1518, 3230, "A", "CLEAR", false),
  chart("s10-spa", "Sol Cosine Job 2", "M-Project", "tricoro", "SP", "A", 12, 1655, 0, "—", "NO PLAY", false),
  chart("s11-spa", "The Limbo", "schwarzweiß", "Lincle", "SP", "A", 12, 1698, 3564, "AA", "HARD", true),
  chart("s12-dpn", "V", "TAKA", "5th style", "DP", "N", 6, 711, 1620, "A", "CLEAR", true),
  chart("s12-dpa", "V", "TAKA", "5th style", "DP", "A", 11, 1810, 0, "—", "NO PLAY", false),
  chart("s13-dph", "冥", "Amuro vs Killer", "HAPPY SKY", "DP", "H", 10, 1535, 0, "—", "NO PLAY", false),
  chart("s14-dpa", "天空の夜明け", "Cuvelia", "Lincle", "DP", "A", 12, 2150, 0, "—", "NO PLAY", false),
  chart("s15-spa", "音楽", "弁士カンタビレオ", "beatmania IIDX 16", "SP", "A", 12, 1820, 0, "—", "NO PLAY", false),
  chart("s16-spa", "DXY!", "TaQ", "4th style", "SP", "A", 11, 1310, 2698, "A", "CLEAR", false),
  chart("s17-spa", "gigadelic", "teranoid feat. MC Natsack", "HAPPY SKY", "SP", "A", 12, 1683, 0, "—", "NO PLAY", false),
  chart("s18-spa", "Almagest", "Galdeia", "SIRIUS", "SP", "A", 12, 1645, 0, "—", "NO PLAY", false),
  chart("s19-spa", "Bad Maniacs", "kors k as teranoid", "SIRIUS", "SP", "A", 12, 1664, 0, "—", "NO PLAY", false),
  chart("s20-spa", "Feel The Beat", "Falsion", "PENDUAL", "SP", "A", 11, 1478, 2884, "AA", "EASY", false),
];

export function cloneDemoCharts() {
  return DEMO_CHARTS.map((item) => ({ ...item }));
}

