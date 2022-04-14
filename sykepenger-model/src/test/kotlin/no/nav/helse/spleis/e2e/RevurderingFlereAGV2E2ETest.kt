package no.nav.helse.spleis.e2e

import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype.Feriedag
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_UFERDIG
import no.nav.helse.person.TilstandType.MOTTATT_SYKMELDING_UFERDIG_GAP
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype.REVURDERING
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RevurderingFlereAGV2E2ETest: AbstractEndToEndTest() {

    @BeforeEach
    fun setup() {
        Toggle.NyRevurdering.enable()
    }

    @AfterEach
    fun tearDown() {
        Toggle.NyRevurdering.pop()
    }

    @Test
    fun `revurdere første periode - flere ag - ag 1`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a1, a2)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), orgnummer = a1)
        håndterYtelser(3.vedtaksperiode, orgnummer = a1)
        håndterSimulering(3.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVSLUTTET, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, TIL_UTBETALING, AVSLUTTET, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere første periode - flere ag - ag 1 - har generert utbetaling`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a1, a2)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), orgnummer = a1)
        håndterYtelser(3.vedtaksperiode, orgnummer = a1)
        håndterSimulering(3.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_SIMULERING_REVURDERING, AVVENTER_GODKJENNING_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a2)

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
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a1, a2)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)), orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere tredje periode - flere ag - ag1`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a1, a2)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.mars, Feriedag)), orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere første periode - flere ag - ag 2`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a1, a2)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.januar, Feriedag)), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere andre periode - flere ag - ag 2`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a1, a2)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.februar, Feriedag)), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revurdere tredje periode - flere ag - ag2`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        forlengVedtak(1.mars, 31.mars, a1, a2)
        nullstillTilstandsendringer()
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(5.mars, Feriedag)), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `starte revurdering av ag1 igjen etter at ag2 har startet revurdering`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        nullstillTilstandsendringer()

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING, orgnummer = a2)
    }

    @Test
    fun `revudering påvirkes ikke av gjenoppta behandling ved avsluttet uten utbetaling`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        forlengVedtak(1.februar, 28.februar, a1, a2)
        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Feriedag)), a1)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        nullstillTilstandsendringer()
        håndterSykmelding(Sykmeldingsperiode(1.juni, 10.juni, 100.prosent), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.juni, 10.juni, 100.prosent), orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING, orgnummer = a1)
        assertTilstander(2.vedtaksperiode, TIL_UTBETALING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a2)
        assertForventetFeil(
            nå = {
                assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
            },
            ønsket = {
                assertTilstander(3.vedtaksperiode, START, MOTTATT_SYKMELDING_UFERDIG_GAP, AVVENTER_UFERDIG, orgnummer = a2)
            }
        )
    }
}