package no.nav.helse

sealed class Toggles(var enabled: Boolean = false) {
    private fun runWith(enable: Boolean, block: () -> Unit) {
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

    object SpeilInntekterVol2Enabled : Toggles()
    object NyInntekt : Toggles()
    object ReplayEnabled : Toggles()
    object FlereArbeidsgivereEnabled : Toggles()
}
