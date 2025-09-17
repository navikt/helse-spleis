package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.BehandlingView
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_ANNULLERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_ANNULLERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.sisteBehov
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.UtbetalingView
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AnnullerUtbetalingTest : AbstractDslTest() {

    @Test
    fun `Vedtaksperioden skal være med i annulleringskandidater`() {
        a1 {
            nyttVedtak(januar)

            val annulleringskandidater = inspektør.yrkesaktivitet.view().aktiveVedtaksperioder.first().annulleringskandidater.map { it.id }
            assertEquals(listOf(1.vedtaksperiode), annulleringskandidater)
        }
    }

    @Test
    fun `Etterfølgende, utbetalte vedtaksperioder skal være med i annulleringskandidater`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)

            val annulleringskandidater = inspektør.yrkesaktivitet.view().aktiveVedtaksperioder.first().annulleringskandidater.map { it.id }
            assertEquals(listOf(1.vedtaksperiode, 2.vedtaksperiode), annulleringskandidater)
        }
    }

    @Test
    fun `Tidligere, utbetalte vedtaksperioder skal ikke være med i annulleringskandidater`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)

            val annulleringskandidater = inspektør.yrkesaktivitet.view().aktiveVedtaksperioder.last().annulleringskandidater.map { it.id }
            assertEquals(listOf(2.vedtaksperiode), annulleringskandidater)
        }
    }

    @Test
    fun `Uberegnede vedtaksperioder skal ikke være med i annulleringskandidater`() {
        a1 {
            nyttVedtak(januar)
            nyPeriode(februar)

            val annulleringskandidater = inspektør.yrkesaktivitet.view().aktiveVedtaksperioder.first().annulleringskandidater.map { it.id }
            assertEquals(listOf(1.vedtaksperiode), annulleringskandidater)
        }
    }

    @Test
    fun `Bare vedtaksperioder med samme agp skal være med i annulleringskandidater`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)

            nyttVedtak(april)

            val annulleringskandidater = inspektør.yrkesaktivitet.view().aktiveVedtaksperioder.first().annulleringskandidater.map { it.id }
            assertEquals(listOf(1.vedtaksperiode, 2.vedtaksperiode), annulleringskandidater)
        }
    }

    @Test
    fun `Annullerer en ikke ferdigbehandlet revurdering`() {
        a1 {
            nyttVedtak(januar, grad = 50.prosent)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(23.januar, Dagtype.Sykedag, 100)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(1))
            assertSisteTilstand(1.vedtaksperiode, TIL_ANNULLERING)
        }
    }


    @Test
    fun `Annullerer en pågående revurdering`() {
        a1 {

            nyttVedtak(januar, grad = 50.prosent)
            forlengVedtak(februar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(23.januar, Dagtype.Sykedag, 100)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(0))
            assertSisteTilstand(1.vedtaksperiode, TIL_ANNULLERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_ANNULLERING)
        }
    }

    @Test
    fun `kun én vedtaksperiode skal annulleres`() {
        a1 {
            nyttVedtak(januar)

            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)

            nullstillTilstandsendringer()

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(0))

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertEquals(2, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            val annullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
        }
    }

    @Test
    fun `begge vedtaksperioder annulleres når vi annullerer den første`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)

            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            nullstillTilstandsendringer()

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(0))

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING)
            assertEquals(2, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_ANNULLERING,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            val annullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            val annulleringFebruar = inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling
            assertAnnullering(-28620, Utbetalingstatus.OVERFØRT, 1.februar, 1.februar til 28.februar, annulleringFebruar!!)

            håndterUtbetalt()

            val utførtAnnulleringFebruar = inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertAnnullering(-28620, Utbetalingstatus.ANNULLERT, 1.februar, 1.februar til 28.februar, utførtAnnulleringFebruar!!)
            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
        }
    }

    @Test
    fun `kun siste vedtaksperiode annulleres når det er denne som forsøkes annullert`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)

            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            nullstillTilstandsendringer()

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(1))

            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            assertEquals(
                BehandlingView.TilstandView.VEDTAK_IVERKSATT,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            val annullering = inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling
            assertAnnullering(-28620, Utbetalingstatus.OVERFØRT, 1.februar, 1.februar til 28.februar, annullering!!)

            håndterUtbetalt()

            val utførtAnnulleringFebruar = inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertAnnullering(-28620, Utbetalingstatus.ANNULLERT, 1.februar, 1.februar til 28.februar, utførtAnnulleringFebruar!!)
            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
        }
    }

    @Test
    fun `annullerer bare perioder etter den som forsøkes annullert`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            forlengVedtak(mars)

            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.size)

            nullstillTilstandsendringer()

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(1))

            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING)
            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.size)

            assertEquals(
                BehandlingView.TilstandView.VEDTAK_IVERKSATT,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_ANNULLERING,
                inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            val annullering = inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling
            assertAnnullering(-28620, Utbetalingstatus.OVERFØRT, 1.februar, 1.februar til 28.februar, annullering!!)

            håndterUtbetalt()

            val utførtAnnulleringFebruar = inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertAnnullering(-28620, Utbetalingstatus.ANNULLERT, 1.februar, 1.februar til 28.februar, utførtAnnulleringFebruar!!)
            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            val annulleringMars = inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling
            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertAnnullering(-31482, Utbetalingstatus.OVERFØRT, 1.mars, 1.mars til 31.mars, annulleringMars!!)

            håndterUtbetalt()

            val utførtAnnulleringMars = inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertAnnullering(-31482, Utbetalingstatus.ANNULLERT, 1.mars, 1.mars til 31.mars, utførtAnnulleringMars!!)
            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
        }
    }

    @Test
    fun `annullerer bare i sammenhengende agp`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            nyttVedtak(april)

            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.size)

            nullstillTilstandsendringer()

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(0))

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertEquals(2, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.size)

            assertVarsel(Varselkode.RV_RV_7, 3.vedtaksperiode.filter())

            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_ANNULLERING,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_REVURDERING,
                inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            val annullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
        }
    }

    @Test
    fun `annullerer også etter kort gap`() {
        a1 {
            nyttVedtak(januar)
            nyttVedtak(10.februar til 28.februar)

            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            nullstillTilstandsendringer()

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(0))

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING)
            assertEquals(2, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_ANNULLERING,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            val annullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
        }
    }

    @Test
    fun `annullerer periode som har ny uberegnet periode etter seg`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)

            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            nullstillTilstandsendringer()

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(0))

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE)
            assertVarsel(Varselkode.RV_RV_7, 2.vedtaksperiode.filter())

            assertEquals(2, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            val annullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
        }
    }

    @Test
    fun `annullerer periode som har ny beregnet periode etter seg`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)

            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            nullstillTilstandsendringer()

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(0))

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_SIMULERING, AVVENTER_BLOKKERENDE_PERIODE)
            assertTrue(inspektør.utbetaling(1).erForkastet)
            assertVarsel(Varselkode.RV_RV_7, 2.vedtaksperiode.filter())

            assertEquals(2, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            val annullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_SIMULERING, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
        }
    }

    @Test
    fun `annullerer periode som har pågående beregnet revurdering etter seg`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar, 50.prosent)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(28.februar, Dagtype.Sykedag, 100)))
            håndterYtelser(2.vedtaksperiode)

            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            nullstillTilstandsendringer()

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(0))

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING, AVVENTER_ANNULLERING)
            assertTrue(inspektør.utbetaling(2).erForkastet)

            assertEquals(2, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_ANNULLERING,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            val annullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertTilstander(2.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            håndterUtbetalt()

            val utførtAnnulleringFebruar = inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertAnnullering(-14300, Utbetalingstatus.ANNULLERT, 1.februar, 1.februar til 28.februar, utførtAnnulleringFebruar!!)
            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
        }
    }

    @Test
    fun `annullerer periode som har pågående uberegnet revurdering etter seg`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar, 50.prosent)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(28.februar, Dagtype.Sykedag, 100)))

            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            nullstillTilstandsendringer()

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(0))

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_ANNULLERING)

            assertEquals(2, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_ANNULLERING,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            val annullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            håndterUtbetalt()

            val utførtAnnulleringFebruar = inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertAnnullering(-14300, Utbetalingstatus.ANNULLERT, 1.februar, 1.februar til 28.februar, utførtAnnulleringFebruar!!)
            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
        }
    }

    @Test
    fun `annulleringer på vedtaksperioder med samme utbetaling`() {
        medJSONPerson("/personer/to_vedtak_samme_fagsystem_id.json")
        a1 {
            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            nullstillTilstandsendringer()

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(0))

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING)
            assertEquals(2, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_ANNULLERING,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            val annullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling
            assertAnnullering(-32913, Utbetalingstatus.OVERFØRT, 19.januar, 3.januar til 20.februar, annullering!!)

            håndterUtbetalt()

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)

            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            val forventet = setOf(
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.first().endringer.last().utbetaling,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.first().endringer.last().utbetaling,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling,
            )
            assertEquals(
                forventet,
                inspektør.yrkesaktivitet.view().utbetalinger.toSet()
            )
        }
    }

    @Test
    fun `annullering av siste periode og vedtaksperioder med samme utbetaling`() {
        medJSONPerson("/personer/to_vedtak_samme_fagsystem_id.json")

        a1 {
            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(1))
            håndterYtelser(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_ANNULLERING)
            assertEquals(2, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)
            assertVarsel(Varselkode.RV_RV_7, 1.vedtaksperiode.filter())

            assertEquals(3, inspektør.utbetalinger.size)
            val utbetalingForlengelse = inspektør.utbetaling(1)
            val utbetalingRevurdering = inspektør.utbetaling(2)

            assertEquals(24327, utbetalingForlengelse.nettobeløp)
            assertEquals(-24327, utbetalingRevurdering.nettobeløp)
            assertEquals(1, utbetalingRevurdering.arbeidsgiverOppdrag.size)
            assertEquals(19.januar til 26.januar, utbetalingRevurdering.arbeidsgiverOppdrag[0].periode)
            assertEquals(3.januar til 26.januar, utbetalingRevurdering.periode)

            val utbetalingslinje = utbetalingRevurdering.arbeidsgiverOppdrag.linjer.first()
            assertEquals(Endringskode.ENDR, utbetalingslinje.endringskode)
            assertEquals(19.januar, utbetalingslinje.fom)
            assertEquals(26.januar, utbetalingslinje.tom)

            assertEquals(
                BehandlingView.TilstandView.BEREGNET_REVURDERING,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_ANNULLERING,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterUtbetalt()

            assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)

            val annullering = inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertTomAnnulleringsutbetaling(annullering!!)
        }
    }

    @Test
    fun `annullering av midterste periode og vedtaksperioder med samme utbetaling`() {
        medJSONPerson("/personer/tre_vedtak_samme_fagsystem_id.json")

        a1 {
            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.size)

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(1))
            håndterYtelser(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_ANNULLERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_ANNULLERING)
            assertEquals(2, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.size)
            assertVarsel(Varselkode.RV_RV_7, 1.vedtaksperiode.filter())

            assertEquals(4, inspektør.utbetalinger.size)
            val utbetalingRevurdering = inspektør.utbetaling(3)

            assertEquals(-(24327 + 18603), utbetalingRevurdering.nettobeløp)
            assertEquals(1, utbetalingRevurdering.arbeidsgiverOppdrag.size)
            assertEquals(19.januar til 26.januar, utbetalingRevurdering.arbeidsgiverOppdrag[0].periode)
            assertEquals(3.januar til 26.januar, utbetalingRevurdering.periode)

            val utbetalingslinje = utbetalingRevurdering.arbeidsgiverOppdrag.linjer.first()
            assertEquals(Endringskode.ENDR, utbetalingslinje.endringskode)
            assertEquals(19.januar, utbetalingslinje.fom)
            assertEquals(26.januar, utbetalingslinje.tom)

            assertEquals(
                BehandlingView.TilstandView.BEREGNET_REVURDERING,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_ANNULLERING,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_ANNULLERING,
                inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterUtbetalt()

            assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)

            val annullering = inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertTomAnnulleringsutbetaling(annullering!!)
        }
    }

    @Test
    fun `annullering av siste periode og vedtaksperioder med samme utbetaling og vi er til revurdering`() {
        medJSONPerson("/personer/tre_vedtak_samme_fagsystem_id.json")

        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(18.januar, Dagtype.Sykedag, 80)))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_REVURDERING)

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(2))
            håndterYtelser(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_ANNULLERING)
            assertEquals(2, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.size)

            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt() // utbetaler første revurdering

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_ANNULLERING)

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterUtbetalt() // utbetaler andre revurdering

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)

            assertEquals(6, inspektør.utbetalinger.size)
            val annulleringsutbetaling = inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling

            assertTomAnnulleringsutbetaling(annulleringsutbetaling!!)
        }
    }

    @Test
    fun `annullering av andra periode hvor første er AUU og vedtaksperioder med samme utbetaling`() {
        medJSONPerson("/personer/tre_vedtak_samme_fagsystem_id_forste_periode_AUU.json")

        a1 {
            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.size)

            håndterAnnullering(utbetalingId = inspektør.utbetalingId(0))

            assertEquals(inspektør.vedtaksperioder(1.vedtaksperiode).tilstand, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, TIL_ANNULLERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_ANNULLERING)

            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.size)

            assertEquals(3, inspektør.utbetalinger.size)

            assertEquals(
                BehandlingView.TilstandView.AVSLUTTET_UTEN_VEDTAK,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_ANNULLERING,
                inspektør.vedtaksperioder(3.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            nullstillTilstandsendringer()
            håndterUtbetalt()

            assertEquals(
                BehandlingView.TilstandView.ANNULLERT_PERIODE,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            assertEquals(inspektør.vedtaksperioder(1.vedtaksperiode).tilstand, AVSLUTTET_UTEN_UTBETALING)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
        }

    }

    @Test
    fun `annullerer ikke ennå perioder på tvers av arbeidsgivere ved samme sykefravær`() {
        (a1 og a2).nyeVedtak(januar, inntekt = 31000.månedlig)
        (a1 og a2).forlengVedtak(februar)

        assertEquals(1, inspektør(a2).vedtaksperioder(1.vedtaksperiode(a2)).behandlinger.behandlinger.size)
        assertEquals(1, inspektør(a2).vedtaksperioder(2.vedtaksperiode(a2)).behandlinger.behandlinger.size)
        a1 {
            assertEquals(1, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(1, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            nullstillTilstandsendringer()
            håndterAnnullering(utbetalingId = inspektør.utbetalingId(0))

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING)
            assertEquals(2, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)

            assertEquals(
                BehandlingView.TilstandView.OVERFØRT_ANNULLERING,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_ANNULLERING,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )

            val annullering = inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().endringer.last().utbetaling
            assertAnnullering(-11880, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertEquals(2, inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.size)
            assertEquals(2, inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.size)

            assertVarsel(Varselkode.RV_RV_7, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_RV_7, 2.vedtaksperiode.filter())

            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_REVURDERING,
                inspektør.vedtaksperioder(1.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
            assertEquals(
                BehandlingView.TilstandView.UBEREGNET_REVURDERING,
                inspektør.vedtaksperioder(2.vedtaksperiode).behandlinger.behandlinger.last().tilstand
            )
        }
    }

    @Test
    fun `avvis hvis arbeidsgiver er ukjent`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            assertThrows<Aktivitetslogg.AktivitetException> { håndterAnnullering(utbetalingId = UUID.randomUUID(), orgnummer = a2) }
            assertTrue(testperson.personlogg.harFunksjonelleFeilEllerVerre(), testperson.personlogg.toString())
        }
    }

    @Test
    fun `annuller siste utbetaling`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            val behovTeller = testperson.personlogg.behov.size
            håndterAnnullering(utbetalingId = inspektør.utbetalingId(0))
            assertIngenFunksjonelleFeil()
            val behov = testperson.personlogg.sisteBehov(Behovtype.Utbetaling)

            @Suppress("UNCHECKED_CAST")
            val statusForUtbetaling = (behov.detaljer()["linjer"] as List<Map<String, Any>>)[0]["statuskode"]
            assertEquals("OPPH", statusForUtbetaling)
            håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)
            assertFalse(testperson.personlogg.harFunksjonelleFeilEllerVerre())
            assertEquals(2, inspektør.antallUtbetalinger)
            assertEquals(1, testperson.personlogg.behov.size - behovTeller)
            inspektør.utbetaling(1).arbeidsgiverOppdrag.inspektør.also {
                assertEquals(19.januar, it.fom(0))
                assertEquals(26.januar, it.tom(0))
                assertEquals(19.januar, it.datoStatusFom(0))
            }
            testperson.personlogg.behov.last().also {
                assertEquals(Behovtype.Utbetaling, it.type)
                assertNull(it.detaljer()["maksdato"])
                assertEquals("SPREF", it.detaljer()["fagområde"])
            }
        }
    }

    @Test
    fun `Annuller flere fagsystemid for samme arbeidsgiver`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            nyttVedtak(mars, 100.prosent)
            håndterAnnullering(utbetalingId = inspektør.sisteUtbetalingId(2.vedtaksperiode))
            håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)
            sisteBehovErAnnullering(2.vedtaksperiode)
            håndterAnnullering(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
            håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)
            sisteBehovErAnnullering(1.vedtaksperiode)
        }
    }

    private fun sisteBehovErAnnullering(vedtaksperiodeIdInnhenter: UUID) {
        testperson.personlogg.behov.last().also {
            assertEquals(Behovtype.Utbetaling, it.type)
            assertEquals(inspektør.sisteArbeidsgiveroppdragFagsystemId(vedtaksperiodeIdInnhenter), it.detaljer()["fagsystemId"])
            assertEquals("OPPH", it.hentLinjer()[0]["statuskode"])
        }
    }

    private fun assertIngenAnnulleringsbehov() {
        assertFalse(
            testperson.personlogg.behov
                .filter { it.type == Behovtype.Utbetaling }
                .any {
                    it.hentLinjer().any { linje ->
                        linje["statuskode"] == "OPPH"
                    }
                }
        )
    }

    @Test
    fun `Kan annullere hvis noen vedtaksperioder er til utbetaling`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            tilGodkjenning(mars, 100.prosent)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterAnnullering(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
            håndterUtbetalt()

            assertVarsel(Varselkode.RV_RV_7, 2.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, TIL_ANNULLERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    private fun Aktivitet.Behov.hentLinjer() =
        @Suppress("UNCHECKED_CAST")
        (detaljer()["linjer"] as List<Map<String, Any>>)

    @Test
    fun `Ved feilet annulleringsutbetaling settes utbetaling til annullering feilet`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            nullstillTilstandsendringer()
            håndterAnnullering(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
            håndterUtbetalt(status = Oppdragstatus.FEIL)
            assertFalse(testperson.personlogg.harFunksjonelleFeilEllerVerre())
            assertEquals(Utbetalingstatus.OVERFØRT, inspektør.utbetaling(1).tilstand)
            assertSisteTilstand(1.vedtaksperiode, TIL_ANNULLERING)
        }
    }

    @Test
    fun `Periode som håndterer avvist annullering i TilAnnullering blir værende i TilAnnullering`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            nullstillTilstandsendringer()
            håndterAnnullering(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
            håndterUtbetalt(status = Oppdragstatus.AVVIST)
            assertFalse(testperson.personlogg.harFunksjonelleFeilEllerVerre())
            assertEquals(Utbetalingstatus.OVERFØRT, inspektør.utbetaling(1).tilstand)
            assertSisteTilstand(1.vedtaksperiode, TIL_ANNULLERING)
        }
    }

    @Test
    fun `Periode som håndterer godkjent annullering i TilAnnullering blir forkastet`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            nullstillTilstandsendringer()
            håndterAnnullering(inspektør.sisteUtbetalingId(1.vedtaksperiode))
            håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)
            assertFalse(testperson.personlogg.harFunksjonelleFeilEllerVerre(), testperson.personlogg.toString())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
        }
    }


    @Test
    fun `Annullering av én periode fører kun til at sammehengende utbetalte perioder blir forkastet og værende i Avsluttet`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            forlengVedtak(27.januar til 30.januar, 100.prosent)
            nyttVedtak(1.mars til 20.mars, 100.prosent)
            val behovTeller = testperson.personlogg.behov.size
            nullstillTilstandsendringer()

            // Annuler 1 mars til 20 mars
            håndterAnnullering(inspektør.sisteUtbetalingId(3.vedtaksperiode))
            håndterUtbetalt()
            assertFalse(testperson.personlogg.harFunksjonelleFeilEllerVerre(), testperson.personlogg.toString())
            assertEquals(1, testperson.personlogg.behov.size - behovTeller, testperson.personlogg.toString())
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET)
            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `publiserer et event ved annullering av full refusjon`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            håndterAnnullering(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
            håndterUtbetalt(
                status = Oppdragstatus.AKSEPTERT
            )

            val annullering = observatør.annulleringer.lastOrNull()
            assertNotNull(annullering)

            val utbetalingInspektør = inspektør.utbetaling(0)
            assertEquals(utbetalingInspektør.arbeidsgiverOppdrag.inspektør.fagsystemId(), annullering.arbeidsgiverFagsystemId)
            assertEquals(utbetalingInspektør.personOppdrag.inspektør.fagsystemId(), annullering.personFagsystemId)

            assertEquals("tbd@nav.no", annullering.saksbehandlerEpost)
            assertEquals(3.januar, annullering.fom)
            assertEquals(26.januar, annullering.tom)
        }
    }

    @Test
    fun `annuller over ikke utbetalt forlengelse`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            håndterSykmelding(Sykmeldingsperiode(27.januar, 31.januar))
            håndterSøknad(27.januar til 31.januar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode, false)

            håndterAnnullering(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
            val annullering = inspektør.utbetaling(2)
            sisteBehovErAnnullering(1.vedtaksperiode)
            assertTrue(annullering.erAnnullering)
            assertEquals(26.januar, annullering.arbeidsgiverOppdrag.inspektør.periode?.endInclusive)
            assertEquals(19.januar, annullering.arbeidsgiverOppdrag.first().inspektør.fom)
            assertEquals(26.januar, annullering.arbeidsgiverOppdrag.last().inspektør.tom)
        }
    }

    @Test
    fun `UtbetalingAnnullertEvent inneholder saksbehandlerident`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            håndterAnnullering(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
            håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)

            assertEquals("Ola Nordmann", observatør.annulleringer.first().saksbehandlerIdent)
        }
    }

    @Test
    fun `skal ikke forkaste utbetalte perioder, med mindre de blir annullert`() {
        a1 {
            // lag en periode
            nyttVedtak(januar)
            // prøv å forkast, ikke klar det
            håndterSykmelding(Sykmeldingsperiode(1.februar, 19.februar))
            håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar))
            håndterSøknad(1.februar til 19.februar)
            håndterSøknad(1.februar til 20.februar)

            assertTrue(inspektør.periodeErIkkeForkastet(1.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
            // annullér
            håndterAnnullering(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
            håndterUtbetalt()
            // sjekk at _nå_ er den forkasta
            assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
        }
    }

    @Test
    fun `skal kunne annullere tidligere utbetaling dersom siste utbetaling er uten utbetaling`() {
        a1 {
            nyttVedtak(januar)
            håndterSykmelding(Sykmeldingsperiode(1.mars, 20.mars))
            håndterSøknad(Sykdom(1.mars, 20.mars, 100.prosent), Søknad.Søknadsperiode.Ferie(17.mars, 20.mars))
            håndterInntektsmelding(listOf(1.mars til 16.mars))
            håndterAnnullering(utbetalingId = inspektør.sisteUtbetalingId(1.vedtaksperiode))
            håndterUtbetalt()
            assertVarsel(Varselkode.RV_RV_7, 2.vedtaksperiode.filter())
            assertFalse(testperson.personlogg.harFunksjonelleFeilEllerVerre())
            assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
        }
    }

    @Test
    fun `annullering av periode medfører at låser på sykdomstidslinje blir forkastet`() {
        a1 {
            nyttVedtak(januar)
            håndterAnnullering(inspektør.sisteUtbetalingId(1.vedtaksperiode))
            inspektør.sykdomstidslinje.inspektør.låstePerioder.also {
                assertEquals(0, it.size)
            }
        }
    }

    private fun assertAnnullering(nettobeløp: Int, status: Utbetalingstatus, datoStatusFom: LocalDate, periode: Periode, annullering: UtbetalingView) {
        assertEquals(true, annullering.inspektør.erAnnullering)
        assertEquals(status, annullering.inspektør.tilstand)
        assertEquals(nettobeløp, annullering.inspektør.nettobeløp)
        assertEquals(datoStatusFom, annullering.inspektør.arbeidsgiverOppdrag.linjer.first().datoStatusFom)
        assertEquals(periode, annullering.inspektør.periode)
    }

    private fun assertTomAnnulleringsutbetaling(annullering: UtbetalingView) {
        assertEquals(true, annullering.inspektør.erAnnullering)
        assertEquals(Utbetalingstatus.FORKASTET, annullering.inspektør.tilstand)
        assertEquals(0, annullering.inspektør.nettobeløp)
        assertEquals(emptyList<Utbetalingslinje>(), annullering.inspektør.arbeidsgiverOppdrag.linjer)
        assertEquals(emptyList<Utbetalingslinje>(), annullering.inspektør.personOppdrag.linjer)
    }
}
