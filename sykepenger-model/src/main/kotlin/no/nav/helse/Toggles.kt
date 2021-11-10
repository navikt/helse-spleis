package no.nav.helse

import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Utbetaling

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

    object OppretteVedtaksperioderVedSøknad : Toggles(false)
    object RebregnUtbetalingVedHistorikkendring : Toggles()
    object OverlappendeSykmelding : Toggles(true)
    object SendFeriepengeOppdrag: Toggles(false)
    object DatoRangeJson : Toggles(true)
    object Etterlevelse: Toggles()
    object SpeilApiV2: Toggles("SPEIL_API_V2")
    object RevurdereInntektMedFlereArbeidsgivere: Toggles(false)

    internal object LageBrukerutbetaling: Toggles(false) {
        fun kanIkkeFortsette(aktivitetslogg: IAktivitetslogg, utbetaling: Utbetaling, harBrukerutbetaling: Boolean): Boolean {
            if (disabled && harBrukerutbetaling) aktivitetslogg.error("Utbetalingstidslinje inneholder brukerutbetaling")
            else if (enabled && utbetaling.harDelvisRefusjon()) aktivitetslogg.error("Støtter ikke brukerutbetaling med delvis refusjon")
            return aktivitetslogg.hasErrorsOrWorse()
        }
    }
}
