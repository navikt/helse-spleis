package no.nav.helse.hendelser

import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Sykepengegrunnlag
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class InntektForSykepengegrunnlagTest {

    @Test
    fun `Beregner riktig antall måneder mellom første og siste inntektsmåned i sykepengegrunnlaget`() {
        val inntekter = listOf(
            ArbeidsgiverInntekt(
                "orgnummer",
                (1..3).map {
                    Sykepengegrunnlag(
                        YearMonth.of(2017, it),
                        31000.månedlig, LØNNSINNTEKT, "hva som helst", "hva som helst"
                    )
                }
            ),
            ArbeidsgiverInntekt(
                "orgnummer2", (1..2).map {
                    Sykepengegrunnlag(
                        YearMonth.of(2017, it),
                        31000.månedlig, LØNNSINNTEKT, "hva som helst", "hva som helst"
                    )
                }
            )
        )
        assertEquals(3, inntekter.antallMåneder())
    }

    @Test
    fun `Gir ikke error hvis inntekter for sykepengegrunnlag har færre enn 3 inntektsmåneder`() {
        val aktivitetslogg = Aktivitetslogg()
        val inntekter = listOf(
            ArbeidsgiverInntekt(
                "orgnummer",
                (1..2).map {
                    Sykepengegrunnlag(
                        YearMonth.of(2017, it),
                        31000.månedlig, LØNNSINNTEKT, "hva som helst", "hva som helst"
                    )
                }
            ),
            ArbeidsgiverInntekt(
                "orgnummer2", (1..2).map {
                    Sykepengegrunnlag(
                        YearMonth.of(2017, it),
                        31000.månedlig, LØNNSINNTEKT, "hva som helst", "hva som helst"
                    )
                }
            )
        )
        val inntektForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter)
        assertFalse(inntektForSykepengegrunnlag.valider(aktivitetslogg).hasErrorsOrWorse())
    }

    @Test
    fun `Gir error hvis inntekter for sykepengegrunnlag har mer enn 3 inntektsmåneder`() {
        val aktivitetslogg = Aktivitetslogg()
        val inntekter = listOf(
            ArbeidsgiverInntekt(
                "orgnummer",
                (1..4).map {
                    Sykepengegrunnlag(
                        YearMonth.of(2017, it),
                        31000.månedlig, LØNNSINNTEKT, "hva som helst", "hva som helst"
                    )
                }
            ),
        )
        val inntektForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter)
        assertTrue(inntektForSykepengegrunnlag.valider(aktivitetslogg).hasErrorsOrWorse())
    }

    @Test
    fun `Gir ingen error hvis inntekter for sykepengegrunnlag er 3 inntektsmåneder`() {
        val aktivitetslogg = Aktivitetslogg()
        val inntekter = listOf(
            ArbeidsgiverInntekt(
                "orgnummer",
                (1..3).map {
                    Sykepengegrunnlag(
                        YearMonth.of(2017, it),
                        31000.månedlig, LØNNSINNTEKT, "hva som helst", "hva som helst"
                    )
                }
            ),
        )
        val inntektForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter)
        assertFalse(inntektForSykepengegrunnlag.valider(aktivitetslogg).hasErrorsOrWorse())
    }


}
