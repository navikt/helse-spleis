package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET
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

internal class InntektsvurderingTest {
    private companion object {
        private const val ORGNR = "123456789"
        private val INNTEKT = 1000.0.månedlig
    }

    private lateinit var aktivitetslogg: Aktivitetslogg

    @Test
    fun `ugyldige verdier`() {
        assertFalse(validererOk(inntektsvurdering(emptyList()), sykepengegrunnlag(INGEN), INGEN, 1))
        assertFalse(
            validererOk(
                inntektsvurdering(
                    inntektperioderForSammenligningsgrunnlag {
                        LocalDate.now() til LocalDate.now() inntekter {
                            ORGNR inntekt INGEN
                        }
                    }
                ), sykepengegrunnlag(INGEN), INGEN, 1
            )
        )
    }

    @Test
    fun `skal kunne beregne avvik mellom innmeldt lønn fra inntektsmelding og lønn fra inntektskomponenten`() {
        val inntektsvurdering = inntektsvurdering()
        assertFalse(validererOk(inntektsvurdering, sykepengegrunnlag(1250.01.månedlig), INNTEKT, 1))
        assertFalse(validererOk(inntektsvurdering, sykepengegrunnlag(749.99.månedlig), INNTEKT, 1))
        assertTrue(validererOk(inntektsvurdering, sykepengegrunnlag(1000.00.månedlig), INNTEKT, 1))
        assertTrue(validererOk(inntektsvurdering, sykepengegrunnlag(1250.00.månedlig), INNTEKT, 1))
        assertTrue(validererOk(inntektsvurdering, sykepengegrunnlag(750.00.månedlig), INNTEKT, 1))
    }

    private fun validererOk(
        inntektsvurdering: Inntektsvurdering,
        grunnlagForSykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Inntekt,
        antallArbeidsgivereFraAareg: Int
    ): Boolean {
        aktivitetslogg = Aktivitetslogg()
        return inntektsvurdering.valider(
            aktivitetslogg,
            grunnlagForSykepengegrunnlag,
            sammenligningsgrunnlag,
            antallArbeidsgivereFraAareg
        )
    }

    private fun sykepengegrunnlag(inntekt: Inntekt = INNTEKT) = Sykepengegrunnlag(
        arbeidsgiverInntektsopplysninger = listOf(),
        sykepengegrunnlag = inntekt,
        grunnlagForSykepengegrunnlag = inntekt,
        begrensning = ER_IKKE_6G_BEGRENSET
    )

    private fun inntektsvurdering(
        inntektsmåneder: List<ArbeidsgiverInntekt> = inntektperioderForSykepengegrunnlag {
            1.januar(2017) til 1.desember(2017) inntekter {
                ORGNR inntekt INNTEKT
            }
        }
    ) = Inntektsvurdering(inntektsmåneder)
}
