package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Periodetype
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class InntektsvurderingTest {
    private companion object {
        private const val ORGNR = "123456789"
        private val INNTEKT = 1000.0.månedlig
    }

    private lateinit var aktivitetslogg: Aktivitetslogg

    @Test
    internal fun `ugyldige verdier`() {
        assertTrue(hasErrors(inntektsvurdering(emptyList()), INGEN))
        assertTrue(
            hasErrors(
                inntektsvurdering(
                    listOf(
                        Inntektsvurdering.ArbeidsgiverInntekt(
                            ORGNR,
                            listOf(
                                Inntektsvurdering.ArbeidsgiverInntekt.MånedligInntekt(
                                    YearMonth.now(),
                                    INGEN,
                                    Inntektsvurdering.Inntekttype.LØNNSINNTEKT,
                                    Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                                )
                            )
                        )
                    )
                ), INGEN
            )
        )
    }

    @Test
    internal fun `skal kunne beregne avvik mellom innmeldt lønn fra inntektsmelding og lønn fra inntektskomponenten`() {
        val inntektsvurdering = inntektsvurdering()
        assertTrue(hasErrors(inntektsvurdering, 1250.01.månedlig))
        assertTrue(hasErrors(inntektsvurdering, 749.99.månedlig))
        assertFalse(hasErrors(inntektsvurdering, 1000.00.månedlig))
        assertFalse(hasErrors(inntektsvurdering, 1250.00.månedlig))
        assertFalse(hasErrors(inntektsvurdering, 750.00.månedlig))
    }

    @Test
    fun `flere organisasjoner siste 3 måneder gir warning`() {
        val annenInntekt = "etAnnetOrgnr" to INNTEKT
        assertFalse(hasErrors(inntektsvurdering(inntekter(YearMonth.of(2017, 12), annenInntekt)), INNTEKT))
        assertTrue(aktivitetslogg.hasWarnings())
        assertFalse(hasErrors(inntektsvurdering(inntekter(YearMonth.of(2017, 11), annenInntekt)), INNTEKT))
        assertTrue(aktivitetslogg.hasWarnings())
        assertFalse(hasErrors(inntektsvurdering(inntekter(YearMonth.of(2017, 10), annenInntekt)), INNTEKT))
        assertTrue(aktivitetslogg.hasWarnings())
        assertFalse(hasErrors(inntektsvurdering(inntekter(YearMonth.of(2017, 9), annenInntekt)), INNTEKT))
        assertFalse(aktivitetslogg.hasWarnings())
    }

    private fun inntekter(periode: YearMonth, inntekt: Pair<String, Inntekt>) =
        inntektperioder {
            1.januar(2017) til 1.desember(2017) inntekter {
                ORGNR inntekt INNTEKT
            }
            periode.atDay(1) til periode.atDay(1) inntekter {
                inntekt.first inntekt inntekt.second
            }
        }

    private fun hasErrors(inntektsvurdering: Inntektsvurdering, beregnetInntekt: Inntekt): Boolean {
        aktivitetslogg = Aktivitetslogg()
        return inntektsvurdering.valider(
            aktivitetslogg,
            beregnetInntekt,
            Periodetype.FØRSTEGANGSBEHANDLING
        ).hasErrors()
    }

    private fun inntektsvurdering(
        inntektsmåneder: List<Inntektsvurdering.ArbeidsgiverInntekt> = inntektperioder {
            1.januar(2017) til 1.desember(2017) inntekter {
                ORGNR inntekt INNTEKT
            }
        }
    ) = Inntektsvurdering(inntektsmåneder)
}
