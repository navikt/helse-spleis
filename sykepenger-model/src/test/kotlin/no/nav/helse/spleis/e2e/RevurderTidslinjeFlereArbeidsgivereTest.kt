package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.person.TilstandType.*
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RevurderTidslinjeFlereArbeidsgivereTest : AbstractEndToEndTest() {

    private companion object {
        private val aadvokatene = "123456789".somOrganisasjonsnummer()
        private val haandtverkerne = "987612345".somOrganisasjonsnummer()
    }

    @Test
    fun `forlengelse av revurderte flere arbeidsgivere bør da virke`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = 20000.månedlig,
            orgnummer = aadvokatene
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = 20000.månedlig,
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

        håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = aadvokatene)
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
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = 20000.månedlig,
            orgnummer = haandtverkerne
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = 20000.månedlig,
            orgnummer = aadvokatene
        )

        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = haandtverkerne,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    haandtverkerne inntekt 20000.månedlig
                    aadvokatene inntekt 20000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = haandtverkerne)

        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterSimulering(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = aadvokatene)

        håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = haandtverkerne)
        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = haandtverkerne)

        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)

        inspektør(haandtverkerne) {
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

        inspektør(aadvokatene) {
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
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = 20000.månedlig,
            orgnummer = haandtverkerne
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = 20000.månedlig,
            orgnummer = aadvokatene
        )

        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = haandtverkerne,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    haandtverkerne inntekt 20000.månedlig
                    aadvokatene inntekt 20000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = haandtverkerne)

        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterSimulering(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = aadvokatene)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = haandtverkerne)

        håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = haandtverkerne)
        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = haandtverkerne)

        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)

        inspektør(haandtverkerne) {
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

        inspektør(aadvokatene) {
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
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = 20000.månedlig,
            orgnummer = haandtverkerne
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = 20000.månedlig,
            orgnummer = aadvokatene
        )

        håndterYtelser(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            orgnummer = haandtverkerne,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    haandtverkerne inntekt 20000.månedlig
                    aadvokatene inntekt 20000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = haandtverkerne)

        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterSimulering(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = aadvokatene)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = haandtverkerne)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = aadvokatene)

        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = aadvokatene)
        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = haandtverkerne)

        håndterSimulering(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = haandtverkerne)

        håndterYtelser(vedtaksperiodeIdInnhenter = 2.vedtaksperiode, orgnummer = aadvokatene)

        håndterSimulering(2.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = aadvokatene)

        håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = haandtverkerne)
        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterYtelser(2.vedtaksperiode, orgnummer = haandtverkerne)

        håndterSimulering(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = haandtverkerne)
        håndterUtbetalt(2.vedtaksperiode, orgnummer = haandtverkerne)

        håndterYtelser(2.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = aadvokatene)

        inspektør(haandtverkerne) {
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

        inspektør(aadvokatene) {
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
    fun `kan ikke revurdere én AG hvis en annen AG er til godkjenning`() {
        tilGodkjenning(1.januar, 31.januar, aadvokatene, haandtverkerne)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterUtbetalt(1.vedtaksperiode, orgnummer = aadvokatene)

        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterSimulering(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterOverstyrTidslinje((29.januar til 29.januar).map { manuellFeriedag(it) }, orgnummer = aadvokatene)
        assertErrorTekst(inspektør(haandtverkerne), "Kan ikke overstyre en pågående behandling der én eller flere perioder er behandlet ferdig")
    }

    @Test
    fun `to AG - to perioder på hver - én blir revurdert på én AG`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = aadvokatene)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = haandtverkerne)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = 20000.månedlig,
            orgnummer = aadvokatene
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(Periode(1.januar, 16.januar)),
            beregnetInntekt = 20000.månedlig,
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

        håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) }, orgnummer = aadvokatene)
        håndterYtelser(1.vedtaksperiode, orgnummer = aadvokatene)
        håndterYtelser(1.vedtaksperiode, orgnummer = haandtverkerne)
        håndterYtelser(2.vedtaksperiode, orgnummer = aadvokatene)

        håndterSimulering(2.vedtaksperiode, orgnummer = aadvokatene)

        inspektør(aadvokatene) {
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

        inspektør(haandtverkerne) {
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
}
