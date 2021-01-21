package no.nav.helse

sealed class Toggles(enabled: Boolean = false, private val force: Boolean = false) {
    private val states = mutableListOf(enabled)

    val enabled get() = states.last()

    fun enable() {
        if (force) return
        states.add(true)
    }

    fun disable() {
        if (force) return
        states.add(false)
    }

    fun pop() {
        if (states.size == 1) return
        states.removeLast()
    }

    fun enable(block: () -> Unit) {
        enable()
        runWith(block)
    }

    fun disable(block: () -> Unit) {
        disable()
        runWith(block)
    }

    private fun runWith(block: () -> Unit) {
        try {
            block()
        } finally {
            pop()
        }
    }

    object SpeilInntekterVol2Enabled : Toggles(true)
    object NyInntekt : Toggles(true)
    object ReplayEnabled : Toggles()
    object FlereArbeidsgivereOvergangITEnabled : Toggles()
    object KorrigertSøknadToggle : Toggles(System.getenv().getOrDefault("KORRIGERT_SOKNAD_TOGGLE", "false").toBoolean())
}
