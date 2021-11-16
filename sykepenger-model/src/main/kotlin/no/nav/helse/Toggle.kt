package no.nav.helse

import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Utbetaling

abstract class Toggle internal constructor(enabled: Boolean = false, private val force: Boolean = false) {
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

    internal operator fun plus(toggle: Toggle) = listOf(this, toggle)

    internal companion object {
        internal fun Iterable<Toggle>.enable(block: () -> Unit) {
            forEach(Toggle::enable)
            try {
                block()
            } finally {
                forEach(Toggle::pop)
            }
        }

        internal fun Iterable<Toggle>.disable(block: () -> Unit) {
            forEach(Toggle::disable)
            try {
                block()
            } finally {
                forEach(Toggle::pop)
            }
        }
    }

    object OppretteVedtaksperioderVedSøknad : Toggle(true)
    object RebregnUtbetalingVedHistorikkendring : Toggle()
    object OverlappendeSykmelding : Toggle(true)
    object SendFeriepengeOppdrag : Toggle(false)
    object DatoRangeJson : Toggle(true)
    object Etterlevelse : Toggle()
    object SpeilApiV2 : Toggle("SPEIL_API_V2")
    object GraphQLPlayground : Toggle("GraphQLPlayground")
    object RevurdereInntektMedFlereArbeidsgivere : Toggle(false)

    internal object LageBrukerutbetaling : Toggle("LAGE_BRUKERUTBETALING") {
        fun kanIkkeFortsette(aktivitetslogg: IAktivitetslogg, utbetaling: Utbetaling, harBrukerutbetaling: Boolean): Boolean {
            if (disabled && harBrukerutbetaling) aktivitetslogg.error("Utbetalingstidslinje inneholder brukerutbetaling")
            else if (enabled && utbetaling.harDelvisRefusjon()) aktivitetslogg.error("Støtter ikke brukerutbetaling med delvis refusjon")
            return aktivitetslogg.hasErrorsOrWorse()
        }
    }
}
