package ke.co.brivont.boka.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val email: String? = null,
    val name: String = "Player",
    val avatar: String? = null,
)

@Serializable
data class AuthResponse(
    val token: String? = null,
    val refreshToken: String? = null,
    val user: User? = null,
    val error: String? = null,
    val message: String? = null,
)

// ---- Leaderboard (GET /api/leaderboard) ----
@Serializable
data class LeaderRow(
    val rank: Int = 0,
    val username: String = "",
    val rating: Int = 0,
    val gamesPlayed: Int = 0,
    val isMe: Boolean = false,
)

@Serializable
data class MyRank(
    val rank: Int? = null,
    val rating: Int = 0,
    val gamesPlayed: Int = 0,
    val username: String = "",
)

@Serializable
data class LeaderboardResponse(
    val top: List<LeaderRow> = emptyList(),
    val me: MyRank? = null,
    val total: Int = 0,
)

// ---- My Games (GET /api/games/history) ----
@Serializable
data class PlayerRef(val username: String = "", val rating: Int = 0)

@Serializable
data class ApiGame(
    val id: String = "",
    val white_player_id: String = "",
    val black_player_id: String? = null,
    val pgn: String? = null,
    val winner_id: String? = null,
    val end_reason: String? = null,
    val time_control_initial: Int? = null,
    val time_control_increment: Int? = null,
    val ended_at: String? = null,
    val whitePlayer: PlayerRef? = null,
    val blackPlayer: PlayerRef? = null,
)

// ---- Analysis (POST/GET /api/analyze-full-game) ----
@Serializable
data class MoveEval(val score: Int = 0, val loss: Int = 0)

@Serializable
data class AnalysisMove(
    val moveIndex: Int = 0,
    val fen: String = "",
    val move_san: String = "",
    val move_lan: String = "",
    val color: String = "w",
    val best_move: String? = null,
    val classification: String = "book",
    val reason: String? = null,
    val evaluation: MoveEval = MoveEval(),
)

@Serializable
data class AnalyzeResponse(
    val status: String? = null,         // done | queued | running | limit | error (or absent on 202)
    val jobId: String? = null,
    val position: Int? = null,
    val report: List<AnalysisMove>? = null,
    val premium: Boolean? = null,
    val freeRemaining: Int? = null,
    val error: String? = null,
    val message: String? = null,
)

@Serializable
data class AnalyzeRequest(val pgn: String)

@Serializable
data class PuzzleDto(
    val id: String = "",
    val fen: String = "",
    val moves: String = "",
    val rating: Int = 1500,
    val themes: String = "",
    val puzzleRating: Int? = null,
    val streak: Int? = null,
    val solvedToday: Boolean? = null,
)

@Serializable
data class PuzzleProgressDto(
    val puzzleRating: Int = 1200,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val solved: Int = 0,
    val failed: Int = 0,
    val rushBest: Int = 0,
    val solvedToday: Boolean = false,
)

@Serializable
data class AttemptResultDto(
    val puzzleRating: Int = 1200,
    val delta: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val solved: Int = 0,
    val failed: Int = 0,
)

@Serializable
data class AttemptBody(val puzzleId: String, val solved: Boolean)
