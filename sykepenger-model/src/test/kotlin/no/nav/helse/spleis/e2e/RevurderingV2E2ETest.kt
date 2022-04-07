package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.*
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.IdInnhenter
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_UFERDIG
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype.REVURDERING
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RevurderingV2E2ETest : AbstractEndToEndTest() {

    @BeforeEach
    fun setup() {
        Toggle.NyRevurdering.enable()
    }

    @AfterEach
    fun tearDown() {
        Toggle.NyRevurdering.pop()
    }

    @Test
    fun `revurdere første periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `revurdere periode med forlengelse i avventer godkjenning`() {
        nyttVedtak(1.januar, 31.januar)
        forlengTilGodkjenning(1.februar, 28.februar)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_UFERDIG)
    }

    @Test
    fun `revurdere andre periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `revurdere tredje periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.mars, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `revurdere første periode - flere ag - ag 1`() {
        setupToArbeidsgivereFørstegangsbehandling(1.januar til 31.januar)
        setupToArbeidsgivereForlengelse(1.februar til 28.februar)
        setupToArbeidsgivereForlengelse(1.mars til 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)), orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere første periode - flere ag - ag 1 - har generert utbetaling`() {
        setupToArbeidsgivereFørstegangsbehandling(1.januar til 31.januar)
        setupToArbeidsgivereForlengelse(1.februar til 28.februar)
        setupToArbeidsgivereForlengelse(1.mars til 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)), orgnummer = a1)
        håndterYtelser(3.vedtaksperiode, orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)

        inspektør(a1) {
            assertEquals(REVURDERING, utbetalingstype(3.vedtaksperiode, 1))
            assertEquals(4, utbetalinger.size)
        }

        assertForventetFeil(
            forklaring = "Utbetalinger må være genreret før noe går til godkjenning fordi er avhengig av utbetalinger for å lage generasjoner",
            nå = {
                inspektør(a2) {
                    assertNull(utbetalingstype(3.vedtaksperiode, 2))
                    assertEquals(6, utbetalinger.size)
                }
            },
            ønsket = {
                inspektør(a2) {
                    assertEquals(REVURDERING, utbetalingstype(3.vedtaksperiode, 2))
                    assertEquals(4, utbetalinger.size)
                }
            }
        )
    }

    @Test
    fun `revurdere andre periode - flere ag - ag 1`() {
        setupToArbeidsgivereFørstegangsbehandling(1.januar til 31.januar)
        setupToArbeidsgivereForlengelse(1.februar til 28.februar)
        setupToArbeidsgivereForlengelse(1.mars til 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)), orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere tredje periode - flere ag - ag1`() {
        setupToArbeidsgivereFørstegangsbehandling(1.januar til 31.januar)
        setupToArbeidsgivereForlengelse(1.februar til 28.februar)
        setupToArbeidsgivereForlengelse(1.mars til 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.mars, Feriedag)), orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere første periode - flere ag - ag 2`() {
        setupToArbeidsgivereFørstegangsbehandling(1.januar til 31.januar)
        setupToArbeidsgivereForlengelse(1.februar til 28.februar)
        setupToArbeidsgivereForlengelse(1.mars til 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere andre periode - flere ag - ag 2`() {
        setupToArbeidsgivereFørstegangsbehandling(1.januar til 31.januar)
        setupToArbeidsgivereForlengelse(1.februar til 28.februar)
        setupToArbeidsgivereForlengelse(1.mars til 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere tredje periode - flere ag - ag2`() {
        setupToArbeidsgivereFørstegangsbehandling(1.januar til 31.januar)
        setupToArbeidsgivereForlengelse(1.februar til 28.februar)
        setupToArbeidsgivereForlengelse(1.mars til 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.mars, Feriedag)), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere første to perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Feriedag), ManuellOverskrivingDag(1.februar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `revurdere tildligere utbetaling`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
        assertTilstander(2.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `revurdere periode med nyere førstegangsbehandling innenfor samme agp`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(10.februar, 28.februar, arbeidsgiverperiode = listOf(1.januar til 16.januar))
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)))

        assertForventetFeil(
            nå = {
                assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING)
                assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
            },
            ønsket = {
                assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
                assertTilstander(2.vedtaksperiode, AVSLUTTET)
            }
        )
    }

    private fun setupToArbeidsgivereFørstegangsbehandling(periode: Periode) {
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(periode.start til periode.start.plusDays(15)), førsteFraværsdag = periode.start, orgnummer = a1)
        håndterInntektsmelding(listOf(periode.start til periode.start.plusDays(15)), førsteFraværsdag = periode.start, orgnummer = a2)

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(3))
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 20000.månedlig.repeat(12))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList()),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
    }

    private fun setupToArbeidsgivereForlengelse(periode: Periode) {
        val id: IdInnhenter = observatør.sisteVedtaksperiode()
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent), orgnummer = a2)
        håndterYtelser(id, orgnummer = a1)
        håndterYtelser(id, orgnummer = a2)
        håndterYtelser(id, orgnummer = a1)
        håndterSimulering(id, orgnummer = a1)
        håndterUtbetalingsgodkjenning(id, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(id, orgnummer = a2)
        håndterSimulering(id, orgnummer = a2)
        håndterUtbetalingsgodkjenning(id, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        assertSisteTilstand(id, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(id, AVSLUTTET, orgnummer = a2)
    }
}