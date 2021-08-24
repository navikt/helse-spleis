package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class InntektsvurderingTest {
    private companion object {
        private const val ORGNR = "123456789"
        private val INNTEKT = 1000.0.månedlig
    }

    private lateinit var aktivitetslogg: Aktivitetslogg

    @Test
    fun `ugyldige verdier`() {
        assertFalse(validererOk(inntektsvurdering(emptyList()), sykepengegrunnlag(INGEN), INGEN))
        assertFalse(
            validererOk(
                inntektsvurdering(
                    inntektperioderForSammenligningsgrunnlag {
                        LocalDate.now() til LocalDate.now() inntekter {
                            ORGNR inntekt INGEN
                        }
                    }
                ), sykepengegrunnlag(INGEN), INGEN
            )
        )
    }

    @Test
    fun `skal kunne beregne avvik mellom innmeldt lønn fra inntektsmelding og lønn fra inntektskomponenten`() {
        val inntektsvurdering = inntektsvurdering()
        assertFalse(validererOk(inntektsvurdering, sykepengegrunnlag(1250.01.månedlig), INNTEKT))
        assertFalse(validererOk(inntektsvurdering, sykepengegrunnlag(749.99.månedlig), INNTEKT))
        assertTrue(validererOk(inntektsvurdering, sykepengegrunnlag(1000.00.månedlig), INNTEKT))
        assertTrue(validererOk(inntektsvurdering, sykepengegrunnlag(1250.00.månedlig), INNTEKT))
        assertTrue(validererOk(inntektsvurdering, sykepengegrunnlag(750.00.månedlig), INNTEKT))
    }

    @Test
    fun `flere organisasjoner siste 3 måneder gir warning`() {
        val annenInntekt = "etAnnetOrgnr" to INNTEKT
        assertTrue(validererOk(inntektsvurdering(inntekter(YearMonth.of(2017, 12), annenInntekt)), sykepengegrunnlag(), INNTEKT, 1))
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
        assertTrue(validererOk(inntektsvurdering(inntekter(YearMonth.of(2017, 11), annenInntekt)), sykepengegrunnlag(), INNTEKT))
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
        assertTrue(validererOk(inntektsvurdering(inntekter(YearMonth.of(2017, 10), annenInntekt)), sykepengegrunnlag(), INNTEKT))
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
        assertTrue(validererOk(inntektsvurdering(inntekter(YearMonth.of(2017, 9), annenInntekt)), sykepengegrunnlag(), INNTEKT))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
        assertTrue(validererOk(inntektsvurdering(inntekter(YearMonth.of(2017, 12), annenInntekt)), sykepengegrunnlag(), INNTEKT, 2))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Inntekter for flere arbeidsgivere enn arbeidsgivere med overlappende sykdom gir warning`() {
        val annenInntekt = "etAnnetOrgnr" to INNTEKT
        assertTrue(validererOk(inntektsvurdering(inntekter(YearMonth.of(2017, 12), annenInntekt)), sykepengegrunnlag(), INNTEKT))
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    private fun inntekter(periode: YearMonth, inntekt: Pair<String, Inntekt>) =
        inntektperioderForSykepengegrunnlag {
            1.januar(2017) til 1.desember(2017) inntekter {
                ORGNR inntekt INNTEKT
            }
            periode.atDay(1) til periode.atDay(1) inntekter {
                inntekt.first inntekt inntekt.second
            }
        }

    private fun validererOk(
        inntektsvurdering: Inntektsvurdering,
        grunnlagForSykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Inntekt,
        antallArbeidsgivereMedOverlappendeVedtaksperioder: Int = 1
    ): Boolean {
        aktivitetslogg = Aktivitetslogg()
        return inntektsvurdering.valider(
            aktivitetslogg,
            grunnlagForSykepengegrunnlag,
            sammenligningsgrunnlag,
            Periodetype.FØRSTEGANGSBEHANDLING,
            antallArbeidsgivereMedOverlappendeVedtaksperioder
        )

    }

    private fun sykepengegrunnlag(inntekt: Inntekt = INNTEKT) = Sykepengegrunnlag(
        arbeidsgiverInntektsopplysninger = listOf(),
        sykepengegrunnlag = inntekt,
        grunnlagForSykepengegrunnlag = inntekt
    )

    private fun inntektsvurdering(
        inntektsmåneder: List<ArbeidsgiverInntekt> = inntektperioderForSykepengegrunnlag {
            1.januar(2017) til 1.desember(2017) inntekter {
                ORGNR inntekt INNTEKT
            }
        }
    ) = Inntektsvurdering(inntektsmåneder)
}
