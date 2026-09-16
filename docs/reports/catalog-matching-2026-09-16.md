# Textage / BJM 曲名匹配核对（2026-09-16）

## 范围与结果

- Textage：17,267 张谱面，按应用真实曲目分组规则得到 **2,661 个条目**；同名不同版本/来源可能是不同条目。
- BJM：公开 LDJ 33 代音乐库 **2,017 条**，版本标记 `2026080900`。没有读取账号、Cookie 或用户成绩。
- 修复前未匹配 **591** 条，完整 HTML 实体解码后未匹配 **564** 条；恢复 **27** 个 Textage 条目。原先匹配成功的条目 **0 丢失、0 目标 ID 变更**。
- 剩余：**当前收录 8、删除曲 472、家用版 83、未知 1**。混合 UNKNOWN 的条目按已知删除/家用状态归类，原始状态保留在表中。
- 当前收录条目的 BJM 候选是人工核对线索，**尚未写入应用匹配规则**。其余条目完整列出，不把删除/家用状态当作“必然未收录”的证据。
- 这里核对的是曲目到 BJM music ID 的关联，不是成绩 NOTE/EX/DJ RATE 的兼容性，也不保证所有难度都有雷达。
- 采用同一份公开快照，分别运行修复前后的应用解析与 `buildBjmIndex`。Textage 脚本按兼容 Shift_JIS 的 Windows-31J 转为 UTF-8，再供离线审计读取。

## 当前收录但仍未匹配（优先核对）

| 编号 | Textage 曲名 | BJM 候选 | 差异 / 待确认事项 |
| --- | --- | --- | --- |
| U001 | [CODE:Ø](https://textage.cc/score/26/code_0.html) | CODE:0（ID 26016） | Textage 为 Ø（U+00D8），BJM 为数字 0（U+0030）。 |
| U002 | [FiZZλ_PØT!OИ](https://textage.cc/score/33/fizzyptn.html) | FiZZλ_PØT!0И（ID 33018） | 感叹号后的字符，Textage 为字母 O，BJM 为数字 0。 |
| U003 | [POLꓘAMAИIA](https://textage.cc/score/28/plkmania.html) | POLꞰAMAИIA（ID 28050） | Textage 为 ꓘ（U+A4D8，傈僳字母），BJM 为 Ʞ（U+A7B0，倒置拉丁 K）。 |
| U004 | [uәn](https://textage.cc/score/32/_uen.html) | uən（ID 32006） | Textage 的 ә 是 U+04D9（西里尔字母），BJM 的 ə 是 U+0259（拉丁字母）。 |
| U005 | [Χ-DEN](https://textage.cc/score/26/_kai_den.html) | X-DEN（ID 26007） | Textage 为希腊 Χ（U+03A7），BJM 为拉丁 X（U+0058）。 |
| U006 | [∀](https://textage.cc/score/28/_turn_a.html) | ∀（ID 28005） | 两库均为 ∀；当前匹配规则仅保留字母、数字，导致 Textage 匹配键为空。BJM 的普通标题为 TURN A。 |
| U007 | [≡＋≡](https://textage.cc/score/30/_3plus3.html) | ≡+≡（ID 30029） | NFKC 后两库标题相同，但均为符号，被当前字母/数字过滤规则清空。BJM 普通标题为 3+3。 |
| U008 | [夢色ワンダー](https://textage.cc/score/33/_yumeiro.html) | — | 本次 BJM 快照未查到同名或 hololive IDOL PROJECT 曲师条目，是否尚未收录需确认。 |

## 本次实体解码恢复的 27 个条目

`原文本`保留转义形式，包含同一首曲目的不同版本条目。

| Textage 原文本 | 解码后 | 来源版本 | BJM 目标 |
| --- | --- | --- | --- |
| `&hearts;LOVE&sup2; シュガ→&hearts;` | [♥LOVE² シュガ→♥](https://textage.cc/score/0/_love2sg.html) | Consumer only | LOVE2 シュガ→（1333） |
| `Lagrangian Point &Oslash;` | [Lagrangian Point Ø](https://textage.cc/score/0/lagrang0.html) | Consumer only | Lagrangian Point Ø（1414） |
| `L'amour et la libert&eacute;` | [L'amour et la liberté](https://textage.cc/score/6/lamour.html) | 6th style | L'amour et la liberté（6014） |
| `L'amour et la libert&eacute;` | [L'amour et la liberté](https://textage.cc/score/6/lamour31.html) | 6th style | L'amour et la liberté（6014） |
| `LOVE&hearts;SHINE` | [LOVE♥SHINE](https://textage.cc/score/9/lvshine.html) | 9th style | LOVE♡SHINE（9042） |
| `Sweet Sweet &hearts; Magic` | [Sweet Sweet ♥ Magic](https://textage.cc/score/9/swmagic.html) | 9th style | Sweet Sweet♡Magic（9045） |
| `Raspberry&hearts;Heart (English version)` | [Raspberry♥Heart (English version)](https://textage.cc/score/11/raspbery.html) | IIDX RED | Raspberry♡Heart(English version)（11051） |
| `Double &hearts;&hearts; Loving Heart` | [Double ♥♥ Loving Heart](https://textage.cc/score/13/dbloving.html) | DistorteD | Double♡♡Loving Heart（13047） |
| `&Uuml;bertreffen` | [Übertreffen](https://textage.cc/score/15/ubertref.html) | DJ TROOPERS | Übertreffen（15061） |
| `Punch Love &hearts; 仮面` | [Punch Love ♥ 仮面](https://textage.cc/score/16/punch_lv.html) | EMPRESS | Punch Love♡仮面（16034） |
| `Pr&auml;ludium` | [Präludium](https://textage.cc/score/20/praludim.html) | tricoro | Präludium（20027） |
| `キャトられ&hearts;恋はモ～モク` | [キャトられ♥恋はモ～モク](https://textage.cc/score/20/_kyatora.html) | tricoro | キャトられ♥恋はモ～モク（20007） |
| `超!!遠距離らぶ&hearts;メ～ル` | [超!!遠距離らぶ♥メ～ル](https://textage.cc/score/21/_cholove.html) | SPADA | 超!!遠距離らぶ♡メ～ル（21030） |
| `旋律のドグマ ～Mis&eacute;rables～` | [旋律のドグマ ～Misérables～](https://textage.cc/score/21/_s_dogma.html) | SPADA | 旋律のドグマ～Misérables～（21002） |
| `&AElig;THER` | [ÆTHER](https://textage.cc/score/21/_aether.html) | SPADA | ÆTHER（21060） |
| `表裏一体!?怪盗いいんちょの悩み&hearts;` | [表裏一体!?怪盗いいんちょの悩み♥](https://textage.cc/score/22/_hyouri.html) | PENDUAL | 表裏一体！？怪盗いいんちょの悩み♥（22009） |
| `&iexcl;Viva!` | [¡Viva!](https://textage.cc/score/24/_viva_.html) | SINOBUZ | ¡Viva!（24008） |
| `Amor De Ver&atilde;o` | [Amor De Verão](https://textage.cc/score/25/amrverao.html) | CANNON BALLERS | Amor De Verão（25033） |
| `Xl&oslash;` | [Xlø](https://textage.cc/score/26/xlo.html) | Rootage | Xlø（26070） |
| `M&auml;ch&ouml; M&ouml;nky` | [Mächö Mönky](https://textage.cc/score/26/machomnk.html) | Rootage | Mächö Mönky（26029） |
| `Geirsk&ouml;gul` | [Geirskögul](https://textage.cc/score/26/geirskog.html) | Rootage | Geirskögul（26101） |
| `Dans la nuit de l'&eacute;ternit&eacute;` | [Dans la nuit de l'éternité](https://textage.cc/score/27/dans_la.html) | HEROIC VERSE | Dans la nuit de l'éternité（27004） |
| `Ignis†Ir&aelig;` | [Ignis†Iræ](https://textage.cc/score/28/ignisira.html) | BISTROVER | Ignis†Iræ（28084） |
| `V&Oslash;ID` | [VØID](https://textage.cc/score/30/vo_id.html) | RESIDENT | VØID（30007） |
| `ACT&Oslash;` | [ACTØ](https://textage.cc/score/31/act0.html) | EPOLIS | ACTØ（31075） |
| `Fl&auml;mingo` | [Flämingo](https://textage.cc/score/32/flamingo.html) | Pinky Crush | Flämingo（32034） |
| `Space Battleship S4T&Oslash;` | [Space Battleship S4TØ](https://textage.cc/score/33/spc_s4to.html) | Sparkle Shower | Space Battleship S4TØ（33022） |

## 旧曲库中的近似标题线索（13 个条目）

下列结果来自文字相似度筛选（阈值 0.82），**不是同曲判定**。其中可能是续作、混音或完全不同曲目，不能直接绑定。其余未匹配条目仍见下方完整清单。

| 编号 | Textage | 版本 | 仅供核对的 BJM 近似标题 |
| --- | --- | --- | --- |
| U010 | 2 Player | CANNON BALLERS | 《PL\|RAYER》（31086） |
| U056 | celebrate | 1st style | ACCELERATE（25013） |
| U057 | celebrate | 1st style | ACCELERATE（25013） |
| U144 | Headache | 2nd style | HEARTACHE（29005） |
| U147 | Hitch Hiker | 2nd style | Hitch Hiker2（4023） |
| U205 | LOVE GENERATION | 7th style | Re:GENERATION（20015） |
| U292 | REAL LOVE | 6th style | Really Love（9039） |
| U317 | Scripted Connection⇒ long mix | HAPPY SKY | Scripted Connection⇒ N/H/A mix（12052 / 12053 / 12054） |
| U318 | Scripted Connection⇒ | HAPPY SKY | Scripted Connection⇒ N/H/A mix（12052 / 12053 / 12054） |
| U333 | Shine On | 2nd style | SHION（20102） |
| U395 | TOMORROW | BISTROVER | DUE TOMORROW（13031） |
| U495 | celebrate | 1st style | ACCELERATE（25013） |
| U523 | IceCube Pf. (RX-Ver.S.P.L.) | Consumer only | WaterCube Pf.(RX-Ver.S.P.L.)（31014） / ToyCube Pf.(RX-Ver.S.P.L.)（26097） |

## 全量未匹配：当前收录（8）

| 编号 | Textage 曲名 | 版本 | 曲师 | 原始状态 | Textage key |
| --- | --- | --- | --- | --- | --- |
| U001 | [CODE:Ø](https://textage.cc/score/26/code_0.html) | Rootage | BEMANI Sound Team "HEXA" | CURRENT | code_0 |
| U002 | [FiZZλ_PØT!OИ](https://textage.cc/score/33/fizzyptn.html) | Sparkle Shower | めめめ | CURRENT | fizzyptn |
| U003 | [POLꓘAMAИIA](https://textage.cc/score/28/plkmania.html) | BISTROVER | かめりあ feat. ななひら | CURRENT | plkmania |
| U004 | [uәn](https://textage.cc/score/32/_uen.html) | Pinky Crush | 少年ラジオ | CURRENT | _uen |
| U005 | [Χ-DEN](https://textage.cc/score/26/_kai_den.html) | Rootage | T.Σ.Generation | CURRENT | _kai_den |
| U006 | [∀](https://textage.cc/score/28/_turn_a.html) | BISTROVER | BEMANI Sound Team "HuΣeR" respect for D.J.Amuro | CURRENT | _turn_a |
| U007 | [≡＋≡](https://textage.cc/score/30/_3plus3.html) | RESIDENT | Ryu☆ | CURRENT | _3plus3 |
| U008 | [夢色ワンダー](https://textage.cc/score/33/_yumeiro.html) | Sparkle Shower | hololive IDOL PROJECT | CURRENT | _yumeiro;yumeiro |

## 全量未匹配：删除曲（472）

| 编号 | Textage 曲名 | 版本 | 曲师 | 原始状态 | Textage key |
| --- | --- | --- | --- | --- | --- |
| U009 | [1989](https://textage.cc/score/5/1989.html) | 5th style | Osamu Kubota | DELETED | 1989 |
| U010 | [2 Player](https://textage.cc/score/25/2player.html) | CANNON BALLERS | maras k/marasy×kors k | DELETED | 2player |
| U011 | [20,November](https://textage.cc/score/1/20nov1st.html) | 1st style | dj nagureo | DELETED | 20nov1st |
| U012 | [20,November](https://textage.cc/score/6/20novem.html) | 6th style | dj nagureo | DELETED;UNKNOWN | 20novem;20novem5 |
| U013 | [2002](https://textage.cc/score/7/2002.html) | 7th style | tiger YAMATO | DELETED | 2002 |
| U014 | [3!dolon Forc3](https://textage.cc/score/28/3idolonf.html) | BISTROVER | 青龍×Eagle | DELETED | 3idolonf |
| U015 | [5.8.8.](https://textage.cc/score/10/588.html) | 10th style | dj nagureo | DELETED | 588 |
| U016 | [9 o'clocks](https://textage.cc/score/7/9oclocks.html) | 7th style | SYMPHONIC DEFOGGERS | DELETED | 9oclocks |
| U017 | [9-1](https://textage.cc/score/33/9_1.html) | Sparkle Shower | めめめ | DELETED | 9-1;9_1 |
| U018 | [A New Morning](https://textage.cc/score/6/amorning.html) | 6th style | Yukihiro Fukutomi | DELETED | amorning |
| U019 | [A Tale Hidden In The Abyss](https://textage.cc/score/26/atalehdn.html) | Rootage | BEMANI Sound Team "person09" | DELETED | atalehdn |
| U020 | [A-JAX (3-WAY MIX)](https://textage.cc/score/10/a_jax.html) | 10th style | Mr.T | DELETED | a_jax |
| U021 | [Acidiva 303](https://textage.cc/score/25/acdva303.html) | CANNON BALLERS | ni-21 | DELETED | acdva303 |
| U022 | [Affection](https://textage.cc/score/24/afection.html) | SINOBUZ | ni-21 | DELETED | afection |
| U023 | [AGAiN](https://textage.cc/score/26/again.html) | Rootage | DJ NAGAI | DELETED | again |
| U024 | [ALIEN WORLD](https://textage.cc/score/8/alienwld.html) | 8th style | PINK PONG | DELETED | alienwld |
| U025 | [All is Wrecked](https://textage.cc/score/21/allwreck.html) | SPADA | Destron | DELETED | allwreck |
| U026 | [Almost Love](https://textage.cc/score/3/almostlv.html) | 3rd style | SILVA | DELETED | almostlv |
| U027 | [Amnolys](https://textage.cc/score/22/amnolys.html) | PENDUAL | onoken | DELETED | amnolys |
| U028 | [Angels &amp; Demons](https://textage.cc/score/26/angeldmn.html) | Rootage | 月代 彩 | DELETED | angeldmn |
| U029 | [anthracene](https://textage.cc/score/30/anthracn.html) | RESIDENT | DJ Command (Eurobeat Union) | DELETED | anthracn |
| U030 | [Artificial Impatience](https://textage.cc/score/3/artific.html) | 3rd style | SYMPHONIC DEFOGGERS | DELETED | artific |
| U031 | [Atröpøs](https://textage.cc/score/21/atropos.html) | SPADA | sasakure.UK | DELETED | atropos |
| U032 | [ay carumba!!!!](https://textage.cc/score/13/ay_crmba.html) | DistorteD | teranoid&amp;MC Natsack | DELETED | ay_crmba |
| U033 | [Back to the Dance Floor](https://textage.cc/score/11/backflr.html) | IIDX RED | DJ SIMON | DELETED | backflr |
| U034 | [Be in my paradise](https://textage.cc/score/1/beinpara.html) | 1st style | JJ COMPANY | DELETED | beinpara |
| U035 | [Be Rock U (1998 burst style)](https://textage.cc/score/9/berock_u.html) | 9th style | NAOKI | DELETED | berock_u |
| U036 | [Beast mode](https://textage.cc/score/25/beastmod.html) | CANNON BALLERS | BEMANI Sound Team "TAG" | DELETED | beastmod |
| U037 | [Beat Machine](https://textage.cc/score/3/btmachn.html) | 3rd style | CRUNKY BOY Jr. | DELETED | btmachn |
| U038 | [Beautiful Days](https://textage.cc/score/6/btydays.html) | 6th style | Fantastic Plastic Machine | DELETED | btydays |
| U039 | [been so long](https://textage.cc/score/1/bnsolong.html) | 1st style | m-flo | DELETED | bnsolong |
| U040 | [Beginning of life](https://textage.cc/score/1/begin1st.html) | 1st style | QUADRA | DELETED | begin1st |
| U041 | [believe...?](https://textage.cc/score/11/believe_.html) | IIDX RED | Nao Nakamura | DELETED | believe_ |
| U042 | [Blown My Heart Away](https://textage.cc/score/8/blownmy.html) | 8th style | good-cool feat. Jeff Coote | DELETED | blownmy |
| U043 | [bluemoon](https://textage.cc/score/13/bluemoon.html) | DistorteD | 小野秀幸 | DELETED | bluemoon |
| U044 | [Brahma](https://textage.cc/score/28/brahma.html) | BISTROVER | m1dy | DELETED | brahma |
| U045 | [Brazilian Fire](https://textage.cc/score/17/brazfire.html) | SIRIUS | Ben Franklin | DELETED | brazfire |
| U046 | [Brazilian Rhyme](https://textage.cc/score/2/brazil.html) | 2nd style | Satoru Shionoya + Satoshi Tomiie | DELETED | brazil |
| U047 | [BREAK OUT](https://textage.cc/score/13/breakout.html) | DistorteD | Remixed by Orange Lounge | DELETED | breakout |
| U048 | [Breakin' Rules](https://textage.cc/score/28/brkrules.html) | BISTROVER | BEMANI Sound Team "Sota Fujimori" | DELETED | brkrules |
| U049 | [Breaking Dawn feat. NO+CHIN, AYANO](https://textage.cc/score/20/brk_dawn.html) | tricoro | GUHROOVY | DELETED | brk_dawn |
| U050 | [BREEDING](https://textage.cc/score/9/breeding.html) | 9th style | SLAKE | DELETED | breeding |
| U051 | [BRILLIANT 2U](https://textage.cc/score/s/brill2u.html) | substream | NAOKI | DELETED | brill2u |
| U052 | [BRING HER DOWN](https://textage.cc/score/7/bringdjt.html) | 7th style | AKIRA YAMAOKA | DELETED | bringdjt |
| U053 | [BURNING UP FOR YOU](https://textage.cc/score/7/burnup.html) | 7th style | SARA | DELETED | burnup |
| U054 | [Cansei de S NIK](https://textage.cc/score/18/cansei.html) | Resort Anthem | PRASTIK DANCEFLOOR | DELETED | cansei |
| U055 | [CAR OF YOUR DREAMS](https://textage.cc/score/11/cardream.html) | IIDX RED | DAVE&amp;NUAGE | DELETED | cardream |
| U056 | [celebrate](https://textage.cc/score/1/celeb4.html) | 1st style | JJ COMPANY | DELETED | celeb4 |
| U057 | [celebrate](https://textage.cc/score/1/celebsub.html) | 1st style | JJ COMPANY | DELETED | celebsub |
| U058 | [Changes](https://textage.cc/score/10/change_q.html) | 10th style | TaQ | DELETED | change_q |
| U059 | [CHARLOTTE](https://textage.cc/score/9/charlot.html) | 9th style | Db.saka feat. Piasa | DELETED | charlot |
| U060 | [Chocolate Dancing](https://textage.cc/score/17/chocodan.html) | SIRIUS | Yoche feat. Mayu | DELETED | chocodan |
| U061 | [CHOO CHOO TRAIN (dnb mix)](https://textage.cc/score/10/choochoo.html) | 10th style | SLAKE feat. MIKA | DELETED | choochoo |
| U062 | [CLOUDY MUSIC](https://textage.cc/score/7/cloudy.html) | 7th style | SLAKE | DELETED | cloudy |
| U063 | [Cold Pulse](https://textage.cc/score/3/coldpls.html) | 3rd style | Quadra with DJ FX | DELETED | coldpls |
| U064 | [Come On](https://textage.cc/score/14/comeon.html) | GOLD | John Robinson | DELETED | comeon |
| U065 | [Come With Me](https://textage.cc/score/5/comewith.html) | 5th style | good-cool feat.Aundrea L.Hopkins | DELETED | comewith |
| U066 | [Comment te dire adieu](https://textage.cc/score/6/comment.html) | 6th style | Orange Lounge | DELETED | comment |
| U067 | [CONGA](https://textage.cc/score/4/conga.html) | 4th style | good-cool feat.JP Miles | DELETED | conga |
| U068 | [CONNECT-](https://textage.cc/score/28/connect_.html) | BISTROVER | BEMANI Sound Team "Sota Fujimori" | DELETED | connect_ |
| U069 | [COSMIC RAISE](https://textage.cc/score/10/cosmic.html) | 10th style | Toshiji Katoh | DELETED | cosmic |
| U070 | [Crazy K.I.N.O.](https://textage.cc/score/13/crz_kino.html) | DistorteD | Shoichiro Hirata | DELETED | crz_kino |
| U071 | [DANCER](https://textage.cc/score/8/dancer.html) | 8th style | DE VOL | DELETED | dancer |
| U072 | [Dancin' Into The Night](https://textage.cc/score/2/dancin.html) | 2nd style | good-cool | DELETED | dancin |
| U073 | [Dark Fall](https://textage.cc/score/21/darkfall.html) | SPADA | 黒龍 | DELETED | darkfall |
| U074 | [Deadline](https://textage.cc/score/5/deadline.html) | 5th style | Dutch Force | DELETED | deadline |
| U075 | [Deceive Your Insight](https://textage.cc/score/20/deceive.html) | tricoro | Project B- | DELETED | deceive |
| U076 | [Deep Clear Eyes](https://textage.cc/score/1/deepeyes.html) | 1st style | QUADRA | DELETED | deepeyes |
| U077 | [DENJIN AKATSUKINI TAORERU -SF PureAnalogSynth Mix-](https://textage.cc/score/17/denjinsf.html) | SIRIUS | Remixed by Sota Fujimori | DELETED | denjinsf |
| U078 | [DIAMOND JACKAL](https://textage.cc/score/30/diajackl.html) | RESIDENT | Akira Complex | DELETED | diajackl |
| U079 | [DIAMOND JEALOUSY](https://textage.cc/score/4/diamond.html) | 4th style | AKIRA YAMAOKA | DELETED | diamond |
| U080 | [Digital Skipper](https://textage.cc/score/26/dskipper.html) | Rootage | BEMANI Sound Team "Expander" | DELETED | dskipper |
| U081 | [DISAPPEAR feat. koyomin](https://textage.cc/score/24/disapear.html) | SINOBUZ | DJ Genki | DELETED | disapear |
| U082 | [diving money](https://textage.cc/score/1/dvmny_gd.html) | 1st style | QUADRA | DELETED | dvmny_gd |
| U083 | [Do you love me?](https://textage.cc/score/1/doyulvme.html) | 1st style | reo-nagumo | DELETED | doyulvme |
| U084 | [Doigts de Fatima -ファティマの掌-](https://textage.cc/score/6/dfatima.html) | 6th style | Osamu Kubota featuring Sizzle Ohtaka | DELETED | dfatima |
| U085 | [Don't forget](https://textage.cc/score/10/dontfgt.html) | 10th style | 中山 結衣 | DELETED | dontfgt |
| U086 | [Don't Stop The Music feat.森高千里](https://textage.cc/score/22/dstmusic.html) | PENDUAL | tofubeats | DELETED | dstmusic |
| U087 | [DON'T WAKE ME FROM THE DREAM (2010 Summer Edition)](https://textage.cc/score/19/dontwake.html) | Lincle | YOJI | DELETED | dontwake |
| U088 | [dong-tepo no.1](https://textage.cc/score/2/dongtepo.html) | 2nd style | dj nagureo | DELETED | dongtepo |
| U089 | [DREAM](https://textage.cc/score/3/dream.html) | 3rd style | SHIN Murayama feat. mari saito | DELETED | dream |
| U090 | [Dreamin Train](https://textage.cc/score/23/drmtrain.html) | copula | DJ NAGAI feat. Ayumi Nomiya | DELETED | drmtrain |
| U091 | [Dreamin' Sun](https://textage.cc/score/9/dreamin.html) | 9th style | Yu Takami | DELETED | dreamin |
| U092 | [Dreaming Sweetness](https://textage.cc/score/14/drmsweet.html) | GOLD | Auridy | DELETED | drmsweet |
| U093 | [Drivin'](https://textage.cc/score/8/drivin.html) | 8th style | NAOKI feat. Paula Terry | DELETED | drivin |
| U094 | [Drop It](https://textage.cc/score/27/dropit.html) | HEROIC VERSE | Dazsta | DELETED | dropit |
| U095 | [Drop on the floor](https://textage.cc/score/10/dropflr.html) | 10th style | good-cool | DELETED | dropflr |
| U096 | [Duration](https://textage.cc/score/27/duration.html) | HEROIC VERSE | 佐野電磁 | DELETED | duration |
| U097 | [DYNAMITE RAVE](https://textage.cc/score/3/dynamite.html) | 3rd style | NAOKI | DELETED | dynamite |
| U098 | [e-motion](https://textage.cc/score/1/emo1st.html) | 1st style | e.o.s. | DELETED | emo1st |
| U099 | [E.V CAFE](https://textage.cc/score/7/evcafe.html) | 7th style | reo nagumo feat.hajime.y | DELETED | evcafe |
| U100 | [earth-like planet](https://textage.cc/score/14/eth_like.html) | GOLD | ELEKTEL | DELETED | eth_like |
| U101 | [Echo Of Forever](https://textage.cc/score/20/echo_of.html) | tricoro | kors k | DELETED | echo_of |
| U102 | [ECHOES](https://textage.cc/score/9/echoes.html) | 9th style | DRAGOON | DELETED | echoes |
| U103 | [ELECTRIC MASSIVE DIVER](https://textage.cc/score/18/emassive.html) | Resort Anthem | L.E.D.-G | DELETED | emassive |
| U104 | [Electric Super Highway](https://textage.cc/score/19/ehighway.html) | Lincle | MACHO ROBOT feat. nouvo nude | DELETED | ehighway |
| U105 | [Empathetic](https://textage.cc/score/20/empatic.html) | tricoro | Sota÷Des | DELETED | empatic |
| U106 | [END OF THE CENTURY](https://textage.cc/score/3/endofcen.html) | 3rd style | NO.9 | DELETED | endofcen |
| U107 | [Enjoy your life](https://textage.cc/score/13/enjoy_yl.html) | DistorteD | Mr.T plus H | DELETED | enjoy_yl |
| U108 | [entrance](https://textage.cc/score/7/entrance.html) | 7th style | Kobo project with Masa | DELETED | entrance |
| U109 | [esrev:eR](https://textage.cc/score/21/esrever.html) | SPADA | TAG meets "eimy" | DELETED | esrever |
| U110 | [Estella](https://textage.cc/score/5/estella.html) | 5th style | Osamu Kubota | DELETED | estella |
| U111 | [EVO66](https://textage.cc/score/11/evo66.html) | IIDX RED | WALL5 | DELETED | evo66 |
| U112 | [EXCITE](https://textage.cc/score/27/excite.html) | HEROIC VERSE | Remixed by BEMANI Sound Team "HuΣeR feat.PON" | DELETED | excite |
| U113 | [EXTREMA PT. 2](https://textage.cc/score/18/extrema2.html) | Resort Anthem | Remo-con | DELETED | extrema2 |
| U114 | [EZ DO DANCE](https://textage.cc/score/30/ezdodanc.html) | RESIDENT | Remixed by kors k | DELETED | ezdodanc |
| U115 | [FANTASY](https://textage.cc/score/4/fantasy.html) | 4th style | ULTIMATE DUB | DELETED | fantasy |
| U116 | [FESTA DO SOL](https://textage.cc/score/9/festa.html) | 9th style | Mt.Circle | DELETED | festa |
| U117 | [Final Count Down (MTO CRY BABY STYLE)](https://textage.cc/score/4/fcd.html) | 4th style | dj TAKA feat. Jasmine | DELETED | fcd |
| U118 | [FIRE BALL](https://textage.cc/score/25/fireball.html) | CANNON BALLERS | BEMANI Sound Team "RACER-X" | DELETED | fireball |
| U119 | [five fathoms (beatmaniaII special version)](https://textage.cc/score/3/fathoms.html) | 3rd style | EverythingButTheGirl | DELETED | fathoms |
| U120 | [Five Regrets](https://textage.cc/score/6/fivereg.html) | 6th style | Osamu Kubota | DELETED | fivereg |
| U121 | [Flash of love](https://textage.cc/score/5/flashof.html) | 5th style | good-cool feat.Mickin'Tackin | DELETED | flashof |
| U122 | [Flowtation (Original Mix)](https://textage.cc/score/6/flowtat.html) | 6th style | Vincent de Moor | DELETED | flowtat |
| U123 | [FLUTE MAN](https://textage.cc/score/8/fluteman.html) | 8th style | SPARKER | DELETED | fluteman |
| U124 | [Foundation of our love](https://textage.cc/score/8/found.html) | 8th style | dj TAKA feat.ASAKO | DELETED | found |
| U125 | [Fractal](https://textage.cc/score/20/fractal.html) | tricoro | Sota Fujimori | DELETED | fractal |
| U126 | [FUNKTION](https://textage.cc/score/12/funktion.html) | HAPPY SKY | ULTRAdoof | DELETED | funktion |
| U127 | [FUNKY BINGO PARADISE](https://textage.cc/score/8/fkbingo.html) | 8th style | SAWASAKI YOSHIHIRO | DELETED | fkbingo |
| U128 | [Garden of Love](https://textage.cc/score/3/garden.html) | 3rd style | DJ Mazinger | DELETED | garden |
| U129 | [Get me in your sight](https://textage.cc/score/3/getsight.html) | 3rd style | SYMPHONIC DEFOGGERS | DELETED | getsight |
| U130 | [Get on Beat](https://textage.cc/score/2/getonbt.html) | 2nd style | ON | DELETED | getonbt |
| U131 | [GET ON BEAT (WILD STYLE)](https://textage.cc/score/4/getwild.html) | 4th style | Lion Musashi | DELETED | getwild |
| U132 | [Get Out](https://textage.cc/score/18/getout.html) | Resort Anthem | 壱岐尾彩花 | DELETED | getout |
| U133 | [GET READY!!](https://textage.cc/score/23/getready.html) | copula | DJ Shimamura | DELETED | getready |
| U134 | [GHOSTBUSTERS](https://textage.cc/score/14/gbusters.html) | GOLD | Remixed by DJ Yoshitaka | DELETED | gbusters |
| U135 | [GIRIGIRI DADDY](https://textage.cc/score/4/girigiri.html) | 4th style | Shoichiro Hirata | DELETED | girigiri |
| U136 | [Give Me A Sign](https://textage.cc/score/5/givesign.html) | 5th style | Shoichiro Hirata feat."Red" | DELETED | givesign |
| U137 | [Give Me Your Love](https://textage.cc/score/21/giveyrlv.html) | SPADA | 星野奏子 | DELETED | giveyrlv |
| U138 | [Glitch Nerds](https://textage.cc/score/23/glinerds.html) | copula | かめりあ | DELETED | glinerds |
| U139 | [Gravity](https://textage.cc/score/7/gravity.html) | 7th style | TaQ | DELETED | gravity |
| U140 | [HALF MOON](https://textage.cc/score/14/halfmoon.html) | GOLD | Mutsuhiko Izumi | DELETED | halfmoon |
| U141 | [Hands Up feat. kradness BEMANI Sound Team "Sota Fujimori" Remix](https://textage.cc/score/28/handsupr.html) | BISTROVER | BEMANI Sound Team "Sota Fujimori" | DELETED | handsupr |
| U142 | [Hardcore Mania](https://textage.cc/score/18/hc_mania.html) | Resort Anthem | DJ Weaver | DELETED | hc_mania |
| U143 | [HARMONY](https://textage.cc/score/11/harmony.html) | IIDX RED | REGINA | DELETED | harmony |
| U144 | [Headache](https://textage.cc/score/2/headache.html) | 2nd style | good-cool | DELETED | headache |
| U145 | [HEARTBEAT](https://textage.cc/score/7/heartbt.html) | 7th style | NATHALIE | DELETED | heartbt |
| U146 | [HI SCHOOL DREAM](https://textage.cc/score/10/hischdrm.html) | 10th style | PINK PONG | DELETED | hischdrm |
| U147 | [Hitch Hiker](https://textage.cc/score/2/hitch1.html) | 2nd style | good-cool | DELETED | hitch1 |
| U148 | [Holy Snow](https://textage.cc/score/20/holysnow.html) | tricoro | Mutsuhiko Izumi | DELETED | holysnow |
| U149 | [Honey](https://textage.cc/score/9/honey.html) | 9th style | good-cool | DELETED | honey |
| U150 | [HONEY♂PUNCH](https://textage.cc/score/14/hnypunch.html) | GOLD | 小阪りゆ | DELETED | hnypunch |
| U151 | [Horizons of Promise](https://textage.cc/score/27/hpromise.html) | HEROIC VERSE | BEMANI Sound Team "SPIRITUAL RIDE" | DELETED | hpromise |
| U152 | [Hormiga obrera](https://textage.cc/score/8/hormiga.html) | 8th style | Shawn The Horny Master | DELETED | hormiga |
| U153 | [HOT LIMIT](https://textage.cc/score/26/hotlimit.html) | Rootage | Remixed by BEMANI Sound Team "HuΣeR feat.PON" | DELETED | hotlimit |
| U154 | [HOUSE NATION](https://textage.cc/score/16/house_na.html) | EMPRESS | ravex | DELETED | house_na |
| U155 | [I can fly, I've got reason](https://textage.cc/score/9/icanfly.html) | 9th style | Yu Takami | DELETED | icanfly |
| U156 | [i feel...](https://textage.cc/score/7/i_feel.html) | 7th style | AKIRA YAMAOKA | DELETED | i-feel;i_feel |
| U157 | [I'm Coming (2017 Version)](https://textage.cc/score/25/imcoming.html) | CANNON BALLERS | Pa's Lam System | DELETED | imcoming |
| U158 | [I'M FOR REAL](https://textage.cc/score/4/im4real.html) | 4th style | SLAKE feat.JP Miles | DELETED | im4real |
| U159 | [I'm In Love Again -DJ YOSHITAKA REMIX-](https://textage.cc/score/15/im4ctaka.html) | DJ TROOPERS | Remixed by DJ YOSHITAKA | DELETED | im4ctaka |
| U160 | [I'm In Love Again -Y&amp;Co. EURO MIX-](https://textage.cc/score/9/imin_r.html) | 9th style | dj TAKA Remixed by Y&amp;Co. | DELETED | imin-r;imin_r |
| U161 | [I'm In Love Again](https://textage.cc/score/2/iminlove.html) | 2nd style | dj TAKA | DELETED | iminlove |
| U162 | [I.C.F.5800](https://textage.cc/score/8/icf5800.html) | 8th style | Reo Nagumo feat. Mayumi Shizawa | DELETED | icf5800 |
| U163 | [iFUTURELIST](https://textage.cc/score/13/ifuture.html) | DistorteD | AKIRA YAMAOKA | DELETED | ifuture |
| U164 | [IIDX](https://textage.cc/score/5/iidx.html) | 5th style | DJ SIMON | DELETED | iidx |
| U165 | [ILAYZA](https://textage.cc/score/29/ilayza.html) | CastHour | BEMANI Sound Team "TAG" | DELETED | ilayza |
| U166 | [Immortal](https://textage.cc/score/21/immortal.html) | SPADA | DJ NAGAI | DELETED | immortal |
| U167 | [INFINITE PRAYER](https://textage.cc/score/4/infinite.html) | 4th style | L.E.D. feat. GORO | DELETED | infinite |
| U168 | [INJECTION OF LOVE](https://textage.cc/score/11/inject.html) | IIDX RED | 新谷あきら | DELETED | inject |
| U169 | [INSERTiON](https://textage.cc/score/5/insert.html) | 5th style | NAOKI underground | DELETED | insert |
| U170 | [Into The Sunlight](https://textage.cc/score/19/into_sun.html) | Lincle | kobo feat. RIO | DELETED | into_sun |
| U171 | [into the world](https://textage.cc/score/1/intworld.html) | 1st style | QUADRA | DELETED | intworld |
| U172 | [IS THIS LOVE?](https://textage.cc/score/3/isthis.html) | 3rd style | Asuka.M | DELETED | isthis |
| U173 | [JAM](https://textage.cc/score/10/jam.html) | 10th style | TAKA with Junpei &amp; 三上 | DELETED | jam |
| U174 | [jam jam reggae](https://textage.cc/score/1/jamregae.html) | 1st style | Jam Master'73 | DELETED | jamregae |
| U175 | [Jetcoaster Windy](https://textage.cc/score/27/jetwindy.html) | HEROIC VERSE | BEMANI Sound Team "dj TAKA" feat.のの | DELETED | jetwindy |
| U176 | [JEWELLERY STORM](https://textage.cc/score/16/jewelstm.html) | EMPRESS | L.E.D.-G fw.Eriko Tanzawa | DELETED | jewelstm |
| U177 | [JIVE INTO THE NIGHT](https://textage.cc/score/4/jive.html) | 4th style | CYDNEY | DELETED | jive |
| U178 | [JOURNEY TO "FANTASICA" (IIDX LIMITED)](https://textage.cc/score/15/journey2.html) | DJ TROOPERS | S.S.D.FANTASICA feat. EMI with ARATA | DELETED | journey2 |
| U179 | [Junglist King](https://textage.cc/score/2/junglist.html) | 2nd style | Hirofumi Asamoto (ram jam world) | DELETED | junglist |
| U180 | [Junglist King (LONG)](https://textage.cc/score/2/junglong.html) | 2nd style | Hirofumi Asamoto (ram jam world) | DELETED | junglong |
| U181 | [JUST DO IT](https://textage.cc/score/6/justdoit.html) | 6th style | NIKI | DELETED | justdoit |
| U182 | [Kecak](https://textage.cc/score/11/kecak.html) | IIDX RED | John Robinson | DELETED | kecak |
| U183 | [KING OF GROOVE](https://textage.cc/score/11/kingrove.html) | IIDX RED | DJ 19 | DELETED | kingrove |
| U184 | [Kiss me all night long](https://textage.cc/score/5/kissme.html) | 5th style | NAOKI J-STYLE feat.MIU | DELETED | kissme |
| U185 | [KI・SE・KI (IIDX RED EDIT)](https://textage.cc/score/11/kiseki.html) | IIDX RED | BeForU | DELETED | kiseki |
| U186 | [L.O.T. (Love Or Truth)](https://textage.cc/score/3/lot.html) | 3rd style | m-flo | DELETED | lot |
| U187 | [LA FESTA LA VITA!!](https://textage.cc/score/21/lafesta.html) | SPADA | GUHROOVY | DELETED | lafesta |
| U188 | [La Mar](https://textage.cc/score/18/la_mar.html) | Resort Anthem | seiya-murai feat.David Solanes Venzala | DELETED | la_mar |
| U189 | [Legendary Treasures](https://textage.cc/score/29/legendtr.html) | CastHour | BEMANI Sound Team "劇団レコード" | DELETED | legendtr |
| U190 | [Les filles balancent](https://textage.cc/score/11/lesfill.html) | IIDX RED | Orange Lounge | DELETED | lesfill |
| U191 | [LET THE BEAT HIT EM! BASS MIX](https://textage.cc/score/2/letbtem.html) | 2nd style | STONE BROS. | DELETED | letbtem |
| U192 | [Let's Bounce !!](https://textage.cc/score/25/ltbounce.html) | CANNON BALLERS | BEMANI Sound Team "Sota F." | DELETED | ltbounce |
| U193 | [Let's run](https://textage.cc/score/10/letsrun.html) | 10th style | Mitsuto Suzuki | DELETED | letsrun |
| U194 | [Let's say Hello!](https://textage.cc/score/9/lethello.html) | 9th style | Takuma Saiki | DELETED | lethello |
| U195 | [Let's talk it over](https://textage.cc/score/3/letstalk.html) | 3rd style | SHIN Murayama feat. Argie Phine | DELETED | letstalk |
| U196 | [LIFE SCROLLING](https://textage.cc/score/18/life_scr.html) | Resort Anthem | HIROSHI WATANABE | DELETED | life_scr |
| U197 | [Light My Fire](https://textage.cc/score/22/litefire.html) | PENDUAL | GUHROOVY feat. NO+CHIN | DELETED | litefire |
| U198 | [LIGHTS ft. EVO+](https://textage.cc/score/25/lits_evo.html) | CANNON BALLERS | lapix | DELETED | lits_evo |
| U199 | [LIKE A VAMPIRE](https://textage.cc/score/28/likevamp.html) | BISTROVER | koyomi,星野奏子 by BEMANI Sound Team "TAKA" | DELETED | likevamp |
| U200 | [LIMITED](https://textage.cc/score/10/limited.html) | 10th style | SLAKE | DELETED | limited |
| U201 | [listen to yourself](https://textage.cc/score/11/listento.html) | IIDX RED | Tetsuya Uchida | DELETED | listento |
| U202 | [Listen up](https://textage.cc/score/12/listenup.html) | HAPPY SKY | Mitsuto Suzuki | DELETED | listenup |
| U203 | [London Affairs Beckoned With Money Loved By Yellow Papers.](https://textage.cc/score/17/london.html) | SIRIUS | Paddington Private Detective | DELETED | london |
| U204 | [Look To The Sky (cyber True Color)](https://textage.cc/score/13/looksky.html) | DistorteD | Sota Fujimori | DELETED | looksky |
| U205 | [LOVE GENERATION](https://textage.cc/score/7/lovegene.html) | 7th style | SUZY LAZY | DELETED | lovegene |
| U206 | [LOVE IS DREAMINESS](https://textage.cc/score/6/lvdream.html) | 6th style | L.E.D.-G VS GUHROOVY fw/asuka | DELETED | lvdream |
| U207 | [LOVE IS DROWING](https://textage.cc/score/9/lovedrow.html) | 9th style | SLAKE feat.EMIKO | DELETED | lovedrow |
| U208 | [LOVE SO GROOVY](https://textage.cc/score/1/lvgroovy.html) | 1st style | LOVEMINTS | DELETED | lvgroovy |
| U209 | [Love♥km](https://textage.cc/score/18/love_km.html) | Resort Anthem | dj TAKA feat. REN | DELETED | love_km |
| U210 | [lovin' you](https://textage.cc/score/2/lovinyou.html) | 2nd style | MONDAY MICHIRU | DELETED | lovinyou |
| U211 | [LOW](https://textage.cc/score/10/low.html) | 10th style | RAM | DELETED | low |
| U212 | [Ludus In Tenebris](https://textage.cc/score/22/ludus_in.html) | PENDUAL | Akhuta | DELETED | ludus_in |
| U213 | [Luna Plena](https://textage.cc/score/30/lunaplna.html) | RESIDENT | アオワイファイ | DELETED | lunaplna |
| U214 | [LUV TO ME (UCCHIE'S EDITION)](https://textage.cc/score/4/luvuchi.html) | 4th style | tiger YAMATO | DELETED | luvuchi |
| U215 | [M-02stp.ver.1.01](https://textage.cc/score/6/m02stp.html) | 6th style | Shoichiro Hirata | DELETED | m02stp |
| U216 | [m1dy Dynamic](https://textage.cc/score/23/m1dydyna.html) | copula | m1dy | DELETED | m1dydyna |
| U217 | [m1dy Festival](https://textage.cc/score/22/m1dy_fes.html) | PENDUAL | m1dy | DELETED | m1dy_fes |
| U218 | [Make Your Move](https://textage.cc/score/3/makemove.html) | 3rd style | good-cool feat. JP Miles | DELETED | makemove |
| U219 | [Marmalade Reverie](https://textage.cc/score/7/marm_rev.html) | 7th style | Orange Lounge | DELETED | marm_rev |
| U220 | [Mars beach](https://textage.cc/score/28/mrsbeach.html) | BISTROVER | さよひめぼう | DELETED | mrsbeach |
| U221 | [mathematical good-bye](https://textage.cc/score/30/mathe_gb.html) | RESIDENT | 三代目 ADULTIC TEACHERS feat. BEMANI Sound Team "スコーピオン志村" | DELETED | mathe_gb |
| U222 | [Melody Life](https://textage.cc/score/13/melolife.html) | DistorteD | Noria | DELETED | melolife |
| U223 | [Melt in my arms](https://textage.cc/score/1/meltarms.html) | 1st style | Honey P feat. Asuka.M | DELETED | meltarms |
| U224 | [Mira](https://textage.cc/score/25/mira.html) | CANNON BALLERS | Akhuta | DELETED | mira |
| U225 | [Mirrorball Satellite 2012](https://textage.cc/score/2/mirrball.html) | 2nd style | m-flo | DELETED | mirrball |
| U226 | [MIRU key way](https://textage.cc/score/17/miru_key.html) | SIRIUS | Jacca PoP | DELETED | miru_key |
| U227 | [Mobo★Moga](https://textage.cc/score/5/mobomoga.html) | 5th style | Orange Lounge | DELETED | mobomoga |
| U228 | [more deep (ver2.1)](https://textage.cc/score/7/moredeep.html) | 7th style | Togo project feat.Sana | DELETED | moredeep |
| U229 | [More Move](https://textage.cc/score/11/moremove.html) | IIDX RED | T-Hirono feat.PAULINHO | DELETED | moremove |
| U230 | [morning prayer](https://textage.cc/score/2/m_prayer.html) | 2nd style | SILVA | DELETED | m_prayer |
| U231 | [morning prayer](https://textage.cc/score/2/m_prayr3.html) | 2nd style | SILVA | DELETED | m_prayr3 |
| U232 | [Move Me](https://textage.cc/score/11/moveme.html) | IIDX RED | good-cool feat. Raj Ramayya | DELETED | moveme |
| U233 | [Move UR Body](https://textage.cc/score/30/moveurbd.html) | RESIDENT | nora2r | DELETED | moveurbd |
| U234 | [MY FUTURE](https://textage.cc/score/16/myfuture.html) | EMPRESS | PINK PONG | DELETED | myfuture |
| U235 | [My Only Shining Star](https://textage.cc/score/14/myonly.html) | GOLD | NAOKI feat. Becky Lucinda | DELETED | myonly |
| U236 | [Mysterious Time](https://textage.cc/score/17/mys_time.html) | SIRIUS | Y&amp;Co. | DELETED | mys_time |
| U237 | [NaHaNaHa vs. Gattchoon Battle](https://textage.cc/score/s/nahanaha.html) | substream | DJ Senda &amp; Tiny-K | DELETED | nahanaha |
| U238 | [Nasty!](https://textage.cc/score/4/nasty.html) | 4th style | Baby Weapon feat. Asuka.M | DELETED | nasty |
| U239 | [never let you down](https://textage.cc/score/3/nvrdown.html) | 3rd style | good-cool feat. JP Miles | DELETED | nvrdown |
| U240 | [Never Look Back](https://textage.cc/score/7/nvrlook.html) | 7th style | DuMonde | DELETED | nvrlook |
| U241 | [New York](https://textage.cc/score/7/newyork.html) | 7th style | Fuzita Blender | DELETED | newyork |
| U242 | [NEW YORK CITY BOY](https://textage.cc/score/4/nycity.html) | 4th style | GUY THOMAS | DELETED | nycity |
| U243 | [NIGHT FLIGHT TO TOKYO](https://textage.cc/score/11/nitokyo.html) | IIDX RED | MATT LAND | DELETED | nitokyo |
| U244 | [NIGHT OF FIRE](https://textage.cc/score/6/nightof.html) | 6th style | NIKO | DELETED | nightof |
| U245 | [NO DOUBT GET LOUD](https://textage.cc/score/10/nodoubt.html) | 10th style | ASLETICS | DELETED | nodoubt |
| U246 | [NORTH](https://textage.cc/score/4/north.html) | 4th style | WALL5 | DELETED | north |
| U247 | [Nothing Ain't Stoppin' Us](https://textage.cc/score/4/nothing.html) | 4th style | Shoichiro Hirata | DELETED | nothing |
| U248 | [Now and Forever](https://textage.cc/score/15/now4ever.html) | DJ TROOPERS | StripE vs MUNETICA feat.ARISA | DELETED | now4ever |
| U249 | [NRG STAR '86 feat. 大山愛未](https://textage.cc/score/27/nrgstr86.html) | HEROIC VERSE | Y&amp;Co. | DELETED | nrgstr86 |
| U250 | [Ohayo!](https://textage.cc/score/27/ohayo.html) | HEROIC VERSE | KSUKE | DELETED | ohayo |
| U251 | [On My Wings (Hardstyle IIDX)](https://textage.cc/score/22/onmywing.html) | PENDUAL | kors k | DELETED | onmywing |
| U252 | [ON THE TUBE](https://textage.cc/score/11/on_tube.html) | IIDX RED | Q'HEY | DELETED | on_tube |
| U253 | [One of A Kind](https://textage.cc/score/17/one_kind.html) | SIRIUS | Crystal Begley | DELETED | one_kind |
| U254 | [Our Song](https://textage.cc/score/16/our_song.html) | EMPRESS | Shinich Osawa | DELETED | our_song |
| U255 | [OUTER LIMITS](https://textage.cc/score/8/outlmt16.html) | 8th style | L.E.D.-G | DELETED | outlmt16 |
| U256 | [OUTER LIMITS ALTERNATIVE](https://textage.cc/score/23/outeralt.html) | copula | L.E.D.-G | DELETED | outeralt |
| U257 | [OVER THE CLOUDS](https://textage.cc/score/5/ovcloud.html) | 5th style | Lala Moore | DELETED | ovcloud |
| U258 | [OVER THE CLOUDS -Flying Grind mix-](https://textage.cc/score/9/ovcloudr.html) | 9th style | Lala Moore Remixed by Flying Grind | DELETED | ovcloudr |
| U259 | [OVERDOSER](https://textage.cc/score/1/ovdoser.html) | 1st style | MIRAK | DELETED | ovdoser |
| U260 | [Panorama](https://textage.cc/score/2/panorama.html) | 2nd style | Hirofumi Asamoto(jam jam world) | DELETED | panorama |
| U261 | [PARAPARA PARADISE](https://textage.cc/score/6/ppprdise.html) | 6th style | DOMINO | DELETED | ppprdise |
| U262 | [Parasite World](https://textage.cc/score/13/parasite.html) | DistorteD | TЁЯRA underground | DELETED | parasite |
| U263 | [Particle Arts](https://textage.cc/score/26/ptclarts.html) | Rootage | Virtual Self | DELETED | ptclarts |
| U264 | [Party Starter](https://textage.cc/score/28/ptystart.html) | BISTROVER | ARM (IOSYS) × cotowari ft. anporin | DELETED | ptystart |
| U265 | [PATRIOTISM](https://textage.cc/score/12/patriot.html) | HAPPY SKY | kobo | DELETED | patriot |
| U266 | [patsenner](https://textage.cc/score/1/patsen4.html) | 1st style | dj nagureo | DELETED | patsen4 |
| U267 | [Peng-Ging Sky High!!](https://textage.cc/score/30/pengging.html) | RESIDENT | moimoi + BEMANI Sound Team "S-C-U" | DELETED | pengging |
| U268 | [PentaCube Gt. (RX-Ver.S.P.L.)](https://textage.cc/score/19/penta_cb.html) | Lincle | 高田雅史 | DELETED | penta_cb |
| U269 | [perfect free](https://textage.cc/score/1/perfect1.html) | 1st style | nite system | DELETED | perfect1 |
| U270 | [PERFECTWORLD](https://textage.cc/score/11/perworld.html) | IIDX RED | N.A.R.D feat masayo | DELETED | perworld |
| U271 | [PLATONIC-XXX](https://textage.cc/score/10/platxxx.html) | 10th style | platoniX(RB style) | DELETED | platxxx |
| U272 | [Play back hate you](https://textage.cc/score/14/playback.html) | GOLD | AKIRA YAMAOKA | DELETED | playback |
| U273 | [Playball](https://textage.cc/score/28/playball.html) | BISTROVER | sanodg | DELETED | playball |
| U274 | [Please Welcome Mr.C](https://textage.cc/score/26/pls_mr_c.html) | Rootage | C-Show | DELETED | pls_mr_c |
| U275 | [POP TEAM EPIC (kors k Remix)](https://textage.cc/score/27/popteamk.html) | HEROIC VERSE | 上坂すみれ | DELETED | popteamk |
| U276 | [portal](https://textage.cc/score/20/portal.html) | tricoro | 猫叉Master+ | DELETED | portal |
| U277 | [POWER DREAM](https://textage.cc/score/9/powerdrm.html) | 9th style | PINK PONG | DELETED | powerdrm |
| U278 | [Prelude](https://textage.cc/score/9/prelude.html) | 9th style | NAOKI underground | DELETED | prelude |
| U279 | [Pretty Punisher](https://textage.cc/score/13/pre_puni.html) | DistorteD | baby weapon feat. asuka.m | DELETED | pre_puni |
| U280 | [Prince on a star](https://textage.cc/score/1/prince.html) | 1st style | SPIRITUAL RIDE | DELETED | prince |
| U281 | [PSYCHE PLANET-GT](https://textage.cc/score/16/psychegt.html) | EMPRESS | Remixed by L.E.D. | DELETED | psychegt |
| U282 | [Punching Down (IIDX Mix)](https://textage.cc/score/27/punchdwn.html) | HEROIC VERSE | Masayoshi Iimori | DELETED | punchdwn |
| U283 | [Queen's Jamaica (astria mix)](https://textage.cc/score/1/qjamaica.html) | 1st style | Crunky Boy featuring Muhammad | DELETED | qjamaica |
| U284 | [R2](https://textage.cc/score/10/r2.html) | 10th style | tiger YAMATO | DELETED | r2 |
| U285 | [radius](https://textage.cc/score/15/radius.html) | DJ TROOPERS | yasuhiro abe | DELETED | radius |
| U286 | [Raise your hands](https://textage.cc/score/18/raisehnd.html) | Resort Anthem | Keiichi Ueno feat. RIISA | DELETED | raisehnd |
| U287 | [Rave lithosphere](https://textage.cc/score/27/ravelith.html) | HEROIC VERSE | Sampling Masters MEGA | DELETED | ravelith |
| U288 | [Rave Saves You feat. Cardz (Exclusive IIDX Mix)](https://textage.cc/score/22/ravesave.html) | PENDUAL | REMO-CON | DELETED | ravesave |
| U289 | [Re:Story](https://textage.cc/score/26/re_story.html) | Rootage | ももいろクローバーZ | DELETED | re_story |
| U290 | [Ready To Rockit Blues](https://textage.cc/score/10/rockit.html) | 10th style | SLAKE | DELETED | rockit |
| U291 | [Ready To Rockit Blues](https://textage.cc/score/10/rockit_t.html) | 10th style | SLAKE | DELETED | rockit_t |
| U292 | [REAL LOVE](https://textage.cc/score/6/reallove.html) | 6th style | MAZIK K. | DELETED | reallove |
| U293 | [Red Nikita](https://textage.cc/score/8/rnikita.html) | 8th style | Osamu Kubota | DELETED | rnikita |
| U294 | [Red Rocket Rising](https://textage.cc/score/14/rrrising.html) | GOLD | BeForU | DELETED | rrrising |
| U295 | [REMEMBER ME](https://textage.cc/score/7/remem_m.html) | 7th style | LESLIE PARRISH | DELETED | remem_m |
| U296 | [Remember You](https://textage.cc/score/5/remem_u.html) | 5th style | NAOKI feat.Julie | DELETED | remem-u;remem_u |
| U297 | [Rest my mind](https://textage.cc/score/4/restmind.html) | 4th style | dj nagureo feat. Doublecheese | DELETED | restmind |
| U298 | [Ride To The Core](https://textage.cc/score/23/ridecore.html) | copula | GUHROOVY feat NO+CHIN &amp; Yuki | DELETED | ridecore |
| U299 | [Right Now](https://textage.cc/score/3/rightnow.html) | 3rd style | atomic kitten | DELETED | rightnow |
| U300 | [Rise Circuit](https://textage.cc/score/25/rcircuit.html) | CANNON BALLERS | COSIO | DELETED | rcircuit |
| U301 | [RISING FIRE HAWK](https://textage.cc/score/24/risehawk.html) | SINOBUZ | L.E.D.-G | DELETED | risehawk |
| U302 | [ROCK ME NOW](https://textage.cc/score/15/rokmenow.html) | DJ TROOPERS | GUHROOVY fw. NO+CHIN | DELETED | rokmenow |
| U303 | [ROK DA WORLD](https://textage.cc/score/11/rokworld.html) | IIDX RED | DJ TOMO | DELETED | rokworld |
| U304 | [ROMEO&amp;JULIET](https://textage.cc/score/6/romeo.html) | 6th style | LOLITA | DELETED | romeo |
| U305 | [ROZA DE ANDALUCIA](https://textage.cc/score/26/rozaanda.html) | Rootage | SOUND HOLIC Vs. T.Kakuta feat. Nana Takahashi | DELETED | rozaanda |
| U306 | [Rub-a Dub-a](https://textage.cc/score/28/rubaduba.html) | BISTROVER | kors k | DELETED | rubaduba |
| U307 | [R壱萬](https://textage.cc/score/4/r10k.html) | 4th style | tiger YAMATO | DELETED | r10k |
| U308 | [S.O.S.](https://textage.cc/score/2/sos2nd.html) | 2nd style | DR.BOMBAY | DELETED | sos2nd |
| U309 | [S.O.S. (THE TIGER TOOK MY FAMILY)](https://textage.cc/score/3/sos3rd.html) | 3rd style | DR.JEKYLL | DELETED | sos3rd |
| U310 | [Salamander Beat Crush mix](https://textage.cc/score/1/salaman.html) | 1st style | NITE SYSTEM | DELETED | salaman |
| U311 | [SAMBA DE JANEIRO](https://textage.cc/score/13/samba_de.html) | DistorteD | Remixed by Lion MUSASHI | DELETED | samba_de |
| U312 | [SANA MOLLETTE NE ENTE](https://textage.cc/score/4/sanamol.html) | 4th style | Togo Project feat. Sana | DELETED | sanamol |
| U313 | [sanctus](https://textage.cc/score/4/sanctus.html) | 4th style | Osamu Kubota | DELETED | sanctus |
| U314 | [Scandal](https://textage.cc/score/28/scandal.html) | BISTROVER | Osamu Kubota | DELETED | scandal |
| U315 | [scherzo](https://textage.cc/score/10/scherzo.html) | 10th style | Osamu Kubota | DELETED | scherzo |
| U316 | [SCREAM THE LIFE FEAT.KYONO](https://textage.cc/score/24/scrmlife.html) | SINOBUZ | DJ BAKU | DELETED | scrmlife |
| U317 | [Scripted Connection⇒ long mix](https://textage.cc/score/12/script_l.html) | HAPPY SKY | DJ MURASAME | DELETED | script_l |
| U318 | [Scripted Connection⇒](https://textage.cc/score/12/scripted.html) | HAPPY SKY | DJ MURASAME | DELETED | scripted |
| U319 | [Second Style (Hip Hop Paradise)](https://textage.cc/score/2/second.html) | 2nd style | dj TAKA &amp; DAY BREAKERS | DELETED | second |
| U320 | [Secret Tale](https://textage.cc/score/7/secret.html) | 7th style | dj nagureo feat.asuka.m | DELETED | secret |
| U321 | [SEITEN NO TERIYAKI](https://textage.cc/score/23/sei_teri.html) | copula | Kobaryo | DELETED | sei_teri |
| U322 | [Seize the Day](https://textage.cc/score/24/seizeday.html) | SINOBUZ | DJ'TEKINA//SOMETHING feat.yuyoyuppe | DELETED | seizeday |
| U323 | [Sellout](https://textage.cc/score/27/sellout.html) | HEROIC VERSE | banvox | DELETED | sellout |
| U324 | [Sense](https://textage.cc/score/3/sense.html) | 3rd style | good-cool | DELETED | sense |
| U325 | [Serious?](https://textage.cc/score/26/serious.html) | Rootage | KSUKE | DELETED | serious |
| U326 | [Session 12 -Esther-](https://textage.cc/score/19/sesion12.html) | Lincle | PRASTIK DANCEFLOOR | DELETED | sesion12 |
| U327 | [SEXYSEXYCHEVY](https://textage.cc/score/12/sexysexy.html) | HAPPY SKY | MATALLY | DELETED | sexysexy |
| U328 | [Shades of Grey](https://textage.cc/score/15/shadesof.html) | DJ TROOPERS | Fracus | DELETED | shadesof |
| U329 | [Shake](https://textage.cc/score/2/shake.html) | 2nd style | double | DELETED | shake |
| U330 | [Shakin'31](https://textage.cc/score/10/shakin31.html) | 10th style | DJ Remo-con | DELETED | shakin31 |
| U331 | [Shattered control](https://textage.cc/score/21/shatterd.html) | SPADA | Vivian | DELETED | shatterd |
| U332 | [SHIFT](https://textage.cc/score/15/shift.html) | DJ TROOPERS | AKIRA YAMAOKA | DELETED | shift |
| U333 | [Shine On](https://textage.cc/score/2/shineon.html) | 2nd style | Shorai | DELETED | shineon |
| U334 | [Shoot'Em All](https://textage.cc/score/24/shoot_em.html) | SINOBUZ | Manabu Namiki | DELETED | shoot_em |
| U335 | [SHOX](https://textage.cc/score/0/shox5.html) | Consumer only | RAM | DELETED;UNKNOWN | shox;shox5 |
| U336 | [Sidechained Threats](https://textage.cc/score/26/sidchain.html) | Rootage | sanodg | DELETED | sidchain |
| U337 | [SIGMA](https://textage.cc/score/26/sigma.html) | Rootage | BEMANI Sound Team "Mutsuhiko Izumi" | DELETED | sigma |
| U338 | [Silhoette of My Mind](https://textage.cc/score/6/silhoet.html) | 6th style | ReR | DELETED | silhoet |
| U339 | [Six String Proof](https://textage.cc/score/26/sixproof.html) | Rootage | BEMANI Sound Team "Yvya × Mutsuhiko Izumi" | DELETED | sixproof |
| U340 | [ska a go go](https://textage.cc/score/1/skaagogo.html) | 1st style | THE BALD HEADS | DELETED | skaagogo |
| U341 | [SKIN](https://textage.cc/score/3/skin.html) | 3rd style | CHARLOTTE | DELETED | skin |
| U342 | [Skreaming for Salvation](https://textage.cc/score/29/skrmsalv.html) | CastHour | RoughSkreamZ | DELETED | skrmsalv |
| U343 | [Small clone](https://textage.cc/score/8/smlclone.html) | 8th style | Yu Takami | DELETED | smlclone |
| U344 | [Small Waves](https://textage.cc/score/4/smallwav.html) | 4th style | dj TAKA | DELETED | smallwav |
| U345 | [Smell Like This](https://textage.cc/score/10/smell.html) | 10th style | Y&amp;Co. | DELETED | smell |
| U346 | [Smoke](https://textage.cc/score/8/smoke.html) | 8th style | Aya | DELETED | smoke |
| U347 | [Smug Face -どうだ、オレの生き様は- (ONLY ONE EDITION)](https://textage.cc/score/21/smugface.html) | SPADA | SUPER STAR 満-MITSURU- | DELETED | smugface |
| U348 | [So Punky](https://textage.cc/score/28/sopunky.html) | BISTROVER | Y&amp;Co. feat. 大山愛未 | DELETED | sopunky |
| U349 | [So Real](https://textage.cc/score/14/soreal.html) | GOLD | Y&amp;Co. feat.mioco | DELETED | soreal |
| U350 | [SOFT LANDING ON THE BODY](https://textage.cc/score/2/soflan.html) | 2nd style | DJ SIMON | DELETED | soflan |
| U351 | [soldier's waltz](https://textage.cc/score/15/solwlz27.html) | DJ TROOPERS | DAJI | DELETED | solwlz27 |
| U352 | [SOLID GOLD](https://textage.cc/score/7/solid_g.html) | 7th style | DUSTY | DELETED | solid-g;solid_g |
| U353 | [Sometimes feat. Kanae Asaba](https://textage.cc/score/22/somtimes.html) | PENDUAL | Y&amp;Co. | DELETED | somtimes |
| U354 | [Son, not gabba now](https://textage.cc/score/25/songabba.html) | CANNON BALLERS | Y&amp;Co. | DELETED | songabba |
| U355 | [SPARK !](https://textage.cc/score/12/spark.html) | HAPPY SKY | SILVER FOX PRODUCTIONS feat.星野奏子 | DELETED | spark |
| U356 | [SPARK ! -essential RMX-](https://textage.cc/score/17/spark_r.html) | SIRIUS | Remixed by dj TAKA VS PINK PONG | DELETED | spark_r |
| U357 | [Special energy](https://textage.cc/score/1/spen_dd.html) | 1st style | DJ FX | DELETED | spen_dd |
| U358 | [SPEED](https://textage.cc/score/25/speed.html) | CANNON BALLERS | BEMANI Sound Team "Mutsuhiko Izumi" | DELETED | speed |
| U359 | [SPEED TRANCE MACH 3](https://textage.cc/score/8/stmach3.html) | 8th style | SAWASAKI YOSHIHIRO | DELETED | stmach3 |
| U360 | [Spinning Around](https://textage.cc/score/20/spinning.html) | tricoro | Dirty Androids | DELETED | spinning |
| U361 | [Spooky](https://textage.cc/score/7/spooky.html) | 7th style | good-cool | DELETED | spooky |
| U362 | [SPRING RAIN (LLUVIA DE PRIMAVERA)](https://textage.cc/score/13/spr_rain.html) | DistorteD | Remixed by DJ YOSHITAKA | DELETED | spr_rain |
| U363 | [STAR DREAM](https://textage.cc/score/8/stardrm.html) | 8th style | PINK PONG | DELETED | stardrm |
| U364 | [Starry Night](https://textage.cc/score/26/starrynt.html) | Rootage | Massive New Krew feat.松平なな | DELETED | starrynt |
| U365 | [STARS☆☆☆ (Re-tuned by HΛL)-IIDX EDITION-](https://textage.cc/score/14/stars_rt.html) | GOLD | TЁЯRA | DELETED | stars_rt |
| U366 | [Stella Sinistra](https://textage.cc/score/21/sinistra.html) | SPADA | Akhuta Philharmonic Orchestra | DELETED | sinistra |
| U367 | [Stick Around](https://textage.cc/score/8/stick.html) | 8th style | Megu with Scotty D. | DELETED | stick |
| U368 | [subtractive](https://textage.cc/score/22/subtract.html) | PENDUAL | Expander | DELETED | subtract |
| U369 | [Summer](https://textage.cc/score/24/summerbx.html) | SINOBUZ | banvox | DELETED | summerbx |
| U370 | [SUNLiGHT (IIDX Mix)](https://textage.cc/score/25/sunlight.html) | CANNON BALLERS | 日龍 | DELETED | sunlight |
| U371 | [Sunrise](https://textage.cc/score/17/sunrise.html) | SIRIUS | good-cool ft. KOЯO | DELETED | sunrise |
| U372 | [Survival Games](https://textage.cc/score/18/surgames.html) | Resort Anthem | VENUS | DELETED | surgames |
| U373 | [Sweet Clapper](https://textage.cc/score/24/sclapper.html) | SINOBUZ | livetune+ | DELETED | sclapper |
| U374 | [tablets](https://textage.cc/score/5/tablets.html) | 5th style | sampling masters MEGA | DELETED | tablets |
| U375 | [TAKE ON ME](https://textage.cc/score/3/takeonme.html) | 3rd style | ANGELA | DELETED | takeonme |
| U376 | [tant pis pour toi](https://textage.cc/score/12/tant_pis.html) | HAPPY SKY | AKIRA YAMAOKA | DELETED | tant_pis |
| U377 | [Tell Me More…](https://textage.cc/score/11/tellme.html) | IIDX RED | Togo-chef feat.朝比奈亜希 | DELETED | tellme |
| U378 | [TEXTURE](https://textage.cc/score/11/texture.html) | IIDX RED | SLAKE | DELETED | texture |
| U379 | [The Beauty Of Silence](https://textage.cc/score/7/tsilence.html) | 7th style | Svenson &amp; Gielen | DELETED | tsilence |
| U380 | [THE BIG VOYAGER -INFINITE PRAYER REINTERPRETATION-](https://textage.cc/score/5/tvoyager.html) | 5th style | L.E.D. | DELETED | tvoyager |
| U381 | [The Biggest Roaster](https://textage.cc/score/9/t_roast.html) | 9th style | D.J.Spugna | DELETED | t-roast;t_roast |
| U382 | [The end of my spiritually](https://textage.cc/score/9/t_mysprt.html) | 9th style | EeL | DELETED | t-mysprt;t_mysprt |
| U383 | [The Revolution (VIP Mix)](https://textage.cc/score/25/trevovip.html) | CANNON BALLERS | Scott Brown &amp; M-Project | DELETED | trevovip |
| U384 | [The Rhyme Brokers](https://textage.cc/score/1/therhyme.html) | 1st style | m-flo | DELETED | therhyme |
| U385 | [The Sealer ～ア・ミリアとミリアの民～](https://textage.cc/score/26/t_sealer.html) | Rootage | Zektbach | DELETED | t_sealer |
| U386 | [The Sound Of Goodbye](https://textage.cc/score/7/tgoodbye.html) | 7th style | Armin van Buuren Presents: Perpetuous Dreamer | DELETED | tgoodbye |
| U387 | [The Theme from "Flo-jack"](https://textage.cc/score/1/tflojack.html) | 1st style | m-flo | DELETED | tflojack |
| U388 | [Think of me](https://textage.cc/score/10/thinkof.html) | 10th style | good-cool feat. Sana | DELETED | thinkof |
| U389 | [tiger yamato](https://textage.cc/score/13/tgyamato.html) | DistorteD | tiger YAMATO | DELETED | tgyamato |
| U390 | [Time is money](https://textage.cc/score/5/timoney.html) | 5th style | good-cool | DELETED | timoney |
| U391 | [To my star](https://textage.cc/score/20/tomystar.html) | tricoro | 星野奏子 | DELETED | tomystar |
| U392 | [To the Future](https://textage.cc/score/17/tofuture.html) | SIRIUS | seiya-murai | DELETED | tofuture |
| U393 | [To The Paradise](https://textage.cc/score/23/toprdise.html) | copula | DJ Genki feat. SHIN from HYPERNOVA | DELETED | toprdise |
| U394 | [TOE JAM](https://textage.cc/score/12/toejam.html) | HAPPY SKY | BIG IDEA | DELETED | toejam |
| U395 | [TOMORROW](https://textage.cc/score/28/tmrwknsb.html) | BISTROVER | Remixed by まろん feat. ricono | DELETED | tmrwknsb |
| U396 | [Tonight?](https://textage.cc/score/13/tonight.html) | DistorteD | dj REMO-CON | DELETED | tonight |
| U397 | [Tp-RZ](https://textage.cc/score/20/tp_rz.html) | tricoro | CAPACITY GATE | DELETED | tp_rz |
| U398 | [Transport](https://textage.cc/score/23/transprt.html) | copula | Sota Fujimori | DELETED | transprt |
| U399 | [TRIBAL MASTER](https://textage.cc/score/8/tribal.html) | 8th style | NAPAKICK | DELETED | tribal |
| U400 | [tripping contact](https://textage.cc/score/13/tripping.html) | DistorteD | kors k vs teranoid | DELETED | tripping |
| U401 | [Trust -MATERIAL ver- (IIDX Edition)](https://textage.cc/score/19/trust2dx.html) | Lincle | Tatsh feat. ヨーコ | DELETED | trust2dx |
| U402 | [Twin Bee (Generation X)](https://textage.cc/score/4/twinbee.html) | 4th style | FinalOffset | DELETED | twinbee |
| U403 | [Two DAYS OF LOVE](https://textage.cc/score/9/twodays.html) | 9th style | tiger YAMATO with マイク吉川 feat. ma-sa | DELETED | twodays |
| U404 | [u gotta groove -extend joy style-](https://textage.cc/score/9/ugotta_r.html) | 9th style | dj nagureo Remixed by Mr.T | DELETED | ugotta_r |
| U405 | [U.S.A.](https://textage.cc/score/26/usa.html) | Rootage | Remixed by BEMANI Sound Team "PHQUASE feat.TRANDER Jr." | DELETED | usa |
| U406 | [Ubiquitous Fantastic Ride](https://textage.cc/score/13/ubiquits.html) | DistorteD | ELEKTEL | DELETED | ubiquits |
| U407 | [ULTRA HIGH-HEELS](https://textage.cc/score/4/ultrahh.html) | 4th style | dj TAKA feat.ANGEL | DELETED | ultrahh |
| U408 | [Under Construction](https://textage.cc/score/4/undercon.html) | 4th style | good-cool | DELETED | undercon |
| U409 | [V35](https://textage.cc/score/8/v35.html) | 8th style | tiger YAMATO | DELETED | v35 |
| U410 | [Vienna](https://textage.cc/score/4/vienna.html) | 4th style | Osamu Kubota | DELETED | vienna |
| U411 | [VIRTUAL MIND](https://textage.cc/score/5/virtual.html) | 5th style | Brian Morris feat.Thomas | DELETED | virtual |
| U412 | [Votania Beat](https://textage.cc/score/29/votaniab.html) | CastHour | まろん vs. ビートまりお | DELETED | votaniab |
| U413 | [VOX RUSH](https://textage.cc/score/25/vox_rush.html) | CANNON BALLERS | 青龍×sampling masters MEGA &amp; sampling masters AYA | DELETED | vox_rush |
| U414 | [VR - Virtual Reality (prod.by Snail's House)](https://textage.cc/score/29/vr_kmnz.html) | CastHour | KMNZ | DELETED | vr_kmnz |
| U415 | [WANNA TELL THAT WORD](https://textage.cc/score/9/wannatel.html) | 9th style | SADA | DELETED | wannatel |
| U416 | [WE LOVE SHONAN](https://textage.cc/score/18/weshonan.html) | Resort Anthem | Studio Bongo Mango feat. Likkle Mai | DELETED | weshonan |
| U417 | [Wheel of Journey](https://textage.cc/score/23/wheeljny.html) | copula | Vivian | DELETED | wheeljny |
| U418 | [Winning Eleven9 Theme (IIDX EDITION)](https://textage.cc/score/13/win11_9.html) | DistorteD | Sota Fujimori | DELETED | win11_9 |
| U419 | [wish](https://textage.cc/score/13/wish.html) | DistorteD | DJ Yoshitaka feat.杉村ことみ | DELETED | wish |
| U420 | [with me...](https://textage.cc/score/17/withme_.html) | SIRIUS | Sota Fujimori feat.Kemy | DELETED | withme_ |
| U421 | [WOBBLE IMPACT](https://textage.cc/score/21/wobbleip.html) | SPADA | Sota Fujimori | DELETED | wobbleip |
| U422 | [Won(*3*)Chu KissMe!](https://textage.cc/score/26/won_chu.html) | Rootage | Remixed by BEMANI Sound Team "L.E.D." &amp; IOSYS | DELETED | won_chu |
| U423 | [Wow Wow 70's](https://textage.cc/score/6/wow70.html) | 6th style | sampling masters AYA | DELETED | wow70 |
| U424 | [X-rated](https://textage.cc/score/14/x_rated.html) | GOLD | SADA | DELETED | x_rated |
| U425 | [Y31](https://textage.cc/score/6/y31.html) | 6th style | tiger YAMATO | DELETED | y31 |
| U426 | [Yabis Starlight](https://textage.cc/score/14/yabis_sl.html) | GOLD | DAJI | DELETED | yabis_sl |
| U427 | [YELLOW FROG from Steel Chronicle](https://textage.cc/score/20/ylw_frog.html) | tricoro | 劇団レコード | DELETED | ylw_frog |
| U428 | [YESTERDAY](https://textage.cc/score/6/yester.html) | 6th style | CHERRY | DELETED | yester |
| U429 | [YOU MAKE ME](https://textage.cc/score/1/yumakeme.html) | 1st style | MONDAY MICHIRU | DELETED | yumakeme |
| U430 | [You Were The One](https://textage.cc/score/18/youwere1.html) | Resort Anthem | good-cool ft. Brenda Vaughn | DELETED | youwere1 |
| U431 | [Your Body](https://textage.cc/score/9/yourbody.html) | 9th style | good-cool feat. Andrea L.Hopkins | DELETED | yourbody |
| U432 | [ËVOLUTIΦN](https://textage.cc/score/19/evlution.html) | Lincle | TЁЯRA | DELETED | evlution |
| U433 | [あいつら全員同窓会](https://textage.cc/score/29/_aitsura.html) | CastHour | ずっと真夜中でいいのに。 | DELETED | _aitsura |
| U434 | [いつかオトナになれるといいね。](https://textage.cc/score/30/_itsuoto.html) | RESIDENT | ツユ | DELETED | _itsuoto |
| U435 | [お命ちょうDAI！901娘](https://textage.cc/score/24/_oinochi.html) | SINOBUZ | DJ SHARPNEL feat. みらい | DELETED | _oinochi |
| U436 | [ぐだふわエブリデー](https://textage.cc/score/29/_gudafwa.html) | CastHour | Remixed by Xceon feat. Marcia(幽閉サテライト) | DELETED | _gudafwa |
| U437 | [ちょっときいてな (ZANSHIN-NA MIX)](https://textage.cc/score/s/_chotto.html) | substream | Laugh &amp; Peace | DELETED | _chotto |
| U438 | [どんなときも。](https://textage.cc/score/10/_donna.html) | 10th style | Mr.T と Brother Hiro | DELETED | _donna |
| U439 | [アタックNO.3](https://textage.cc/score/16/_atk_no3.html) | EMPRESS | IDEA NOTE | DELETED | _atk_no3 |
| U440 | [カミロ・ウナ・メンデス](https://textage.cc/score/14/_camiro.html) | GOLD | 山根ミチル | DELETED | _camiro |
| U441 | [ガヴリールドロップキック](https://textage.cc/score/27/_gabriel.html) | HEROIC VERSE | Remixed by かめりあ | DELETED | _gabriel |
| U442 | [キャッシュレスは愛情消すティッシュ](https://textage.cc/score/12/_tissue.html) | HAPPY SKY | GINGER | DELETED | _tissue |
| U443 | [サクラあっぱれーしょん](https://textage.cc/score/26/_sappare.html) | Rootage | Remixed by BEMANI Sound Team "TAG" feat. 柊木りお | DELETED | _sappare |
| U444 | [シティ・エンジェル](https://textage.cc/score/14/_ctangel.html) | GOLD | PINK PONG | DELETED | _ctangel |
| U445 | [デモーニッシュ](https://textage.cc/score/29/_damonis.html) | CastHour | ツユ | DELETED | _damonis |
| U446 | [デンドロビウム](https://textage.cc/score/22/_dendrob.html) | PENDUAL | TAG | DELETED | _dendrob |
| U447 | [フェイクアウト](https://textage.cc/score/22/_fakeout.html) | PENDUAL | Last Note. feat.mirin | DELETED | _fakeout |
| U448 | [フェティッシュペイパー ～脇の汗回転ガール～](https://textage.cc/score/17/_fetish.html) | SIRIUS | ガキ大将ティーム | DELETED | _fetish |
| U449 | [マインド・ゲーム](https://textage.cc/score/21/_mind_gm.html) | SPADA | 96 with メカショッチョー | DELETED | _mind_gm |
| U450 | [マツケンサンバII](https://textage.cc/score/30/_matuken.html) | RESIDENT | Remixed by Ryu☆ feat. BEMANI Sound Team "ショッチョー" &amp; イオシスコーラス隊 | DELETED | _matuken |
| U451 | [ラクエン Feat.Chiharu Chonan -JAKA respect for K.S.K. Remix](https://textage.cc/score/18/_rakuenr.html) | Resort Anthem | Remixed by JAKAZiD | DELETED | _rakuenr |
| U452 | [一途な恋 (HYPER J-EURO MIX)](https://textage.cc/score/10/_ichizu.html) | 10th style | TЁЯRA | DELETED | _ichizu |
| U453 | [令和 (1991RAVE REMIX)](https://textage.cc/score/27/_reiwa_r.html) | HEROIC VERSE | ゴールデンボンバー | DELETED | _reiwa_r |
| U454 | [俺ら東京さ行ぐだ](https://textage.cc/score/27/_oratoky.html) | HEROIC VERSE | Remixed by すわひでお &amp; uno(IOSYS) | DELETED | _oratoky |
| U455 | [傷つけど、愛してる。](https://textage.cc/score/31/_kizutkd.html) | EPOLIS | ツユ | DELETED | _kizutkd |
| U456 | [全力 SPECIAL VACATION!! ～限りある休日～](https://textage.cc/score/22/_fullpwr.html) | PENDUAL | DJ GW | DELETED | _fullpwr |
| U457 | [叶うまでは](https://textage.cc/score/15/_kanau.html) | DJ TROOPERS | ウッチーズ | DELETED | _kanau |
| U458 | [合体せよ!ストロングイェーガー!!](https://textage.cc/score/8/_tstrong.html) | 8th style | L.E.D. | DELETED | _tstrong |
| U459 | [夜のサングラス](https://textage.cc/score/8/_yorusun.html) | 8th style | School(good-cool feat. すわひでお) | DELETED | _yorusun |
| U460 | [天国のキッス ～D.J.TAKA'S STYLE～](https://textage.cc/score/5/_tenkiss.html) | 5th style | オセロ | DELETED | _tenkiss;tenkiss |
| U461 | [太陽SUNSUNボンジュールアバンチュール](https://textage.cc/score/23/_taiysun.html) | copula | Project B- | DELETED | _taiysun |
| U462 | [幻想系世界修復少女](https://textage.cc/score/21/_gensouk.html) | SPADA | Last Note. | DELETED | _gensouk |
| U463 | [御千手メディテーション](https://textage.cc/score/21/_osenju.html) | SPADA | 昇天家族 | DELETED | _osenju |
| U464 | [恋愛レボリューション21 -秋葉工房mix-](https://textage.cc/score/18/_love21.html) | Resort Anthem | DJ Command feat. うさ＆ともみん | DELETED | _love21 |
| U465 | [惑 -perplexity-](https://textage.cc/score/11/_perplex.html) | IIDX RED | Osamu Kubota | DELETED | _perplex |
| U466 | [新時代](https://textage.cc/score/30/_s_jidai.html) | RESIDENT | Remixed by Nhato feat. 花たん | DELETED | _s_jidai |
| U467 | [昭和企業戦士荒山課長](https://textage.cc/score/9/_showa.html) | 9th style | AKIRA YAMAOKA | DELETED | _showa;showa |
| U468 | [晴天Bon Voyage](https://textage.cc/score/20/_seiten.html) | tricoro | TOMOSUKE × seiya-murai feat. ALT | DELETED | _seiten |
| U469 | [死神自爆中二妹アイドルももかりん(1歳)](https://textage.cc/score/21/_momoka.html) | SPADA | 山本椛 | DELETED | _momoka |
| U470 | [湘南族 -cannibal coast-](https://textage.cc/score/15/_shonan.html) | DJ TROOPERS | Aural Vampire | DELETED | _shonan |
| U471 | [炸裂！イェーガー電光チョップ!! (JAEGER FINAL ATTACK)](https://textage.cc/score/23/_jfinala.html) | copula | L.E.D. | DELETED | _jfinala |
| U472 | [生きるよすが](https://textage.cc/score/30/_ikiruyo.html) | RESIDENT | 月詠み | DELETED | _ikiruyo |
| U473 | [紅蓮華](https://textage.cc/score/28/_gurenge.html) | BISTROVER | Covered by BEMANI Sound Team "HuΣeR" feat. Mayumi Morinaga | DELETED | _gurenge |
| U474 | [虹色の花](https://textage.cc/score/20/_njihana.html) | tricoro | Akhuta y OJ | DELETED | _njihana |
| U475 | [踊](https://textage.cc/score/29/_odo.html) | CastHour | Ado | DELETED | _odo |
| U476 | [踊る！福神漬 (CALCUTTA)](https://textage.cc/score/2/_calcuta.html) | 2nd style | DR.BOMBAY | DELETED | _calcuta |
| U477 | [野球の遊び方 そしてその歴史 ～決定版～](https://textage.cc/score/21/_howbase.html) | SPADA | あさき大監督 | DELETED | _howbase |
| U478 | [電人イェーガーのテーマ (Theme of DENJIN J)](https://textage.cc/score/6/_theme_j.html) | 6th style | L.E.D. | DELETED | _theme_j;theme-j |
| U479 | [霹靂](https://textage.cc/score/33/_hekirek.html) | Sparkle Shower | Yuta Imai | DELETED | _hekirek;hekirek |
| U480 | [鬼言集](https://textage.cc/score/14/_kgonshu.html) | GOLD | あさき | DELETED | _kgonshu |

## 全量未匹配：家用版（83）

| 编号 | Textage 曲名 | 版本 | 曲师 | 原始状态 | Textage key |
| --- | --- | --- | --- | --- | --- |
| U481 | [321 STARS](https://textage.cc/score/0/321stars.html) | Consumer only | DJ SIMON | CONSUMER_ONLY;UNKNOWN | 321star5;321stars |
| U482 | [5PM ETERNAL](https://textage.cc/score/0/5eternal.html) | Consumer only | GUHROOVY fw. NO+CHIN | CONSUMER_ONLY | 5eternal |
| U483 | [80's CAPSULE](https://textage.cc/score/0/80s_caps.html) | Consumer only | め組upper-slope | CONSUMER_ONLY | 80s_caps |
| U484 | [Attack the music](https://textage.cc/score/0/atkmusic.html) | Consumer only | DJ FX | CONSUMER_ONLY;UNKNOWN | atkmusc5;atkmusic |
| U485 | [Bad boy](https://textage.cc/score/2/badboy.html) | 2nd style | Vivi | CONSUMER_ONLY | badboy |
| U486 | [Bad boy](https://textage.cc/score/2/badboy3.html) | 2nd style | Vivi | CONSUMER_ONLY | badboy3 |
| U487 | [Beginning of life (THE GROUND PULSE MIX)](https://textage.cc/score/0/begin_cr.html) | Consumer only | QUADRA / Remixed by QUADRA | CONSUMER_ONLY;UNKNOWN | begin_cr;begincr5 |
| U488 | [Beginning of life](https://textage.cc/score/1/begining.html) | 1st style | QUADRA | CONSUMER_ONLY | begining |
| U489 | [Biometrics Warrior](https://textage.cc/score/0/biometri.html) | Consumer only | GUHROOVY fw. NO+CHIN | CONSUMER_ONLY | biometri |
| U490 | [Bleeding Luv ～Immorality act～](https://textage.cc/score/0/bleedluv.html) | Consumer only | Xephia (Tatsh+Yuta) | CONSUMER_ONLY | bleedluv |
| U491 | [breathless](https://textage.cc/score/0/breathls.html) | Consumer only | Studio Bongo Mango feat.Tomoko Kataoka | CONSUMER_ONLY | breathls |
| U492 | [BRING HER DOWN](https://textage.cc/score/7/bringher.html) | 7th style | AKIRA YAMAOKA | CONSUMER_ONLY | bringher |
| U493 | [c-r-a-c-k-ER](https://textage.cc/score/0/cracker.html) | Consumer only | GWASHI | CONSUMER_ONLY | cracker |
| U494 | [CALDERA](https://textage.cc/score/0/caldera.html) | Consumer only | CALF | CONSUMER_ONLY;UNKNOWN | caldera;caldera5 |
| U495 | [celebrate](https://textage.cc/score/1/celeb.html) | 1st style | JJ COMPANY | CONSUMER_ONLY | celeb |
| U496 | [Celebration](https://textage.cc/score/0/clbrtion.html) | Consumer only | WaveGroup/DJ TK-ST | CONSUMER_ONLY | clbrtion |
| U497 | [Changes](https://textage.cc/score/0/change_r.html) | Consumer only | RYOTA MITSUNAGA | CONSUMER_ONLY | change-r;change_r |
| U498 | [CRYMSON](https://textage.cc/score/0/crymson.html) | Consumer only | RAM | CONSUMER_ONLY;UNKNOWN | crymson;crymson5 |
| U499 | [DENGUE](https://textage.cc/score/0/dengue.html) | Consumer only | Macky | CONSUMER_ONLY | dengue |
| U500 | [dissolve](https://textage.cc/score/8/disolv16.html) | 8th style | kobo | CONSUMER_ONLY | disolv16 |
| U501 | [dissolve](https://textage.cc/score/8/dissolve.html) | 8th style | kobo | CONSUMER_ONLY | dissolve |
| U502 | [diving money](https://textage.cc/score/1/dvmoney.html) | 1st style | QUADRA | CONSUMER_ONLY | dvmoney |
| U503 | [DUNE](https://textage.cc/score/0/dune.html) | Consumer only | napakick feat. Taja | CONSUMER_ONLY | dune |
| U504 | [e-motion](https://textage.cc/score/1/emo1stus.html) | 1st style | e.o.s. | CONSUMER_ONLY | emo1stus |
| U505 | [Electrified](https://textage.cc/score/0/elecfied.html) | Consumer only | SySF. | CONSUMER_ONLY | elecfied |
| U506 | [Electrorgasm](https://textage.cc/score/0/elecgasm.html) | Consumer only | Sota Fujimori | CONSUMER_ONLY | elecgasm |
| U507 | [Endless Summer Story](https://textage.cc/score/0/endless.html) | Consumer only | DJ Yoshitaka feat.星野奏子 | CONSUMER_ONLY | endless |
| U508 | [ERaSeR EnGinE DistorteD](https://textage.cc/score/0/eraserdd.html) | Consumer only | L.E.D.-G VS GUHROOVY | CONSUMER_ONLY | eraserdd |
| U509 | [ErAseRmoToR maXimUM](https://textage.cc/score/0/eraser.html) | Consumer only | L.E.D.-G VS GUHROOVY | CONSUMER_ONLY | eraser |
| U510 | [feeling of love](https://textage.cc/score/0/feelinl5.html) | Consumer only | youhei shimizu | CONSUMER_ONLY;UNKNOWN | feelinl5;feelinlv |
| U511 | [First Day](https://textage.cc/score/0/firstday.html) | Consumer only | Timo Maas | CONSUMER_ONLY | firstday |
| U512 | [FLAG OF PEACE](https://textage.cc/score/0/flag_pce.html) | Consumer only | GUHROOVY fw. NO+CHIN | CONSUMER_ONLY | flag_pce |
| U513 | [Funkytown](https://textage.cc/score/0/funktown.html) | Consumer only | Lipps Inc. | CONSUMER_ONLY | funktown |
| U514 | [g.m.d.](https://textage.cc/score/1/gmd.html) | 1st style | DJ mazinger featuring Muhammad | CONSUMER_ONLY | gmd |
| U515 | [g.m.d.](https://textage.cc/score/1/gmd_us.html) | 1st style | DJ mazinger featuring Muhammad | CONSUMER_ONLY | gmd-us;gmd_us |
| U516 | [g.m.d.](https://textage.cc/score/1/gmd3a.html) | 1st style | DJ mazinger featuring Muhammad | CONSUMER_ONLY | gmd3a |
| U517 | [General Relativity](https://textage.cc/score/7/general.html) | 7th style | SYMPHONIC DEFOGGERS | CONSUMER_ONLY | general |
| U518 | [General Relativity](https://textage.cc/score/7/generald.html) | 7th style | SYMPHONIC DEFOGGERS | CONSUMER_ONLY | generald |
| U519 | [Go Berzerk](https://textage.cc/score/0/go_brzk.html) | Consumer only | Scott Brown | CONSUMER_ONLY | go_brzk |
| U520 | [Guilt &amp; Love](https://textage.cc/score/0/guilt_lv.html) | Consumer only | The Plastic Ambition(jun &amp; DJ YOSHITAKA) | CONSUMER_ONLY | guilt_lv |
| U521 | [Gymnopedie 009](https://textage.cc/score/0/gymno009.html) | Consumer only | Studio Bongo Mango feat.Junko Wada(BE THE VOICE) | CONSUMER_ONLY | gymno009 |
| U522 | [Hybrid Landscape](https://textage.cc/score/0/hybrid_l.html) | Consumer only | Sota Fujimori | CONSUMER_ONLY | hybrid_l |
| U523 | [IceCube Pf. (RX-Ver.S.P.L.)](https://textage.cc/score/0/icecube.html) | Consumer only | 高田雅史 | CONSUMER_ONLY | icecube |
| U524 | [INFINITE PRAYER -floating flock style-](https://textage.cc/score/0/inf_ffs.html) | Consumer only | L.E.D.feat.GORO | CONSUMER_ONLY | inf_ffs |
| U525 | [jelly kiss](https://textage.cc/score/8/jelly.html) | 8th style | Togo project feat. Sana | CONSUMER_ONLY | jelly |
| U526 | [jelly kiss -Midihead's Smack Mix-](https://textage.cc/score/0/jelly_r.html) | Consumer only | Togo project feat.Sana(Remixed by Midihead) | CONSUMER_ONLY | jelly_r |
| U527 | [jelly kiss](https://textage.cc/score/8/jellyemp.html) | 8th style | Togo project feat. Sana | CONSUMER_ONLY | jellyemp |
| U528 | [Lift Me Up](https://textage.cc/score/0/liftmeup.html) | Consumer only | Moby | CONSUMER_ONLY | liftmeup |
| U529 | [LIGHT MOTION](https://textage.cc/score/0/ltmotion.html) | Consumer only | RAM | CONSUMER_ONLY;UNKNOWN | lmotion5;ltmotion |
| U530 | [LOVE BOX](https://textage.cc/score/0/lovebox.html) | Consumer only | め組upper-slope | CONSUMER_ONLY | lovebox |
| U531 | [LUV TO ME (disco mix)](https://textage.cc/score/1/luvdis3a.html) | 1st style | tigerYAMATO | CONSUMER_ONLY | luvdis3a |
| U532 | [LUV TO ME (disco mix)](https://textage.cc/score/1/luvdisco.html) | 1st style | tigerYAMATO | CONSUMER_ONLY | luvdisco |
| U533 | [Melt in my arms](https://textage.cc/score/1/melt_cs.html) | 1st style | Honey P feat. Asuka.M | CONSUMER_ONLY | melt_cs |
| U534 | [OUTER LIMITS](https://textage.cc/score/8/outlmtdd.html) | 8th style | L.E.D.-G | CONSUMER_ONLY | outlmtdd |
| U535 | [OUTER LIMITS](https://textage.cc/score/8/outlmts.html) | 8th style | L.E.D.-G | CONSUMER_ONLY | outlmts |
| U536 | [paramnesia](https://textage.cc/score/0/prmnesia.html) | Consumer only | Shinji Ushiroda | CONSUMER_ONLY | prmnesia |
| U537 | [patsenner](https://textage.cc/score/1/patsen.html) | 1st style | dj nagureo | CONSUMER_ONLY | patsen |
| U538 | [perfect free](https://textage.cc/score/1/perfect.html) | 1st style | nite system | CONSUMER_ONLY | perfect |
| U539 | [Pluto](https://textage.cc/score/0/pluto.html) | Consumer only | Black∞Hole | CONSUMER_ONLY | pluto |
| U540 | [PRAT FALL](https://textage.cc/score/0/pratfall.html) | Consumer only | SPON | CONSUMER_ONLY | pratfall |
| U541 | [SANA MOLLETTE NE ENTE (B.L.T.STYLE)](https://textage.cc/score/0/sana_blt.html) | Consumer only | Togo Project feat. Sana | CONSUMER_ONLY | sana_blt |
| U542 | [Sense](https://textage.cc/score/3/sense3a.html) | 3rd style | good-cool | CONSUMER_ONLY | sense3a |
| U543 | [soldier's waltz](https://textage.cc/score/15/solwaltz.html) | DJ TROOPERS | DAJI | CONSUMER_ONLY | solwaltz |
| U544 | [Special energy](https://textage.cc/score/1/spenergy.html) | 1st style | DJ FX | CONSUMER_ONLY | spenergy |
| U545 | [STEP INTO THE NEW WORLD](https://textage.cc/score/0/stepinto.html) | Consumer only | L.E.D.-G VS GUHROOVY fw NO+CHIN | CONSUMER_ONLY | stepinto |
| U546 | [Strong Woman feat.DABO](https://textage.cc/score/0/strwoman.html) | Consumer only | Asami | CONSUMER_ONLY | strwoman |
| U547 | [suggestion](https://textage.cc/score/0/suggest.html) | Consumer only | kita-g | CONSUMER_ONLY | suggest |
| U548 | [Sunshine Hero](https://textage.cc/score/0/sunshero.html) | Consumer only | kors k feat.Mari*Co | CONSUMER_ONLY | sunshero |
| U549 | [super highway](https://textage.cc/score/0/shighway.html) | Consumer only | nouvo nude | CONSUMER_ONLY;UNKNOWN | shighway;shighwy5 |
| U550 | [symptom](https://textage.cc/score/0/symptom.html) | Consumer only | kobo vs kr:ague | CONSUMER_ONLY | symptom |
| U551 | [The Way You Move](https://textage.cc/score/0/theway_u.html) | Consumer only | Paul Grogan Featuring Natalie Martin | CONSUMER_ONLY | theway-u;theway_u |
| U552 | [Toxic](https://textage.cc/score/0/toxic.html) | Consumer only | WaveGroup/Shoichiro Hirata | CONSUMER_ONLY | toxic |
| U553 | [Trancemission](https://textage.cc/score/0/tmission.html) | Consumer only | napakick | CONSUMER_ONLY | tmission |
| U554 | [Treasure×Star](https://textage.cc/score/0/treastar.html) | Consumer only | NAOKI &amp; Ryu☆ fw.さちまゆ | CONSUMER_ONLY | treastar |
| U555 | [Troposphere](https://textage.cc/score/0/troposph.html) | Consumer only | kobo | CONSUMER_ONLY | troposph |
| U556 | [Turning the motor over](https://textage.cc/score/0/turning.html) | Consumer only | Delaware feat. Jeff Coote | CONSUMER_ONLY;UNKNOWN | turning;turning5 |
| U557 | [VANISHING POINT](https://textage.cc/score/0/vanishpt.html) | Consumer only | DJ CHUCKY | CONSUMER_ONLY | vanishpt |
| U558 | [Virtual Insanity](https://textage.cc/score/0/vinsanty.html) | Consumer only | WaveGroup/DJ TK-ST | CONSUMER_ONLY | vinsanty |
| U559 | [WHAT'S NEXT?](https://textage.cc/score/0/whatnext.html) | Consumer only | SLAKE feat. DAINA NORMAN | CONSUMER_ONLY;UNKNOWN | whatnext;whatnxt5 |
| U560 | [You Really Got Me](https://textage.cc/score/0/yougotme.html) | Consumer only | WaveGroup/Shoichiro Hirata | CONSUMER_ONLY | yougotme |
| U561 | [エブリデイ・ラブリデイ -L.E.D.STYLE MIX-](https://textage.cc/score/0/_evryday.html) | Consumer only | Togo-chef feat.Sana | CONSUMER_ONLY | _evryday |
| U562 | [炸裂！イェーガー電光チョップ!!](https://textage.cc/score/23/_j_final.html) | copula | L.E.D. | CONSUMER_ONLY | _j_final |
| U563 | [風の谷のDREAM](https://textage.cc/score/0/_kazedrm.html) | Consumer only | PINK PONG | CONSUMER_ONLY | _kazedrm |

## 全量未匹配：未知（1）

| 编号 | Textage 曲名 | 版本 | 曲师 | 原始状态 | Textage key |
| --- | --- | --- | --- | --- | --- |
| U564 | [Beginning of life](https://textage.cc/score/1/beginin5.html) | 1st style | QUADRA | UNKNOWN | beginin5 |

## 快照来源与复现

抓取时间（UTC）：`2026-09-16T07:18:52.874722+00:00`。

用 README 中的 `:app:auditCatalog` 任务对下载并转码后的公开快照执行相同匹配；输出 `songs.tsv` 中 `bjm_id` 为空即未匹配。实体解码使用 [jsoup 标准解码接口](https://jsoup.org/apidocs/org/jsoup/parser/Parser#unescapeEntities(java.lang.String,boolean))。

| 来源 | SHA-256（原始字节） |
| --- | --- |
| [titletbl.js](https://textage.cc/score/titletbl.js) | `7f6c7b46a63876caa6bf8ad87956204fa5d1af91b6a83f01df8da685554ec18b` |
| [actbl.js](https://textage.cc/score/actbl.js) | `cfa05e3013f4b56466cc5d33a44f573a58514d589cee86e8b0cc70a16fb8b197` |
| [cstbl.js](https://textage.cc/score/cstbl.js) | `7b07e8dcb559f06657587659ffa5545eefaa95f8357feb179d8ccf20e7c60ca1` |
| [cstbl1.js](https://textage.cc/score/cstbl1.js) | `8ee1c3e46b567ccb17ae6f6d52d8f4486b4231e14c1c38fbec1050d5f394dc07` |
| [cstbl2.js](https://textage.cc/score/cstbl2.js) | `a311f6bf526bc55eed6369261ce271e95c2bb6ba13c72c3a7301c987887095fa` |
| [cltbl.js](https://textage.cc/score/cltbl.js) | `a7db086feee28e3c8fe4b2f9841550e727166d778d67b580844aa50ecfa892d5` |
| [stepup.js](https://textage.cc/score/stepup.js) | `1add0603df7d9a2972ad05916cb6f6cd8a77f663c46b01b42f7f90c3a3097fb5` |
| [datatbl.js](https://textage.cc/score/datatbl.js) | `3324c03417f4064a83a652eace8930ed8389b17b80e20b8742b53d1d105a686f` |
| [scrlist.js](https://textage.cc/score/scrlist.js) | `dbdac5b3126b484618f9644483acb1e6554039ea88d64793b6d5e2856a0be764` |
| [LDJ_mdb_33.bin](https://assets.bjmania.com/mdb/LDJ_mdb_33.bin) | `dc4d793f78d8a2841ac5eab656c6ba6a15754486ff4f07170c5a96b0b9a7008a` |
