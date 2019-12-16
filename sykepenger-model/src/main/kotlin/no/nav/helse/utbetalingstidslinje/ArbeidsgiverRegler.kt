package no.nav.helse.utbetalingstidslinje

internal interface ArbeidsgiverRegler {
    fun burdeStarteNyArbeidsgiverperiode(oppholdsdagerBrukt: Int): Boolean
    fun arbeidsgiverperiodenGjennomført(arbeidsgiverperiodedagerBrukt: Int): Boolean
    fun prosentLønn(): Double
    fun maksSykepengedager(): Int

    companion object {
        internal object NormalArbeidstaker: ArbeidsgiverRegler {
            override fun burdeStarteNyArbeidsgiverperiode(oppholdsdagerBrukt: Int) = oppholdsdagerBrukt >= 16

            override fun arbeidsgiverperiodenGjennomført(arbeidsgiverperiodedagerBrukt: Int) =
                arbeidsgiverperiodedagerBrukt >= 16

            override fun prosentLønn() = 1.0
            override fun maksSykepengedager() = 248
        }
    }
}
