package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OppdragBuilderTest {

    private lateinit var oppdrag: Oppdrag

    private companion object {
        private const val ORGNUMMER = "987654321"
    }

    @Test
    fun `konverter enkel Utbetalingstidslinje til Utbetalingslinjer`() {
        opprett(1.AP, 4.NAV, 2.HELG, 3.NAV)

        assertEquals(1, oppdrag.size)
        assertEquals(7, oppdrag.antallDager)
        assertLinje(0, 2.januar, 10.januar, null)
    }

    @Test
    fun `helg ved start og slutt i perioden utelates ikke`() {
        opprett(1.AP, 1.HELG(1200), 5.NAV(1200), 2.HELG(1200))

        assertEquals(1, oppdrag.size)
        assertEquals(6, oppdrag.antallDager)
        assertLinje(0, 2.januar, 9.januar, null, sats = 1200, grad = 100.0)
    }

    @Test
    fun `kun helgedager`() {
        opprett(1.AP, 2.HELG)
        assertEquals(0, oppdrag.antallDager)
        assertEquals(0, oppdrag.size)
    }

    @Test
    fun `kun arbeidsdag`() {
        opprett(2.ARB)

        assertEquals(0, oppdrag.size)
    }

    @Test
    fun `Blanding av dagtyper`() {
        opprett(4.FRI, 2.NAV, 4.FRI, 2.HELG, 4.FRI)

        assertEquals(1, oppdrag.size)
    }

    @Test
    fun `kun helgedager med feriedager`() {
        opprett(4.FRI, 2.HELG, 4.FRI, 2.HELG, 4.FRI)

        assertEquals(0, oppdrag.size)
    }

    @Test
    fun `gap-dag som første og siste dag i perioden`() {
        opprett(1.ARB, 3.NAV, 1.ARB)

        assertEquals(1, oppdrag.size)
        assertLinje(0, 2.januar, 4.januar, null)
    }

    @Test
    fun `grad endres i løpet av helgen`() {
        opprett(5.NAV(1500), 1.HELG(1500), 1.HELG(1500, 80.0), 5.NAV(1500, 80.0))

        assertEquals(2, oppdrag.size)
        assertLinje(0, 1.januar, 6.januar, null, sats = 1500, grad = 100.0)
        assertLinje(1, 7.januar, 12.januar, sats = (1500 * 0.8).toInt(), grad = 80.0)
    }

    @Test
    fun `gap i vedtaksperiode`() {
        assertNyLinjeVedGap(1.ARB)
        assertNyLinjeVedGap(1.FRI)
        assertNyLinjeVedGap(1.AVV)
        assertNyLinjeVedGap(1.FOR)
    }

    @Test
    fun `Utbetalingslinjer genereres kun fra dagen etter siste AGP-dag`() {
        opprett(2.NAV, 2.AP, 2.NAV, 2.HELG, 3.NAV)

        assertEquals(1, oppdrag.size)
        assertLinje(0, 5.januar, 11.januar, null)
        assertEquals(4.januar, oppdrag.sisteArbeidsgiverdag)
    }

    @Test
    fun `Utbetalingslinjer genereres kun fra dagen etter siste AGP-dag 2`() {
        opprett(2.NAV, 2.AP, 2.NAV, 2.HELG, 2.AP, 3.NAV)

        assertEquals(1, oppdrag.size)
        assertLinje(0, 11.januar, 13.januar, null)
        assertEquals(10.januar, oppdrag.sisteArbeidsgiverdag)
    }

    @Test
    fun `Endring i sats`() {
        opprett(3.NAV(1200), 2.NAV(1500), 2.HELG, 2.NAV(1500))

        assertEquals(2, oppdrag.size)
        assertLinje(0, 1.januar, 3.januar, null, sats = 1200)
        assertLinje(1, 4.januar, 9.januar, sats = 1500)
    }

    @Test
    fun `Endring i utbetaling pga grad`() {
        opprett(3.NAV(1500, 100.0), 2.NAV(1500, 60.0), 2.HELG(1500, 60.0), 2.NAV(1500, 60.0))

        assertEquals(2, oppdrag.size)
        assertLinje(0, 1.januar, 3.januar, null, sats = 1500, grad = 100.0)
        assertLinje(1, 4.januar, 9.januar, sats = (1500 * 0.6).toInt(), grad = 60.0)
    }

    @Test
    fun `Endring i utbetaling pga grad og inntekt, der utbetalingsbeløpet blir likt`() {
        opprett(3.NAV(1500, 100.0), 2.NAV(1875, 80.0), 2.HELG(1500, 80.0), 2.NAV(1500, 80.0))

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
    fun `Endring i sykdomsgrad`() {
        opprett(3.NAV(1500, 100.0), 2.NAV(1500, 80.0), 2.HELG(1500, 80.0), 2.NAV(1500, 80.0))

        assertEquals(2, oppdrag.size)
        assertLinje(0, 1.januar, 3.januar, null, sats = 1500, grad = 100.0)
        assertLinje(1, 4.januar, 9.januar, sats = (1500 * 0.8).toInt(), grad = 80.0)
    }

    @Test
    fun `Utbetalingslinje kan starte og ende på helgedag`() {
        opprett(1.AP, 1.FRI, 1.HELG, 5.NAV, 2.HELG)

        assertEquals(1, oppdrag.size)
        assertLinje(0, 3.januar, 10.januar, null)
    }

    private fun assertLinje(
        index: Int,
        fom: LocalDate,
        tom: LocalDate,
        refFagsystemId: String? = oppdrag.fagsystemId(),
        sats: Int? = oppdrag[index].beløp,
        grad: Double = oppdrag[index].grad,
        delytelseId: Int = oppdrag[index]["delytelseId"],
        refDelytelseId: Int? = oppdrag[index]["refDelytelseId"]
    ) {
        assertEquals(fom, oppdrag[index].fom)
        assertEquals(tom, oppdrag[index].tom)
        assertEquals(grad, oppdrag[index].grad)
        assertEquals(sats, oppdrag[index].beløp)
        assertEquals(delytelseId, oppdrag[index]["delytelseId"])
        assertEquals(refDelytelseId, oppdrag[index].get<Int?>("refDelytelseId"))
        assertEquals(refFagsystemId, oppdrag[index].refFagsystemId)
    }

    private val Oppdrag.sisteArbeidsgiverdag get() = this.get<LocalDate?>("sisteArbeidsgiverdag")

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
            listOf(tidslinje),
            Aktivitetslogg(),
            1.januar
        ).betal()
        oppdrag = OppdragBuilder(
            tidslinje,
            ORGNUMMER,
            SykepengerRefusjon,
            sisteDato ?: tidslinje.sisteDato()
        ).result()
    }

    private val Oppdrag.antallDager get() = this.sumBy { it.dager().size }
}
