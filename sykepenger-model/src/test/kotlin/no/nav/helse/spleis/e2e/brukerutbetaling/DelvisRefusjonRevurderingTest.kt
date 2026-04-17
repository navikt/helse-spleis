package no.nav.helse.spleis.e2e.brukerutbetaling

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.a2
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class DelvisRefusjonRevurderingTest : AbstractDslTest() {

    @Test
    fun `korrigerende inntektsmelding med halvering av inntekt setter riktig refusjonsbeløp fra nyeste inntektsmelding`() {
        a1 {
            nyttVedtak(januar, 100.prosent, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)

            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT / 2,
                refusjon = Inntektsmelding.Refusjon(INNTEKT / 2, null, emptyList())
            )

            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())

            val skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode)
            håndterOverstyrInntekt(skjæringstidspunkt, INNTEKT / 2)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterSkjønnsmessigFastsettelse(skjæringstidspunkt, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT / 2)))

            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()

            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 715, subset = 1.januar til 16.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 715, 715, subset = 17.januar til 31.januar)
        }
    }

    @Test
    fun `overstyring av inntekt med økning av inntekt uten nytt refusjonsbeløp`() {
        a1 {
            nyttVedtak(januar, 100.prosent, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)
            nullstillTilstandsendringer()
            håndterOverstyrInntekt(inspektør.skjæringstidspunkt(1.vedtaksperiode), 50000.månedlig)
            håndterYtelser(1.vedtaksperiode)
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING)
        }
    }

    @Test
    fun `overstyring av inntekt med nedjustering av inntekt uten nytt refusjonsbeløp`() {
        a1 {
            nyttVedtak(januar, 100.prosent, refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()))
            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar til 31.januar)

            håndterOverstyrInntekt(inspektør.skjæringstidspunkt(1.vedtaksperiode), INNTEKT / 2)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT / 2)))

            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()

            assertUtbetalingsbeløp(1.vedtaksperiode, 0, 1431, subset = 1.januar til 16.januar)
            assertUtbetalingsbeløp(1.vedtaksperiode, 715, 1431, subset = 17.januar til 31.januar)
        }
    }

    @Test
    fun `to arbeidsgivere hvor inntekten på den ene senkes slik at den andre arbeidsgiveren får brukerutbetalinger`() {
        val a1Inntekt = 50000.månedlig
        val a2Inntekt = 10000.månedlig

        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        }
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = a1Inntekt,
                refusjon = Inntektsmelding.Refusjon(a1Inntekt, null, emptyList()),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = a2Inntekt,
                refusjon = Inntektsmelding.Refusjon(INGEN, null, emptyList()),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        }
        a1 {
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 2308,
                subset = 1.januar til 16.januar
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 2161,
                forventetArbeidsgiverRefusjonsbeløp = 2308,
                subset = 17.januar til 31.januar
            )
        }
        a2 {
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 0,
                subset = 1.januar til 16.januar
            )
            assertUtbetalingsbeløp(
                vedtaksperiodeId = 1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 0,
                forventetArbeidsgiverRefusjonsbeløp = 0,
                subset = 17.januar til 31.januar
            )
        }
        a1 {
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                beregnetInntekt = a1Inntekt / 2,
                refusjon = Inntektsmelding.Refusjon(a1Inntekt / 2, null, emptyList())
            )
            håndterOverstyrInntekt(
                skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode),
                inntekt = a1Inntekt / 2
            )
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_IM_4, Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REVURDERING) }
    }
}
