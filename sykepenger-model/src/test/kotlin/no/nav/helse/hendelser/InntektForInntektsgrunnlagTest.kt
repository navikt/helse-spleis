package no.nav.helse.hendelser

import java.time.YearMonth
import no.nav.helse.april
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.februar
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class InntektForInntektsgrunnlagTest {

    @Test
    fun `Beregner riktig antall måneder mellom første og siste inntektsmåned i sykepengegrunnlaget`() {
        val inntekter = listOf(
            ArbeidsgiverInntekt(
                "orgnummer",
                (1..3).map {
                    ArbeidsgiverInntekt.MånedligInntekt(
                        YearMonth.of(2017, it),
                        31000.månedlig, LØNNSINNTEKT, "hva som helst", "hva som helst"
                    )
                }
            ),
            ArbeidsgiverInntekt(
                "orgnummer2", (1..2).map {
                ArbeidsgiverInntekt.MånedligInntekt(
                    YearMonth.of(2017, it),
                    31000.månedlig, LØNNSINNTEKT, "hva som helst", "hva som helst"
                )
            }
            )
        )
        assertEquals(3, inntekter.antallMåneder())
    }

    @Test
    fun `Gir error hvis inntekter for sykepengegrunnlag har mer enn 3 inntektsmåneder`() {
        val inntekter = listOf(
            ArbeidsgiverInntekt(
                "orgnummer",
                (1..4).map {
                    ArbeidsgiverInntekt.MånedligInntekt(
                        YearMonth.of(2017, it),
                        31000.månedlig, LØNNSINNTEKT, "hva som helst", "hva som helst"
                    )
                }
            ),
        )
        assertThrows<IllegalArgumentException> {
            InntektForSykepengegrunnlag(inntekter = inntekter)
        }
    }

    @Test
    fun `Frilanserinntekt i løpet av de 3 månedene gir error`() {
        val aktivitetslogg = Aktivitetslogg()
        val inntektForSykepengegrunnlag = lagStandardSykepengegrunnlag("orgnummer", 31000.månedlig, 1.april(2017))
        val arbeidsforhold = Vilkårsgrunnlag.Arbeidsforhold("orgnummer", 1.januar(2017), 28.februar(2017), Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.FRILANSER)
        arbeidsforhold.validerFrilans(aktivitetslogg, 1.februar(2017), emptyList(), inntektForSykepengegrunnlag)
        assertTrue(aktivitetslogg.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `Frilanserarbeidsforhold og frilanserinntekt i forskjellige måneder gir ikke error`() {
        val aktivitetslogg = Aktivitetslogg()
        val inntekter = listOf(
            ArbeidsgiverInntekt(
                "orgnummer",
                listOf(
                    ArbeidsgiverInntekt.MånedligInntekt(
                        YearMonth.of(2017, 1),
                        31000.månedlig, LØNNSINNTEKT, "hva som helst", "hva som helst"
                    )
                )
            ),
        )
        val inntektForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter)
        val arbeidsforhold = Vilkårsgrunnlag.Arbeidsforhold("orgnummer", 1.februar(2017), 28.februar(2017), Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.FRILANSER)
        arbeidsforhold.validerFrilans(aktivitetslogg, 1.februar(2017), emptyList(), inntektForSykepengegrunnlag)
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `Frilanser-arbeidsforhold uten inntekt de siste 3 månedene gir ikke error`() {
        val aktivitetslogg = Aktivitetslogg()
        val inntektForSykepengegrunnlag = lagStandardSykepengegrunnlag("orgnummer", 31000.månedlig, 1.april(2017))
        val arbeidsforhold = Vilkårsgrunnlag.Arbeidsforhold("orgnummer2", 1.januar(2017), 31.januar(2017), Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.FRILANSER)
        arbeidsforhold.validerFrilans(aktivitetslogg, 1.februar(2017), emptyList(), inntektForSykepengegrunnlag)
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `Finner frilansinntekt måneden før skjæringstidspunkt`() {
        val inntektForSykepengegrunnlag = lagStandardSykepengegrunnlag("orgnummer", 31000.månedlig, 1.april(2017))
        val arbeidsforhold = Vilkårsgrunnlag.Arbeidsforhold("orgnummer", 1.januar(2017), 31.januar(2017), Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.FRILANSER)
        val aktivitetslogg = Aktivitetslogg()
        arbeidsforhold.validerFrilans(aktivitetslogg, 1.april(2017), emptyList(), inntektForSykepengegrunnlag)
        assertTrue(aktivitetslogg.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `Finner ikke frilansinntekt måneden før skjæringstidspunkt`() {
        val inntektForSykepengegrunnlag = lagStandardSykepengegrunnlag("orgnummer", 31000.månedlig, 1.april(2017))
        val arbeidsforhold = Vilkårsgrunnlag.Arbeidsforhold("orgnummer2", 1.januar(2017), 31.januar(2017), Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype.FRILANSER)
        val aktivitetslogg = Aktivitetslogg()
        arbeidsforhold.validerFrilans(aktivitetslogg, 1.april(2017), emptyList(), inntektForSykepengegrunnlag)
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
    }
}
