package no.nav.helse.utbetalingstidslinje

import no.nav.helse.økonomi.DekningsgradKilde

internal interface ArbeidsgiverRegler: DekningsgradKilde {
    fun burdeStarteNyArbeidsgiverperiode(oppholdsdagerBrukt: Int): Boolean
    fun arbeidsgiverperiodenGjennomført(arbeidsgiverperiodedagerBrukt: Int): Boolean
    override fun dekningsgrad(): Double
    fun maksSykepengedager(): Int
    fun maksSykepengedagerOver67(): Int

    companion object {
        internal object NormalArbeidstaker: ArbeidsgiverRegler {
            private const val arbeidsgiverperiodelengde = 16
            private const val oppholdsdagerFørNyArbeidsgiverperiode = 16

            override fun burdeStarteNyArbeidsgiverperiode(oppholdsdagerBrukt: Int) =
                oppholdsdagerBrukt >= oppholdsdagerFørNyArbeidsgiverperiode

            override fun arbeidsgiverperiodenGjennomført(arbeidsgiverperiodedagerBrukt: Int) =
                arbeidsgiverperiodedagerBrukt >= arbeidsgiverperiodelengde

            override fun dekningsgrad() = 1.0
            override fun maksSykepengedager() = 248
            override fun maksSykepengedagerOver67() = 60
        }
    }
}
