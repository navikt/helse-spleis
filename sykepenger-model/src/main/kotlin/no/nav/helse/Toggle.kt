package no.nav.helse

class Toggle private constructor(enabled: Boolean) {
    private val states = ThreadLocal.withInitial { mutableListOf(enabled) }
    val enabled get() = states.get().last()
    val disabled get() = !enabled

    private fun enable() {
        states.get().add(true)
    }

    fun enable(block: () -> Unit) {
        enable()
        runWith(block)
    }

    private fun disable() {
        states.get().add(false)
    }

    fun disable(block: () -> Unit) {
        disable()
        runWith(block)
    }

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

    internal operator fun plus(toggle: Toggle) = listOf(this, toggle)

    companion object {
        val TilkommenArbeidsgiver = fraEnv("TILKOMMEN_ARBEIDSGIVER", false)
        val SendFeriepengeOppdrag = fraEnv("SEND_FERIEPENGEOPPDRAG", false) // Denne MÅ settes til false når man er ferdig å kjøre feriepenger. Ref. den mystiske feriepengejobben som startet av seg selv (?) 08.08.2024
        val EgenmeldingStrekkerIkkeSykdomstidslinje = fraEnv("EGENMELDING_STREKKER_IKKE_SYKDOMSTIDSLINJE", false)

        fun fraEnv(navn: String, defaultValue: Boolean) = Toggle(System.getenv(navn)?.lowercase()?.toBooleanStrictOrNull() ?: defaultValue)

        fun Iterable<Toggle>.enable(block: () -> Unit) {
            forEach(Toggle::enable)
            try {
                block()
            } finally {
                forEach(Toggle::pop)
            }
        }

        fun Iterable<Toggle>.disable(block: () -> Unit) {
            forEach(Toggle::disable)
            try {
                block()
            } finally {
                forEach(Toggle::pop)
            }
        }
    }
}
