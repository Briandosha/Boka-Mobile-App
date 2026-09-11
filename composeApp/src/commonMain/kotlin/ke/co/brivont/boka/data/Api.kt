package ke.co.brivont.boka.data

import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import ke.co.brivont.boka.core.Config
import ke.co.brivont.boka.core.Session
import ke.co.brivont.boka.core.accessTokenExpired
import ke.co.brivont.boka.core.httpClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private fun HttpRequestBuilder.bearer() {
    Session.accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
}

/** Run an authed request; on 401/403 refresh the token once and retry (mirrors the web). */
private suspend fun authed(call: suspend () -> HttpResponse): HttpResponse {
    var res = call()
    if (res.status == HttpStatusCode.Unauthorized || res.status == HttpStatusCode.Forbidden) {
        if (AuthApi.refresh()) res = call()
    }
    return res
}

/** Auth against the .NET processor (login / register / refresh). */
object AuthApi {
    suspend fun login(email: String, password: String): AuthResponse {
        val res = httpClient.post(Config.AUTH + "api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("email" to email, "password" to password))
        }.body<AuthResponse>()
        persist(res)
        return res
    }

    suspend fun register(email: String, password: String, fullName: String, userName: String): AuthResponse {
        val res = httpClient.post(Config.AUTH + "api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("email" to email, "password" to password, "FullName" to fullName, "UserName" to userName))
        }.body<AuthResponse>()
        persist(res)
        return res
    }

    /** Request a password-reset email. Backend always returns 200 (never reveals
     *  whether the address exists); the user completes the reset via the emailed link. */
    suspend fun forgotPassword(email: String): Boolean = try {
        httpClient.post(Config.AUTH + "api/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("email" to email))
        }.status.isSuccess()
    } catch (e: Throwable) { false }

    // Refresh tokens are ROTATED server-side: the first refresh invalidates the old
    // pair, so concurrent refreshes (socket connect + REST 401 retry) would burn a
    // valid token and log the user out. Single-flight everything through a mutex.
    private val refreshMutex = Mutex()

    /**
     * Exchange the (possibly expired) access token + refresh token for a fresh pair.
     * The .NET /api/auth/refresh needs BOTH and rotates the refresh token. Returns
     * true on success (new tokens persisted), false otherwise. Safe to call
     * concurrently — only one network refresh runs; late callers reuse its result.
     */
    suspend fun refresh(): Boolean = refreshMutex.withLock {
        val at = Session.accessToken
        val rt = Session.refreshToken
        // No tokens at all — nothing to refresh; the session is already dead.
        if (at.isNullOrBlank() || rt.isNullOrBlank()) { Session.expire(); return@withLock false }
        // If a refresh completed while we waited on the lock, the access token is
        // fresh again — don't spend the (single-use) refresh token a second time.
        if (!accessTokenExpired(skewSeconds = 5)) return@withLock true
        val res = try {
            httpClient.post(Config.AUTH + "api/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("accessToken" to at, "refreshToken" to rt))
            }
        } catch (e: Throwable) {
            // Network / transient failure — keep the session so a later retry
            // (reconnect, pull-to-refresh) can still succeed. Do NOT log out.
            return@withLock false
        }
        return@withLock when {
            res.status.isSuccess() -> {
                val body = runCatching { res.body<AuthResponse>() }.getOrNull()
                if (!body?.token.isNullOrBlank()) { persist(body!!); true } else false
            }
            // 400 invalid_refresh / invalid_token, or 401 refresh-token expired:
            // the server has definitively rejected us → force a clean logout.
            res.status == HttpStatusCode.BadRequest || res.status == HttpStatusCode.Unauthorized -> {
                Session.expire(); false
            }
            // 5xx and friends — treat as transient, keep the session.
            else -> false
        }
    }

    private fun persist(res: AuthResponse) {
        if (!res.token.isNullOrBlank()) {
            Session.accessToken = res.token
            if (!res.refreshToken.isNullOrBlank()) Session.refreshToken = res.refreshToken
        }
    }
}

/** Game / analysis REST (chess-api). */
object GameApi {
    suspend fun leaderboard(): LeaderboardResponse =
        authed { httpClient.get(Config.API + "/api/leaderboard") { bearer() } }.body()

    suspend fun history(): List<ApiGame> =
        authed { httpClient.get(Config.API + "/api/games/history") { bearer() } }.body()

    /** Enqueue a review; returns {jobId} (202) or {status:'done',report} / {status:'limit'} / 402. */
    suspend fun enqueueAnalysis(pgn: String): AnalyzeResponse =
        authed {
            httpClient.post(Config.API + "/api/analyze-full-game") {
                bearer()
                contentType(ContentType.Application.Json)
                setBody(AnalyzeRequest(pgn))
            }
        }.body()

    suspend fun pollAnalysis(jobId: String): AnalyzeResponse =
        authed { httpClient.get(Config.API + "/api/analyze-full-game/$jobId") { bearer() } }.body()
}

sealed class PuzzleResult {
    data class Loaded(val p: PuzzleDto) : PuzzleResult()
    object Limit : PuzzleResult()
    object Empty : PuzzleResult()
    object Error : PuzzleResult()
}

object PuzzleApi {
    private suspend fun fetch(path: String): PuzzleResult = try {
        val res = authed { httpClient.get(Config.API + path) { bearer() } }
        when {
            res.status.isSuccess() -> PuzzleResult.Loaded(res.body())
            res.status.value == 402 -> PuzzleResult.Limit
            res.status.value == 503 -> PuzzleResult.Empty
            else -> PuzzleResult.Error
        }
    } catch (e: Throwable) {
        PuzzleResult.Error
    }

    suspend fun next(): PuzzleResult = fetch("/api/puzzles/next")
    suspend fun daily(): PuzzleResult = fetch("/api/puzzles/daily")

    /** Record a Puzzle Rush score; the server keeps the personal best. */
    suspend fun rushResult(score: Int): RushResultDto? = try {
        authed {
            httpClient.post(Config.API + "/api/puzzles/rush-result") {
                bearer()
                contentType(ContentType.Application.Json)
                setBody(RushBody(score))
            }
        }.body()
    } catch (e: Throwable) {
        null
    }

    /** A batch near the user's rating (ascending difficulty) — used for the offline cache and Rush. */
    suspend fun batch(count: Int = 30): List<PuzzleDto>? = try {
        val res = authed { httpClient.get(Config.API + "/api/puzzles/batch?count=$count") { bearer() } }
        if (res.status.isSuccess()) res.body<PuzzleBatchDto>().puzzles else null
    } catch (e: Throwable) {
        null
    }

    suspend fun progress(): PuzzleProgressDto? = try {
        authed { httpClient.get(Config.API + "/api/puzzles/progress") { bearer() } }.body()
    } catch (e: Throwable) {
        null
    }

    suspend fun attempt(id: String, solved: Boolean): AttemptResultDto? = try {
        authed {
            httpClient.post(Config.API + "/api/puzzles/attempt") {
                bearer()
                contentType(ContentType.Application.Json)
                setBody(AttemptBody(id, solved))
            }
        }.body()
    } catch (e: Throwable) {
        null
    }
}

object InsightsApi {
    suspend fun insights(): InsightsDto? = try {
        authed { httpClient.get(Config.API + "/api/insights") { bearer() } }.body()
    } catch (e: Throwable) {
        null
    }
}
