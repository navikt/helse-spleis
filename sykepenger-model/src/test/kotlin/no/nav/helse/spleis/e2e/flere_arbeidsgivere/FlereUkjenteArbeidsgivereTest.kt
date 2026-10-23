package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import no.nav.helse.dsl.*
import no.nav.helse.februar
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.arbeidsgiver
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.person.tilstandsmaskin.TilstandType.*
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

internal class FlereUkjenteArbeidsgivereTest : AbstractDslTest() {
    @Test
    fun `AI fjerner gammel IM - to arbeidsgivere - ny overlappende førstegangsbehandlingen hos ag2`() {
        val inntektA1 = INNTEKT + 500.daglig
        val inntektA2 = INNTEKT

        listOf(a1, a2).nyeVedtak(januar, inntekt = INNTEKT)
        a1 { forlengVedtak(februar) }
        a1 { forlengVedtak(mars) }

        a1 {
            håndterKorrigerteArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beregnetInntekt = inntektA1,
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }

        a2 { assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter()) }

        nullstillTilstandsendringer()
        a2 {
            håndterSøknad(Sykdom(1.mars, 20.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                beregnetInntekt = inntektA2,
            )
        }

        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, inntektA1, inntektA1)
                assertInntektsgrunnlag(a2, inntektA2, inntektA2)
            }
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
                vedtaksperiodeId = 1.vedtaksperiode,
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
        nullstillTilstandsendringer()

        // a2 sent til festen
        val id =
            a2 {
                håndterGammelInntektsmeldingForÅBliFangetOppAvReplay(
                    listOf(1.januar til 16.januar),
                    begrunnelseForReduksjonEllerIkkeUtbetalt = "ja",
                )
            }
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
                AVVENTER_GODKJENNING,
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
                vedtaksperiodeId = 1.vedtaksperiode,
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
        val imId =
            MeldingsreferanseId(
                a2 {
                    håndterGammelInntektsmeldingForÅBliFangetOppAvReplay(
                        listOf(1.januar til 16.januar),
                        begrunnelseForReduksjonEllerIkkeUtbetalt = "ja",
                    )
                },
            )
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
                    Dokumentsporing.inntektsmeldingRefusjon(imId),
                    Dokumentsporing.inntektsmeldingInntekt(imId),
                ),
                inspektør.hendelser(1.vedtaksperiode),
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
                AVSLUTTET,
            )
        }
        a2 { assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK) }
    }

    @Test
    fun `to arbeidsgivere - ny overlappende førstegangsbehandling hos ag2 som først var antatt å være frisk - men tidlig inntektsmelding`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar)) }
        val imId =
            MeldingsreferanseId(
                a2 {
                    håndterGammelInntektsmeldingForÅBliFangetOppAvReplay(
                        listOf(1.januar til 16.januar),
                        begrunnelseForReduksjonEllerIkkeUtbetalt = "ja",
                    )
                },
            )

        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode,
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
                    Dokumentsporing.inntektsmeldingRefusjon(imId),
                    Dokumentsporing.inntektsmeldingInntekt(imId),
                ),
                inspektør.hendelser(1.vedtaksperiode),
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
                AVSLUTTET,
            )
        }
        a2 { assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK) }
    }
}
