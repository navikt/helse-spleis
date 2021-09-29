package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle

@TestInstance(Lifecycle.PER_CLASS)
internal class RevurderTidslinjeFlereArbeidsgivereTest : AbstractEndToEndTest() {

    private companion object {
        private const val aadvokatene = "123456789"
        private const val haandtverkerne = "987612345"
    }

    @Test
    fun `forlengelse av revurderte flere arbeidsgivere bør da virke`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = aadvokatene
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = haandtverkerne
        )

        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = aadvokatene)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = aadvokatene,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    aadvokatene inntekt 20000.månedlig
                    haandtverkerne inntekt 20000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterSimulering(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = aadvokatene)

        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = haandtverkerne)

        håndterOverstyring((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = aadvokatene)
        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterSimulering(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = aadvokatene)

        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)

        // og så forlenger vi.
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = aadvokatene)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = haandtverkerne)

        håndterYtelser(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterYtelser(2.vedtaksperiode, orgnummer = aadvokatene)

        håndterSimulering(2.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = aadvokatene)

        håndterYtelser(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = haandtverkerne)


        inspektør(aadvokatene) {
            assertTilstander(
                2.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertHasNoErrors()
            assertEquals(3, utbetalinger.filter { it.erAvsluttet() }.size)
        }

        inspektør(haandtverkerne) {
            assertTilstander(
                2.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
                AVVENTER_HISTORIKK,
                AVVENTER_ARBEIDSGIVERE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertHasNoErrors()
            assertEquals(3, utbetalinger.filter { it.erAvsluttet() }.size)
        }
    }

    @Test
    fun `to AG - én periode på hver - én blir revurdert`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = aadvokatene
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = haandtverkerne
        )

        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = aadvokatene)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = aadvokatene,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    aadvokatene inntekt 20000.månedlig
                    haandtverkerne inntekt 20000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterSimulering(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = aadvokatene)

        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = haandtverkerne)

        håndterOverstyring((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = aadvokatene)
        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterSimulering(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = aadvokatene)

        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)

        inspektør(aadvokatene) {
            assertTilstander(
                1.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_GAP,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
                AVVENTER_ARBEIDSGIVERE,
                AVVENTER_HISTORIKK,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,
                TIL_UTBETALING,
                AVSLUTTET
            )
            assertHasNoErrors()
            assertEquals(2, utbetalinger.filter { it.erAvsluttet() }.size)
        }

        inspektør(haandtverkerne) {
            assertTilstander(
                1.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_GAP,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
                AVVENTER_ARBEIDSGIVERE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET,
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
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = aadvokatene
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = haandtverkerne
        )

        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = aadvokatene)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = aadvokatene,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    aadvokatene inntekt 20000.månedlig
                    haandtverkerne inntekt 20000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterSimulering(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = aadvokatene)

        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = haandtverkerne)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = aadvokatene)

        håndterOverstyring((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = aadvokatene)
        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterSimulering(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = aadvokatene)

        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)

        inspektør(aadvokatene) {
            assertTilstander(
                1.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_GAP,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
                AVVENTER_ARBEIDSGIVERE,
                AVVENTER_HISTORIKK,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET,
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

        inspektør(haandtverkerne) {
            assertTilstander(
                1.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_GAP,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
                AVVENTER_ARBEIDSGIVERE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET,
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
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = aadvokatene
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            refusjon = Refusjon(null, 20000.månedlig, emptyList()),
            orgnummer = haandtverkerne
        )

        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = aadvokatene)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = aadvokatene,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    aadvokatene inntekt 20000.månedlig
                    haandtverkerne inntekt 20000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterSimulering(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = aadvokatene)

        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = haandtverkerne)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = aadvokatene)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = haandtverkerne)

        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = aadvokatene)
        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = aadvokatene)

        håndterSimulering(2.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = aadvokatene)

        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = haandtverkerne)

        håndterSimulering(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = haandtverkerne)

        håndterOverstyring((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = aadvokatene)
        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterYtelser(2.vedtaksperiode, orgnummer = aadvokatene)

        håndterSimulering(2.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = aadvokatene)

        håndterYtelser(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = haandtverkerne)

        inspektør(aadvokatene) {
            assertTilstander(
                1.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_GAP,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
                AVVENTER_ARBEIDSGIVERE,
                AVVENTER_HISTORIKK,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
                AVVENTER_HISTORIKK,
                AVVENTER_ARBEIDSGIVERE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET,
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

        inspektør(haandtverkerne) {
            assertTilstander(
                1.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_GAP,
                AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
                AVVENTER_ARBEIDSGIVERE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET,
                AVVENTER_ARBEIDSGIVERE_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_GJENNOMFØRT_REVURDERING,
                AVSLUTTET
            )
            assertTilstander(
                2.vedtaksperiode,
                START,
                MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
                AVVENTER_HISTORIKK,
                AVVENTER_ARBEIDSGIVERE,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                TIL_UTBETALING,
                AVSLUTTET,
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
    fun `kan ikke revurdere én AG hvis en annen AG er til godkjenning`() {
        tilGodkjenning(1.januar, 31.januar, aadvokatene, haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = aadvokatene)

        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterOverstyring((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = aadvokatene)
        assertErrorTekst(inspektør(haandtverkerne), "Kan ikke overstyre en pågående behandling der én eller flere perioder er behandlet ferdig")
    }
}
