package no.nav.helse.utbetalingslinjer

import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Endringskode.*
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingslinjer.OppdragBuilderTest.Dagtype
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OppdragBuilderTest {

    private companion object {
        private const val ORGNUMMER = "987654321"
    }

    @Test
    fun `konverter enkel Utbetalingstidslinje til Utbetalingslinjer`() {
        val oppdrag = tilArbeidsgiver(1.AP, 9.NAV)

        assertEquals(1, oppdrag.size)
        assertEquals(7, oppdrag.antallDager)
        oppdrag.assertLinje(0, 2.januar, 10.januar, null)
    }

    @Test
    fun `helg i slutt utelates, men ikke helg i start`() {
        val oppdrag = tilArbeidsgiver(6.AP, 8.NAV)

        assertEquals(1, oppdrag.size)
        assertEquals(5, oppdrag.antallDager)
        oppdrag.assertLinje(0, 7.januar, 12.januar, null)
    }

    @Test
    fun `helg i midten utelates ikke`() {
        val oppdrag = tilArbeidsgiver(12.NAV)

        assertEquals(1, oppdrag.size)
        assertEquals(10, oppdrag.antallDager)
        oppdrag.assertLinje(0, 1.januar, 12.januar, null)
    }

    @Test
    fun `kun helgedager`() {
        val oppdrag = tilArbeidsgiver(5.AP, 2.NAV)
        assertEquals(0, oppdrag.antallDager)
        assertEquals(0, oppdrag.size)
    }

    @Test
    fun `kun arbeidsdag`() {
        val oppdrag = tilArbeidsgiver(2.ARB)
        assertEquals(0, oppdrag.size)
    }

    @Test
    fun `siste arbeidsgiverperiodedag er siste ukjent dag fra Infotrygd`() {
        val oppdrag = tilArbeidsgiver(16.AP, 15.NAV, 10.NAV, 30.NAV, infotrygdtidslinje = tidslinjeOf(10.NAV, startDato = 1.februar))
        assertEquals(10.februar, oppdrag.sisteArbeidsgiverperiodedag)
    }

    @Test
    fun `siste arbeidsgiverperiodedag er siste dag i arbeidsgiverperioden`() {
        val oppdrag = tilArbeidsgiver(16.AP, 15.NAV)
        assertEquals(16.januar, oppdrag.sisteArbeidsgiverperiodedag)
    }

    @Test
    fun `siste arbeidsgiverperiodedag er siste dag i arbeidsgiverperioden 2`() {
        val oppdrag = tilArbeidsgiver(16.AP, 15.NAV, 17.ARB, 16.AP, 10.NAV)
        assertEquals(5.mars, oppdrag.sisteArbeidsgiverperiodedag)
    }

    @Test
    fun `Blanding av dagtyper`() {
        val oppdrag = tilArbeidsgiver(3.FRI, 2.NAV, 7.FRI, 2.HELG, 4.FRI)
        assertEquals(1, oppdrag.size)
    }

    @Test
    fun `kun helge- og fridager`() {
        val oppdrag = tilArbeidsgiver(5.FRI, 2.HELG, 5.FRI, 2.HELG, 5.FRI)

        assertEquals(0, oppdrag.size)
    }

    @Test
    fun `gap-dag som første og siste dag i perioden`() {
        val oppdrag = tilArbeidsgiver(1.ARB, 3.NAV, 1.ARB)

        assertEquals(1, oppdrag.size)
        oppdrag.assertLinje(0, 2.januar, 4.januar, null)
    }

    @Test
    fun `grad endres i løpet av helgen`() {
        val oppdrag = tilArbeidsgiver(6.NAV(1500), 6.NAV(1500, 80.0))
        assertEquals(2, oppdrag.size)
        oppdrag.assertLinje(0, 1.januar, 6.januar, null, sats = 1500, grad = 100)
        oppdrag.assertLinje(1, 7.januar, 12.januar, sats = (1500 * 0.8).toInt(), grad = 80)
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
        val oppdrag = tilArbeidsgiver(2.NAV, 2.AP, 7.NAV)

        assertEquals(1, oppdrag.size)
        oppdrag.assertLinje(0, 5.januar, 11.januar, null)
        assertEquals(4.januar, oppdrag.sisteArbeidsgiverperiodedag)
    }

    @Test
    fun `Utbetalingslinjer genereres kun fra dagen etter siste AGP-dag 2`() {
        val oppdrag = tilArbeidsgiver(2.NAV, 2.AP, 4.NAV, 2.AP, 4.NAV)

        assertEquals(1, oppdrag.size)
        oppdrag.assertLinje(0, 11.januar, 12.januar, null, klassekode = Klassekode.RefusjonIkkeOpplysningspliktig)
        assertEquals(10.januar, oppdrag.sisteArbeidsgiverperiodedag)
    }

    @Test
    fun `Endring i sats`() {
        val oppdrag = tilArbeidsgiver(3.NAV(1200), 6.NAV(1500))

        assertEquals(2, oppdrag.size)
        oppdrag.assertLinje(0, 1.januar, 3.januar, null, sats = 1200)
        oppdrag.assertLinje(1, 4.januar, 9.januar, sats = 1500)
    }

    @Test
    fun `dager som ikke lenger skal utbetales skal opphøres`() {
        val oppdragTilUtbetaling1 = tilArbeidsgiver(
            1.januar til 18.januar er NAVDAGER,
            19.januar er FRI
        )
        val oppdragskladd2 = tilArbeidsgiver(
            1.januar til 18.januar er NAVDAGER,
            19.januar er FRI,
            20.januar til 27.januar er NAVDAGER
        )
        val oppdragskladd3 = tilArbeidsgiver(
            1.januar til 18.januar er NAVDAGER,
            19.januar til 23.januar er FRI,
            24.januar til 28.januar er NAVDAGER
        )

        val oppdragTilUtbetaling2 = oppdragskladd2.minus(oppdragTilUtbetaling1, Aktivitetslogg())
        val oppdragTilUtbetaling3 = oppdragskladd3.minus(oppdragTilUtbetaling2, Aktivitetslogg())

        assertEquals(1, oppdragTilUtbetaling1.size)
        assertEquals(2, oppdragTilUtbetaling2.size)
        assertEquals(3, oppdragTilUtbetaling3.size)
        oppdragTilUtbetaling3.apply {
            assertLinje(0, 1.januar, 18.januar, delytelseId = 1, refDelytelseId = null, refFagsystemId = null)
            assertLinje(1, 20.januar, 26.januar, delytelseId = 2, refDelytelseId = null, endringskode = ENDR, datoStatusFom = 20.januar, refFagsystemId = null)
            assertLinje(2, 24.januar, 26.januar, delytelseId = 3, refDelytelseId = 2, endringskode = NY, refFagsystemId = oppdragTilUtbetaling3.fagsystemId())
        }
    }

    @Test
    fun `dager som ikke lenger skal utbetales skal opphøres 2`() {
        val oppdragTilUtbetaling1 = tilArbeidsgiver(
            1.januar til 18.januar er NAVDAGER,
            19.januar er FRI
        )
        val oppdragskladd2 = tilArbeidsgiver(
            1.januar til 18.januar er NAVDAGER,
            19.januar er FRI,
            20.januar til 26.januar er NAVDAGER
        )
        val oppdragskladd3 = tilArbeidsgiver(18.NAV, 1.FRI, 2.HELG, 2.FRI, 5.NAV(1100), 5.NAV(1300, 40.0))

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

        val originaltOppdrag = tilArbeidsgiver(
            10.januar til 31.januar er NAVDAGER,
            1.februar til 5.februar er NAVDAGER medBeløp 1400,
            startdato = 10.januar
        )
        val oppdragUtenStartenAvFebruar = tilArbeidsgiver(
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
            assertLinje(
                1,
                1.februar,
                5.februar,
                delytelseId = 2,
                refDelytelseId = 1,
                endringskode = NY,
                refFagsystemId = fagsystemId()
            ) //Opphører linje som har blitt overskrevet av nytt oppdrag
        }
        oppdragTilUtbetaling.apply {
            assertLinje(0, 5.januar, 31.januar, delytelseId = 3, refDelytelseId = 2, endringskode = NY, refFagsystemId = fagsystemId())
            assertLinje(
                1,
                3.februar,
                5.februar,
                delytelseId = 4,
                refDelytelseId = 3,
                endringskode = NY,
                refFagsystemId = fagsystemId()
            ) //Opphører linje som har blitt overskrevet av nytt oppdrag
        }
    }

    @Test
    fun `dager som ikke lenger skal utbetales skal opphøres 3`() {
        val oppdragTilUtbetaling1 = tilArbeidsgiver(
            1.januar til 18.januar er NAVDAGER,
            19.januar til 19.januar er FRI
        )
        val oppdragskladd2 = tilArbeidsgiver(
            1.januar til 18.januar er NAVDAGER,
            19.januar til 19.januar er FRI,
            20.januar til 26.januar er NAVDAGER
        )
        val oppdragskladd3 = tilArbeidsgiver(
            1.januar til 18.januar er NAVDAGER,
            19.januar til 23.januar er FRI,
            24.januar til 29.januar er NAVDAGER medBeløp 1100,
            30.januar til 3.februar er NAVDAGER medBeløp 1300 medGrad 40.0
        )
        val oppdragskladd4 = tilArbeidsgiver(
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
            assertLinje(
                1,
                20.januar,
                26.januar,
                delytelseId = 2,
                refDelytelseId = null,
                endringskode = ENDR,
                datoStatusFom = 20.januar,
                refFagsystemId = null
            ) //Opphører linje som har blitt overskrevet av nytt oppdrag
            assertLinje(2, 24.januar, 29.januar, delytelseId = 3, refDelytelseId = 2, endringskode = NY)
            assertLinje(3, 30.januar, 2.februar, delytelseId = 4, refDelytelseId = 3, endringskode = NY, sats = 520, grad = 40)
        }

        oppdragTilUtbetaling4.apply {
            //Gammel opphørslinje er filtrert vekk
            assertLinje(0, 1.januar, 18.januar, delytelseId = 1, refDelytelseId = null, refFagsystemId = null)
            assertLinje(1, 24.januar, 29.januar, delytelseId = 3, refDelytelseId = 2, endringskode = UEND, datoStatusFom = null, sats = 1100)
            assertLinje(
                2,
                30.januar,
                2.februar,
                delytelseId = 4,
                refDelytelseId = null,
                refFagsystemId = null,
                endringskode = ENDR,
                datoStatusFom = 30.januar,
                sats = 520
            )
            assertLinje(3, 1.februar, 2.februar, delytelseId = 5, refDelytelseId = 4, endringskode = NY, sats = 520, grad = 40)
        }
    }

    @Test
    fun `det funker å forlenge et oppdrag hvor vi har opphørt alle linjer`() {
        val original = tilArbeidsgiver(1.januar til 31.januar er NAVDAGER)
        val endret = tilArbeidsgiver(
            1.januar til 5.januar er FRI,
            6.januar til 15.januar er NAVDAGER,
            16.januar til 20.januar er FRI,
            21.januar til 31.januar er NAVDAGER
        ).minus(original, Aktivitetslogg())

        assertEquals(ENDR, endret.inspektør.endringskode)
        endret.apply {
            assertLinje(0, 1.januar, 31.januar, delytelseId = 1, refDelytelseId = null, datoStatusFom = 1.januar, endringskode = ENDR, refFagsystemId = null)
            assertLinje(1, 6.januar, 15.januar, delytelseId = 2, refDelytelseId = 1, datoStatusFom = null, endringskode = NY)
            assertLinje(2, 21.januar, 31.januar, delytelseId = 3, refDelytelseId = 2, datoStatusFom = null, endringskode = NY)
        }
    }

    @Test
    fun `Endring i utbetaling pga grad`() {
        val oppdrag = tilArbeidsgiver(
            1.januar til 3.januar er NAVDAGER medBeløp 1500 medGrad 100.0,
            4.januar til 9.januar er NAVDAGER medBeløp 1500 medGrad 60.0
        )

        assertEquals(2, oppdrag.size)
        oppdrag.assertLinje(0, 1.januar, 3.januar, null, sats = 1500, grad = 100)
        oppdrag.assertLinje(1, 4.januar, 9.januar, sats = (1500 * 0.6).toInt(), grad = 60)
    }

    @Test
    fun `Endring i utbetaling pga grad og inntekt, der utbetalingsbeløpet blir likt`() {
        val oppdrag = tilArbeidsgiver(
            1.januar til 3.januar er NAVDAGER medBeløp 1500 medGrad 100.0,
            4.januar til 5.januar er NAVDAGER medBeløp 1875 medGrad 80.0,
            6.januar til 9.januar er NAVDAGER medBeløp 1500 medGrad 80.0
        )

        assertEquals(3, oppdrag.size)
        oppdrag.assertLinje(0, 1.januar, 3.januar, null, sats = 1500, grad = 100, delytelseId = 1)
        oppdrag.assertLinje(1, 4.januar, 5.januar, sats = 1500, grad = 80, delytelseId = 2, refDelytelseId = 1)
        oppdrag.assertLinje(
            2,
            6.januar,
            9.januar,
            sats = (1500 * 0.8).toInt(),
            grad = 80,
            delytelseId = 3,
            refDelytelseId = 2
        )
    }

    @Test
    fun `Endring i sykdomsgrad`() {
        val oppdrag = tilArbeidsgiver(
            1.januar til 3.januar er NAVDAGER medGrad 100.0 medBeløp 1500,
            4.januar til 9.januar er NAVDAGER medGrad 80.0 medBeløp 1500,
        )

        assertEquals(2, oppdrag.size)
        oppdrag.assertLinje(0, 1.januar, 3.januar, null, sats = 1500, grad = 100)
        oppdrag.assertLinje(1, 4.januar, 9.januar, sats = (1500 * 0.8).toInt(), grad = 80)
    }

    @Test
    fun `Utbetalingstidslinje med utbetalingsdager der arbeidsgiverbeløp er 0 skal ikke generere linje i arbeidsgiveroppdrag`() {
        val oppdrag = tilArbeidsgiver(1.januar til 31.januar er NAVDAGER medGrad 100.0 medRefusjon 0)
        assertEquals(0, oppdrag.size)
    }

    @Test
    fun `Utbetalingstidslinje med utbetalingsdager med full refusjon skal ikke generere linje i personoppdrag`() {
        val oppdrag = tilSykmeldte(1.januar til 31.januar er NAVDAGER medGrad 100.0 medBeløp 1200 medRefusjon 1200)
        assertEquals(0, oppdrag.size)
    }

    @Test
    fun `Utbetalingstidslinje med delvis refusjon medfører et oppdrag til sykmeldte med en linje`() {
        val oppdrag = tilSykmeldte(1.januar til 31.januar er NAVDAGER medGrad 100.0 medBeløp 1200 medRefusjon 600)
        assertEquals(1, oppdrag.size)
        assertEquals(Klassekode.SykepengerArbeidstakerOrdinær, oppdrag[0].klassekode)
    }

    @Test
    fun `utbetaling som slutter på helg`() {
        val oppdrag = tilSykmeldte(1.januar til 6.januar er NAVDAGER medGrad 100.0 medBeløp 1200 medRefusjon 0)
        assertEquals(1, oppdrag.size)
        assertEquals(Klassekode.SykepengerArbeidstakerOrdinær, oppdrag[0].klassekode)
    }

    @Test
    fun `Utbetalingstidslinje med delvis refusjon medfører et oppdrag til arbeidsgiver med en linje`() {
        val oppdrag = tilArbeidsgiver(1.januar til 31.januar er NAVDAGER medGrad 100.0 medBeløp 1200 medRefusjon 600)
        assertEquals(1, oppdrag.size)
    }

    @Test
    fun `oppretter personoppdrag`() {
        val oppdrag = tilSykmeldte(16.AP, 15.NAV.medRefusjon(0))
        assertEquals(1, oppdrag.size)
        assertEquals(Fagområde.Sykepenger, oppdrag.inspektør.fagområde)
        oppdrag.assertLinje(0, 17.januar, 31.januar, null, 1200, 100, endringskode = NY, klassekode = Klassekode.SykepengerArbeidstakerOrdinær)
    }

    private val Utbetalingslinje.klassekode get() = this.get<Klassekode>("klassekode")

    private fun Oppdrag.assertLinje(
        index: Int,
        fom: LocalDate,
        tom: LocalDate,
        refFagsystemId: String? = this.fagsystemId(),
        sats: Int? = this[index].beløp,
        grad: Int? = this[index].grad,
        delytelseId: Int = this[index]["delytelseId"],
        refDelytelseId: Int? = this[index]["refDelytelseId"],
        datoStatusFom: LocalDate? = null,
        endringskode: Endringskode = this[index]["endringskode"],
        klassekode: Klassekode = this[index]["klassekode"]
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
        assertEquals(klassekode, this[index].get<Klassekode>("klassekode"))
    }

    private val Oppdrag.sisteArbeidsgiverperiodedag get() = this.get<LocalDate?>("sisteArbeidsgiverdag")

    private fun assertNyLinjeVedGap(gapDay: Utbetalingsdager) {
        val oppdrag = tilArbeidsgiver(2.NAV, gapDay, 7.NAV)

        assertEquals(2, oppdrag.size)
        assertEquals(1.januar, oppdrag.first().fom)
        assertEquals(2.januar, oppdrag.first().tom)
        assertEquals(4.januar, oppdrag.last().fom)
        assertEquals(10.januar, oppdrag.last().tom)
    }

    private fun tilArbeidsgiver(vararg dager: Utbetalingsdager, infotrygdtidslinje: Utbetalingstidslinje = Utbetalingstidslinje(), sisteDato: LocalDate? = null, startdato: LocalDate = 1.januar): Oppdrag =
        opprett(dager = dager, infotrygdtidslinje, sisteDato, startdato, fagområde = SykepengerRefusjon)

    private fun tilSykmeldte(vararg dager: Utbetalingsdager, infotrygdtidslinje: Utbetalingstidslinje = Utbetalingstidslinje(), sisteDato: LocalDate? = null, startdato: LocalDate = 1.januar): Oppdrag =
        opprett(dager = dager, infotrygdtidslinje, sisteDato, startdato, fagområde = Fagområde.Sykepenger)

    private fun opprett(
        vararg dager: Utbetalingsdager, infotrygdtidslinje: Utbetalingstidslinje, sisteDato: LocalDate? = null, startdato: LocalDate = 1.januar, fagområde: Fagområde
    ): Oppdrag {
        val tidslinje = tidslinjeOf(*dager, startDato = startdato).plus(infotrygdtidslinje) { spleisdag, infotrygddag ->
            when (infotrygddag) {
                is NavDag, is NavHelgDag -> UkjentDag(spleisdag.dato, spleisdag.økonomi)
                else -> spleisdag
            }
        }
        MaksimumUtbetaling(
            listOf(tidslinje),
            Aktivitetslogg(),
            startdato
        ).betal()
        return OppdragBuilder(
            tidslinje,
            ORGNUMMER,
            fagområde,
            sisteDato ?: tidslinje.periode().endInclusive
        ).build(null, Aktivitetslogg())
    }

    private val Oppdrag.antallDager get() = this.sumOf { it.dager().size }

    private infix fun Periode.er(dagtype: Dagtype) = dagtype.dager(this)
    private infix fun LocalDate.er(dagtype: Dagtype) = dagtype.dager(this.somPeriode())
    private infix fun Utbetalingsdager.medBeløp(beløp: Int) = this.copyWith(beløp = beløp)
    private infix fun Utbetalingsdager.medGrad(grad: Double) = this.copyWith(grad = grad)
    private infix fun Utbetalingsdager.medRefusjon(beløp: Int) = this.copyWith(arbeidsgiverbeløp = beløp)

    private fun interface Dagtype {
        fun dager(periode: Periode): Utbetalingsdager
    }

    private val NAVDAGER = Dagtype { periode -> periode.count().NAV }
    private val FRI = Dagtype { periode -> periode.count().FRI }
}
