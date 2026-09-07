package ke.co.brivont.boka.data

/**
 * Original "how most people play vs how you should play" curriculum (ported from
 * the web app's data/coaching.ts). Each lesson has the recommended line
 * (bestLine) and the typical club move + its consequence (commonLine), both from
 * the start. All lines are chess.js-validated on the web side. Our own writing.
 */
data class Lesson(
    val id: String,
    val title: String,
    val group: String,
    val forWhom: String,     // "White" | "Black"
    val summary: String,
    val commonMove: String,
    val bestMove: String,
    val bestLine: String,
    val commonLine: String,
    val why: List<String>,
)

val COACHING: List<Lesson> = listOf(
    Lesson(
        id = "scholars", title = "Beat the Scholar’s Mate", group = "Opening traps", forWhom = "Black",
        summary = "White throws the queen out for a 4-move mate. Punish it by developing — don’t panic.",
        commonMove = "Nf6??", bestMove = "Nc6",
        bestLine = "e4 e5 Qh5 Nc6 Bc4 g6 Qf3 Nf6",
        commonLine = "e4 e5 Qh5 Nf6 Qxe5+",
        why = listOf(
            "After 2.Qh5 the threats are Qxe5+ (winning the pawn) and Qxf7# — you must meet BOTH.",
            "The natural-looking 2…Nf6?? loses on the spot to 3.Qxe5+ forking king and pawn.",
            "2…Nc6 defends e5 and develops; then 3…g6 kicks the queen and 4…Nf6 gains more time.",
            "An early queen raid is bad for the raider — you develop with tempo while the queen runs.",
        ),
    ),
    Lesson(
        id = "fried-liver", title = "Survive the Fried Liver", group = "Opening traps", forWhom = "Black",
        summary = "Against 4.Ng5, recapturing on d5 walks into a raging attack. Hit the bishop instead.",
        commonMove = "Nxd5?!", bestMove = "Na5",
        bestLine = "e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5 d5 exd5 Na5 Bb5+ c6 dxc6 bxc6",
        commonLine = "e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5 d5 exd5 Nxd5 Nxf7 Kxf7 Qf3+",
        why = listOf(
            "4.Ng5 attacks f7. After 4…d5 5.exd5, the tempting 5…Nxd5?! runs into 6.Nxf7! — the Fried Liver.",
            "After 6.Nxf7 Kxf7 7.Qf3+ your king is dragged out and White’s attack is worth more than the piece.",
            "Play 5…Na5! hitting the c4-bishop; you give back the pawn but get a big lead in development.",
            "Rule: when a knight leaps to g5 hitting f7, look for a counter-hit before you grab material.",
        ),
    ),
    Lesson(
        id = "petrov-kick", title = "Petrov: kick before you take", group = "Opening traps", forWhom = "Black",
        summary = "In the Petrov, snatching the e4 pawn straight back walks into a queen-winning trick.",
        commonMove = "Nxe4??", bestMove = "d6",
        bestLine = "e4 e5 Nf3 Nf6 Nxe5 d6 Nf3 Nxe4 d4 d5",
        commonLine = "e4 e5 Nf3 Nf6 Nxe5 Nxe4 Qe2 Nf6 Nc6+",
        why = listOf(
            "After 3.Nxe5, the symmetrical 3…Nxe4?? is a well-known trap: 4.Qe2! and Black is stuck.",
            "If 4…Nf6?? then 5.Nc6+! is a discovered check that wins the black queen.",
            "Play 3…d6 first to kick the e5-knight; only THEN 4…Nxe4 with a healthy, equal game.",
            "General idea: don’t restore material immediately if a small preparatory move removes the danger.",
        ),
    ),
    Lesson(
        id = "qga-not-gambit", title = "The Queen’s Gambit is not a gambit", group = "Principles", forWhom = "Black",
        summary = "You can take on c4 — but you can’t keep it. Trying to hold the pawn wrecks your queenside.",
        commonMove = "b5??", bestMove = "Nf6",
        bestLine = "d4 d5 c4 dxc4 Nf3 Nf6 e3 e6 Bxc4",
        commonLine = "d4 d5 c4 dxc4 e3 b5 a4 c6 axb5 cxb5 Qf3",
        why = listOf(
            "2.c4 offers a pawn you cannot safely hold — 2…dxc4 is fine, but hanging on with …b5 is not.",
            "…b5?? runs into a4! blasting the queenside open; here it ends with Qf3 hitting the a8-rook.",
            "Instead develop: …Nf6, …e6, and let White recapture on c4 — you get a comfortable, equal game.",
            "Grabbing a pawn is only worth it if you can hold it without falling far behind in development.",
        ),
    ),
    Lesson(
        id = "scandi-a5", title = "Scandinavian: retreat to a5, not home", group = "Principles", forWhom = "Black",
        summary = "After the queen recaptures on d5 and gets hit by Nc3, retreating all the way back wastes moves.",
        commonMove = "Qd8?!", bestMove = "Qa5",
        bestLine = "e4 d5 exd5 Qxd5 Nc3 Qa5 d4 Nf6 Nf3 c6 Bc4 Bf5",
        commonLine = "e4 d5 exd5 Qxd5 Nc3 Qd8 d4 Nf6 Nf3 c6 Bc4",
        why = listOf(
            "After 3.Nc3 the queen must move; 3…Qa5 keeps it active and eyes the c3-knight and a-file.",
            "3…Qd8?! puts the queen back home — you spent two moves to be developed like White minus a tempo.",
            "From a5 you follow up naturally with …c6, …Bf5 (bishop out before …e6!), …e6, …Nbd7.",
            "Every tempo counts in the opening — don’t undevelop unless you must.",
        ),
    ),
    Lesson(
        id = "center-game-queen", title = "Don’t bring the queen out early (White)", group = "Principles", forWhom = "White",
        summary = "Recapturing on d4 with the queen feels natural, but it hands Black free development.",
        commonMove = "Qxd4?!", bestMove = "Nf3",
        bestLine = "e4 e5 d4 exd4 Nf3 Nc6 Bc4",
        commonLine = "e4 e5 d4 exd4 Qxd4 Nc6 Qe3 Nf6 Nc3 Bb4",
        why = listOf(
            "After 2.d4 exd4, 3.Qxd4?! looks clean but 3…Nc6! develops a piece AND hits the queen.",
            "The queen shuffles (Qe3) while Black brings out …Nf6 and …Bb4 with gain of time.",
            "Keep the queen home: 3.Nf3 (Scotch) or a gambit develops your pieces first and fights for the centre.",
            "In open positions the side that develops faster usually gets the attack.",
        ),
    ),
    Lesson(
        id = "legal-trap", title = "Beware Légal’s Mate — don’t trust a fake pin", group = "Opening traps", forWhom = "Black",
        summary = "A lazy …Bg4 “pin” plus passive play lets White sacrifice the queen for a picture mate.",
        commonMove = "Bg4?!", bestMove = "Bc5",
        bestLine = "e4 e5 Nf3 Nc6 Bc4 Bc5 c3 Nf6 d3 d6 O-O O-O",
        commonLine = "e4 e5 Nf3 Nc6 Bc4 d6 Nc3 Bg4 Nxe5 Bxd1 Bxf7+ Ke7 Nd5#",
        why = listOf(
            "A pin is only real if the pinned piece is truly stuck; after …d6 and …Bg4 the f3-knight isn’t.",
            "White plays Nxe5! — if you grab the queen (…Bxd1), Bxf7+ Ke7 Nd5# is Légal’s Mate.",
            "Develop soundly instead: …Bc5, …Nf6, …O-O — no loose pins, no tricks for the opponent.",
            "Before you pin with …Bg4, ask: “what happens if the ‘pinned’ knight just moves?”",
        ),
    ),
    Lesson(
        id = "recapture-center", title = "Recapture toward the centre", group = "Principles", forWhom = "Black",
        summary = "When you recapture, choose the pawn that captures toward the middle — it opens lines and frees pieces.",
        commonMove = "bxc6?!", bestMove = "dxc6",
        bestLine = "e4 e5 Nf3 Nc6 Bb5 a6 Bxc6 dxc6 O-O",
        commonLine = "e4 e5 Nf3 Nc6 Bb5 a6 Bxc6 bxc6 O-O",
        why = listOf(
            "In the Exchange Ruy, both …dxc6 and …bxc6 are legal — but they lead to very different games.",
            "…dxc6! opens the d-file for your queen and, crucially, frees the c8-bishop; your structure stays healthy.",
            "…bxc6?! leaves the light-squared bishop hemmed in and a clumsy queenside majority far from the centre.",
            "Default rule: recapture toward the centre unless there is a concrete reason not to.",
        ),
    ),
    Lesson(
        id = "damiano", title = "Never defend e5 with …f6", group = "Opening traps", forWhom = "Black",
        summary = "Propping up the e5-pawn with …f6 opens the diagonal to your king — the Damiano disaster.",
        commonMove = "f6??", bestMove = "Nc6",
        bestLine = "e4 e5 Nf3 Nc6 Bc4 Bc5",
        commonLine = "e4 e5 Nf3 f6 Nxe5 fxe5 Qh5+ Ke7 Qxe5+",
        why = listOf(
            "Defending e5 with 2…f6?? is one of the oldest blunders: it weakens the a2–g8 diagonal and the king.",
            "White plays 3.Nxe5! fxe5 4.Qh5+ and after 4…Ke7 5.Qxe5+ regains the piece with a crushing attack.",
            "Defend the centre with pieces, not with the f-pawn — 2…Nc6 develops and holds e5 safely.",
            "Almost any early …f6 (or f3 for White) is suspect: it opens the door to your own king.",
        ),
    ),
    Lesson(
        id = "wandering-queen", title = "Don’t defend with the queen", group = "Principles", forWhom = "Black",
        summary = "Guarding e5 with the queen looks fine, but the queen becomes a target and you fall behind.",
        commonMove = "Qf6?!", bestMove = "Nc6",
        bestLine = "e4 e5 Nf3 Nc6 Bc4 Bc5",
        commonLine = "e4 e5 Nf3 Qf6 Bc4 Qg6 O-O",
        why = listOf(
            "The queen is your most valuable piece — do not use it to do a knight’s job in the opening.",
            "After 2…Qf6?! White just develops (Bc4, Nc3, d3) and every move gains time by nudging the queen around.",
            "It also blocks your own knight’s best square (f6), slowing your development further.",
            "Bring out knights and bishops first; the queen comes into play once the minor pieces are set.",
        ),
    ),
)
