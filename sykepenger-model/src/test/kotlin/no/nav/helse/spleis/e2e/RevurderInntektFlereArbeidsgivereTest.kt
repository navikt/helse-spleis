package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.Kilde.SAKSBEHANDLER
import no.nav.helse.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.RevurdereInntektMedFlereArbeidsgivere::class)
internal class RevurderInntektFlereArbeidsgivereTest: AbstractDslTest() {

    @Test
    fun `kun den arbeidsgiveren som har fått overstyrt inntekt som faktisk lagrer inntekten`() {
        a2 {
            nyttVedtak(1.januar(2017), 31.januar(2017), 100.prosent) // gammelt vedtak
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterYtelser(1.vedtaksperiode)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                    inntekter = listOf(
                        sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, 1.januar, INNTEKT.repeat(3))
                    ), arbeidsforhold = emptyList()
                ),
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            håndterOverstyrInntekt(skjæringstidspunkt = 1.januar, inntekt = 25000.månedlig)
            assertAntallInntektsopplysninger(1, SAKSBEHANDLER)
        }
        a2 {
            assertAntallInntektsopplysninger(0, SAKSBEHANDLER)
        }
    }

    @Test
    fun `alle perioder for alle arbeidsgivere med aktuelt skjæringstidspunkt skal ha hendelseIden`() {
        (a1 og a2).nyeVedtak(1.januar til 31.januar)
        val hendelseId = UUID.randomUUID()
        a1 {
            håndterOverstyrInntekt(hendelseId, 1.januar, 25000.månedlig)
            assertHarHendelseIder(1.vedtaksperiode, hendelseId)
        }
        a2 {
            assertHarIkkeHendelseIder(1.vedtaksperiode, hendelseId)
        }
    }
}
