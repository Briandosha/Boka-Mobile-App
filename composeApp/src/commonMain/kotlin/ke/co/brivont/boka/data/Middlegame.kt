package ke.co.brivont.boka.data

/**
 * Original middlegame theory (ported from the web app's data/middlegame.ts).
 * Each theme is shown by a short line played FROM THE START that reaches the
 * characteristic structure, plus the plans for both sides. Lines are legality-
 * checked on the web side. Our own explanations — no third-party text.
 */
data class Middlegame(
    val id: String,
    val name: String,
    /** Theme family, shown as a chip. */
    val theme: String,
    val summary: String,
    val ideas: List<String>,
    /** SAN moves from the start position reaching the thematic structure. */
    val line: String,
)

val MIDDLEGAME: List<Middlegame> = listOf(
    Middlegame(
        id = "iqp", name = "The Isolated Queen’s Pawn (IQP)", theme = "Pawn structure",
        summary = "A lone d-pawn with no neighbours on the c- and e-files: a dynamic asset in the middlegame, a target in the endgame.",
        ideas = listOf(
            "The side WITH the IQP wants pieces and attack: the d-pawn grants the e5 and c5 outposts and open c/e files.",
            "Typical plan: pile up for a d4-d5 break that frees the position and opens lines toward the king.",
            "The side AGAINST the IQP blockades the pawn (a knight on d5/d4), trades pieces, and heads for the endgame.",
            "Rule of thumb: trade pieces and the isolani becomes weak; keep pieces on and it becomes strong.",
        ),
        line = "d4 d5 c4 e6 Nc3 c5 cxd5 exd5 Nf3 Nc6 g3 Nf6 Bg2 Be7 O-O O-O",
    ),
    Middlegame(
        id = "carlsbad-minority", name = "Carlsbad & the Minority Attack", theme = "Pawn structure",
        summary = "The Exchange-QGD structure, where White’s a/b pawn majority marches to create a lasting weakness on c6.",
        ideas = listOf(
            "White’s minority attack: push b4-b5xc6 so Black is left with a backward c6 pawn on a half-open file.",
            "White then triples on the c-file and wins the c6 pawn, or ties Black to its defence.",
            "Black counters with ...Ne4 and a kingside expansion (…f5), playing for an attack while White works the queenside.",
            "Structure, not material, is the battleground — both sides play for a long-term pawn weakness.",
        ),
        line = "d4 d5 c4 e6 Nc3 Nf6 cxd5 exd5 Bg5 c6 e3 Be7 Qc2 O-O Bd3 Nbd7 Nf3 Re8 O-O Nf8",
    ),
    Middlegame(
        id = "outpost", name = "The Outpost", theme = "Piece play",
        summary = "A square the enemy can no longer defend with a pawn — plant a knight there and it dominates.",
        ideas = listOf(
            "An outpost is a hole in the opponent’s camp: a square no enemy pawn can ever attack.",
            "A knight on a central outpost (here d5) is worth far more than a passive bishop — often decisive.",
            "Support the outpost piece with a pawn or a second piece so it can never be dislodged.",
            "To fight an outpost, arrange to trade off the piece that occupies it, even at the cost of a bishop.",
        ),
        line = "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 Nf6 Nc3 e5 Ndb5 d6 Nd5",
    ),
    Middlegame(
        id = "bad-bishop", name = "Good Bishop vs Bad Bishop", theme = "Piece play",
        summary = "A bishop hemmed in by its own fixed pawns is a “bad” bishop — plan the whole game around it.",
        ideas = listOf(
            "A bishop is “bad” when its own pawns are fixed on its colour (here Black’s light-squared bishop behind e6/d5).",
            "The side with the good bishop targets the colour complex the bad bishop cannot defend.",
            "The owner of the bad bishop should trade it off, or free it with a pawn break (…f6 or …c5 in the French).",
            "A good knight routinely beats a bad bishop — activity and colour control decide, not just the name of the piece.",
        ),
        line = "e4 e6 d4 d5 e5 c5 c3 Nc6 Nf3 Qb6 a3 Bd7",
    ),
    Middlegame(
        id = "opposite-castle-attack", name = "Opposite-Side Castling: the Pawn Storm", theme = "King attack",
        summary = "When the kings castle on opposite wings, both sides throw pawns at the enemy king — it is a race.",
        ideas = listOf(
            "Storm the pawns in front of the ENEMY king (here White plays g4-h4-h5), not your own.",
            "Do not fear loosening your king’s pawns — your own pawns stay home; you attack with the far-wing pawns.",
            "Open a file against the king and the heavy pieces pour in — speed matters more than material.",
            "Whoever’s attack arrives first usually wins; count tempi and sacrifice to open lines.",
        ),
        line = "e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6 Be3 Bg7 f3 O-O Qd2 Nc6 O-O-O",
    ),
    Middlegame(
        id = "maroczy-bind", name = "Space & the Bind", theme = "Strategy",
        summary = "A big pawn centre (the Maróczy c4+e4 bind) cramps the opponent — squeeze before you strike.",
        ideas = listOf(
            "A space advantage means your pieces have more squares and the opponent’s trip over each other.",
            "The cramped side wants to trade pieces to get breathing room — so the space-holder AVOIDS trades.",
            "Restrain the freeing breaks (here …b5 and …d5) before improving your pieces to the max.",
            "Convert space into an attack on one wing once the opponent is fully passive.",
        ),
        line = "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 g6 c4 Bg7 Be3 Nf6 Nc3 O-O Be2 d6 O-O",
    ),
)
