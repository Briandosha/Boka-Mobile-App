package ke.co.brivont.boka

// iOS sound playback is deferred (bundle the WAVs into the iOS app target and
// play via AVAudioPlayer). No-op for now so the shared module builds cleanly.
actual fun playSfx(name: String) {
}
