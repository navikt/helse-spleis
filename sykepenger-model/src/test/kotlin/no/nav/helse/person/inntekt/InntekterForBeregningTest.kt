package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
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
    fun `kan ikke endre inntekt på skjæringstidspunktet for en arbeidsgiver som finnes i inntektsgrunnlaget ved hjelp av en inntektsendring`() {
        val inntekterForBeregning = inntekterForBeregning(januar) {
            fraInntektsgrunnlag(a1, INNTEKT)
            inntektsendringer(a1, 1.januar, 31.januar, INNTEKT * 2)
        }

        val (_, inntektstidslinje) = inntekterForBeregning.tilBeregning(a1)

        val forventetTidslinje = ARBEIDSGIVER.beløpstidslinje(1.januar.somPeriode(), INNTEKT) + SYSTEM.beløpstidslinje(2.januar til 31.januar, INNTEKT * 2)
        assertBeløpstidslinje(forventetTidslinje, inntektstidslinje, ignoreMeldingsreferanseId = true)
    }

    @Test
    fun `kan sette inntekt på skjæringstidspunktet for en arbeidsgiver som ikke finnes i inntektsgrunnlaget`() {
        val inntekterForBeregning = inntekterForBeregning(januar) {
            fraInntektsgrunnlag(a1, INNTEKT)
            inntektsendringer(a2, 1.januar, 31.januar, INNTEKT * 2)
        }

        val (fastsattÅrsinntektA1, inntektstidslinjeForA1) = inntekterForBeregning.tilBeregning(a1)
        val (fastsattÅrsinntektA2, inntektstidslinjeForA2) = inntekterForBeregning.tilBeregning(a2)

        val forventetTidslinjeA1 = ARBEIDSGIVER.beløpstidslinje(januar, INNTEKT)
        val forventetTidslinjeA2 = SYSTEM.beløpstidslinje(januar, INNTEKT * 2)

        assertBeløpstidslinje(forventetTidslinjeA1, inntektstidslinjeForA1, ignoreMeldingsreferanseId = true)
        assertBeløpstidslinje(forventetTidslinjeA2, inntektstidslinjeForA2, ignoreMeldingsreferanseId = true)
        assertEquals(INNTEKT, fastsattÅrsinntektA1)
        assertEquals(INGEN, fastsattÅrsinntektA2)
    }

    @Test
    fun `kan endre inntekt på skjæringstidspunktet for en deaktivert arbeidsgiver i inntektsgrunnlaget ved hjelp av en inntektsendring`() {
        val inntekterForBeregning = inntekterForBeregning(januar) {
            fraInntektsgrunnlag(a1, INNTEKT)
            deaktivertFraInntektsgrunnlag(a2)
            inntektsendringer(a2, 1.januar, 31.januar, INNTEKT * 2)
        }

        val (fastsattÅrsinntektA1, inntektstidslinjeForA1) = inntekterForBeregning.tilBeregning(a1)
        val (fastsattÅrsinntektA2, inntektstidslinjeForA2) = inntekterForBeregning.tilBeregning(a2)

        val forventetTidslinjeA1 = ARBEIDSGIVER.beløpstidslinje(januar, INNTEKT)
        val forventetTidslinjeA2 = SYSTEM.beløpstidslinje(januar, INNTEKT * 2)

        assertBeløpstidslinje(forventetTidslinjeA1, inntektstidslinjeForA1, ignoreMeldingsreferanseId = true)
        assertBeløpstidslinje(forventetTidslinjeA2, inntektstidslinjeForA2, ignoreMeldingsreferanseId = true)
        assertEquals(INNTEKT, fastsattÅrsinntektA1)
        assertEquals(INGEN, fastsattÅrsinntektA2)
    }

    private fun inntekterForBeregning(periode: Periode, skjæringstidspunkt: LocalDate = periode.start, block: InntekterForBeregning.Builder.() -> Unit) = with(InntekterForBeregning.Builder(periode, skjæringstidspunkt)) {
        medGjeldende6G(Grunnbeløp.`6G`.beløp(skjæringstidspunkt))
        block()
        build()
    }

    private fun InntekterForBeregning.Builder.fraInntektsgrunnlag(organisasjonsnummer: String, fastsattÅrsinntekt: Inntekt, meldingsreferanseId: MeldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())) =
        fraInntektsgrunnlag(organisasjonsnummer, fastsattÅrsinntekt, meldingsreferanseId.id.arbeidsgiver)

    private fun InntekterForBeregning.Builder.deaktivertFraInntektsgrunnlag(organisasjonsnummer: String,meldingsreferanseId: MeldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())) =
        deaktivertFraInntektsgrunnlag(organisasjonsnummer, meldingsreferanseId.id.saksbehandler)

    private fun InntekterForBeregning.Builder.inntektsendringer(organisasjonsnummer: String, fom: LocalDate, tom: LocalDate?, inntekt: Inntekt, meldingsreferanseId: MeldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())) =
        inntektsendringer(organisasjonsnummer, fom, tom, inntekt, meldingsreferanseId)
}
