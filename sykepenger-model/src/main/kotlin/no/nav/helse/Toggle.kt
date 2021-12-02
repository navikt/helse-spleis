package no.nav.helse

import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Utbetaling

/**
 * En toggle opprettes ved å angi initiell tilstand, og alternativt om tilstanden skal låses ved å angi [force]
 *
 * Tilstanden til togglen kan endres ved kall til [enable] eller [disable], så fremt [force] ikke har verdien `true`
 *
 * For å tvinge togglen til å alltid være i initiell tilstand kan [force] settes til `true`
 * Dette kan brukes for å finne hvilke tester som er avhengige av en gitt tilstand på togglen, og sideeffekten av å skru på en toggle kommer tydelig frem
 *
 * **Merk: Hvis tilstanden endres ved kall til [enable] eller [disable] uten `block`, så må tilstanden senere tilbakestilles ved kall til [pop]**
 *
 * @param[enabled] Initiell tilstand
 * @param[force] Om tilstanden til toggelen skal låses
 */
abstract class Toggle internal constructor(enabled: Boolean = false, private val force: Boolean = false) {
    /**
     * Den andre konstruktøren
     *
     * @param[key]
     * @param[default]
     *
     * @see Toggle
     */
    private constructor(key: String, default: Boolean = false) : this(System.getenv()[key]?.toBoolean() ?: default)

    /**
     *
     */
    private val states = mutableListOf(enabled)

    /**
     *
     */
    val enabled get() = states.last()

    /**
     *
     */
    val disabled get() = !enabled

    /**
     * Brukes sammen med [pop] {@link Toggles#pop() pop()}
     * statically bound {@link ILoggerFactory} instance
     * @link Toggles
     * @return Unit
     */
    fun enable() {
        if (force) return
        states.add(true)
    }

    /**
     * Denne bruker ikke [pop]
     */
    fun enable(block: () -> Unit) {
        enable()
        runWith(block)
    }

    /**
     *
     */
    fun disable() {
        if (force) return
        states.add(false)
    }

    /**
     *
     */
    fun disable(block: () -> Unit) {
        disable()
        runWith(block)
    }

    /**
     * Brukes av [enable]
     */
    fun pop() {
        if (states.size == 1) return
        states.removeLast()
    }

    private fun runWith(block: () -> Unit) {
        try {
            block()
        } finally {
            pop()
        }
    }

    /**
     *
     */
    internal operator fun plus(toggle: Toggle) = listOf(this, toggle)

    companion object {
        /**
         *
         */
        fun Iterable<Toggle>.enable(block: () -> Unit) {
            forEach(Toggle::enable)
            try {
                block()
            } finally {
                forEach(Toggle::pop)
            }
        }

        /**
         *
         */
        fun Iterable<Toggle>.disable(block: () -> Unit) {
            forEach(Toggle::disable)
            try {
                block()
            } finally {
                forEach(Toggle::pop)
            }
        }
    }

    object RebregnUtbetalingVedHistorikkendring : Toggle()
    object OverlappendeSykmelding : Toggle(true)
    object SendFeriepengeOppdrag : Toggle(false)
    object DatoRangeJson : Toggle(true)
    object Etterlevelse : Toggle()
    object SpeilApiV2 : Toggle("SPEIL_API_V2")
    object GraphQLPlayground : Toggle("GraphQLPlayground")
    object RevurdereInntektMedFlereArbeidsgivere : Toggle(false)
    object DelvisRefusjon : Toggle("DELVIS_REFUSJON",false)

    object LageBrukerutbetaling : Toggle("LAGE_BRUKERUTBETALING") {
        internal fun kanIkkeFortsette(aktivitetslogg: IAktivitetslogg, utbetaling: Utbetaling, harBrukerutbetaling: Boolean): Boolean {
            if (DelvisRefusjon.disabled && utbetaling.harDelvisRefusjon()) aktivitetslogg.error("Utbetalingen har endringer i både arbeidsgiver- og personoppdrag")
            else if (disabled && harBrukerutbetaling) aktivitetslogg.error("Utbetalingstidslinje inneholder brukerutbetaling")
            else if (disabled && utbetaling.harBrukerutbetaling()) aktivitetslogg.error("Utbetaling inneholder brukerutbetaling (men ikke for den aktuelle vedtaksperioden)")
            return aktivitetslogg.hasErrorsOrWorse()
        }
    }
}
