package no.nav.helse

sealed class Toggles(enabled: Boolean = false, private val force: Boolean = false) {
    var enabled = enabled
        set(value) {
            if (!force) field = value
        }

    private fun runWith(enable: Boolean, block: () -> Unit) {
        if (force) return block()
        val prev = this.enabled
        this.enabled = enable
        try {
            block()
        } finally {
            this.enabled = prev
        }
    }

    fun enable(block: () -> Unit) = runWith(true, block)
    fun disable(block: () -> Unit) = runWith(false, block)

    object SpeilInntekterVol2Enabled : Toggles(true)
    object NyInntekt : Toggles(true)
    object ReplayEnabled : Toggles()
    object FlereArbeidsgivereEnabled : Toggles()
}
