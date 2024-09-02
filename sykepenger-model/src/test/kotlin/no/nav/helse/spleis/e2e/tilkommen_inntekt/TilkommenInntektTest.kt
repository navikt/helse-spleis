package no.nav.helse.spleis.e2e.tilkommen_inntekt

import java.time.LocalDate
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.TilkommenInntekt
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.inntekt.InntektFraSøknad
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TilkommenInntektTest : AbstractDslTest() {

    @Test
    fun `oppdaterer sykepengegrunnlag med inntekter fra søknaden`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = 31000.månedlig)
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), tilkomneInntekter = listOf(TilkommenInntekt(fom = 1.februar, tom = null, orgnummer = "a2", beløp = 10000.månedlig)))
            assertVarsel(Varselkode.RV_SV_5)
            assertEquals(2, inspektør.vilkårsgrunnlagHistorikkInnslag().size)
            inspektør.vilkårsgrunnlag(1.januar)!!.inspektør.sykepengegrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(31000.månedlig, sykepengegrunnlagInspektør.sykepengegrunnlag)
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                val inntektA1 = sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a1]
                val inntektA2 = sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a2]
                assertEquals(1.januar til LocalDate.MAX, inntektA1!!.inspektør.gjelder)
                assertEquals(31000.månedlig, inntektA1.inspektør.inntektsopplysning.fastsattÅrsinntekt())
                assertTrue(inntektA1.inspektør.inntektsopplysning is Inntektsmelding)
                assertEquals(1.februar til LocalDate.MAX, inntektA2!!.inspektør.gjelder)
                assertEquals(10000.månedlig, inntektA2.inspektør.inntektsopplysning.fastsattÅrsinntekt())
                assertTrue(inntektA2.inspektør.inntektsopplysning is InntektFraSøknad)
            }
        }
    }

    @Test
    fun `inntekt fra søknad på førstegangsbehandling med fom lik skjæringstidspunktet`() {
        a1 {
            håndterSøknad(
                Sykdom(1.januar, 31.januar, 100.prosent),
                orgnummer = a1,
                tilkomneInntekter = listOf(
                    TilkommenInntekt(
                        fom = 1.januar,
                        tom = null,
                        orgnummer = a2,
                        beløp = 10000.månedlig
                    )
                )
            )
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(
                    arbeidsgivere = listOf(a1 to INNTEKT, a2 to 10000.månedlig),
                    skjæringstidspunkt = 1.januar,
                ),
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(
                        a1,
                        LocalDate.EPOCH,
                        null,
                        Arbeidsforholdtype.ORDINÆRT
                    ),
                    Vilkårsgrunnlag.Arbeidsforhold(
                        a2,
                        1.januar,
                        null,
                        Arbeidsforholdtype.ORDINÆRT
                    )
                )
            )
            inspektør.vilkårsgrunnlag(1.vedtaksperiode)!!.inspektør.sykepengegrunnlag.inspektør.let { sykepengegrunnlagInspektør ->
                assertEquals(2, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a1]!!.inspektør.inntektsopplysning is Inntektsmelding)
                assertTrue(sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver[a2]!!.inspektør.inntektsopplysning is SkattSykepengegrunnlag)
            }
        }
    }
}