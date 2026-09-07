package ke.co.brivont.boka.chess

/**
 * A compact, dependency-free chess engine (shared across Android & iOS).
 * Provides legal-move generation, make-move, check/checkmate/stalemate and SAN —
 * everything the app needs to play Coach offline, highlight legal moves and scrub
 * through a game. Squares are 0..63 with index = rank*8 + file (a1 = 0, h8 = 63).
 */

data class Move(val from: Int, val to: Int, val promo: Char? = null)

private val KNIGHT = arrayOf(
    intArrayOf(1, 2), intArrayOf(2, 1), intArrayOf(-1, 2), intArrayOf(-2, 1),
    intArrayOf(1, -2), intArrayOf(2, -1), intArrayOf(-1, -2), intArrayOf(-2, -1),
)
private val ORTHO = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
private val DIAG = arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1))

fun sq(file: Int, rank: Int) = rank * 8 + file
fun fileOf(s: Int) = s % 8
fun rankOf(s: Int) = s / 8
fun squareName(s: Int): String = "${'a' + (s % 8)}${(s / 8) + 1}"
fun nameToSquare(n: String): Int = (n[1] - '1') * 8 + (n[0] - 'a')
private fun isWhitePiece(c: Char) = c in 'A'..'Z'

class Position(
    val board: CharArray,
    val whiteToMove: Boolean,
    val castling: String,
    val ep: Int,
    val halfmove: Int,
    val fullmove: Int,
) {
    companion object {
        const val START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        fun start() = fromFen(START)
        fun fromFen(fen: String): Position {
            val p = fen.trim().split(" ")
            val rows = p[0].split("/")
            val b = CharArray(64) { ' ' }
            for (r in 0..7) {
                var f = 0
                for (ch in rows.getOrElse(r) { "8" }) {
                    if (ch.isDigit()) f += ch - '0' else { if (f in 0..7) b[(7 - r) * 8 + f] = ch; f++ }
                }
            }
            val white = (p.getOrNull(1) ?: "w") == "w"
            val cast = (p.getOrNull(2) ?: "-").let { if (it == "-") "" else it }
            val epStr = p.getOrNull(3) ?: "-"
            val ep = if (epStr == "-" || epStr.length < 2) -1 else nameToSquare(epStr)
            return Position(b, white, cast, ep, p.getOrNull(4)?.toIntOrNull() ?: 0, p.getOrNull(5)?.toIntOrNull() ?: 1)
        }
    }

    fun toFen(): String {
        val sb = StringBuilder()
        for (r in 7 downTo 0) {
            var empty = 0
            for (f in 0..7) {
                val c = board[r * 8 + f]
                if (c == ' ') empty++ else { if (empty > 0) { sb.append(empty); empty = 0 }; sb.append(c) }
            }
            if (empty > 0) sb.append(empty)
            if (r > 0) sb.append('/')
        }
        sb.append(' ').append(if (whiteToMove) 'w' else 'b')
        sb.append(' ').append(if (castling.isEmpty()) "-" else castling)
        sb.append(' ').append(if (ep < 0) "-" else squareName(ep))
        sb.append(' ').append(halfmove).append(' ').append(fullmove)
        return sb.toString()
    }

    fun kingSquare(white: Boolean): Int {
        val k = if (white) 'K' else 'k'
        for (i in 0..63) if (board[i] == k) return i
        return -1
    }

    /** Is [square] attacked by the side [byWhite]? */
    fun isAttacked(square: Int, byWhite: Boolean): Boolean {
        val f0 = fileOf(square); val r0 = rankOf(square)
        // pawn: a white pawn attacking `square` sits one rank below it
        val pawn = if (byWhite) 'P' else 'p'
        val pr = r0 + (if (byWhite) -1 else 1)
        for (df in intArrayOf(-1, 1)) {
            val f = f0 + df
            if (f in 0..7 && pr in 0..7 && board[pr * 8 + f] == pawn) return true
        }
        val kn = if (byWhite) 'N' else 'n'
        for (o in KNIGHT) { val f = f0 + o[0]; val r = r0 + o[1]; if (f in 0..7 && r in 0..7 && board[r * 8 + f] == kn) return true }
        val kg = if (byWhite) 'K' else 'k'
        for (df in -1..1) for (dr in -1..1) { if (df == 0 && dr == 0) continue; val f = f0 + df; val r = r0 + dr; if (f in 0..7 && r in 0..7 && board[r * 8 + f] == kg) return true }
        val rq = if (byWhite) "RQ" else "rq"
        for (d in ORTHO) { var f = f0 + d[0]; var r = r0 + d[1]; while (f in 0..7 && r in 0..7) { val c = board[r * 8 + f]; if (c != ' ') { if (c in rq) return true; break }; f += d[0]; r += d[1] } }
        val bq = if (byWhite) "BQ" else "bq"
        for (d in DIAG) { var f = f0 + d[0]; var r = r0 + d[1]; while (f in 0..7 && r in 0..7) { val c = board[r * 8 + f]; if (c != ' ') { if (c in bq) return true; break }; f += d[0]; r += d[1] } }
        return false
    }

    fun inCheck(white: Boolean = whiteToMove): Boolean {
        val ks = kingSquare(white); return ks >= 0 && isAttacked(ks, !white)
    }

    private fun addPawnMove(list: MutableList<Move>, from: Int, to: Int, promo: Boolean) {
        if (promo) for (p in charArrayOf('q', 'r', 'b', 'n')) list.add(Move(from, to, p))
        else list.add(Move(from, to))
    }

    private fun slide(list: MutableList<Move>, from: Int, dirs: Array<IntArray>, white: Boolean) {
        val f0 = fileOf(from); val r0 = rankOf(from)
        for (d in dirs) {
            var f = f0 + d[0]; var r = r0 + d[1]
            while (f in 0..7 && r in 0..7) {
                val t = r * 8 + f; val c = board[t]
                if (c == ' ') list.add(Move(from, t))
                else { if (isWhitePiece(c) != white) list.add(Move(from, t)); break }
                f += d[0]; r += d[1]
            }
        }
    }

    private fun pseudoMoves(): MutableList<Move> {
        val m = ArrayList<Move>(48)
        val white = whiteToMove
        for (s in 0..63) {
            val c = board[s]; if (c == ' ' || isWhitePiece(c) != white) continue
            val f = fileOf(s); val r = rankOf(s)
            when (c.lowercaseChar()) {
                'p' -> {
                    val dir = if (white) 1 else -1
                    val startRank = if (white) 1 else 6
                    val promoRank = if (white) 7 else 0
                    val r1 = r + dir
                    if (r1 in 0..7 && board[r1 * 8 + f] == ' ') {
                        addPawnMove(m, s, r1 * 8 + f, r1 == promoRank)
                        if (r == startRank && board[(r + 2 * dir) * 8 + f] == ' ') m.add(Move(s, (r + 2 * dir) * 8 + f))
                    }
                    for (df in intArrayOf(-1, 1)) {
                        val cf = f + df; val cr = r + dir
                        if (cf in 0..7 && cr in 0..7) {
                            val t = cr * 8 + cf; val tc = board[t]
                            if (tc != ' ' && isWhitePiece(tc) != white) addPawnMove(m, s, t, cr == promoRank)
                            else if (t == ep) m.add(Move(s, t))
                        }
                    }
                }
                'n' -> for (o in KNIGHT) { val cf = f + o[0]; val cr = r + o[1]; if (cf in 0..7 && cr in 0..7) { val t = cr * 8 + cf; val tc = board[t]; if (tc == ' ' || isWhitePiece(tc) != white) m.add(Move(s, t)) } }
                'b' -> slide(m, s, DIAG, white)
                'r' -> slide(m, s, ORTHO, white)
                'q' -> { slide(m, s, ORTHO, white); slide(m, s, DIAG, white) }
                'k' -> {
                    for (df in -1..1) for (dr in -1..1) { if (df == 0 && dr == 0) continue; val cf = f + df; val cr = r + dr; if (cf in 0..7 && cr in 0..7) { val t = cr * 8 + cf; val tc = board[t]; if (tc == ' ' || isWhitePiece(tc) != white) m.add(Move(s, t)) } }
                    // castling
                    val rk = if (white) 0 else 7
                    if (s == sq(4, rk) && !inCheck(white)) {
                        val kSide = if (white) 'K' else 'k'; val qSide = if (white) 'Q' else 'q'
                        if (castling.contains(kSide) && board[sq(5, rk)] == ' ' && board[sq(6, rk)] == ' ' &&
                            !isAttacked(sq(5, rk), !white) && !isAttacked(sq(6, rk), !white)) m.add(Move(s, sq(6, rk)))
                        if (castling.contains(qSide) && board[sq(3, rk)] == ' ' && board[sq(2, rk)] == ' ' && board[sq(1, rk)] == ' ' &&
                            !isAttacked(sq(3, rk), !white) && !isAttacked(sq(2, rk), !white)) m.add(Move(s, sq(2, rk)))
                    }
                }
            }
        }
        return m
    }

    fun legalMoves(): List<Move> = pseudoMoves().filter { mv ->
        val next = makeRaw(mv)
        !next.isAttacked(next.kingSquare(whiteToMove), !whiteToMove)
    }

    /** Apply without legality check (used internally). Caller guarantees pseudo-legality. */
    private fun makeRaw(m: Move): Position {
        val b = board.copyOf()
        val piece = b[m.from]
        val white = isWhitePiece(piece)
        val lower = piece.lowercaseChar()
        var newEp = -1
        var cast = castling
        val isPawn = lower == 'p'
        val capture = b[m.to] != ' ' || (isPawn && m.to == ep)

        // en passant capture removes the passed pawn
        if (isPawn && m.to == ep && ep >= 0) {
            val capRank = rankOf(m.from)
            b[rankOf(m.to).let { capRank } * 8 + fileOf(m.to)] = ' '
        }
        // move the piece
        b[m.to] = piece
        b[m.from] = ' '
        // promotion
        if (m.promo != null) b[m.to] = if (white) m.promo.uppercaseChar() else m.promo
        // double pawn push sets ep target
        if (isPawn && kotlin.math.abs(rankOf(m.to) - rankOf(m.from)) == 2) newEp = (rankOf(m.from) + rankOf(m.to)) / 2 * 8 + fileOf(m.from)
        // castling: move the rook
        if (lower == 'k' && kotlin.math.abs(fileOf(m.to) - fileOf(m.from)) == 2) {
            val rk = rankOf(m.from)
            if (fileOf(m.to) == 6) { b[sq(5, rk)] = b[sq(7, rk)]; b[sq(7, rk)] = ' ' }
            else { b[sq(3, rk)] = b[sq(0, rk)]; b[sq(0, rk)] = ' ' }
        }
        // update castling rights
        if (lower == 'k') cast = cast.filter { if (white) it != 'K' && it != 'Q' else it != 'k' && it != 'q' }
        fun dropRookRight(square: Int) {
            cast = when (square) {
                sq(0, 0) -> cast.replace("Q", ""); sq(7, 0) -> cast.replace("K", "")
                sq(0, 7) -> cast.replace("q", ""); sq(7, 7) -> cast.replace("k", "")
                else -> cast
            }
        }
        dropRookRight(m.from); dropRookRight(m.to) // rook moved or was captured

        val newHalf = if (isPawn || capture) 0 else halfmove + 1
        val newFull = if (white) fullmove else fullmove + 1
        return Position(b, !white, cast, newEp, newHalf, newFull)
    }

    /** Apply a move if legal; returns the new position or null. */
    fun makeMove(from: Int, to: Int, promo: Char? = null): Position? {
        val legal = legalMoves().firstOrNull { it.from == from && it.to == to && (promo == null || it.promo == promo || it.promo == null) }
            ?: return null
        // if a promotion is needed but none given, default to queen
        val eff = if (legal.promo != null && promo == null) legal.copy(promo = 'q') else legal.copy(promo = promo ?: legal.promo)
        return makeRaw(eff)
    }

    fun isCheckmate() = inCheck() && legalMoves().isEmpty()
    fun isStalemate() = !inCheck() && legalMoves().isEmpty()
    fun isGameOver() = legalMoves().isEmpty() || halfmove >= 100 || insufficientMaterial()

    private fun insufficientMaterial(): Boolean {
        val pieces = board.filter { it != ' ' }
        if (pieces.size <= 2) return true // K vs K
        if (pieces.size == 3 && pieces.any { it.lowercaseChar() == 'b' || it.lowercaseChar() == 'n' }) return true
        return false
    }

    /** SAN for a legal move (with disambiguation, capture, check/mate) — for the move list. */
    fun sanOf(m: Move): String {
        val piece = board[m.from]
        val lower = piece.lowercaseChar()
        if (lower == 'k' && kotlin.math.abs(fileOf(m.to) - fileOf(m.from)) == 2)
            return (if (fileOf(m.to) == 6) "O-O" else "O-O-O") + checkSuffix(m)
        val sb = StringBuilder()
        val capture = board[m.to] != ' ' || (lower == 'p' && m.to == ep)
        if (lower == 'p') {
            if (capture) sb.append('a' + fileOf(m.from)).append('x')
            sb.append(squareName(m.to))
            if (m.promo != null) sb.append('=').append(m.promo.uppercaseChar())
        } else {
            sb.append(piece.uppercaseChar())
            // disambiguation
            val others = legalMoves().filter { it.to == m.to && it.from != m.from && board[it.from] == piece }
            if (others.isNotEmpty()) {
                val sameFile = others.any { fileOf(it.from) == fileOf(m.from) }
                val sameRank = others.any { rankOf(it.from) == rankOf(m.from) }
                if (!sameFile) sb.append('a' + fileOf(m.from))
                else if (!sameRank) sb.append('1' + rankOf(m.from))
                else sb.append('a' + fileOf(m.from)).append('1' + rankOf(m.from))
            }
            if (capture) sb.append('x')
            sb.append(squareName(m.to))
        }
        return sb.toString() + checkSuffix(m)
    }

    private fun checkSuffix(m: Move): String {
        val next = makeRaw(m)
        return when {
            next.inCheck(next.whiteToMove) && next.legalMoves().isEmpty() -> "#"
            next.inCheck(next.whiteToMove) -> "+"
            else -> ""
        }
    }
}
