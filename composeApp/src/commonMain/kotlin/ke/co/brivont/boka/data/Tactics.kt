package ke.co.brivont.boka.data

/**
 * Original tactics primer (ported from the web app's data/tactics.ts). Each
 * motif starts from a custom FEN and plays a short SAN solution. All FENs +
 * lines are legality-checked on the web side (scripts/validateOpenings.mjs).
 * Our own compositions/explanations — no third-party text.
 */
data class Tactic(
    val id: String,
    val name: String,
    /** Motif family, shown as a chip. */
    val motif: String,
    /** Starting position. */
    val fen: String,
    val summary: String,
    val ideas: List<String>,
    /** Demonstration line in SAN, played from [fen]. */
    val line: String,
)

val TACTICS: List<Tactic> = listOf(
    Tactic(
        id = "back-rank", name = "Back-Rank Mate", motif = "Mating pattern",
        fen = "6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1",
        summary = "A king trapped behind its own unmoved pawns is mated by a rook or queen on the back rank.",
        ideas = listOf(
            "The pawns on f7-g7-h7 form a wall — the king has no \"luft\" (escape square).",
            "A rook or queen reaching the 8th rank with no defender delivers mate.",
            "Prophylaxis: play h3/h6 early to give the king an escape square.",
            "When attacking, look for ways to remove or deflect the defender of the back rank.",
        ),
        line = "Re8#",
    ),
    Tactic(
        id = "knight-fork", name = "Knight Fork", motif = "Fork / double attack",
        fen = "k1q5/8/8/3N4/8/8/8/6K1 w - - 0 1",
        summary = "A knight attacks the king and queen at once; the king must move and the queen falls.",
        ideas = listOf(
            "The knight is the ideal forking piece — nothing it attacks can attack it back.",
            "Look for a square that hits two targets, especially with check (a check forces the reply).",
            "Undefended pieces are the easiest fork targets — always scan for \"loose\" pieces.",
            "Royal forks (king + queen) and family forks (king + queen + rook) win material outright.",
        ),
        line = "Nb6+ Kb8 Nxc8",
    ),
    Tactic(
        id = "skewer", name = "Skewer", motif = "Skewer",
        fen = "q7/8/8/8/k7/8/8/1R4K1 w - - 0 1",
        summary = "Check the king along a line; when it steps aside, capture the piece that stood behind it.",
        ideas = listOf(
            "A skewer is a pin in reverse — the MORE valuable piece is in front and must move.",
            "Rooks, bishops and queens skewer along ranks, files and diagonals.",
            "Try to line up the enemy king and queen (or rook) on one line, then check.",
            "Skewers decide many king-and-pawn endings — the new queen is skewered to its king.",
        ),
        line = "Ra1+ Kb4 Rxa8",
    ),
    Tactic(
        id = "pin", name = "Pin & Win", motif = "Pin",
        fen = "4k3/8/8/4b3/3P4/8/8/4RK2 w - - 0 1",
        summary = "A piece pinned to its king is frozen — attack it again and win it.",
        ideas = listOf(
            "An absolute pin (to the king) makes it illegal to move the pinned piece.",
            "Once a piece is pinned, hit it with a pawn or a lesser piece to win it.",
            "Here the bishop on e5 is pinned by the rook; d4xe5 wins it for free.",
            "Break a pin against you by challenging the pinning piece or making luft for the king.",
        ),
        line = "dxe5",
    ),
    Tactic(
        id = "discovered-check", name = "Discovered Check", motif = "Discovered attack",
        fen = "4k3/1r6/8/8/4B3/8/8/4R1K1 w - - 0 1",
        summary = "Move one piece to unveil a check from the piece behind it — and grab material on the way.",
        ideas = listOf(
            "The moving piece can capture or threaten WHILE the unmasked piece gives check.",
            "A discovered check is brutal: the opponent must answer the check, not the capture.",
            "Line your rook or bishop up behind one of your own pieces, aimed at the enemy king.",
            "The strongest version is double check — only a king move is legal.",
        ),
        line = "Bxb7+",
    ),
    Tactic(
        id = "double-attack", name = "Queen Double Attack", motif = "Fork / double attack",
        fen = "8/6k1/8/8/r7/8/8/3QK3 w - - 0 1",
        summary = "One move, two threats. The queen checks the king and hits a loose rook — only one can be met.",
        ideas = listOf(
            "The queen is the great double-attacker: combine a check with an attack on a loose piece.",
            "Hunt for two undefended targets a single move can hit at once.",
            "Checks that also win material are the cleanest double attacks.",
            "Keep your own pieces defended so you are never the one being forked.",
        ),
        line = "Qd4+ Kg8 Qxa4",
    ),
    Tactic(
        id = "smothered-mate", name = "Smothered Mate", motif = "Mating pattern",
        fen = "6rk/6pp/8/6N1/8/8/8/6K1 w - - 0 1",
        summary = "The king is boxed in by its own pieces and a lone knight delivers mate.",
        ideas = listOf(
            "When the king is hemmed in by its own pawns and pieces, only a knight can reach it.",
            "The famous version uses a queen sacrifice to force a rook to g8, then Nf7#.",
            "Recognise the pattern: king h8, pawns g7-h7, a piece on g8 — Nf7 is mate.",
            "Prevention: keep a flight square and do not let your back rank get boxed in.",
        ),
        line = "Nf7#",
    ),
)
