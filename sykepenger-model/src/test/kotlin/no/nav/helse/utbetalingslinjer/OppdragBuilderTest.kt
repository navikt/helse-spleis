package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Endringskode.*
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OppdragBuilderTest {

    private companion object {
        private const val ORGNUMMER = "987654321"
    }

    @Test
    fun `konverter enkel Utbetalingstidslinje til Utbetalingslinjer`() {
        val oppdrag = opprett(1.AP, 4.NAV, 2.HELG, 3.NAV)

        assertEquals(1, oppdrag.size)
        assertEquals(7, oppdrag.antallDager)
        oppdrag.assertLinje(0, 2.januar, 10.januar, null)
    }

    @Test
    fun `helg ved start og slutt i perioden utelates ikke`() {
        val oppdrag = opprett(1.AP, 1.HELG(1200), 5.NAV(1200), 2.HELG(1200))

        assertEquals(1, oppdrag.size)
        assertEquals(6, oppdrag.antallDager)
        oppdrag.assertLinje(0, 2.januar, 9.januar, null, sats = 1200, grad = 100.0)
    }

    @Test
    fun `kun helgedager`() {
        val oppdrag = opprett(1.AP, 2.HELG)
        assertEquals(0, oppdrag.antallDager)
        assertEquals(0, oppdrag.size)
    }

    @Test
    fun `kun arbeidsdag`() {
        val oppdrag = opprett(2.ARB)

        assertEquals(0, oppdrag.size)
    }

    @Test
    fun `Blanding av dagtyper`() {
        val oppdrag = opprett(4.FRI, 2.NAV, 4.FRI, 2.HELG, 4.FRI)

        assertEquals(1, oppdrag.size)
    }

    @Test
    fun `kun helge- og fridager`() {
        val oppdrag = opprett(4.FRI, 2.HELG, 4.FRI, 2.HELG, 4.FRI)

        assertEquals(0, oppdrag.size)
    }

    @Test
    fun `gap-dag som første og siste dag i perioden`() {
        val oppdrag = opprett(1.ARB, 3.NAV, 1.ARB)

        assertEquals(1, oppdrag.size)
        oppdrag.assertLinje(0, 2.januar, 4.januar, null)
    }

    @Test
    fun `grad endres i løpet av helgen`() {
        val oppdrag = opprett(5.NAV(1500), 1.HELG(1500), 1.HELG(1500, 80.0), 5.NAV(1500, 80.0))

        assertEquals(2, oppdrag.size)
        oppdrag.assertLinje(0, 1.januar, 6.januar, null, sats = 1500, grad = 100.0)
        oppdrag.assertLinje(1, 7.januar, 12.januar, sats = (1500 * 0.8).toInt(), grad = 80.0)
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
        val oppdrag = opprett(2.NAV, 2.AP, 2.NAV, 2.HELG, 3.NAV)

        assertEquals(1, oppdrag.size)
        oppdrag.assertLinje(0, 5.januar, 11.januar, null)
        assertEquals(4.januar, oppdrag.sisteArbeidsgiverdag)
    }

    @Test
    fun `Utbetalingslinjer genereres kun fra dagen etter siste AGP-dag 2`() {
        val oppdrag = opprett(2.NAV, 2.AP, 2.NAV, 2.HELG, 2.AP, 3.NAV)

        assertEquals(1, oppdrag.size)
        oppdrag.assertLinje(0, 11.januar, 13.januar, null)
        assertEquals(10.januar, oppdrag.sisteArbeidsgiverdag)
    }

    @Test
    fun `Endring i sats`() {
        val oppdrag = opprett(3.NAV(1200), 2.NAV(1500), 2.HELG, 2.NAV(1500))

        assertEquals(2, oppdrag.size)
        oppdrag.assertLinje(0, 1.januar, 3.januar, null, sats = 1200)
        oppdrag.assertLinje(1, 4.januar, 9.januar, sats = 1500)
    }

    @Test
    fun `dager som ikke lenger skal utbetales skal opphøres`() {
        val oppdragTilUtbetaling1 = opprett(18.NAV, 1.FRI)
        val oppdragskladd2 = opprett(18.NAV, 1.FRI, 2.HELG, 5.NAV)
        val oppdragskladd3 = opprett(18.NAV, 1.FRI, 2.HELG, 2.FRI, 5.NAV)

        val oppdragTilUtbetaling2 = oppdragskladd2.minus(oppdragTilUtbetaling1, Aktivitetslogg())
        val oppdragTilUtbetaling3 = oppdragskladd3.minus(oppdragTilUtbetaling2, Aktivitetslogg())

        oppdragTilUtbetaling3.assertLinje(0, 1.januar, 18.januar, delytelseId = 1, refDelytelseId = null, refFagsystemId = null)
        oppdragTilUtbetaling3.assertLinje(
            1,
            20.januar,
            26.januar,
            delytelseId = 2,
            refDelytelseId = null,
            endringskode = ENDR,
            datoStatusFom = 20.januar,
            refFagsystemId = null
        )
    }

    @Test
    fun `dager som ikke lenger skal utbetales skal opphøres 2`() {
        val oppdragTilUtbetaling1 = opprett(
            1.januar til 18.januar er NAVDAGER,
            19.januar er FRI
        )
        val oppdragskladd2 = opprett(
            1.januar til 18.januar er NAVDAGER,
            19.januar er FRI,
            20.januar til 26.januar er NAVDAGER
        )
        val oppdragskladd3 = opprett(18.NAV, 1.FRI, 2.HELG, 2.FRI, 5.NAV(1100), 5.NAV(1300, 40.0))

        val oppdragTilUtbetaling2 = oppdragskladd2.minus(oppdragTilUtbetaling1, Aktivitetslogg())
        val oppdragTilUtbetaling3 = oppdragskladd3.minus(oppdragTilUtbetaling2, Aktivitetslogg())

        oppdragTilUtbetaling3.assertLinje(0, 1.januar, 18.januar, delytelseId = 1, refDelytelseId = null, refFagsystemId = null)
        oppdragTilUtbetaling3.assertLinje(1, 20.januar, 26.januar, delytelseId = 2, refDelytelseId = null, endringskode = ENDR, datoStatusFom = 20.januar, refFagsystemId = null)
        oppdragTilUtbetaling3.assertLinje(2, 24.januar, 28.januar, delytelseId = 3, refDelytelseId = 2, endringskode = NY)
        oppdragTilUtbetaling3.assertLinje(3, 29.januar, 2.februar, delytelseId = 4, refDelytelseId = 3, endringskode = NY)
    }

    @Test
    fun `implisitt opphør av dager som ikke lenger er med i oppdrag når tidligere dager sendes på nytt`() {
        /*
         * Når et oppdrag endres slik at
         *  - dager før dato X endrer karakter; dvs blir sendt på nytt til oppdragssystemet
         *  - dager etter dato X lenger finnes i oppdraget
         * Så blir dagene etter X implisitt opphørt av oppdragssystemet
         */

        val originaltOppdrag = opprett(
            10.januar til 31.januar er NAVDAGER,
            1.februar til 5.februar er NAVDAGER medBeløp 1400,
            startdato = 10.januar
        )
        val oppdragUtenStartenAvFebruar = opprett(
            5.januar til 31.januar er NAVDAGER,
            1.februar til 2.februar er FRI,
            3.februar til 5.februar er NAVDAGER medBeløp 1400,
            startdato = 5.januar
            )

        val oppdragTilUtbetaling = oppdragUtenStartenAvFebruar.minus(originaltOppdrag, Aktivitetslogg())
        assertEquals(2, originaltOppdrag.size)
        assertEquals(2, oppdragTilUtbetaling.size)

        originaltOppdrag.apply {
            assertLinje(0, 10.januar, 31.januar, delytelseId = 1, refDelytelseId = null, endringskode = NY, refFagsystemId = null)
            assertLinje(1, 1.februar, 5.februar, delytelseId = 2, refDelytelseId = 1, endringskode = NY, refFagsystemId = fagsystemId()) //Opphører linje som har blitt overskrevet av nytt oppdrag
        }
        oppdragTilUtbetaling.apply {
            assertLinje(0, 5.januar, 31.januar, delytelseId = 3, refDelytelseId = 2, endringskode = NY, refFagsystemId = fagsystemId())
            assertLinje(1, 3.februar, 5.februar, delytelseId = 4, refDelytelseId = 3, endringskode = NY, refFagsystemId = fagsystemId()) //Opphører linje som har blitt overskrevet av nytt oppdrag
        }
    }

    @Test
    fun `dager som ikke lenger skal utbetales skal opphøres 3`() {
        val oppdragTilUtbetaling1 = opprett(
            1.januar til 18.januar er NAVDAGER,
            19.januar til 19.januar er FRI
        )
        val oppdragskladd2 = opprett(
            1.januar til 18.januar er NAVDAGER,
            19.januar til 19.januar er FRI,
            20.januar til 26.januar er NAVDAGER
        )
        val oppdragskladd3 = opprett(
            1.januar til 18.januar er NAVDAGER,
            19.januar til 23.januar er FRI,
            24.januar til 29.januar er NAVDAGER medBeløp 1100,
            30.januar til 3.februar er NAVDAGER medBeløp 1300 medGrad 40.0
        )
        val oppdragskladd4 = opprett(
            1.januar til 18.januar er NAVDAGER,
            19.januar til 23.januar er FRI,
            24.januar til 29.januar er NAVDAGER medBeløp 1100,
            30.januar til 31.januar er FRI,
            1.februar til 3.februar er NAVDAGER medBeløp 1300 medGrad 40.0
        )

        val oppdragTilUtbetaling2 = oppdragskladd2.minus(oppdragTilUtbetaling1, Aktivitetslogg())
        val oppdragTilUtbetaling3 = oppdragskladd3.minus(oppdragTilUtbetaling2, Aktivitetslogg())
        val oppdragTilUtbetaling4 = oppdragskladd4.minus(oppdragTilUtbetaling3, Aktivitetslogg())

        oppdragTilUtbetaling3.apply {
            assertLinje(0, 1.januar, 18.januar, delytelseId = 1, refDelytelseId = null, refFagsystemId = null)
            assertLinje(1, 20.januar, 26.januar, delytelseId = 2, refDelytelseId = null, endringskode = ENDR, datoStatusFom = 20.januar, refFagsystemId = null) //Opphører linje som har blitt overskrevet av nytt oppdrag
            assertLinje(2, 24.januar, 29.januar, delytelseId = 3, refDelytelseId = 2, endringskode = NY)
            assertLinje(3, 30.januar, 3.februar, delytelseId = 4, refDelytelseId = 3, endringskode = NY, sats = 520, grad = 40.0)
        }

        oppdragTilUtbetaling4.apply {
            //Gammel opphørslinje er filtrert vekk
            assertLinje(0, 1.januar, 18.januar, delytelseId = 1, refDelytelseId = null, refFagsystemId = null)
            assertLinje(1, 24.januar, 29.januar, delytelseId = 3, refDelytelseId = 2, endringskode = UEND, datoStatusFom = null, sats = 1100)
            assertLinje(2, 30.januar, 3.februar, delytelseId = 4, refDelytelseId = null, endringskode = ENDR, datoStatusFom = 30.januar, refFagsystemId = null) //Opphører linje som har blitt overskrevet av nytt oppdrag
            assertLinje(3, 1.februar, 3.februar, delytelseId = 5, refDelytelseId = 4, endringskode = NY, sats = 520, grad = 40.0)
        }
    }

    @Test
    fun `Endring i utbetaling pga grad`() {
        val oppdrag = opprett(3.NAV(1500, 100.0), 2.NAV(1500, 60.0), 2.HELG(1500, 60.0), 2.NAV(1500, 60.0))

        assertEquals(2, oppdrag.size)
        oppdrag.assertLinje(0, 1.januar, 3.januar, null, sats = 1500, grad = 100.0)
        oppdrag.assertLinje(1, 4.januar, 9.januar, sats = (1500 * 0.6).toInt(), grad = 60.0)
    }

    @Test
    fun `Endring i utbetaling pga grad og inntekt, der utbetalingsbeløpet blir likt`() {
        val oppdrag = opprett(3.NAV(1500, 100.0), 2.NAV(1875, 80.0), 2.HELG(1500, 80.0), 2.NAV(1500, 80.0))

        assertEquals(3, oppdrag.size)
        oppdrag.assertLinje(0, 1.januar, 3.januar, null, sats = 1500, grad = 100.0, delytelseId = 1)
        oppdrag.assertLinje(1, 4.januar, 5.januar, sats = 1500, grad = 80.0, delytelseId = 2, refDelytelseId = 1)
        oppdrag.assertLinje(
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
        val oppdrag = opprett(3.NAV(1500, 100.0), 2.NAV(1500, 80.0), 2.HELG(1500, 80.0), 2.NAV(1500, 80.0))

        assertEquals(2, oppdrag.size)
        oppdrag.assertLinje(0, 1.januar, 3.januar, null, sats = 1500, grad = 100.0)
        oppdrag.assertLinje(1, 4.januar, 9.januar, sats = (1500 * 0.8).toInt(), grad = 80.0)
    }

    @Test
    fun `Utbetalingslinje kan starte og ende på helgedag`() {
        val oppdrag = opprett(1.AP, 1.FRI, 1.HELG, 5.NAV, 2.HELG)

        assertEquals(1, oppdrag.size)
        oppdrag.assertLinje(0, 3.januar, 10.januar, null)
    }

    private fun Oppdrag.assertLinje(
        index: Int,
        fom: LocalDate,
        tom: LocalDate,
        refFagsystemId: String? = this.fagsystemId(),
        sats: Int? = this[index].beløp,
        grad: Double? = this[index].grad,
        delytelseId: Int = this[index]["delytelseId"],
        refDelytelseId: Int? = this[index]["refDelytelseId"],
        datoStatusFom: LocalDate? = null,
        endringskode: Endringskode = this[index]["endringskode"]
    ) {
        assertEquals(fom, this[index].fom)
        assertEquals(tom, this[index].tom)
        assertEquals(grad, this[index].grad)
        assertEquals(sats, this[index].beløp)
        assertEquals(delytelseId, this[index]["delytelseId"])
        assertEquals(refDelytelseId, this[index].get<Int?>("refDelytelseId"))
        assertEquals(refFagsystemId, this[index].refFagsystemId)
        assertEquals(datoStatusFom, this[index].get<LocalDate?>("datoStatusFom"))
        assertEquals(endringskode, this[index].get<Endringskode?>("endringskode"))
    }

    private val Oppdrag.sisteArbeidsgiverdag get() = this.get<LocalDate?>("sisteArbeidsgiverdag")

    private fun assertNyLinjeVedGap(gapDay: Utbetalingsdager) {
        val oppdrag = opprett(2.NAV, gapDay, 2.NAV, 2.HELG, 3.NAV)

        assertEquals(2, oppdrag.size)
        assertEquals(1.januar, oppdrag.first().fom)
        assertEquals(2.januar, oppdrag.first().tom)
        assertEquals(4.januar, oppdrag.last().fom)
        assertEquals(10.januar, oppdrag.last().tom)
    }

    private fun opprett(vararg dager: Utbetalingsdager, sisteDato: LocalDate? = null, startdato: LocalDate = 1.januar): Oppdrag {
        val tidslinje = tidslinjeOf(*dager, startDato = startdato)
        MaksimumUtbetaling(
            listOf(tidslinje),
            Aktivitetslogg(),
            startdato
        ).betal()
        return OppdragBuilder(
            tidslinje,
            ORGNUMMER,
            SykepengerRefusjon,
            sisteDato ?: tidslinje.periode().endInclusive
        ).result()
    }

    private val Oppdrag.antallDager get() = this.sumBy { it.dager().size }

    private infix fun Periode.er(dagtype: Dagtype) = dagtype.dager(this)
    private infix fun LocalDate.er(dagtype: Dagtype) = dagtype.dager(this til this)
    private infix fun Utbetalingsdager.medBeløp(beløp: Int) = this.copyWith(beløp = beløp)
    private infix fun Utbetalingsdager.medGrad(grad: Double) = this.copyWith(grad = grad)

    private fun interface Dagtype {
        fun dager(periode: Periode): Utbetalingsdager
    }

    private val NAVDAGER = Dagtype { periode -> periode.count().NAVv2 }
    private val FRI = Dagtype { periode ->  periode.count().FRIv2 }
}
