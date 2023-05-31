package no.nav.helse.spleis.e2e.overstyring

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.inntekt.SkjønnsmessigFastsatt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SkjønnsmessigFastsettelseTest: AbstractDslTest() {

    @Test
    fun `skjønnsmessig fastsatt inntekt skal ikke ha avviksvurdering`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(
            orgnummer = a1,
            inntekt = INNTEKT * 2,
            forklaring = "Denne kan vi vel fjerne?",
            subsumsjon = null,
            refusjonsopplysninger = listOf(Triple(1.januar, null, INNTEKT))
        )))
        assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
        val sykepengegrunnlag = inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.sykepengegrunnlag.inspektør
        val inntektsopplysning = sykepengegrunnlag.arbeidsgiverInntektsopplysninger.single().inspektør.inntektsopplysning
        assertTrue(inntektsopplysning is SkjønnsmessigFastsatt)
        assertEquals(0, sykepengegrunnlag.avviksprosent)
        assertEquals(INNTEKT * 2, sykepengegrunnlag.beregningsgrunnlag)
        assertEquals(INNTEKT, sykepengegrunnlag.omregnetÅrsinntekt)
    }

    @Test
    fun `alle inntektene må skjønnsfastsettes ved overstyring`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar)
        a1 {
            assertThrows<IllegalStateException> {
                håndterSkjønnsmessigFastsettelse(
                    1.januar, listOf(
                        OverstyrtArbeidsgiveropplysning(
                            orgnummer = a1,
                            inntekt = INNTEKT * 2,
                            forklaring = "Denne kan vi vel fjerne?",
                            subsumsjon = null,
                            refusjonsopplysninger = listOf(Triple(1.januar, null, INNTEKT))
                        )
                    )
                )
            }
        }
    }
}