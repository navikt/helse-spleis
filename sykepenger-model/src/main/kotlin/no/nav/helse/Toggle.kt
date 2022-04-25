package no.nav.helse

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
    private val states = ThreadLocal.withInitial { mutableListOf(enabled) }

    /**
     *
     */
    val enabled get() = states.get().last()

    /**
     *
     */
    val disabled get() = !enabled

    /**
     * Brukes sammen med [pop] {@link Toggle#pop() pop()}
     * @link Toggle
     * @return Unit
     */
    private fun enable() {
        if (force) return
        states.get().add(true)
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
    private fun disable() {
        if (force) return
        states.get().add(false)
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
        if (states.get().size == 1) return
        states.get().removeLast()
    }

    fun threadLocal() = states

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

    object GraphQLPlayground : Toggle("GraphQLPlayground", false)
    object SendFeriepengeOppdrag : Toggle(false)
    object RevurdereInntektMedFlereArbeidsgivere : Toggle(false)
    object GjenopptaAvsluttetUtenUtbetaling : Toggle(false)
    object ForkastForlengelseAvForkastetPeriode : Toggle(false)
    object NyTilstandsflyt : Toggle("NY_TILSTANDSFLYT",false)
    object NyRevurdering : Toggle(false)
    object RevurdereOutOfOrder : Toggle(false)
}
