package no.nav.helse

abstract class Toggles internal constructor(enabled: Boolean = false, private val force: Boolean = false) {
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    private val states = mutableListOf(enabled)

    val enabled get() = states.last()
    val disabled get() = !enabled

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

    internal operator fun plus(toggle: Toggles) = listOf(this, toggle)
    internal companion object {
        internal fun Iterable<Toggles>.enable(block: () -> Unit) {
            forEach(Toggles::enable)
            try {
                block()
            } finally {
                forEach(Toggles::pop)
            }
        }
    }

    object OppretteVedtaksperioderVedSøknad : Toggles(true)
    object RebregnUtbetalingVedHistorikkendring : Toggles()
    object OverlappendeSykmelding : Toggles(true)
    object SendFeriepengeOppdrag: Toggles(false)
    object DatoRangeJson : Toggles(true)
    object Etterlevelse: Toggles()
    object SpeilApiV2: Toggles("SPEIL_API_V2")
    object RefusjonPerDag: Toggles("REFUSJON_PER_DAG",true)
    object RevurdereInntektMedFlereArbeidsgivere: Toggles(false)
}
