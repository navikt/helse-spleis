package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Sykdomsgrader
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNUMMER = "987654321"
    }

    @BeforeEach
    private fun initEach() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `setter refFagsystemId og refDelytelseId når en linje peker på en annen`() {
        val tidslinje = tidslinjeOf(
            16.AP, 1.NAV, 2.HELG, 5.NAV(inntekt = 1200.0, grad = 50.0),
            startDato = 1.januar(2020)
        )

        beregnUtbetalinger(tidslinje)

        val tidligere = opprettUtbetaling(tidslinje.kutt(19.januar(2020)))
        val utbetaling = opprettUtbetaling(tidslinje.kutt(24.januar(2020)), tidligere = tidligere)

        val inspektør = OppdragInspektør(utbetaling.arbeidsgiverOppdrag())
        assertEquals(2, inspektør.antallLinjer())
        assertNull(inspektør.refDelytelseId(0))
        assertNull(inspektør.refFagsystemId(0))
        assertNotNull(inspektør.refDelytelseId(1))
        assertNotNull(inspektør.refFagsystemId(1))
    }

    @Test
    fun `separate utbetalinger`() {
        val tidslinje = tidslinjeOf(
            16.AP, 1.NAV, 2.HELG, 5.NAV, 2.HELG, 5.AP, 2.HELG, 5.NAV, 2.HELG,5.NAV, 2.HELG,5.NAV, 2.HELG,5.NAV, 2.HELG,
            startDato = 1.januar(2020)
        )

        beregnUtbetalinger(tidslinje)

        val første = opprettUtbetaling(tidslinje.kutt(19.januar(2020)))
        val andre = opprettUtbetaling(tidslinje, tidligere = første)

        val inspektør1 = OppdragInspektør(første.arbeidsgiverOppdrag())
        val inspektør2 = OppdragInspektør(andre.arbeidsgiverOppdrag())
        assertEquals(1, inspektør1.antallLinjer())
        assertEquals(1, inspektør2.antallLinjer())
        assertNull(inspektør1.refDelytelseId(0))
        assertNull(inspektør1.refFagsystemId(0))
        assertNull(inspektør2.refDelytelseId(0))
        assertNull(inspektør2.refFagsystemId(0))

        assertNotEquals(inspektør1.fagSystemId(0), inspektør2.fagSystemId(0))
    }

    @Test
    fun `tre utbetalinger`() {
        val tidslinje = tidslinjeOf(
            16.AP, 1.NAV, 2.HELG, 5.NAV(inntekt = 1200.0, grad = 50.0), 2.HELG, 5.NAV,
            startDato = 1.januar(2020)
        )

        beregnUtbetalinger(tidslinje)

        val første = opprettUtbetaling(tidslinje.kutt(19.januar(2020)))
        val andre = opprettUtbetaling(tidslinje.kutt(24.januar(2020)), tidligere = første)
        val tredje = opprettUtbetaling(tidslinje.kutt(31.januar(2020)), tidligere = andre)

        val inspektør = OppdragInspektør(tredje.arbeidsgiverOppdrag())
        assertEquals(3, inspektør.antallLinjer())
        assertNull(inspektør.refDelytelseId(0))
        assertNull(inspektør.refFagsystemId(0))

        assertEquals(1, inspektør.delytelseId(0))
        assertEquals(2, inspektør.delytelseId(1))
        assertEquals(3, inspektør.delytelseId(2))

        assertEquals(inspektør.delytelseId(0), inspektør.refDelytelseId(1))
        assertEquals(inspektør.delytelseId(1), inspektør.refDelytelseId(2))

        assertEquals(første.arbeidsgiverOppdrag().fagsystemId(), inspektør.refFagsystemId(1))
        assertEquals(andre.arbeidsgiverOppdrag().fagsystemId(), inspektør.refFagsystemId(2))
    }

    private fun beregnUtbetalinger(vararg tidslinjer: Utbetalingstidslinje) =
        MaksimumUtbetaling(
            Sykdomsgrader(listOf(*tidslinjer)),
            listOf(*tidslinjer),
            Periode(tidslinjer.first().førsteDato(), tidslinjer.last().sisteDato()),
            aktivitetslogg
        ).beregn()

    private fun opprettUtbetaling(
        tidslinje: Utbetalingstidslinje,
        tidligere: Utbetaling? = null,
        sisteDato: LocalDate = tidslinje.sisteDato(),
        fødselsnummer: String = UNG_PERSON_FNR_2018,
        orgnummer: String = ORGNUMMER,
        aktivitetslogg: Aktivitetslogg = this.aktivitetslogg
    ) = Utbetaling(fødselsnummer, orgnummer, tidslinje, sisteDato, aktivitetslogg, tidligere)

    private class OppdragInspektør(oppdrag: Oppdrag) : UtbetalingVisitor {
        private var linjeteller = 0
        private val fagsystemIder = mutableListOf<String>()
        private val delytelseIder = mutableListOf<Int>()
        private val refDelytelseIder = mutableListOf<Int?>()
        private val refFagsystemIder = mutableListOf<String?>()

        init {
            oppdrag.accept(this)
        }

        override fun preVisitOppdrag(oppdrag: Oppdrag) {
            fagsystemIder.add(oppdrag.fagsystemId())
        }

        override fun visitUtbetalingslinje(
            linje: Utbetalingslinje,
            fom: LocalDate,
            tom: LocalDate,
            dagsats: Int,
            grad: Double,
            delytelseId: Int,
            refDelytelseId: Int?,
            refFagsystemId: String?,
            endringskode: Endringskode,
            datoStatusFom: LocalDate?
        ) {
            linjeteller += 1
            delytelseIder.add(delytelseId)
            refDelytelseIder.add(refDelytelseId)
            refFagsystemIder.add(refFagsystemId)
        }

        internal fun antallLinjer() = linjeteller
        internal fun fagSystemId(indeks: Int) = fagsystemIder.elementAt(indeks)
        internal fun delytelseId(indeks: Int) = delytelseIder.elementAt(indeks)
        internal fun refDelytelseId(indeks: Int) = refDelytelseIder.elementAt(indeks)
        internal fun refFagsystemId(indeks: Int) = refFagsystemIder.elementAt(indeks)
    }
}
