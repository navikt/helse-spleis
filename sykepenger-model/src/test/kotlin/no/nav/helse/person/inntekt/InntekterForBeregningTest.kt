package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.*
import no.nav.helse.desember
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.dsl.a4
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.saksbehandler
import no.nav.helse.testhelpers.AP
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.UTELATE
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class InntekterForBeregningTest {

    @Test
    fun `lager ghosttidslinjer for dagene vi ikke har beregnet utbetalingstidslinje`() {
        val inntekterForBeregning = inntekterForBeregning(1.januar til 28.januar) {
            fraInntektsgrunnlag(a1, 1000.daglig)
            fraInntektsgrunnlag(a2, 1000.daglig)
        }

        inntekterForBeregning.hensyntattAlleInntektskilder(
            mapOf(
                "a1" to listOf(tidslinjeOf(16.AP, 8.NAV)),
                "a2" to listOf(tidslinjeOf(20.UTELATE, 8.AP))
            )
        ).also { result ->
            assertEquals(2, result.size)
            assertEquals(28, result["a1"]?.size)
            assertEquals(28, result["a2"]?.size)
        }
    }

    @Test
    fun `strekker beløpstidslinene til å matche beregningsperioden`() {
        val inntekterForBeregning = inntekterForBeregning(1.januar til 31.januar) {
            fraInntektsgrunnlag(a1, 500.daglig)
            deaktivertFraInntektsgrunnlag(a2)
            inntektsendringer(Inntektskilde(a3), 10.januar, 15.januar, 250.daglig)
        }

        inntekterForBeregning.tilBeregning(a1).also { actual ->
            val forventetTidslinje = ARBEIDSGIVER.beløpstidslinje(1.januar til 31.januar, 500.daglig)
            assertBeløpstidslinje(forventetTidslinje, actual, ignoreMeldingsreferanseId = true)
        }

        inntekterForBeregning.tilBeregning(a2).also { actual ->
            val forventetTidslinje = SAKSBEHANDLER.beløpstidslinje(1.januar til 31.januar, INGEN)
            assertBeløpstidslinje(forventetTidslinje, actual, ignoreMeldingsreferanseId = true)
        }

        inntekterForBeregning.tilBeregning(a3).also { actual ->
            val forventetTidslinje = SYSTEM.beløpstidslinje(1.januar til 9.januar, INGEN) + SYSTEM.beløpstidslinje(10.januar til 15.januar, 250.daglig) + SYSTEM.beløpstidslinje(16.januar til 31.januar, INGEN)
            assertBeløpstidslinje(forventetTidslinje, actual, ignoreMeldingsreferanseId = true)
        }
    }

    @Test
    fun `begrenser inntektene til en periode`() {
        val inntekterForBeregning = inntekterForBeregning(1.januar til 31.januar) {
            fraInntektsgrunnlag(a1, 500.daglig)
            deaktivertFraInntektsgrunnlag(a2)
            inntektsendringer(Inntektskilde(a3), 10.januar, 15.januar, 250.daglig)
        }

        // Hele perioden
        with(inntekterForBeregning.forPeriode(1.januar til 31.januar)) {
            assertEquals(setOf(Inntektskilde(a1), Inntektskilde(a3)), keys)
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(1.januar til 31.januar, 500.daglig), getValue(Inntektskilde(a1)), ignoreMeldingsreferanseId = true)
            assertBeløpstidslinje(SYSTEM.beløpstidslinje(10.januar til 15.januar, 250.daglig), getValue(Inntektskilde(a3)), ignoreMeldingsreferanseId = true)
        }

        // Uten snute & hale
        with(inntekterForBeregning.forPeriode(2.januar til 30.januar)) {
            assertEquals(setOf(Inntektskilde(a1), Inntektskilde(a3)), keys)
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(2.januar til 30.januar, 500.daglig), getValue(Inntektskilde(a1)), ignoreMeldingsreferanseId = true)
            assertBeløpstidslinje(SYSTEM.beløpstidslinje(10.januar til 15.januar, 250.daglig), getValue(Inntektskilde(a3)), ignoreMeldingsreferanseId = true)
        }


        // Etter inntektsendringen
        with(inntekterForBeregning.forPeriode(16.januar til 31.januar)) {
            assertEquals(setOf(Inntektskilde(a1)), keys)
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(16.januar til 31.januar, 500.daglig), getValue(Inntektskilde(a1)), ignoreMeldingsreferanseId = true)
        }

        // Utenfor beregningsperioden
        assertThrows<IllegalStateException> { inntekterForBeregning.forPeriode(31.desember(2017) til 31.januar) }
        assertThrows<IllegalStateException> { inntekterForBeregning.forPeriode(1.januar til 1.februar) }
    }

    @Test
    fun `perioder med 0 kroner i beløp`() {
        val inntekterForBeregning = inntekterForBeregning(1.januar til 31.januar) {
            fraInntektsgrunnlag(a1, 0.daglig)
            fraInntektsgrunnlag(a2, 1000.daglig)
            inntektsendringer(Inntektskilde(a2), 2.januar, 30.januar, 0.daglig)
            deaktivertFraInntektsgrunnlag(a3)
            inntektsendringer(Inntektskilde(a4), 10.januar, 15.januar, 0.daglig)
        }

        assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(1.januar til 31.januar, 0.daglig), inntekterForBeregning.tilBeregning(a1), ignoreMeldingsreferanseId = true)
        assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(1.januar.somPeriode(), 1000.daglig) + SYSTEM.beløpstidslinje(2.januar til 30.januar, 0.daglig) + ARBEIDSGIVER.beløpstidslinje(31.januar.somPeriode(), 1000.daglig), inntekterForBeregning.tilBeregning(a2), ignoreMeldingsreferanseId = true)
        assertBeløpstidslinje(SAKSBEHANDLER.beløpstidslinje(1.januar til 31.januar, 0.daglig), inntekterForBeregning.tilBeregning(a3), ignoreMeldingsreferanseId = true)
        assertBeløpstidslinje(SYSTEM.beløpstidslinje(1.januar til 31.januar, 0.daglig), inntekterForBeregning.tilBeregning(a4), ignoreMeldingsreferanseId = true)

        with(inntekterForBeregning.forPeriode(1.januar til 31.januar)) {
            assertEquals(setOf(Inntektskilde(a2)), keys)
            assertBeløpstidslinje(ARBEIDSGIVER.beløpstidslinje(1.januar.somPeriode(), 1000.daglig) + ARBEIDSGIVER.beløpstidslinje(31.januar.somPeriode(), 1000.daglig), getValue(Inntektskilde(a2)), ignoreMeldingsreferanseId = true)
        }
    }


    @Test
    fun `kan endre inntekt på skjæringstidspunktet for en arbeidsgiver som finnes i inntektsgrunnlaget ved hjelp av en inntektsendring`() {
        val inntekterForBeregning = inntekterForBeregning(januar) {
            fraInntektsgrunnlag(a1, INNTEKT)
            inntektsendringer(Inntektskilde(a1), 1.januar, 31.januar, INNTEKT * 2)
        }

        val inntektstidslinje = inntekterForBeregning.tilBeregning(a1)

        val forventetTidslinje = SYSTEM.beløpstidslinje(1.januar til 31.januar, INNTEKT * 2)
        assertBeløpstidslinje(forventetTidslinje, inntektstidslinje, ignoreMeldingsreferanseId = true)
    }

    @Test
    fun `kan sette inntekt på skjæringstidspunktet for en arbeidsgiver som ikke finnes i inntektsgrunnlaget`() {
        val inntekterForBeregning = inntekterForBeregning(januar) {
            fraInntektsgrunnlag(a1, INNTEKT)
            inntektsendringer(Inntektskilde(a2), 1.januar, 31.januar, INNTEKT * 2)
        }

        val inntektstidslinjeForA1 = inntekterForBeregning.tilBeregning(a1)
        val inntektstidslinjeForA2 = inntekterForBeregning.tilBeregning(a2)

        val forventetTidslinjeA1 = ARBEIDSGIVER.beløpstidslinje(januar, INNTEKT)
        val forventetTidslinjeA2 = SYSTEM.beløpstidslinje(januar, INNTEKT * 2)

        assertBeløpstidslinje(forventetTidslinjeA1, inntektstidslinjeForA1, ignoreMeldingsreferanseId = true)
        assertBeløpstidslinje(forventetTidslinjeA2, inntektstidslinjeForA2, ignoreMeldingsreferanseId = true)
    }

    @Test
    fun `kan endre inntekt på skjæringstidspunktet for en deaktivert arbeidsgiver i inntektsgrunnlaget ved hjelp av en inntektsendring`() {
        val inntekterForBeregning = inntekterForBeregning(januar) {
            fraInntektsgrunnlag(a1, INNTEKT)
            deaktivertFraInntektsgrunnlag(a2)
            inntektsendringer(Inntektskilde(a2), 1.januar, 31.januar, INNTEKT * 2)
        }

        val inntektstidslinjeForA1 = inntekterForBeregning.tilBeregning(a1)
        val inntektstidslinjeForA2 = inntekterForBeregning.tilBeregning(a2)

        val forventetTidslinjeA1 = ARBEIDSGIVER.beløpstidslinje(januar, INNTEKT)
        val forventetTidslinjeA2 = SYSTEM.beløpstidslinje(januar, INNTEKT * 2)

        assertBeløpstidslinje(forventetTidslinjeA1, inntektstidslinjeForA1, ignoreMeldingsreferanseId = true)
        assertBeløpstidslinje(forventetTidslinjeA2, inntektstidslinjeForA2, ignoreMeldingsreferanseId = true)
    }

    @Test
    fun `kan sende inn inntektsendringer før inntekter fra inntektsgrunnlaget`() {
        val inntekterForBeregning = inntekterForBeregning(januar) {
            inntektsendringer(Inntektskilde(a1), 1.januar, 31.januar, INNTEKT * 2)
            fraInntektsgrunnlag(a1, INNTEKT)
        }
        val inntektstidslinje = inntekterForBeregning.tilBeregning(a1)
        val forventetTidslinje = SYSTEM.beløpstidslinje(1.januar til 31.januar, INNTEKT * 2)
        assertBeløpstidslinje(forventetTidslinje, inntektstidslinje, ignoreMeldingsreferanseId = true)
    }

    @Test
    fun `inntekter for beregning uten noen inntekter`() {
        val inntekterForBeregning = InntekterForBeregning.Builder(1.januar til 16.januar).build()
        val inntekterForPeriode = inntekterForBeregning.forPeriode(1.januar til 16.januar)
        assertEquals(emptyMap<Inntektskilde, Beløpstidslinje>(), inntekterForPeriode)
    }

    private fun inntekterForBeregning(periode: Periode, block: InntekterForBeregning.Builder.() -> Unit) = with(InntekterForBeregning.Builder(periode)) {
        block()
        build()
    }

    private fun InntekterForBeregning.Builder.fraInntektsgrunnlag(organisasjonsnummer: String, fastsattÅrsinntekt: Inntekt, meldingsreferanseId: MeldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())) =
        fraInntektsgrunnlag(organisasjonsnummer, fastsattÅrsinntekt, meldingsreferanseId.id.arbeidsgiver)

    private fun InntekterForBeregning.Builder.deaktivertFraInntektsgrunnlag(organisasjonsnummer: String,meldingsreferanseId: MeldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())) =
        deaktivertFraInntektsgrunnlag(organisasjonsnummer, meldingsreferanseId.id.saksbehandler)

    private fun InntekterForBeregning.Builder.inntektsendringer(inntektskilde: Inntektskilde, fom: LocalDate, tom: LocalDate?, inntekt: Inntekt, meldingsreferanseId: MeldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())) =
        inntektsendringer(inntektskilde, fom, tom, inntekt, meldingsreferanseId)
}
