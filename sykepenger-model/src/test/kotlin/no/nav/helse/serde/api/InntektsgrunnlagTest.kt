package no.nav.helse.serde.api

import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.til
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.oktober
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

internal class InntektsgrunnlagTest : AbstractEndToEndTest() {
    @Test
    fun `Finner inntektsgrunnlag for en arbeidsgiver med inntekt satt av saksbehandler`() {
        val inntektshistorikk = mapOf(ORGNUMMER to InntektshistorikkVol2().apply {
            this {
                addSaksbehandler(1.januar, UUID.randomUUID(), INNTEKT)
            }
        })
        val skjæringstidspunkter = listOf(1.januar)

        val inntektsgrunnlag = inntektsgrunnlag(inntektshistorikk, skjæringstidspunkter)

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.januar }.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt.also { omregnetÅrsinntekt ->
            assertEquals(InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Saksbehandler, omregnetÅrsinntekt.kilde)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, omregnetÅrsinntekt.beløp)
            assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, omregnetÅrsinntekt.månedsbeløp)
        }
    }

    @Test
    fun `Finner inntektsgrunnlag for en arbeidsgiver med en inntektsmelding`() {
        val inntektshistorikk = mapOf(ORGNUMMER to InntektshistorikkVol2().apply {
            inntektsmelding(
                id = UUID.randomUUID(),
                arbeidsgiverperioder = listOf(1.januar til 16.januar)
            ).addInntekt(this, 1.januar)
        })
        val skjæringstidspunkter = listOf(1.januar)

        val inntektsgrunnlag = inntektsgrunnlag(inntektshistorikk, skjæringstidspunkter)

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.januar }.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt.also { omregnetÅrsinntekt ->
            assertEquals(InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Inntektsmelding, omregnetÅrsinntekt.kilde)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, omregnetÅrsinntekt.beløp)
            assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, omregnetÅrsinntekt.månedsbeløp)
        }
    }

    @Test
    fun `Finner inntektsgrunnlag for en arbeidsgiver med inntekt fra Infotrygd`() {
        val inntektshistorikk = mapOf(ORGNUMMER to InntektshistorikkVol2().apply {
            Utbetalingshistorikk.Inntektsopplysning(1.januar, INNTEKT, ORGNUMMER, true)
                .addInntekter(UUID.randomUUID(), ORGNUMMER, this)
        })
        val skjæringstidspunkter = listOf(1.januar)

        val inntektsgrunnlag = inntektsgrunnlag(inntektshistorikk, skjæringstidspunkter)

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.januar }.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt.also { omregnetÅrsinntekt ->
            assertEquals(InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.Infotrygd, omregnetÅrsinntekt.kilde)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig }, omregnetÅrsinntekt.beløp)
            assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, omregnetÅrsinntekt.månedsbeløp)
        }
    }

    @Test
    fun `Finner inntektsgrunnlag for en arbeidsgiver med inntekt fra Skatt`() {
        val inntektshistorikk = mapOf(ORGNUMMER to InntektshistorikkVol2().apply {
            inntektperioder {
                inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SYKEPENGEGRUNNLAG
                1.oktober(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
                1.oktober(2017) til 1.oktober(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }.forEach { it.lagreInntekter(this, 1.januar, UUID.randomUUID()) }
        })
        val skjæringstidspunkter = listOf(1.januar)

        val inntektsgrunnlag = inntektsgrunnlag(inntektshistorikk, skjæringstidspunkter)

        assertTrue(inntektsgrunnlag.isNotEmpty())
        inntektsgrunnlag.single { it.skjæringstidspunkt == 1.januar }.inntekter.single { it.arbeidsgiver == ORGNUMMER }.omregnetÅrsinntekt.also { omregnetÅrsinntekt ->
            assertEquals(InntektsgrunnlagDTO.ArbeidsgiverinntektDTO.OmregnetÅrsinntektDTO.InntektkildeDTO.AOrdningen, omregnetÅrsinntekt.kilde)
            assertEquals(INNTEKT.reflection { årlig, _, _, _ -> årlig } * 4 / 3, omregnetÅrsinntekt.beløp)
            assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd } * 4 / 3, omregnetÅrsinntekt.månedsbeløp)
            omregnetÅrsinntekt.inntekterFraAOrdningen.also {
                requireNotNull(it)
                assertEquals(3, it.size)
                assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd } * 2, it[0].sum)
                assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, it[1].sum)
                assertEquals(INNTEKT.reflection { _, mnd, _, _ -> mnd }, it[2].sum)
            }
        }
    }
}
