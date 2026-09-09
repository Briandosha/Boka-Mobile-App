package ke.co.brivont.boka.data

/**
 * Fundamental checkmating techniques (ported from the web app's data/endgames.ts).
 * Each lesson starts from a custom FEN and plays a short SAN line demonstrating
 * the mate (lines validated on the web side).
 */
data class Endgame(
    val id: String,
    val name: String,
    /** Starting position (custom). */
    val fen: String,
    val summary: String,
    val ideas: List<String>,
    /** Demonstration line in SAN, played from [fen]. */
    val line: String,
)

val ENDGAMES: List<Endgame> = listOf(
    Endgame(
        id = "kq-vs-k",
        name = "Queen + King vs King",
        fen = "4k3/8/4K3/8/8/8/8/3Q4 w - - 0 1",
        summary = "The most common winning endgame — the queen drives the lone king to the edge, the king delivers mate.",
        ideas = listOf(
            "Use the queen to shrink the enemy king's box, staying a KNIGHT'S move away to avoid stalemate.",
            "Walk your own king up to support the queen — the queen alone cannot mate.",
            "Deliver mate on an edge or corner with the king guarding the queen.",
            "Beware stalemate when the lone king has no legal move and is NOT in check.",
        ),
        line = "Qd7+ Kf8 Qf7#",
    ),
    Endgame(
        id = "kr-vs-k",
        name = "Rook + King vs King",
        fen = "4k3/8/4K3/8/8/8/8/R7 w - - 0 1",
        summary = "A standard win: the rook cuts the king off while your king takes the opposition, then the rook mates on the edge.",
        ideas = listOf(
            "Use the rook to confine the king to a shrinking \"box\".",
            "Bring your king up to take the opposition (kings facing, one square apart).",
            "With the kings in opposition, check along the edge rank/file to mate.",
            "Keep the rook safe — never let the lone king attack it without your king nearby.",
        ),
        line = "Ra8#",
    ),
    Endgame(
        id = "two-rooks",
        name = "Two Rooks Mate (the ladder)",
        fen = "8/8/8/4k3/1R6/8/8/R5K1 w - - 0 1",
        summary = "The easiest mate to learn — two rooks \"ladder\" the king to the edge, no king help needed.",
        ideas = listOf(
            "One rook checks the king onto the next rank; the other cuts off the rank behind it.",
            "Alternate checks, walking the king to the edge one rank at a time.",
            "Keep the rooks far from the lone king so it can never attack them.",
            "The last check on the edge rank is mate.",
        ),
        line = "Ra5+ Ke6 Rb6+ Ke7 Ra7+ Ke8 Rb8#",
    ),
    Endgame(
        id = "kp-square-rule",
        name = "King + Pawn: The Square Rule",
        fen = "8/8/8/8/1P5k/8/8/1K6 w - - 0 1",
        summary = "Can the enemy king catch your runner? Draw the \"square\" from the pawn to the last rank — if the king can't step inside it, the pawn queens by itself.",
        ideas = listOf(
            "Imagine a square whose side runs from the pawn to its promotion square.",
            "If the defending king can step INTO that square on its move, it catches the pawn.",
            "Here the king on h4 is outside the b-pawn's square — count it: the pawn is simply faster.",
            "With the king outside the square you don't even need your own king. Push!",
        ),
        line = "b5 Kg5 b6 Kf6 b7 Ke7 b8=Q",
    ),
    Endgame(
        id = "kp-opposition",
        name = "King + Pawn: Opposition & Key Squares",
        fen = "3k4/8/3KP3/8/8/8/8/8 b - - 0 1",
        summary = "King in FRONT of the pawn + the opposition = a forced win. Watch White's king escort the pawn home while Black is in zugzwang.",
        ideas = listOf(
            "The king belongs IN FRONT of its pawn, not behind it.",
            "\"Opposition\": kings face off one square apart — whoever must move gives ground.",
            "Here it's Black to move, so Black must step aside and White's king clears the path.",
            "Careful: the same position with White to move is only a draw — that's zugzwang.",
        ),
        line = "Ke8 e7 Kf7 Kd7 Kf6 e8=Q",
    ),
    Endgame(
        id = "lucena",
        name = "Rook Endgame: The Lucena Bridge",
        fen = "1K6/1P1k4/8/8/8/8/r7/2R5 w - - 0 1",
        summary = "THE most important rook endgame: your pawn is one step from queening but your king is boxed in — \"build a bridge\" with the rook to shelter it from checks.",
        ideas = listOf(
            "Problem: the king can't leave b8 because of endless rook checks from behind.",
            "Step 1: lift your rook to the 4th rank — that's the bridge's foundation.",
            "Step 2: walk the king out; when the checks come, block them by dropping the rook in front.",
            "Every rook-and-pawn ending you ever win funnels into this position. Know it cold.",
        ),
        line = "Rc4 Ra1 Rd4+ Ke6 Kc7 Rc1+ Kb6 Rb1+ Kc6 Rc1+ Kb5 Rb1+ Rb4 Ra1 b8=Q",
    ),
    Endgame(
        id = "rb-vs-r",
        name = "Rook + Bishop vs Rook: The Mating Net",
        fen = "6k1/r7/6K1/8/4B3/8/8/5R2 w - - 0 1",
        summary = "You have rook + bishop, they have a bare rook. Objectively drawish with perfect defence — but the defence is brutally hard, and this net wins it in practice.",
        ideas = listOf(
            "Drive the defending king to the back rank and plant your king in front of it.",
            "The bishop's job is to cover the escape square (here g8) — then the rook delivers on the 8th.",
            "Bd5+ forces the king into the corner (f8 is covered by your rook!), and Rf8 is mate.",
            "As the defender: keep your king OFF the back rank and check from far away.",
        ),
        line = "Bd5+ Kh8 Rf8#",
    ),
    Endgame(
        id = "two-bishops",
        name = "Two Bishops Mate",
        fen = "6k1/8/6K1/8/8/8/4B3/2B5 w - - 0 1",
        summary = "Two bishops working as a laser pair — one seals the escape diagonal, the other delivers along the long diagonal into the corner.",
        ideas = listOf(
            "The bishops herd the king to a corner by controlling two adjacent diagonals.",
            "Your king covers the flight squares next to the corner (here g7 and h7).",
            "Bc4+ forces the king into the corner; Bb2 fires down the long diagonal — mate.",
            "Two bishops always win; bishop + knight also wins but is much harder.",
        ),
        line = "Bc4+ Kh8 Bb2#",
    ),
    Endgame(
        id = "opposition", name = "King & Pawn: the Opposition",
        fen = "4k3/8/4K3/4P3/8/8/8/8 b - - 0 1",
        summary = "The single most important king-and-pawn idea: with your king in front of the pawn and the opposition, you force the enemy king aside and promote.",
        ideas = listOf(
            "The \"opposition\" = the kings face each other with one square between and it is the OTHER side to move — they must give way.",
            "Get your king in FRONT of the pawn (on the 6th rank here) before pushing — the king leads, the pawn follows.",
            "Here Black is to move and must step aside; White seizes the key squares and escorts the pawn home.",
            "Exception: a rook-pawn (a/h file) is only a draw if the defending king reaches the corner.",
        ),
        line = "Kf8 Kd7 Kf7 e6+ Kf8 e7+ Kf7 e8=Q+",
    ),
    Endgame(
        id = "philidor", name = "Philidor Position (the Drawing Defence)",
        fen = "4k3/R7/8/4P3/4K3/8/8/5r2 w - - 0 1",
        summary = "The defender’s key drawing technique in rook endings: hold the 3rd rank, then check from behind once the pawn advances.",
        ideas = listOf(
            "As the DEFENDER (Black here), keep your rook on your 3rd rank (…Rf6) to stop the enemy king advancing.",
            "The moment the pawn pushes to the 6th (e6), it no longer shelters its king — swing the rook behind (…Rf1) and check forever.",
            "The attacking king can never escape the checks without abandoning the pawn — it is a draw.",
            "Know both sides: reach the Lucena to win, hold the Philidor to draw.",
        ),
        line = "Kd4 Rf6 e6 Rf1",
    ),
    Endgame(
        id = "two-rooks-ladder", name = "Two Rooks: the Lawnmower",
        fen = "4k3/R7/8/8/8/8/6K1/1R6 w - - 0 1",
        summary = "Two rooks mate a lone king with no help from their own king — the \"lawnmower\" that walks the king to the edge.",
        ideas = listOf(
            "One rook cuts off a rank; the other checks on the next one, driving the king back a step at a time.",
            "Keep the rooks far from the enemy king so it can never attack them.",
            "Here the king is already on the back rank: Ra7 seals the 7th, and Rb8 delivers mate along the 8th.",
            "The same ladder works with two rooks, rook + queen, or two queens.",
        ),
        line = "Rb8#",
    ),
)
