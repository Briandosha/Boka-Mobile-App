package ke.co.brivont.boka.data

/**
 * Original chess THEORY (ported 1:1 from the web app's data/theory.ts): the
 * general principles that explain WHY moves are good, independent of any one
 * opening. Each principle is illustrated by a short line played FROM THE START
 * that reaches a position where the idea is visible. Lines are legality-checked
 * on the web side (scripts/validateOpenings.mjs). Reuses the Middlegame shape
 * so the Learn screen renders it unchanged. Our own explanations — no
 * third-party text.
 */
val THEORY: List<Middlegame> = listOf(
    Middlegame(
        id = "tempo", name = "Tempo — time is a resource", theme = "Time",
        summary = "A tempo is one useful move. You gain one when the opponent must spend a move undoing or defending; you lose one when you move a piece twice for nothing.",
        ideas = listOf(
            "Develop with threats or purpose so every move also does something else — that is how you get \"ahead in development\".",
            "A wasted move is not neutral: the opponent gets a free move to improve, attack, or castle first.",
            "Chasing pieces with pawns often LOSES tempi — the piece steps to a better square while your pawn is now committed.",
            "In open positions, being two or three tempi ahead is often enough to justify a sacrifice; in closed ones tempi matter less.",
        ),
        line = "e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7 Re1 b5 Bb3 d6 c3 O-O h3",
    ),
    Middlegame(
        id = "space", name = "Space — room to breathe", theme = "Space",
        summary = "Space is the territory your pawns claim. More space means more squares for your pieces and fewer for the opponent’s.",
        ideas = listOf(
            "An advanced pawn chain (like e5 here) cramps the enemy pieces: knights lose their best squares and bishops lose diagonals.",
            "The side with less space must FIGHT for it: play the pawn break that challenges the chain (…c5 and later …f6 in this structure).",
            "Space is only an advantage if you can use it — over-extended pawns become targets if the pieces behind them fall behind.",
            "When cramped, trade pieces: fewer pieces need fewer squares, and the space advantage shrinks in the endgame.",
        ),
        line = "e4 c6 d4 d5 e5 Bf5 Nf3 e6 Be2 c5 Be3 Nc6 O-O",
    ),
    Middlegame(
        id = "outpost", name = "Weak squares & outposts", theme = "Squares",
        summary = "A weak square is one the opponent can no longer guard with a pawn. Park a piece there — an outpost — and it works for the rest of the game.",
        ideas = listOf(
            "Knights love outposts: a knight on d5 here cannot be driven away by a pawn, so it dominates the centre indefinitely.",
            "Weak squares are created by pawn moves — every pawn advance gives up control of the squares it used to guard.",
            "To fight an outpost, trade the piece sitting on it or undermine the pawn that supports it.",
            "Before pushing a pawn, ask which squares it stops protecting; that is often the real cost of the move.",
        ),
        line = "e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 Nf6 Nc3 e5 Ndb5 d6 Bg5 a6 Na3 b5 Nd5",
    ),
    Middlegame(
        id = "open-files", name = "Open files & the seventh rank", theme = "Rooks",
        summary = "Rooks need open lines. Put them on files without pawns, and aim to reach the seventh rank, where they attack pawns from behind and box in the king.",
        ideas = listOf(
            "When a pawn exchange opens a file, the first rook to occupy it usually controls it — and the player who controls it controls entry to the enemy camp.",
            "Doubling rooks on a file turns control into penetration.",
            "A rook on the seventh rank hits every pawn that is still on its starting square and often restricts the king to the back rank.",
            "If you cannot win the file, contest it: put your own rook opposite so the opponent cannot penetrate for free.",
        ),
        line = "e4 e6 d4 d5 exd5 exd5 Nf3 Nf6 Bd3 Bd6 O-O O-O Re1 Re8",
    ),
    Middlegame(
        id = "king-safety", name = "King safety comes first", theme = "King",
        summary = "Material and activity mean nothing if your king is caught in the open. Castle early, keep pawn cover, and be wary of opening lines near your own king.",
        ideas = listOf(
            "Here Black grabbed a pawn and had the king dragged to e6 — every White piece now joins the hunt with tempo.",
            "Castling is not just about the king: it also connects the rooks, which is why delaying it costs development too.",
            "Do not move the pawns in front of your castled king without a concrete reason — each one is a brick in the wall.",
            "When the enemy king is exposed, open lines toward it even at material cost; when yours is, trade off the attackers first.",
        ),
        line = "e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5 d5 exd5 Nxd5 Nxf7 Kxf7 Qf3+ Ke6",
    ),
    Middlegame(
        id = "worst-piece", name = "Improve your worst piece", theme = "Activity",
        summary = "When there is no tactic, find the piece doing the least and give it a better job. Positions are won by the sum of piece activity, not by one hero.",
        ideas = listOf(
            "White’s f3-knight was blocked by its own pawns, so it re-routes Nf3–e1–d3, where it supports both c4-c5 and f2-f4.",
            "Ask of every piece: what is it attacking, what is it defending, and can it do more from another square?",
            "A knight on the rim or a bishop staring at its own pawns is a \"bad piece\" — fixing it is worth several tempi.",
            "Regrouping quietly before the pawn break is what separates a plan from a lunge.",
        ),
        line = "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e5 O-O Nc6 d5 Ne7 Ne1 Nd7 Nd3 f5",
    ),
    Middlegame(
        id = "prophylaxis", name = "Prophylaxis — stop the plan first", theme = "Thinking",
        summary = "Before improving your own position, ask what the opponent WANTS to do and take it away. A quiet preventive move can be stronger than any attack.",
        ideas = listOf(
            "White’s early a3 spends a tempo purely to deny Black the …Bb4 pin — the move Black’s whole setup was built around.",
            "The question to ask every move: \"If I pass, what does my opponent play?\" Prevent that, then pursue your own ideas.",
            "Prophylaxis is not passivity: the best preventive moves also improve your position (a3 later supports b2-b4).",
            "Strong players often lose fewer games not because they attack better but because they allow fewer counter-plans.",
        ),
        line = "d4 Nf6 c4 e6 Nf3 b6 a3 Bb7 Nc3 d5 cxd5 Nxd5 e3 Be7 Bb5+ c6 Bd3 O-O O-O",
    ),
    Middlegame(
        id = "good-bad-bishop", name = "Good bishop, bad bishop", theme = "Pieces",
        summary = "A bishop is only as good as its diagonals. If your own pawns sit on its colour, it is \"bad\"; if they sit on the other colour, it is \"good\".",
        ideas = listOf(
            "In the French structure Black’s pawns on e6 and d5 sit on light squares, so the light-squared bishop is walled in — the classic bad bishop.",
            "The fix for a bad bishop: trade it, re-route it outside the chain, or change the pawn structure so the diagonal opens.",
            "A bad bishop can still be a good DEFENDER: it guards exactly the squares your pawns leave weak.",
            "When you have the good bishop, keep the pawn structure fixed — the longer the position stays closed on those colours, the bigger your edge.",
        ),
        line = "e4 e6 d4 d5 e5 c5 c3 Nc6 Nf3 Qb6 a3 c4 Nbd2 Na5 Be2 Bd7 O-O Ne7",
    ),
    Middlegame(
        id = "two-weaknesses", name = "The principle of two weaknesses", theme = "Planning",
        summary = "One weakness can usually be defended. Create a second one and the defender must choose — pieces cannot be in two places at once.",
        ideas = listOf(
            "Here White’s minority attack (b4-b5) manufactures a weak c6 pawn; later, pressure on the kingside gives Black a second front to worry about.",
            "Attack the first weakness until the defenders are tied to it, then switch to the second — the switch itself is the winning move.",
            "Weaknesses need not be pawns: a bad king, a loose piece, or a colour complex all count.",
            "If you only have one target, do not rush to cash it in; first fix it in place and look for the second.",
        ),
        line = "d4 d5 c4 e6 Nc3 Nf6 cxd5 exd5 Bg5 Be7 e3 c6 Bd3 Nbd7 Qc2 O-O Nf3 Re8 O-O Nf8 Rab1 a5 a3 Ng6 b4 axb4 axb4",
    ),
)
