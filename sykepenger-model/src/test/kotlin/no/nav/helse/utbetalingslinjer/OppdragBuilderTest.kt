package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Fagområde.SPREF
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Sykdomsgrader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OppdragBuilderTest {

    private lateinit var oppdrag: Oppdrag

    protected companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        const val INNTEKT = 31000.00
    }

    @Test
    internal fun `konverter enkel Utbetalingstidslinje til Utbetalingslinjer`() {
        opprett(5.NAV, 2.HELG, 3.NAV)

        assertEquals(1, oppdrag.size)
        assertLinje(0, 1.januar, 10.januar, null)
    }

    @Test
    internal fun `helg ved start og slutt i perioden utelates ikke`() {
        opprett(2.HELG(1200.0), 5.NAV(1200.0), 2.HELG(1200.0))

        assertEquals(1, oppdrag.size)
        assertLinje(0, 1.januar, 9.januar, null, sats = 1200, grad = 100.0)
    }

    @Test
    internal fun `kun helgedager`() {
        opprett(2.HELG)

        assertEquals(0, oppdrag.size)
    }

    @Test
    internal fun `Blanding av dagtyper`() {
        opprett(4.FRI, 2.NAV, 4.FRI, 2.HELG, 4.FRI)

        assertEquals(1, oppdrag.size)
    }

    @Test
    internal fun `kun helgedager med feriedager`() {
        opprett(4.FRI, 2.HELG, 4.FRI, 2.HELG, 4.FRI)

        assertEquals(0, oppdrag.size)
    }

    @Test
    internal fun `gap-dag som første og siste dag i perioden`() {
        opprett(1.ARB, 3.NAV, 1.ARB)

        assertEquals(1, oppdrag.size)
        assertLinje(0, 2.januar, 4.januar, null)
    }

    @Test
    internal fun `grad endres i løpet av helgen`() {
        opprett(5.NAV(1500.0), 1.HELG(1500.0), 1.HELG(1500.0, 80.0), 5.NAV(1500.0, 80.0))

        assertEquals(2, oppdrag.size)
        assertLinje(0, 1.januar, 6.januar, null, sats = 1500, grad = 100.0)
        assertLinje(1, 7.januar, 12.januar, sats = (1500 * 0.8).toInt(), grad = 80.0)
    }

    @Test
    internal fun `gap i vedtaksperiode`() {
        assertNyLinjeVedGap(1.ARB)
        assertNyLinjeVedGap(1.FRI)
        assertNyLinjeVedGap(1.AVV)
        assertNyLinjeVedGap(1.FOR)
    }

    @Test
    internal fun `Utbetalingslinjer genereres kun fra dagen etter siste AGP-dag`() {
        opprett(2.NAV, 1.AP, 2.NAV, 2.HELG, 3.NAV)

        assertEquals(1, oppdrag.size)
        assertLinje(0, 4.januar, 10.januar, null)
    }

    @Test
    internal fun `Endring i sats`() {
        opprett(3.NAV(1200.0), 2.NAV(1500.0), 2.HELG, 2.NAV(1500.0))

        assertEquals(2, oppdrag.size)
        assertLinje(0, 1.januar, 3.januar, null, sats = 1200)
        assertLinje(1, 4.januar, 9.januar, sats = 1500)
    }

    @Test
    internal fun `Endring i utbetaling pga grad`() {
        opprett(3.NAV(1500.0, 100.0), 2.NAV(1500.0, 60.0), 2.HELG(1500.0, 60.0), 2.NAV(1500.0, 60.0))

        assertEquals(2, oppdrag.size)
        assertLinje(0, 1.januar, 3.januar, null, sats = 1500, grad = 100.0)
        assertLinje(1, 4.januar, 9.januar, sats = (1500 * 0.6).toInt(), grad = 60.0)
    }

    @Test
    internal fun `Endring i utbetaling pga grad og inntekt, der utbetalingsbeløpet blir likt`() {
        opprett(3.NAV(1500.0, 100.0), 2.NAV(1875.0, 80.0), 2.HELG(1500.0, 80.0), 2.NAV(1500.0, 80.0))

        assertEquals(3, oppdrag.size)
        assertLinje(0, 1.januar, 3.januar, null, sats = 1500, grad = 100.0, delytelseId = 1)
        assertLinje(1, 4.januar, 5.januar, sats = 1500, grad = 80.0, delytelseId = 2, refDelytelseId = 1)
        assertLinje(
            2,
            6.januar,
            9.januar,
            sats = (1500 * 0.8).toInt(),
            grad = 80.0,
            delytelseId = 3,
            refDelytelseId = 2
        )
    }

    @Test
    internal fun `Endring i sykdomsgrad`() {
        opprett(3.NAV(1500.0, 100.0), 2.NAV(1500.0, 80.0), 2.HELG(1500.0, 80.0), 2.NAV(1500.0, 80.0))

        assertEquals(2, oppdrag.size)
        assertLinje(0, 1.januar, 3.januar, null, sats = 1500, grad = 100.0)
        assertLinje(1, 4.januar, 9.januar, sats = (1500 * 0.8).toInt(), grad = 80.0)
    }

    private fun assertLinje(
        index: Int,
        fom: LocalDate,
        tom: LocalDate,
        refFagsystemId: String? = oppdrag.referanse(),
        sats: Int = oppdrag[index].dagsats,
        grad: Double = oppdrag[index].grad,
        delytelseId: Int = oppdrag[index]["delytelseId"],
        refDelytelseId: Int? = oppdrag[index]["refDelytelseId"]
    ) {
        assertEquals(fom, oppdrag[index].fom)
        assertEquals(tom, oppdrag[index].tom)
        assertEquals(grad, oppdrag[index].grad)
        assertEquals(sats, oppdrag[index].dagsats)
        assertEquals(delytelseId, oppdrag[index]["delytelseId"])
        assertEquals(refDelytelseId, oppdrag[index]["refDelytelseId"] ?: null)
        assertEquals(refFagsystemId, oppdrag[index].refFagsystemId ?: null)
    }

    private fun assertNyLinjeVedGap(gapDay: Utbetalingsdager) {
        opprett(2.NAV, gapDay, 2.NAV, 2.HELG, 3.NAV)

        assertEquals(2, oppdrag.size)
        assertEquals(1.januar, oppdrag.first().fom)
        assertEquals(2.januar, oppdrag.first().tom)
        assertEquals(4.januar, oppdrag.last().fom)
        assertEquals(10.januar, oppdrag.last().tom)
    }

    private fun opprett(vararg dager: Utbetalingsdager, sisteDato: LocalDate? = null) {
        val tidslinje = tidslinjeOf(*dager)
        MaksimumUtbetaling(
            Sykdomsgrader(listOf(tidslinje)),
            listOf(tidslinje),
            Periode(1.januar, 1.mars),
            Aktivitetslogg()
        ).beregn()
        oppdrag = OppdragBuilder(
            tidslinje,
            ORGNUMMER,
            SPREF,
            sisteDato ?: tidslinje.sisteDato()
        ).result()
    }
}
