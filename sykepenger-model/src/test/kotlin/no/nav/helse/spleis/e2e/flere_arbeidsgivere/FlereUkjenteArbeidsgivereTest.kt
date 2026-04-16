package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import java.util.UUID
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.EventSubscription.OverstyringIgangsatt.TypeEndring.OVERSTYRING
import no.nav.helse.person.EventSubscription.OverstyringIgangsatt.TypeEndring.REVURDERING
import no.nav.helse.person.EventSubscription.OverstyringIgangsatt.VedtaksperiodeData
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FlereUkjenteArbeidsgivereTest : AbstractDslTest() {

    @Test
    fun `to arbeidsgivere - ny overlappende førstegangsbehandlingen hos ag2`() {
        val inntektA1 = INNTEKT + 500.daglig
        val inntektA2 = INNTEKT

        listOf(a1, a2).nyeVedtak(januar, inntekt = INNTEKT)
        a1 { forlengVedtak(februar) }
        a1 { forlengVedtak(mars) }

        val im1 = a1 {
            val id = håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = inntektA1
            )
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            id
        }

        a2 { assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter()) }

        val im2 = a2 {
            håndterInntektsmelding(
                listOf(1.mars til 16.mars),
                beregnetInntekt = inntektA2
            )
        }
        nullstillTilstandsendringer()
        val søknad = UUID.randomUUID()
        a2 { håndterSøknad(Sykdom(1.mars, 20.mars, 100.prosent), søknadId = søknad) }

        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, inntektA1, inntektA1)
                assertInntektsgrunnlag(a2, inntektA2, inntektA2)
            }
        }

        val overstyringerIgangsatt = observatør.overstyringIgangsatt
        assertEquals(9, overstyringerIgangsatt.size)

        overstyringerIgangsatt[6].also { event ->
            assertEquals(
                EventSubscription.OverstyringIgangsatt(
                    årsak = "KORRIGERT_INNTEKTSMELDING_INNTEKTSOPPLYSNINGER",
                    skjæringstidspunkt = 1.januar,
                    periodeForEndring = 1.januar til 1.januar,
                    berørtePerioder = listOf(
                        VedtaksperiodeData(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1), 1.vedtaksperiode(a1), 1.januar til 31.januar, 1.januar, REVURDERING),
                        VedtaksperiodeData(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1), 2.vedtaksperiode(a1), februar, 1.januar, REVURDERING),
                        VedtaksperiodeData(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1), 3.vedtaksperiode(a1), 1.mars til 31.mars, 1.januar, REVURDERING),
                        VedtaksperiodeData(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a2), 1.vedtaksperiode(a2), 1.januar til 31.januar, 1.januar, REVURDERING)
                    ),
                    meldingsreferanseId = im1
                ), event
            )
        }

        overstyringerIgangsatt[7].also { event ->
            assertEquals(
                EventSubscription.OverstyringIgangsatt(
                    årsak = "NY_PERIODE",
                    skjæringstidspunkt = 1.januar,
                    periodeForEndring = 1.mars til 20.mars,
                    berørtePerioder = listOf(
                        VedtaksperiodeData(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1), 3.vedtaksperiode(a1), 1.mars til 31.mars, 1.januar, REVURDERING),
                        VedtaksperiodeData(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a2), 2.vedtaksperiode(a2), 1.mars til 20.mars, 1.januar, OVERSTYRING)
                    ),
                    meldingsreferanseId = søknad
                ), event
            )
        }

        overstyringerIgangsatt[8].also { event ->
            assertEquals(
                EventSubscription.OverstyringIgangsatt(
                    årsak = "INNTEKT_FRA_INNTEKTSMELDING",
                    skjæringstidspunkt = 1.mars,
                    periodeForEndring = 1.mars til 20.mars,
                    berørtePerioder = listOf(
                        VedtaksperiodeData(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1), 3.vedtaksperiode(a1), 1.mars til 31.mars, 1.januar, REVURDERING),
                        VedtaksperiodeData(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a2), 2.vedtaksperiode(a2), 1.mars til 20.mars, 1.januar, OVERSTYRING)
                    ),
                    meldingsreferanseId = im2
                ), event
            )
        }

        a1 {
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(3.vedtaksperiode, AVVENTER_REVURDERING)
        }
        a2 {
            assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `to arbeidsgivere - ny overlappende førstegangsbehandling hos ag2 som først var ansett som ghost`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
        nullstillTilstandsendringer()

        // a2 sent til festen
        val id = a2 {
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "ja"
            )
        }
        assertEquals(id, observatør.inntektsmeldingIkkeHåndtert.single())
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }

        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }

        a2 { assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter()) }
        a1 {
            assertTilstander(
                1.vedtaksperiode,
                AVVENTER_GODKJENNING,
                AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING
            )
        }
        a2 { assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE) }
    }

    @Test
    fun `to arbeidsgivere - ny overlappende førstegangsbehandling hos ag2 som først var ansett som ghost - a1 revurderes`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        nullstillTilstandsendringer()

        // a2 sent til festen
        val imId = MeldingsreferanseId(a2 {
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "ja"
            )
        })
        assertEquals(imId.id, observatør.inntektsmeldingIkkeHåndtert.single())
        a2 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar)) }
        val søknadId = UUID.randomUUID()
        a2 { håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), søknadId = søknadId) }

        a2 { assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter()) }

        a2 {
            assertBeløpstidslinje(inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje, januar, INNTEKT, imId.id)

            assertEquals(
                setOf(
                    Dokumentsporing.søknad(MeldingsreferanseId(søknadId)),
                    Dokumentsporing.inntektsmeldingDager(imId),
                    Dokumentsporing.inntektsmeldingInntekt(imId)
                ), inspektør.hendelser(1.vedtaksperiode)
            )
        }

        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }

        a2 { assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter()) }
        a1 {
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVSLUTTET
            )
        }
        a2 { assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK) }
    }

    @Test
    fun `to arbeidsgivere - ny overlappende førstegangsbehandling hos ag2 som først var antatt å være frisk - men tidlig inntektsmelding`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar)) }
        val imId = MeldingsreferanseId(a2 {
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                begrunnelseForReduksjonEllerIkkeUtbetalt = "ja"
            )
        })
        assertEquals(imId.id, observatør.inntektsmeldingIkkeHåndtert.single())

        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        nullstillTilstandsendringer()

        a2 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar)) } // a2 sent til festen, men med ting liggende i vilkårsgrunnlaget
        val søknadId = UUID.randomUUID()
        a2 { håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), søknadId = søknadId) }

        a2 { assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter()) }

        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT)
            }
        }
        a2 {
            assertBeløpstidslinje(Beløpstidslinje.fra(januar, INNTEKT, imId.id.arbeidsgiver), inspektør.refusjon(1.vedtaksperiode))

            assertEquals(
                setOf(
                    Dokumentsporing.søknad(MeldingsreferanseId(søknadId)),
                    Dokumentsporing.inntektsmeldingDager(imId),
                    Dokumentsporing.inntektsmeldingInntekt(imId)
                ), inspektør.hendelser(1.vedtaksperiode)
            )
            assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTilstander(
                1.vedtaksperiode,
                AVSLUTTET,
                AVVENTER_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVSLUTTET
            )
        }
        a2 { assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK) }
    }
}
