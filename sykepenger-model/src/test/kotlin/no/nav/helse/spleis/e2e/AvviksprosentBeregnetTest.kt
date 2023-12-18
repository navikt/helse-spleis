package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.YearMonth
import no.nav.helse.assertForventetFeil
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AvviksprosentBeregnetTest : AbstractEndToEndTest() {

    @Test
    fun `Sender forventet AvviksprosentBeregnetEvent ved enkel førstegangsbehandling`() {
        nyttVedtak(1.januar, 31.januar)

        assertEquals(1, observatør.avviksprosentBeregnetEventer.size)
        assertEvent(
            observatør.avviksprosentBeregnetEventer.first(),
            1.januar,
            0.0,
            372000.0,
            372000.0,
            listOf(PersonObserver.AvviksprosentBeregnetEvent.OmregnetÅrsinntekt(a1, 372000.0)),
            listOf(forventetSammenligningsgrunnlag(a1, 1.januar, 31000.0))
        )
    }

    @Test
    fun `Sender forventet AvviksprosentBeregnetEvent for periode med overstyrt inntekt`() {
        tilGodkjenning(1.mars, 31.mars, a1)
        håndterOverstyrInntekt(22000.månedlig, orgnummer = a1, skjæringstidspunkt = 1.mars)

        assertForventetFeil("Skal sende ut nytt event ved ny avviksvurdering",
            nå = {
                assertEvent(
                    observatør.avviksprosentBeregnetEventer.last(),
                    1.mars,
                    0.0,
                    240000.0,
                    240000.0,
                    listOf(PersonObserver.AvviksprosentBeregnetEvent.OmregnetÅrsinntekt(a1, 240000.0)),
                    listOf(forventetSammenligningsgrunnlag(a1, 1.mars, 20000.0))
                )
            },
            ønsket = {
                assertEvent(
                    observatør.avviksprosentBeregnetEventer.last(),
                    1.mars,
                    10.1,
                    240000.0,
                    252000.0,
                    listOf(PersonObserver.AvviksprosentBeregnetEvent.OmregnetÅrsinntekt(a1, 252000.0)),
                    listOf(forventetSammenligningsgrunnlag(a1, 1.mars, 20000.0))
                )
            }
        )

    }

    private fun assertEvent(
        event: PersonObserver.AvviksprosentBeregnetEvent,
        skjæringstidspunkt: LocalDate,
        avviksprosent: Double,
        sammenligningsgrunnlagTotalbeløp: Double,
        beregningsgrunnlagTotalbeløp: Double,
        omregnedeÅrsinntekter: List<PersonObserver.AvviksprosentBeregnetEvent.OmregnetÅrsinntekt>,
        sammenligningsgrunnlag: List<PersonObserver.AvviksprosentBeregnetEvent.Sammenligningsgrunnlag>
    ) {
        assertEquals(skjæringstidspunkt, event.skjæringstidspunkt)
        assertEquals(avviksprosent, event.avviksprosent)
        assertEquals(sammenligningsgrunnlagTotalbeløp, event.sammenligningsgrunnlagTotalbeløp)
        assertEquals(beregningsgrunnlagTotalbeløp, event.beregningsgrunnlagTotalbeløp)
        assertEquals(omregnedeÅrsinntekter, event.omregnedeÅrsinntekter)
        assertEquals(sammenligningsgrunnlag, event.sammenligningsgrunnlag)
    }

    private fun forventetSammenligningsgrunnlag(
        orgnummer: String,
        skjæringstidspunkt: LocalDate,
        månedligInntekt: Double
    ): PersonObserver.AvviksprosentBeregnetEvent.Sammenligningsgrunnlag {
        val månedligeInntekter =
            (skjæringstidspunkt.minusYears(1) til skjæringstidspunkt.minusMonths(1)).lagSkatteopplysninger(
                månedligInntekt
            )
        return PersonObserver.AvviksprosentBeregnetEvent.Sammenligningsgrunnlag(orgnummer, månedligeInntekter)
    }

    private fun Periode.lagSkatteopplysninger(beløp: Double) =
        this.map(YearMonth::from)
            .distinct()
            .map { måned ->
                PersonObserver.AvviksprosentBeregnetEvent.Sammenligningsgrunnlag.Skatteopplysning(
                    beløp = beløp,
                    måned = måned,
                    type = "LØNNSINNTEKT",
                    fordel = "kontantytelse",
                    beskrivelse = "fastloenn"
                )
            }

}