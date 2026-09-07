package ke.co.brivont.boka.data

/** Curated opening study material (superset of the web app's data/openings.ts). */

data class SampleGame(
    val white: String,
    val black: String,
    val event: String,
    val year: Int,
    val result: String,
    /** Space-separated SAN movetext. */
    val moves: String,
)

/** A named branch: what to play when the opponent deviates from the main line. */
data class Variation(
    val name: String,
    /** Full SAN line from move 1 (space-separated). */
    val moves: String,
    val note: String = "",
)

data class Opening(
    val id: String,
    val name: String,
    val eco: String,
    val forWhom: String,   // "White" | "Black"
    val summary: String,
    val ideas: List<String>,
    /** Canonical main line in SAN (space-separated). */
    val mainline: String,
    val variations: List<Variation> = emptyList(),
    val sampleGames: List<SampleGame> = emptyList(),
)

val OPENINGS: List<Opening> = listOf(
    Opening(
        id = "italian", name = "Italian Game", eco = "C50", forWhom = "White",
        summary = "The oldest classical opening — rapid development and a quick eye on f7.",
        ideas = listOf(
            "Develop the bishop to c4, targeting the weak f7 square.",
            "Build a broad pawn centre with c3 and d4 (the \"Giuoco Piano\" plan).",
            "Castle early and bring the rooks to the central files.",
            "Beware fast tactical traps against an under-developed opponent.",
        ),
        mainline = "e4 e5 Nf3 Nc6 Bc4 Bc5 c3 Nf6 d3 d6 O-O O-O",
        variations = listOf(
            Variation(
                "If 3…d6? (passive) — the Légal pattern",
                "e4 e5 Nf3 Nc6 Bc4 d6 Nc3 Bg4 h3 Bh5 Nxe5 Bxd1 Bxf7+ Ke7 Nd5#",
                "When Black develops slowly and pins your knight, the queen \"sacrifice\" Nxe5! wins: if Black grabs the queen, Bxf7+ and Nd5 is mate.",
            ),
            Variation(
                "If 3…Nf6 (Two Knights) — stay solid with d3",
                "e4 e5 Nf3 Nc6 Bc4 Nf6 d3 Bc5 c3 d6 O-O O-O a4 a6",
                "No need to enter razor-sharp Ng5 lines: d3 + c3 gives the same healthy Italian structure.",
            ),
            Variation(
                "If Black allows the big centre — the main Giuoco push",
                "e4 e5 Nf3 Nc6 Bc4 Bc5 c3 Nf6 d4 exd4 cxd4 Bb4+ Bd2 Bxd2+ Nbxd2 d5 exd5 Nxd5",
                "c3+d4 builds the full centre; after the forcing sequence White develops freely against the isolated d5 knight.",
            ),
        ),
        sampleGames = listOf(
            SampleGame("Paul Morphy", "NN", "Légal-style miniature", 1850, "1-0",
                "e4 e5 Nf3 Nc6 Bc4 d6 Nc3 Bg4 Nxe5 Bxd1 Bxf7+ Ke7 Nd5#"),
        ),
    ),
    Opening(
        id = "ruy-lopez", name = "Ruy López (Spanish)", eco = "C60", forWhom = "White",
        summary = "White pressures the knight defending e5 and plays for a long positional squeeze.",
        ideas = listOf(
            "Bb5 pins/pressures the c6-knight, the defender of e5.",
            "After ...a6, retreat Ba4–b3 keeping the bishop on the a2–g8 diagonal.",
            "Play c3 and d4 to expand; manoeuvre the knight via b1–d2–f1–g3.",
            "The classic plan is slow central and kingside pressure.",
        ),
        mainline = "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b5 Bb3 d6 c3 O-O",
        variations = listOf(
            Variation(
                "If 3…Nd4?! (Bird) — trade and play against d4",
                "e4 e5 Nf3 Nc6 Bb5 Nd4 Nxd4 exd4 O-O c6 Bc4 Nf6 d3 d5 exd5 cxd5 Bb5+ Bd7 Bxd7+ Qxd7",
                "Don't fear the jump to d4: exchange, castle, and the advanced d4-pawn becomes a long-term target.",
            ),
            Variation(
                "If 3…a6 4.Ba4 b5 — keep the diagonal from b3",
                "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 b5 Bb3 Bc5 c3 d6 d4 exd4 cxd4 Bb6",
                "The bishop is excellent on b3, still eyeing f7. Follow with c3–d4 and a big centre.",
            ),
        ),
    ),
    Opening(
        id = "berlin", name = "Berlin Defence", eco = "C65", forWhom = "Black",
        summary = "The rock-solid \"Berlin Wall\" — Black trades into a resilient endgame.",
        ideas = listOf(
            "Counterattack e4 immediately with ...Nf6 instead of ...a6.",
            "After the queen trade, Black accepts doubled pawns for the bishop pair.",
            "Black is slightly worse but extremely solid — famous as a drawing weapon.",
            "King safety via ...Kd8–c8 and activating the bishops.",
        ),
        mainline = "e4 e5 Nf3 Nc6 Bb5 Nf6 O-O Nxe4 d4 Nd6 Bxc6 dxc6 dxe5 Nf5 Qxd8+ Kxd8",
        variations = listOf(
            Variation(
                "If White avoids with 4.d3 — the anti-Berlin",
                "e4 e5 Nf3 Nc6 Bb5 Nf6 d3 Bc5 c3 O-O O-O d6",
                "Against the quiet d3, just develop classically — …Bc5, castle, …d6 — and you're fully equal.",
            ),
            Variation(
                "If 4.O-O Nxe4 5.Re1 — retreat and simplify",
                "e4 e5 Nf3 Nc6 Bb5 Nf6 O-O Nxe4 Re1 Nd6 Nxe5 Be7 Bf1 Nxe5 Rxe5 O-O",
                "5.Re1 (instead of 5.d4) leads to mass exchanges: after …Nd6 and …Nxe5 Black castles into a comfortable, symmetric game.",
            ),
        ),
    ),
    Opening(
        id = "sicilian-najdorf", name = "Sicilian Defence (Najdorf)", eco = "B90", forWhom = "Black",
        summary = "Black's sharpest, most respected answer to 1.e4 — fights for the initiative.",
        ideas = listOf(
            "...c5 fights for the centre asymmetrically and unbalances the game.",
            "...a6 (the Najdorf move) prepares ...e5/...e6 and prevents Bb5/Nb5.",
            "Black aims for queenside play with ...b5 and pressure on the c-file.",
            "Both sides often castle opposite and race with pawn storms.",
        ),
        mainline = "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6",
        variations = listOf(
            Variation(
                "If 6.Bg5 (main line) — answer …e6 and …Be7",
                "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Bg5 e6 f4 Be7 Qf3 Qc7",
                "The critical test. Black holds the centre with …e6, unpins with …Be7 and puts the queen on c7, ready for …b5.",
            ),
            Variation(
                "If 6.Be2 (quiet) — grab space with …e5",
                "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6 Be2 e5 Nb3 Be7 O-O O-O",
                "Against slow setups the thematic …e5 kicks the knight and stakes out the centre — the classical Najdorf plan.",
            ),
        ),
    ),
    Opening(
        id = "french", name = "French Defence (Winawer)", eco = "C18", forWhom = "Black",
        summary = "A resilient counterattacking defence; Black accepts a cramped but tough position.",
        ideas = listOf(
            "...e6 and ...d5 strike at the centre; the c8-bishop is the \"problem piece\".",
            "In the Winawer, ...Bb4 pins and then trades on c3, damaging White's pawns.",
            "Black counterattacks the d4 pawn with ...c5 and pressure on the c-file.",
            "White gets the bishop pair and space; Black gets a solid structure and targets.",
        ),
        mainline = "e4 e6 d4 d5 Nc3 Bb4 e5 c5 a3 Bxc3+ bxc3 Ne7",
        variations = listOf(
            Variation(
                "If 3.Nd2 (Tarrasch) — free your game with …c5",
                "e4 e6 d4 d5 Nd2 c5 exd5 exd5 Ngf3 Nc6 Bb5 Bd6",
                "Against the Tarrasch knight, …c5! challenges the centre at once; accept the isolated pawn for free, active pieces.",
            ),
            Variation(
                "If 3.e5 (Advance) — attack the chain with …c5 and …Qb6",
                "e4 e6 d4 d5 e5 c5 c3 Nc6 Nf3 Qb6 Be2 cxd4 cxd4 Nh6",
                "The base of the pawn chain (d4) is the target: pile on it with …c5, …Nc6, …Qb6 and reroute the h6-knight to f5.",
            ),
        ),
    ),
    Opening(
        id = "caro-kann", name = "Caro-Kann Defence (Classical)", eco = "B18", forWhom = "Black",
        summary = "Solid and sound — Black gets the light-squared bishop out before ...e6.",
        ideas = listOf(
            "...c6 and ...d5 challenge e4 without locking in the c8-bishop.",
            "In the Classical, develop the bishop to f5/g6 before playing ...e6.",
            "Aim for a sturdy structure and a safe king; outplay in the endgame.",
            "A favourite of positional players for its reliability.",
        ),
        mainline = "e4 c6 d4 d5 Nc3 dxe4 Nxe4 Bf5 Ng3 Bg6 h4 h6 Nf3 Nd7",
        variations = listOf(
            Variation(
                "If 3.e5 (Advance) — bishop out FIRST, then …e6",
                "e4 c6 d4 d5 e5 Bf5 Nf3 e6 Be2 Nd7 O-O Ne7",
                "The whole point of the Caro: …Bf5 escapes before the pawn chain locks. Then …e6 with a French structure minus the bad bishop.",
            ),
            Variation(
                "If 3.exd5 (Exchange) — develop naturally, watch b7",
                "e4 c6 d4 d5 exd5 cxd5 Bd3 Nc6 c3 Nf6 Bf4 Bg4 Qb3 Qd7",
                "Symmetry favours the better developed side. …Bg4 and …Qd7 calmly meet the Qb3 poke at b7/d5.",
            ),
        ),
    ),
    Opening(
        id = "qgd", name = "Queen's Gambit Declined", eco = "D35", forWhom = "Black",
        summary = "A classical, dependable answer to 1.d4 — Black holds the centre with ...e6.",
        ideas = listOf(
            "Decline the gambit with ...e6, keeping a strong point on d5.",
            "Develop naturally; solve the c8-bishop with ...b6/...Bb7 or ...Nbd7.",
            "White presses with the minority attack (b4–b5) on the queenside.",
            "Black seeks central freeing breaks with ...c5 or ...e5.",
        ),
        mainline = "d4 d5 c4 e6 Nc3 Nf6 Bg5 Be7 e3 O-O Nf3 h6 Bh4 b6",
        variations = listOf(
            Variation(
                "If 3.cxd5 (Exchange) — the Carlsbad structure",
                "d4 d5 c4 e6 cxd5 exd5 Nc3 Nf6 Bg5 Be7 e3 c6 Bd3 Nbd7",
                "Recapture with the e-pawn and build the famous Carlsbad wall (…c6, …Nbd7). Watch for White's minority attack b4–b5.",
            ),
            Variation(
                "The Tartakower plan — …h6, …b6 and …Bb7",
                "d4 d5 c4 e6 Nc3 Nf6 Nf3 Be7 Bg5 O-O e3 h6 Bh4 b6",
                "The most reliable way to solve the problem bishop: fianchetto it to b7 behind a rock-solid centre.",
            ),
        ),
    ),
    Opening(
        id = "kid", name = "King's Indian Defence", eco = "E90", forWhom = "Black",
        summary = "A hypermodern fighting defence — let White build a big centre, then strike it.",
        ideas = listOf(
            "Fianchetto with ...g6/...Bg7 and let White occupy the centre.",
            "Strike back with ...e5 (or ...c5) once developed.",
            "In closed positions, storm the kingside with ...f5–f4 and ...g5.",
            "Sharp, double-edged — a favourite of attacking players.",
        ),
        mainline = "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e5",
        variations = listOf(
            Variation(
                "If White trades 7.dxe5? — the …Nxe4! trick",
                "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e5 dxe5 dxe5 Qxd8 Rxd8 Nxe5 Nxe4 Nxe4 Bxe5",
                "The exchange \"win a pawn\" is an illusion: after the queen trade, Nxe5 runs into …Nxe4! and the g7-bishop regains everything with a fine game.",
            ),
            Variation(
                "If 5.f3 (Sämisch) — classical …e5 and …Nh5–f4 play",
                "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 f3 O-O Be3 e5 d5 Nh5 Qd2 f5",
                "Against the Sämisch wall, castle, hit the centre with …e5, then launch the classic kingside storm with …Nh5 and …f5.",
            ),
        ),
    ),
    Opening(
        id = "philidor", name = "Philidor Defence", eco = "C41", forWhom = "Black",
        summary = "A solid if passive defence — the setting of the immortal \"Opera Game\".",
        ideas = listOf(
            "...d6 supports e5 but can be passive if Black is careless.",
            "Avoid falling behind in development — the Opera Game shows the danger.",
            "Aim to complete development and challenge the centre with ...exd4 or ...c6/...d5.",
        ),
        mainline = "e4 e5 Nf3 d6 d4 exd4 Nxd4 Nf6 Nc3 Be7",
        variations = listOf(
            Variation(
                "The Hanham setup — …Nd7 keeps the centre closed",
                "e4 e5 Nf3 d6 d4 Nd7 Bc4 c6 O-O Be7 dxe5 dxe5",
                "…Nd7 holds e5 without exchanging. Play …c6 first (stopping Bc4–d5 tricks), then …Be7 and castle.",
            ),
        ),
        sampleGames = listOf(
            SampleGame("Paul Morphy", "Duke of Brunswick & Count Isouard", "Paris (Opera Game)", 1858, "1-0",
                "e4 e5 Nf3 d6 d4 Bg4 dxe5 Bxf3 Qxf3 dxe5 Bc4 Nf6 Qb3 Qe7 Nc3 c6 Bg5 b5 Nxb5 cxb5 Bxb5+ Nbd7 O-O-O Rd8 Rxd7 Rxd7 Rd1 Qe6 Bxd7+ Nxd7 Qb8+ Nxb8 Rd8#"),
        ),
    ),
    Opening(
        id = "kings-gambit", name = "King's Gambit", eco = "C33", forWhom = "White",
        summary = "A romantic, swashbuckling gambit — sacrifice a pawn for rapid attack.",
        ideas = listOf(
            "Offer the f-pawn to rip open the f-file and seize the centre with d4.",
            "Develop fast (Nf3, Bc4) and target f7 and the black king.",
            "Risky against precise defence, but devastating against passive play.",
            "The setting of Anderssen's \"Immortal Game\".",
        ),
        mainline = "e4 e5 f4 exf4 Nf3 g5 Bc4 Bg7 d4 d6",
        variations = listOf(
            Variation(
                "If Black declines with 2…Bc5 — don't take, build",
                "e4 e5 f4 Bc5 Nf3 d6 c3 Nf6 d4 exd4 cxd4 Bb6",
                "Against the Declined, the bishop on c5 stops you castling — so blunt it with c3+d4 and take the whole centre.",
            ),
            Variation(
                "If 3…g5 4.h4! — the Kieseritzky crack",
                "e4 e5 f4 exf4 Nf3 g5 h4 g4 Ne5 Nf6 d4 d6 Nd3 Nxe4 Bxf4",
                "When Black defends the extra pawn with …g5, h4! shatters the chain: after …g4 the knight sits on e5/d3 and White regains f4 with a huge centre.",
            ),
        ),
        sampleGames = listOf(
            SampleGame("Adolf Anderssen", "Lionel Kieseritzky", "London (The Immortal Game)", 1851, "1-0",
                "e4 e5 f4 exf4 Bc4 Qh4+ Kf1 b5 Bxb5 Nf6 Nf3 Qh6 d3 Nh5 Nh4 Qg5 Nf5 c6 g4 Nf6 Rg1 cxb5 h4 Qg6 h5 Qg5 Qf3 Ng8 Bxf4 Qf6 Nc3 Bc5 Nd5 Qxb2 Bd6 Bxg1 e5 Qxa1+ Ke2 Na6 Nxg7+ Kd8 Qf6+ Nxf6 Be7#"),
        ),
    ),
    Opening(
        id = "london", name = "London System", eco = "D02", forWhom = "White",
        summary = "A rock-solid, easy-to-learn system: build the same setup against almost anything and aim at the kingside.",
        ideas = listOf(
            "Play the same pieces out every game: d4, Bf4, e3, Nf3, c3, Nbd2, Bd3 — low theory, high reliability.",
            "Keep the Bf4-Bd3 battery aimed at the black kingside; a knight often lands on e5.",
            "The c3 + e3 wall makes your centre very hard to break; castle and expand when ready.",
            "Watch for …Nh5 or …Bd6 trying to trade off your good dark-squared bishop — retreat to g3.",
        ),
        mainline = "d4 d5 Bf4 Nf6 e3 e6 Nf3 c5 c3 Nc6 Nbd2 Bd6 Bg3 O-O Bd3 b6 O-O Bb7",
        variations = listOf(
            Variation("If Black plays a King’s-Indian setup (…g6)",
                "d4 Nf6 Bf4 g6 Nf3 Bg7 e3 O-O Be2 d6 h3 Nbd7 O-O",
                "Against …g6 the same London pieces work; h3 gives the bishop a retreat and you play for e4 later."),
        ),
    ),
    Opening(
        id = "scandinavian", name = "Scandinavian Defence", eco = "B01", forWhom = "Black",
        summary = "Strike at e4 on move one and get an easy, sound game — Black trades central tension for quick, clear development.",
        ideas = listOf(
            "After 1.e4 d5 2.exd5 Qxd5 3.Nc3, retreat the queen to a5 (or d6) and develop naturally.",
            "Aim for a Caro-Kann-like structure: …c6, …Bf5 (get the light bishop out before …e6), …e6, …Nbd7.",
            "It is solid and low-theory — you reach the same reliable positions against most of White’s tries.",
            "Do not leave the queen exposed: know where it belongs (a5/d6) so you never lose time.",
        ),
        mainline = "e4 d5 exd5 Qxd5 Nc3 Qa5 d4 Nf6 Nf3 c6 Bc4 Bf5 Bd2 e6 Qe2 Bb4 O-O-O Nbd7",
        variations = listOf(
            Variation("The …Nf6 gambit line (…Qxd5 avoided)",
                "e4 d5 exd5 Nf6 d4 Nxd5 Nf3 g6 Be2 Bg7 O-O O-O",
                "Instead of recapturing with the queen, …Nf6 and …Nxd5 gives a fast, harmonious fianchetto setup."),
        ),
    ),
    Opening(
        id = "english", name = "English Opening", eco = "A20", forWhom = "White",
        summary = "A flexible flank opening (1.c4) that fights for the centre from the side and can transpose into many structures.",
        ideas = listOf(
            "Fianchetto the king’s bishop (g3, Bg2) to pressure the long light-squared diagonal and d5.",
            "1.c4 controls d5 without committing the centre pawns — stay flexible and react to Black’s setup.",
            "Reversed Sicilian: after 1…e5 you play a Sicilian a tempo up; the extra move matters.",
            "Be ready to transpose to Queen’s-Gambit or Catalan structures if Black plays …d5 and …e6.",
        ),
        mainline = "c4 e5 Nc3 Nf6 Nf3 Nc6 g3 d5 cxd5 Nxd5 Bg2 Nb6 O-O Be7 d3 O-O a3 a5",
        variations = listOf(
            Variation("Symmetrical English (1…c5)",
                "c4 c5 Nc3 Nc6 g3 g6 Bg2 Bg7 Nf3 Nf6 O-O O-O d4 cxd4 Nxd4",
                "When Black mirrors with …c5, a well-timed d4 break opens the centre for your better-placed pieces."),
        ),
    ),
)
