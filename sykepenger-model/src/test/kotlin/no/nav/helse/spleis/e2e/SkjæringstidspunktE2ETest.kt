package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SkjæringstidspunktE2ETest : AbstractDslTest() {

    @Test
    fun `skjæringstidspunkt skal ikke hensynta sykedager i et senere sykefraværstilefelle`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)

            håndterSøknad(mars)
            håndterSøknad(mai)

            håndterOverstyrTidslinje((1.februar til 31.mars).map { manuellForeldrepengedag(it) })
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
            assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
            assertEquals(1.januar, inspektør.skjæringstidspunkt(3.vedtaksperiode))
        }
    }

    @Test
    fun `periode med bare ferie - tidligere sykdom`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 10.januar)
            )
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), Ferie(1.mars, 31.mars))
            assertEquals(1.mars, inspektør.skjæringstidspunkt(1.vedtaksperiode))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `Finner skjæringstidspunkt for periode med arbeidsdager på slutten som overlapper med sykdom hos annen arbeidsgiver`() {
        a1 {

            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
            håndterSykmelding(Sykmeldingsperiode(24.februar, 24.mars), orgnummer = a1)
        }
        a2 {

            håndterSykmelding(Sykmeldingsperiode(25.januar, 25.februar), orgnummer = a2)
        }

        a1 {
            håndterSøknad(januar)
            håndterSøknad(24.februar til 24.mars)
        }
        a2 {
            håndterSøknad(Sykdom(25.januar, 25.februar, 100.prosent), Arbeid(20.februar, 25.februar), orgnummer = a2)
        }

        a1 {
            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beregnetInntekt = 15000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(25.januar til 10.februar),
                beregnetInntekt = 16000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }


        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            assertEquals(1.januar, inspektør(a1).skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(24.februar, inspektør(a1).skjæringstidspunkt(2.vedtaksperiode))

        }
        a2 {
            assertEquals(1.januar, inspektør(a2).skjæringstidspunkt(1.vedtaksperiode))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
        }

        a1 {
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
            nullstillTilstandsendringer()
        }

        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(24.februar til 11.mars),
                beregnetInntekt = 17000.månedlig,
                vedtaksperiodeId = 2.vedtaksperiode
            )
        }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2, orgnummer = a1)
            assertVarsel(Varselkode.RV_VV_2, 2.vedtaksperiode.filter())
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
        }

        a1 {
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
        }
    }

    @Test
    fun `Finner skjæringstidspunkt for periode med arbeidsdager på slutten som overlapper med sykdom hos annen arbeidsgiver - siste skjæringstidspunkt mangler inntekt`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSykmelding(Sykmeldingsperiode(23.februar, 24.mars))

        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(25.januar, 25.februar))
        }

        a1 {
            håndterSøknad(januar)
            håndterSøknad(23.februar til 24.mars)
        }
        a2 {
            håndterSøknad(Sykdom(25.januar, 25.februar, 100.prosent), Arbeid(20.februar, 25.februar), orgnummer = a2)
        }

        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = 15000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(25.januar til 10.februar),
                beregnetInntekt = 16000.månedlig,
                vedtaksperiodeId = 1.vedtaksperiode
            )
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterUtbetalt()
            assertEquals(1.januar, inspektør(a1).skjæringstidspunkt(1.vedtaksperiode))
            assertEquals(23.februar, inspektør(a1).skjæringstidspunkt(2.vedtaksperiode))
        }


        a2 {
            assertEquals(1.januar, inspektør(a2).skjæringstidspunkt(1.vedtaksperiode))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }

        a1 {
            assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING)
            nullstillTilstandsendringer()
        }

        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(23.februar til 10.mars),
                beregnetInntekt = 15000.månedlig,
                vedtaksperiodeId = 2.vedtaksperiode
            )
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)
        }
    }
}
