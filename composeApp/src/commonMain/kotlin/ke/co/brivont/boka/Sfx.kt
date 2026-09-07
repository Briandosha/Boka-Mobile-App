package ke.co.brivont.boka

/**
 * Low-latency game sound effects. Names: "move", "capture", "check", "castle",
 * "promote", "notify". Backed by the project's own (non-proprietary) WAV clips —
 * we deliberately do NOT bundle any third-party/chess.com audio.
 */
expect fun playSfx(name: String)
