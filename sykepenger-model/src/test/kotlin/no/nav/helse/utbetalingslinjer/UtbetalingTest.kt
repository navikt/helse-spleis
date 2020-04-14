package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Sykdomsgrader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingTest {

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNUMMER = "987654321"
    }

    @Test
    fun `setter refFagsystemId og refDelytelseId når en linje peker på en annen`() {
        val tidslinje = tidslinjeOf(
            16.AP, 1.NAV, 2.HELG, 5.NAV(inntekt = 1200.0, grad = 50.0),
            startDato = 1.januar(2020)
        )
        val aktivitetslogg = Aktivitetslogg()
        MaksimumUtbetaling(
            Sykdomsgrader(listOf(tidslinje)),
            listOf(tidslinje),
            Periode(1.januar, 31.mars),
            aktivitetslogg
        ).beregn()

        val tidligere = Utbetaling(
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            utbetalingstidslinje = tidslinje.kutt(19.januar(2020)),
            sisteDato = 19.januar(2020),
            aktivitetslogg = aktivitetslogg,
            tidligere = null
        )
        val utbetaling = Utbetaling(
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            utbetalingstidslinje = tidslinje.kutt(24.januar(2020)),
            sisteDato = 24.januar(2020),
            aktivitetslogg = aktivitetslogg,
            tidligere = tidligere
        )
        val inspektør = OppdragInspektør(utbetaling.arbeidsgiverUtbetalingslinjer())
        assertEquals(2, inspektør.antallLinjer())
        assertNull(inspektør.refDelytelseId(0))
        assertNull(inspektør.refFagsystemId(0))
        assertNotNull(inspektør.refDelytelseId(1))
        assertNotNull(inspektør.refFagsystemId(1))
    }

    private class OppdragInspektør(oppdrag: Oppdrag) : UtbetalingVisitor {
        private var linjeteller = 0
        private val refDelytelseIder = mutableListOf<Int?>()
        private val refFagsystemIder = mutableListOf<String?>()

        init {
            oppdrag.accept(this)
        }

        override fun visitUtbetalingslinje(
            utbetalingslinje: Utbetalingslinje,
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int,
            grad: Double,
            delytelseId: Int,
            refDelytelseId: Int?
        ) {
            linjeteller += 1
            refDelytelseIder.add(refDelytelseId)
            refFagsystemIder.add(utbetalingslinje.refFagsystemId)
        }

        internal fun antallLinjer() = linjeteller
        internal fun refDelytelseId(indeks: Int) = refDelytelseIder.elementAt(indeks)
        internal fun refFagsystemId(indeks: Int) = refFagsystemIder.elementAt(indeks)
    }
}
