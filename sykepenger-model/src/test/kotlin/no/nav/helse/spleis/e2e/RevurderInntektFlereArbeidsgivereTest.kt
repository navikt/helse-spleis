package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.Kilde
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class RevurderInntektFlereArbeidsgivereTest: AbstractEndToEndTest() {
    private companion object {
        val AG1 = "123456789".somOrganisasjonsnummer()
        val AG2 = "987654321".somOrganisasjonsnummer()
    }

    @Test
    fun `kun den arbeidsgiveren som har fått overstyrt inntekt som faktisk lagrer inntekten`() {
        nyttVedtak(1.januar(2017), 31.januar(2017), 100.prosent, orgnummer= AG2) // gammelt vedtak
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = AG1)
        håndterYtelser(orgnummer = AG1)
        val skjæringstidspunkt = inspektør(AG1).skjæringstidspunkt(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, orgnummer = AG1, inntektsvurdering = Inntektsvurdering(
                inntekter = listOf(
                    sammenligningsgrunnlag(AG1, skjæringstidspunkt, INNTEKT.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    grunnlag(AG1, skjæringstidspunkt, INNTEKT.repeat(3))
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Arbeidsforhold(AG1.toString(), LocalDate.EPOCH, null),
                Arbeidsforhold(AG2.toString(), LocalDate.EPOCH, null)
            )
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
