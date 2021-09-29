package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
internal class RevurderTidslinjeFlereAGTest : AbstractEndToEndTest() {

    private companion object {
        private const val AG1 = "123456789"
        private const val AG2 = "987612345"
    }

    @Test
    fun `to AG - én periode på hver - én blir revurdert`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = AG2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = AG2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = AG1
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = AG2
        )

        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = AG1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = AG1,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    AG1 inntekt 20000.månedlig
                    AG2 inntekt 20000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = AG1)

        håndterYtelser(1.vedtaksperiode, orgnummer = AG2)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = AG2)

        håndterOverstyring((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = AG1)

        håndterYtelser(1.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG2)

        inspektør(AG1) {
            assertTilstander(
                1.vedtaksperiode,
                *TIL_AVSLUTTET_FØRSTEGANGSBEHANDLING(),
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertHasNoErrors()
            assertEquals(2, utbetalinger.filter { it.erAvsluttet() }.size)
        }

        inspektør(AG2) {
            assertTilstander(
                1.vedtaksperiode,
                *TIL_AVSLUTTET_FØRSTEGANGSBEHANDLING(false),
                AVVENTER_ARBEIDSGIVERE_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVSLUTTET
            )
            assertHasNoErrors()
            assertEquals(2, utbetalinger.filter { it.erAvsluttet() }.size)
        }
    }

    @Test
    fun `to AG - to perioder på den ene der den siste er ufullstendig, én periode på den andre - én blir revurdert`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = AG2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = AG2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = AG1
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = AG2
        )

        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = AG1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = AG1,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    AG1 inntekt 20000.månedlig
                    AG2 inntekt 20000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = AG1)

        håndterYtelser(1.vedtaksperiode, orgnummer = AG2)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = AG2)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = AG1)

        håndterOverstyring((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = AG1)

        håndterYtelser(1.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG2)

        inspektør(AG1) {
            assertTilstander(
                1.vedtaksperiode,
                *TIL_AVSLUTTET_FØRSTEGANGSBEHANDLING(),
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
                MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
                MOTTATT_SYKMELDING_FERDIG_FORLENGELSE
            )
            assertHasNoErrors()
            assertEquals(2, utbetalinger.filter { it.erAvsluttet() }.size)
        }

        inspektør(AG2) {
            assertTilstander(
                1.vedtaksperiode,
                *TIL_AVSLUTTET_FØRSTEGANGSBEHANDLING(false),
                AVVENTER_ARBEIDSGIVERE_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVSLUTTET
            )
            assertHasNoErrors()
            assertEquals(2, utbetalinger.filter { it.erAvsluttet() }.size)
        }
    }

    @Test
    fun `to AG - to perioder på hver - første periode blir revurdert på én AG og avventer godkjenning`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = AG2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = AG2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = AG1
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = AG2
        )

        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = AG1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = AG1,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    AG1 inntekt 20000.månedlig
                    AG2 inntekt 20000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = AG1)

        håndterYtelser(1.vedtaksperiode, orgnummer = AG2)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = AG2)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = AG1)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = AG2)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = AG1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = AG2)

        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = AG1)
        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = AG2)
        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = AG1)

        håndterSimulering(2.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = AG1)

        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = AG2)

        håndterSimulering(2.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = AG2)

        håndterOverstyring((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)
        håndterYtelser(1.vedtaksperiode, orgnummer = AG2)
        håndterYtelser(2.vedtaksperiode, orgnummer = AG1)

        håndterSimulering(2.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = AG1)

        håndterYtelser(2.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = AG2)

        inspektør(AG1) {
            assertTilstander(
                1.vedtaksperiode,
                *TIL_AVSLUTTET_FØRSTEGANGSBEHANDLING(),
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                *TIL_AVSLUTTET_FORLENGELSE(),
                AVVENTER_ARBEIDSGIVERE_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertHasNoErrors()
            assertEquals(2, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(2, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }

        inspektør(AG2) {
            assertTilstander(
                1.vedtaksperiode,
                *TIL_AVSLUTTET_FØRSTEGANGSBEHANDLING(false),
                AVVENTER_ARBEIDSGIVERE_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                *TIL_AVSLUTTET_FORLENGELSE(false),
                AVVENTER_ARBEIDSGIVERE_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                AVSLUTTET
            )
            assertHasNoErrors()
            assertEquals(2, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(2, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }
    }

    @Test
    fun `to AG - to perioder på hver - én blir revurdert på én AG`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = AG2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = AG2)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = AG1
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = AG2
        )

        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = AG1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = AG1,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    AG1 inntekt 20000.månedlig
                    AG2 inntekt 20000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = AG1)

        håndterYtelser(1.vedtaksperiode, orgnummer = AG2)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = AG2)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = AG1)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = AG2)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = AG1)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = AG2)

        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = AG1)
        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = AG2)
        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = AG1)

        håndterSimulering(2.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = AG1)

        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = AG2)

        håndterSimulering(2.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = AG2)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = AG2)

        håndterOverstyring((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)
        håndterYtelser(1.vedtaksperiode, orgnummer = AG2)
        håndterYtelser(2.vedtaksperiode, orgnummer = AG1)

        håndterSimulering(2.vedtaksperiode, orgnummer = AG1)

        inspektør(AG1) {
            assertTilstander(
                1.vedtaksperiode,
                *TIL_AVSLUTTET_FØRSTEGANGSBEHANDLING(),
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING
            )
            assertTilstander(
                2.vedtaksperiode,
                *TIL_AVSLUTTET_FORLENGELSE(),
                AVVENTER_ARBEIDSGIVERE_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING
            )
            assertHasNoErrors()
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(1, ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }

        inspektør(AG2) {
            assertTilstander(
                1.vedtaksperiode,
                *TIL_AVSLUTTET_FØRSTEGANGSBEHANDLING(false),
                AVVENTER_ARBEIDSGIVERE_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING
            )
            assertTilstander(
                2.vedtaksperiode,
                *TIL_AVSLUTTET_FORLENGELSE(false),
                AVVENTER_ARBEIDSGIVERE_REVURDERING
            )
            assertHasNoErrors()
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, ikkeUtbetalteUtbetalingerForVedtaksperiode(1.vedtaksperiode).size)
            assertEquals(1, avsluttedeUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
            assertEquals(1, ikkeUtbetalteUtbetalingerForVedtaksperiode(2.vedtaksperiode).size)
        }
    }

    @Test
    fun `kan ikke revurdere én AG hvis en annen AG er til godkjenning`() {
        tilGodkjenning(1.januar, 31.januar, AG1, AG2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = AG1)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = AG1)

        håndterYtelser(1.vedtaksperiode, orgnummer = AG2)
        håndterSimulering(1.vedtaksperiode, orgnummer = AG2)
        håndterOverstyring((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = AG1)
        assertErrorTekst(inspektør(AG2), "Kan ikke overstyre en pågående behandling der én eller flere perioder er behandlet ferdig")
    }
}
