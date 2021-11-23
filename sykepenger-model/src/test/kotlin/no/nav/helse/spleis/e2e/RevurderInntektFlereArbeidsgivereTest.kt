package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.Kilde
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.util.*

@Isolated
internal class RevurderInntektFlereArbeidsgivereTest: AbstractEndToEndTest() {
    private companion object {
        const val AG1 = "123456789"
        const val AG2 = "987654321"
    }

    @Test
    fun `kun den arbeidsgiveren som har fått overstyrt inntekt som faktisk lagrer inntekten`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = AG1)
        håndterYtelser(orgnummer = AG1)
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = AG1, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                val skjæringstidspunkt = inspektør(AG1).skjæringstidspunkt(1.vedtaksperiode)
                skjæringstidspunkt.minusMonths(12L).withDayOfMonth(1) til skjæringstidspunkt.minusMonths(1L).withDayOfMonth(1) inntekter {
                    AG1 inntekt INNTEKT
                }
                skjæringstidspunkt.minusMonths(24L).withDayOfMonth(1) til skjæringstidspunkt.minusMonths(13L).withDayOfMonth(1) inntekter {
                    AG2 inntekt INNTEKT
                }
            })
        )
        håndterYtelser(orgnummer = AG1)
        håndterSimulering(orgnummer = AG1)
        håndterUtbetalingsgodkjenning(orgnummer = AG1)
        håndterUtbetalt(orgnummer = AG1)

        håndterOverstyrInntekt(orgnummer = AG1, skjæringstidspunkt = 1.januar)
        assertEquals(1, inspektør(AG1).inntektInspektør.antallOpplysinger(Kilde.SAKSBEHANDLER))
        assertEquals(0, inspektør(AG2).inntektInspektør.antallOpplysinger(Kilde.SAKSBEHANDLER))
    }

    @Test
    fun `alle perioder for alle arbeidsgivere med aktuelt skjæringstidspunkt skal ha hendelseIden`() {
        Toggle.RevurdereInntektMedFlereArbeidsgivere.enable {
            nyeVedtak(1.januar, 31.januar, AG1, AG2)
            val hendelseId = UUID.randomUUID()
            håndterOverstyrInntekt(orgnummer = AG1, skjæringstidspunkt = 1.januar, meldingsreferanseId = hendelseId)

            assertHarHendelseIder(1.vedtaksperiode, hendelseId, orgnummer = AG1)
            assertHarIkkeHendelseIder(1.vedtaksperiode, hendelseId, orgnummer = AG2)
        }
    }
}
