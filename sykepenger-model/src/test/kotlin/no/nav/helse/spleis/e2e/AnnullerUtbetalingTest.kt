package no.nav.helse.spleis.e2e

import no.nav.helse.april
import no.nav.helse.dsl.*
import no.nav.helse.dsl.UgyldigeSituasjonerObservatør.Companion.assertUgyldigSituasjon
import no.nav.helse.februar
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Behandlinger.Behandling.Tilstand
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.tilstandsmaskin.TilstandType.*
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.util.*

internal class AnnullerUtbetalingTest : AbstractDslTest() {
    @Test
    fun `Vedtaksperioden skal være med i annulleringskandidater`() {
        a1 {
            nyttVedtak(januar)

            val annulleringskandidater =
                inspektør.yrkesaktivitet
                    .vedtaksperioder()
                    .first()
                    .inspektør.annulleringskandidater
                    .map { it.id }
            assertEquals(listOf(1.vedtaksperiode), annulleringskandidater)
        }
    }

    @Test
    fun `Etterfølgende, utbetalte vedtaksperioder skal være med i annulleringskandidater`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)

            val annulleringskandidater =
                inspektør.yrkesaktivitet
                    .vedtaksperioder()
                    .first()
                    .inspektør.annulleringskandidater
                    .map { it.id }
            assertEquals(listOf(1.vedtaksperiode, 2.vedtaksperiode), annulleringskandidater)
        }
    }

    @Test
    fun `Tidligere, utbetalte vedtaksperioder skal ikke være med i annulleringskandidater`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)

            val annulleringskandidater =
                inspektør.yrkesaktivitet
                    .vedtaksperioder()
                    .last()
                    .inspektør.annulleringskandidater
                    .map { it.id }
            assertEquals(listOf(2.vedtaksperiode), annulleringskandidater)
        }
    }

    @Test
    fun `Uberegnede vedtaksperioder skal ikke være med i annulleringskandidater`() {
        a1 {
            nyttVedtak(januar)
            nyPeriode(februar)

            val annulleringskandidater =
                inspektør.yrkesaktivitet
                    .vedtaksperioder()
                    .first()
                    .inspektør.annulleringskandidater
                    .map { it.id }
            assertEquals(listOf(1.vedtaksperiode), annulleringskandidater)
        }
    }

    @Test
    fun `Bare vedtaksperioder med samme agp skal være med i annulleringskandidater`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)

            nyttVedtak(april)

            val annulleringskandidater =
                inspektør.yrkesaktivitet
                    .vedtaksperioder()
                    .first()
                    .inspektør.annulleringskandidater
                    .map { it.id }
            assertEquals(listOf(1.vedtaksperiode, 2.vedtaksperiode), annulleringskandidater)
        }
    }

    @Test
    fun `Tillater saksbehandler å forkaste auu-er som omgjøres`() {
        a1 {
            håndterSøknad(1.januar til 10.januar)
            håndterSøknad(11.januar til 14.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            håndterSøknad(15.januar til 31.januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(3.vedtaksperiode)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

            // AUU-en skal utbetales allikevel
            håndterSelvbestemtArbeidsgiveropplysninger(emptyList(), begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening", vedtaksperiodeId = 1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_AO_3, 1.vedtaksperiode.filter())

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            assertSkjæringstidspunktOgVenteperiode(3.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))

            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false, automatiskBehandling = false)

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 11.januar, listOf(11.januar til 26.januar))
            assertSkjæringstidspunktOgVenteperiode(3.vedtaksperiode, 11.januar, listOf(11.januar til 26.januar))

            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)

            håndterVilkårsgrunnlag(3.vedtaksperiode)
            assertVarsel(RV_IV_7, 3.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Saksbehandler avviser auu under omgjøring og annullerer etterfulgt utbetalt periode`() {
        a1 {
            håndterSøknad(1.januar til 10.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            håndterSøknad(11.januar til 31.januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)

            // AUU-en skal utbetales allikevel
            håndterSelvbestemtArbeidsgiveropplysninger(emptyList(), begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening", vedtaksperiodeId = 1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_AO_3, 1.vedtaksperiode.filter())

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))

            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false, automatiskBehandling = false)

            assertSkjæringstidspunktOgVenteperiode(1.vedtaksperiode, 1.januar, listOf(1.januar til 16.januar))
            assertSkjæringstidspunktOgVenteperiode(2.vedtaksperiode, 11.januar, listOf(11.januar til 26.januar))

            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING_REVURDERING)

            håndterVilkårsgrunnlag(2.vedtaksperiode)
            håndterAnnullering(2.vedtaksperiode)

            assertSisteTilstand(2.vedtaksperiode, TIL_ANNULLERING)

            assertVarsler(2.vedtaksperiode, RV_IV_7)
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

            håndterAnnullering(1.vedtaksperiode)
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

            håndterAnnullering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, TIL_ANNULLERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_ANNULLERING)
        }
    }

    @Test
    fun `kun én vedtaksperiode skal annulleres`() {
        a1 {
            nyttVedtak(januar)

            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            nullstillTilstandsendringer()

            håndterAnnullering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            val annullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
        }
    }

    @Test
    fun `begge vedtaksperioder annulleres når vi annullerer den første`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)

            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            nullstillTilstandsendringer()

            håndterAnnullering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING)
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.UberegnetAnnullering,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            val annullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            val annulleringFebruar =
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling
            assertAnnullering(-28620, Utbetalingstatus.OVERFØRT, 1.februar, 1.februar til 28.februar, annulleringFebruar!!)

            håndterUtbetalt()

            val utførtAnnulleringFebruar =
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertAnnullering(-28620, Utbetalingstatus.ANNULLERT, 1.februar, 1.februar til 28.februar, utførtAnnulleringFebruar!!)
            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
        }
    }

    @Test
    fun `kun siste vedtaksperiode annulleres når det er denne som forsøkes annullert`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)

            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            nullstillTilstandsendringer()

            håndterAnnullering(2.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            assertEquals(
                Tilstand.VedtakIverksatt,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            val annullering =
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling
            assertAnnullering(-28620, Utbetalingstatus.OVERFØRT, 1.februar, 1.februar til 28.februar, annullering!!)

            håndterUtbetalt()

            val utførtAnnulleringFebruar =
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertAnnullering(-28620, Utbetalingstatus.ANNULLERT, 1.februar, 1.februar til 28.februar, utførtAnnulleringFebruar!!)
            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
        }
    }

    @Test
    fun `annullerer bare perioder etter den som forsøkes annullert`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            forlengVedtak(mars)

            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            nullstillTilstandsendringer()

            håndterAnnullering(2.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING)
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            assertEquals(
                Tilstand.VedtakIverksatt,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.UberegnetAnnullering,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            val annullering =
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling
            assertAnnullering(-28620, Utbetalingstatus.OVERFØRT, 1.februar, 1.februar til 28.februar, annullering!!)

            håndterUtbetalt()

            val utførtAnnulleringFebruar =
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertAnnullering(-28620, Utbetalingstatus.ANNULLERT, 1.februar, 1.februar til 28.februar, utførtAnnulleringFebruar!!)
            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            val annulleringMars =
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling
            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertAnnullering(-31482, Utbetalingstatus.OVERFØRT, 1.mars, 1.mars til 31.mars, annulleringMars!!)

            håndterUtbetalt()

            val utførtAnnulleringMars =
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertAnnullering(-31482, Utbetalingstatus.ANNULLERT, 1.mars, 1.mars til 31.mars, utførtAnnulleringMars!!)
            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
        }
    }

    @Test
    fun `annullerer bare i sammenhengende agp`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            nyttVedtak(april)

            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            nullstillTilstandsendringer()

            håndterAnnullering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.UberegnetAnnullering,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.UberegnetRevurdering,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            val annullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertVarsel(Varselkode.RV_RV_7, 3.vedtaksperiode.filter())

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
        }
    }

    @Test
    fun `annullerer også etter kort gap`() {
        a1 {
            nyttVedtak(januar)
            nyttVedtak(10.februar til 28.februar)

            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            nullstillTilstandsendringer()

            håndterAnnullering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING)
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.UberegnetAnnullering,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            val annullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
        }
    }

    @Test
    fun `annullerer periode som har ny uberegnet periode etter seg`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)

            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            nullstillTilstandsendringer()

            håndterAnnullering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE)

            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.Uberegnet,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            val annullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
        }
    }

    @Test
    fun `annullerer periode som har ny beregnet periode etter seg`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(februar)
            håndterYtelser(2.vedtaksperiode)

            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            nullstillTilstandsendringer()

            håndterAnnullering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_SIMULERING, AVVENTER_BLOKKERENDE_PERIODE)
            assertTrue(inspektør.utbetaling(1).erForkastet)

            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.Uberegnet,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            val annullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_SIMULERING, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
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

            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            nullstillTilstandsendringer()

            håndterAnnullering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING, AVVENTER_ANNULLERING)
            assertTrue(inspektør.utbetaling(2).erForkastet)

            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.UberegnetAnnullering,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            val annullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertTilstander(2.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            håndterUtbetalt()

            val utførtAnnulleringFebruar =
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertAnnullering(-14300, Utbetalingstatus.ANNULLERT, 1.februar, 1.februar til 28.februar, utførtAnnulleringFebruar!!)
            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
        }
    }

    @Test
    fun `annullerer periode som har pågående uberegnet revurdering etter seg`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar, 50.prosent)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(28.februar, Dagtype.Sykedag, 100)))

            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            nullstillTilstandsendringer()

            håndterAnnullering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_ANNULLERING)

            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.UberegnetAnnullering,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            val annullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling
            assertAnnullering(-15741, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)

            håndterUtbetalt()

            val utførtAnnullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertAnnullering(-15741, Utbetalingstatus.ANNULLERT, 17.januar, 1.januar til 31.januar, utførtAnnullering!!)
            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            håndterUtbetalt()

            val utførtAnnulleringFebruar =
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertAnnullering(-14300, Utbetalingstatus.ANNULLERT, 1.februar, 1.februar til 28.februar, utførtAnnulleringFebruar!!)
            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
        }
    }

    @Test
    fun `annulleringer på vedtaksperioder med samme utbetaling`() {
        medJSONPerson("/personer/to_vedtak_samme_fagsystem_id.json", 334)
        a1 {
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            nullstillTilstandsendringer()

            håndterAnnullering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING)
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.UberegnetAnnullering,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            val annullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling
            assertAnnullering(-32913, Utbetalingstatus.OVERFØRT, 19.januar, 3.januar til 20.februar, annullering!!)

            håndterUtbetalt()

            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_INFOTRYGD)

            assertEquals(listOf(1.vedtaksperiode, 2.vedtaksperiode), observatør.vedtaksperiodeAnnullertEventer.map { it.vedtaksperiodeId })

            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            val forventet =
                setOf(
                    inspektør
                        .vedtaksperioder(1.vedtaksperiode)
                        .behandlinger
                        .behandlinger()
                        .first()
                        .endringer()
                        .last()
                        .utbetaling,
                    inspektør
                        .vedtaksperioder(2.vedtaksperiode)
                        .behandlinger
                        .behandlinger()
                        .first()
                        .endringer()
                        .last()
                        .utbetaling,
                    inspektør
                        .vedtaksperioder(1.vedtaksperiode)
                        .behandlinger
                        .behandlinger()
                        .last()
                        .endringer()
                        .last()
                        .utbetaling,
                    inspektør
                        .vedtaksperioder(2.vedtaksperiode)
                        .behandlinger
                        .behandlinger()
                        .last()
                        .endringer()
                        .last()
                        .utbetaling,
                )
            assertEquals(
                forventet,
                inspektør.yrkesaktivitet.utbetalinger.toSet(),
            )
        }
    }

    @Test
    fun `annullering av siste periode og vedtaksperioder med samme utbetaling`() {
        medJSONPerson("/personer/to_vedtak_samme_fagsystem_id.json", 334)

        a1 {
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            håndterAnnullering(2.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_ANNULLERING)
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
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
                Tilstand.BeregnetRevurdering,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.UberegnetAnnullering,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterUtbetalt()

            assertEquals(listOf(2.vedtaksperiode), observatør.vedtaksperiodeAnnullertEventer.map { it.vedtaksperiodeId })

            assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_ANNULLERING, TIL_INFOTRYGD)

            val annullering =
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertTomAnnulleringsutbetaling(annullering!!)
        }
    }

    @Test
    fun `annullering av midterste periode og vedtaksperioder med samme utbetaling`() {
        medJSONPerson("/personer/tre_vedtak_samme_fagsystem_id.json", 320)

        a1 {
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            håndterAnnullering(2.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_ANNULLERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_ANNULLERING)
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

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
                Tilstand.BeregnetRevurdering,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.UberegnetAnnullering,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.UberegnetAnnullering,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterUtbetalt()

            assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_ANNULLERING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_ANNULLERING, TIL_INFOTRYGD)

            assertEquals(listOf(2.vedtaksperiode, 3.vedtaksperiode), observatør.vedtaksperiodeAnnullertEventer.map { it.vedtaksperiodeId })
            val annullering =
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertTomAnnulleringsutbetaling(annullering!!)
        }
    }

    @Test
    fun `annullering av siste periode og vedtaksperioder med samme utbetaling og vi er til revurdering`() {
        medJSONPerson("/personer/tre_vedtak_samme_fagsystem_id.json", 320)

        a1 {
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(18.januar, Dagtype.Sykedag, 80)))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_REVURDERING)

            håndterAnnullering(3.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_ANNULLERING)
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

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
            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_ANNULLERING, TIL_INFOTRYGD)

            assertEquals(listOf(3.vedtaksperiode), observatør.vedtaksperiodeAnnullertEventer.map { it.vedtaksperiodeId })

            assertEquals(6, inspektør.utbetalinger.size)
            val annulleringsutbetaling =
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling

            assertTomAnnulleringsutbetaling(annulleringsutbetaling!!)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `overstyring av arbeidsgiveropplysninger lar revurderinger fullføres før senere periode annulleres`(endreInntekt: Boolean) {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            forlengVedtak(mars)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(18.januar, Dagtype.Sykedag, 80)))
            håndterAnnullering(3.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_ANNULLERING)
            assertEquals(
                Tilstand.UberegnetAnnullering,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            val annulleringsbehandling =
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
            val annulleringsendringer = annulleringsbehandling.endringer().toList()

            val skjæringstidspunkt = inspektør.skjæringstidspunkt(1.vedtaksperiode)
            val overstyring =
                if (endreInntekt) {
                    OverstyrtArbeidsgiveropplysning(a1, 32000.månedlig, emptyList())
                } else {
                    OverstyrtArbeidsgiveropplysning(
                        a1,
                        31000.månedlig,
                        listOf(Triple(skjæringstidspunkt, null, 0.månedlig)),
                    )
                }

            håndterOverstyrArbeidsgiveropplysninger(skjæringstidspunkt, listOf(overstyring))

            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_ANNULLERING)
            assertEquals(Tilstand.UberegnetAnnullering, annulleringsbehandling.tilstand)
            assertEquals(
                annulleringsbehandling,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last(),
            )
            assertEquals(annulleringsendringer, annulleringsbehandling.endringer())
            listOf(1.vedtaksperiode, 2.vedtaksperiode).forEach { vedtaksperiode ->
                if (endreInntekt) {
                    assertEquals(32000.månedlig, inspektør.korrigertInntekt(vedtaksperiode)?.inntektsdata?.beløp)
                } else {
                    assertEquals(
                        0.månedlig,
                        inspektør.refusjon(vedtaksperiode)[inspektør.periode(vedtaksperiode).start].beløp,
                    )
                }
            }

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_ANNULLERING)

            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(3.vedtaksperiode, TIL_ANNULLERING)

            håndterUtbetalt()
            assertSisteTilstand(3.vedtaksperiode, TIL_INFOTRYGD)

            assertEquals(Tilstand.AnnullertPeriode, annulleringsbehandling.tilstand)
            assertEquals(
                listOf(3.vedtaksperiode),
                observatør.vedtaksperiodeAnnullertEventer.map { it.vedtaksperiodeId },
            )
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `overstyring hopper over annulleringsperioder også mens utbetaling pågår`(venterPåUtbetaling: Boolean) {
        a1 {
            nyttVedtak(januar)
            forlengelseTilGodkjenning(februar)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            if (!venterPåUtbetaling) håndterUtbetalt()
            håndterAnnullering(2.vedtaksperiode)

            val forventetTilstand = if (venterPåUtbetaling) AVVENTER_ANNULLERING_TIL_UTBETALING else TIL_ANNULLERING
            assertSisteTilstand(2.vedtaksperiode, forventetTilstand)
            val annulleringsbehandling =
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
            val behandlingstilstand = annulleringsbehandling.tilstand
            val annulleringsendringer = annulleringsbehandling.endringer().toList()

            håndterOverstyrArbeidsgiveropplysninger(
                1.januar,
                listOf(
                    OverstyrtArbeidsgiveropplysning(
                        a1,
                        32000.månedlig,
                        listOf(Triple(1.januar, 31.mars, 16000.månedlig)),
                    ),
                ),
            )

            assertSisteTilstand(2.vedtaksperiode, forventetTilstand)
            assertEquals(behandlingstilstand, annulleringsbehandling.tilstand)
            assertEquals(
                annulleringsbehandling,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last(),
            )
            assertEquals(annulleringsendringer, annulleringsbehandling.endringer())
            assertEquals(32000.månedlig, inspektør.korrigertInntekt(1.vedtaksperiode)?.inntektsdata?.beløp)
            assertEquals(16000.månedlig, inspektør.refusjon(1.vedtaksperiode)[17.januar].beløp)
            val fremtidigRefusjon = inspektør.ubrukteRefusjonsopplysninger[1.januar]
            assertEquals(1.mars, fremtidigRefusjon?.first()?.dato)
            assertEquals(31.mars, fremtidigRefusjon?.last()?.dato)
            assertEquals(16000.månedlig, fremtidigRefusjon?.get(1.mars)?.beløp)
        }
    }

    @Test
    fun `annullering av andra periode hvor første er AUU og vedtaksperioder med samme utbetaling`() {
        medJSONPerson("/personer/tre_vedtak_samme_fagsystem_id_forste_periode_AUU.json", 320)

        a1 {
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            håndterAnnullering(1.vedtaksperiode)

            assertEquals(inspektør.vedtaksperioder(1.vedtaksperiode).tilstand.type, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, TIL_ANNULLERING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_ANNULLERING)

            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            assertEquals(3, inspektør.utbetalinger.size)

            assertEquals(
                Tilstand.AvsluttetUtenVedtak,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.UberegnetAnnullering,
                inspektør
                    .vedtaksperioder(3.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            nullstillTilstandsendringer()
            håndterUtbetalt()

            assertEquals(
                Tilstand.AnnullertPeriode,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            assertEquals(listOf(2.vedtaksperiode, 3.vedtaksperiode), observatør.vedtaksperiodeAnnullertEventer.map { it.vedtaksperiodeId })

            assertEquals(inspektør.vedtaksperioder(1.vedtaksperiode).tilstand.type, AVSLUTTET_UTEN_UTBETALING)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVVENTER_ANNULLERING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `annullerer ikke ennå perioder på tvers av arbeidsgivere ved samme sykefravær`() {
        (a1 og a2).nyeVedtak(januar, inntekt = 31000.månedlig)
        (a1 og a2).forlengVedtak(februar)

        assertEquals(
            1,
            inspektør(a2)
                .vedtaksperioder(1.vedtaksperiode(a2))
                .behandlinger
                .behandlinger()
                .size,
        )
        assertEquals(
            1,
            inspektør(a2)
                .vedtaksperioder(2.vedtaksperiode(a2))
                .behandlinger
                .behandlinger()
                .size,
        )
        a1 {
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                1,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            nullstillTilstandsendringer()
            håndterAnnullering(1.vedtaksperiode)

            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING)
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            assertEquals(
                Tilstand.OverførtAnnullering,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.UberegnetAnnullering,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )

            val annullering =
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .endringer()
                    .last()
                    .utbetaling
            assertAnnullering(-11880, Utbetalingstatus.OVERFØRT, 17.januar, 1.januar til 31.januar, annullering!!)
        }

        a2 {
            assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertTilstander(2.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING)
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )
            assertEquals(
                2,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .size,
            )

            assertEquals(
                Tilstand.UberegnetRevurdering,
                inspektør
                    .vedtaksperioder(1.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
            assertEquals(
                Tilstand.UberegnetRevurdering,
                inspektør
                    .vedtaksperioder(2.vedtaksperiode)
                    .behandlinger
                    .behandlinger()
                    .last()
                    .tilstand,
            )
        }
    }

    @Test
    fun `avvis hvis arbeidsgiver er ukjent`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
        }
        assertThrows<IllegalStateException> { a2 { håndterAnnullering(UUID.randomUUID()) } }
    }

    @Test
    fun `annuller siste utbetaling`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)

            val annulleringnehov =
                annulleringsbehov(1.vedtaksperiode) {
                    håndterAnnullering(1.vedtaksperiode)
                }

            assertIngenFunksjonelleFeil()

            håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)
            assertFalse(testperson.personlogg.harFunksjonelleFeil())
            assertEquals(2, inspektør.antallUtbetalinger)
            inspektør.utbetaling(1).arbeidsgiverOppdrag.inspektør.also {
                assertEquals(19.januar, it.fom(0))
                assertEquals(26.januar, it.tom(0))
                assertEquals(19.januar, it.datoStatusFom(0))
            }

            with(annulleringnehov) {
                assertNull(maksdato)
                assertEquals("SPREF", fagområde)
                assertEquals("OPPH", linjer.single().statuskode)
            }
        }
    }

    @Test
    fun `Annuller periode til utbetaling`() {
        a1 {
            tilGodkjenning(januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
            håndterUtbetalt()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_ANNULLERING_TIL_UTBETALING, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Annuller revurdering til utbetaling`() {
        a1 {
            nyttVedtak(januar, 80.prosent)
            håndterSøknad(januar)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
            håndterUtbetalt()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_ANNULLERING_TIL_UTBETALING, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Annuller revurdering mens førstegangsbehandlingen er til utbetaling`() {
        a1 {
            tilGodkjenning(januar, 80.prosent)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterSøknad(januar)
            nullstillTilstandsendringer()
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
            håndterUtbetalt()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_REVURDERING_TIL_UTBETALING, AVVENTER_ANNULLERING_TIL_UTBETALING, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Annuller flere fagsystemid for samme arbeidsgiver`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            nyttVedtak(mars, 100.prosent)
            annulleringsbehov(2.vedtaksperiode) {
                håndterAnnullering(2.vedtaksperiode)
            }
            håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)

            annulleringsbehov(1.vedtaksperiode) {
                håndterAnnullering(1.vedtaksperiode)
            }
            håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)
        }
    }

    @Test
    fun `påminne annullering til utbetaling`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt(status = Oppdragstatus.OVERFØRT)
            assertSisteTilstand(1.vedtaksperiode, TIL_ANNULLERING)
            håndterPåminnelse(1.vedtaksperiode, TIL_ANNULLERING)
            håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)
            assertSisteForkastetTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    private fun TestPerson.TestArbeidsgiver.annulleringsbehov(
        vedtaksperiodeId: UUID,
        block: () -> Unit,
    ): Behovsoppsamler.Behovsdetaljer.Utbetaling {
        val behovet =
            behovSomOppstårSomFølgeAv<Behovsoppsamler.Behovsdetaljer.Utbetaling> {
                block()
            }.single { it.vedtaksperiodeId == vedtaksperiodeId }
        assertEquals(inspektør.sisteArbeidsgiveroppdragFagsystemId(vedtaksperiodeId), behovet.fagsystemId)
        assertEquals("OPPH", behovet.linjer.single().statuskode)
        return behovet
    }

    @Test
    fun `Kan annullere hvis noen vedtaksperioder er til utbetaling`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            tilGodkjenning(mars, 100.prosent)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()

            assertSisteTilstand(1.vedtaksperiode, TIL_ANNULLERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        }
    }

    @Test
    fun `Ved feilet annulleringsutbetaling settes utbetaling til annullering feilet`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            nullstillTilstandsendringer()
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt(status = Oppdragstatus.FEIL)
            assertFalse(testperson.personlogg.harFunksjonelleFeil())
            assertEquals(Utbetalingstatus.OVERFØRT, inspektør.utbetaling(1).tilstand)
            assertSisteTilstand(1.vedtaksperiode, TIL_ANNULLERING)
        }
    }

    @Test
    fun `Periode som håndterer avvist annullering i TilAnnullering blir værende i TilAnnullering`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            nullstillTilstandsendringer()
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt(status = Oppdragstatus.AVVIST)
            assertFalse(testperson.personlogg.harFunksjonelleFeil())
            assertEquals(Utbetalingstatus.OVERFØRT, inspektør.utbetaling(1).tilstand)
            assertSisteTilstand(1.vedtaksperiode, TIL_ANNULLERING)
        }
    }

    @Test
    fun `Periode som håndterer godkjent annullering i TilAnnullering blir forkastet`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            nullstillTilstandsendringer()
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt(status = Oppdragstatus.AKSEPTERT)
            assertFalse(testperson.personlogg.harFunksjonelleFeil(), testperson.personlogg.toString())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `Annullering av én periode fører kun til at sammehengende utbetalte perioder blir forkastet og værende i Avsluttet`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            forlengVedtak(27.januar til 30.januar, 100.prosent)
            nyttVedtak(1.mars til 20.mars, 100.prosent)
            nullstillTilstandsendringer()

            // Annuler 1 mars til 20 mars
            annulleringsbehov(3.vedtaksperiode) {
                håndterAnnullering(3.vedtaksperiode)
            }
            håndterUtbetalt()
            assertFalse(testperson.personlogg.harFunksjonelleFeil(), testperson.personlogg.toString())
            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertTilstander(2.vedtaksperiode, AVSLUTTET)
            assertForkastetPeriodeTilstander(3.vedtaksperiode, AVSLUTTET, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `publiserer et event ved annullering av full refusjon`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt(
                status = Oppdragstatus.AKSEPTERT,
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

            annulleringsbehov(1.vedtaksperiode) {
                håndterAnnullering(1.vedtaksperiode)
            }
            val annullering = inspektør.utbetaling(2)

            assertTrue(annullering.erAnnullering)
            assertEquals(
                26.januar,
                annullering.arbeidsgiverOppdrag.inspektør.periode
                    ?.endInclusive,
            )
            assertEquals(
                19.januar,
                annullering.arbeidsgiverOppdrag
                    .first()
                    .inspektør.fom,
            )
            assertEquals(
                26.januar,
                annullering.arbeidsgiverOppdrag
                    .last()
                    .inspektør.tom,
            )
        }
    }

    @Test
    fun `UtbetalingAnnullertEvent inneholder saksbehandlerident`() {
        a1 {
            nyttVedtak(3.januar til 26.januar, 100.prosent)
            håndterAnnullering(1.vedtaksperiode)
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
            håndterAnnullering(1.vedtaksperiode)
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
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars))
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
            assertFalse(testperson.personlogg.harFunksjonelleFeil())
            assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
            assertTrue(inspektør.periodeErForkastet(2.vedtaksperiode))
        }
    }

    @Test
    fun `annullering av periode medfører at låser på sykdomstidslinje blir forkastet`() {
        a1 {
            nyttVedtak(januar)
            håndterAnnullering(1.vedtaksperiode)
            inspektør.sykdomstidslinje.inspektør.låstePerioder.also {
                assertEquals(0, it.size)
            }
        }
    }

    @Test
    fun `annullering etter revurdering feilet`() {
        a1 {
            nyttVedtak(3.januar til 26.januar)
            forlengVedtak(29.januar til 26.februar)

            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(26.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertUgyldigSituasjon("En vedtaksperiode i AVVENTER_GODKJENNING_REVURDERING trenger hjelp!") {
                håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            }
            assertVarsler(listOf(RV_UT_23, Varselkode.RV_UT_24), 1.vedtaksperiode.filter())
            nullstillTilstandsendringer()
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
            assertForkastetPeriodeTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_ANNULLERING, TIL_ANNULLERING, TIL_INFOTRYGD)
            assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING, AVVENTER_ANNULLERING, TIL_ANNULLERING)
        }
    }

    private fun assertAnnullering(
        nettobeløp: Int,
        status: Utbetalingstatus,
        datoStatusFom: LocalDate,
        periode: Periode,
        annullering: Utbetaling,
    ) {
        assertEquals(true, annullering.inspektør.erAnnullering)
        assertEquals(status, annullering.inspektør.tilstand)
        assertEquals(nettobeløp, annullering.inspektør.nettobeløp)
        assertEquals(
            datoStatusFom,
            annullering.inspektør.arbeidsgiverOppdrag.linjer
                .first()
                .datoStatusFom,
        )
        assertEquals(periode, annullering.inspektør.periode)
    }

    private fun assertTomAnnulleringsutbetaling(annullering: Utbetaling) {
        assertEquals(true, annullering.inspektør.erAnnullering)
        assertEquals(Utbetalingstatus.ANNULLERT, annullering.inspektør.tilstand)
        assertEquals(0, annullering.inspektør.nettobeløp)
        assertEquals(emptyList<Utbetalingslinje>(), annullering.inspektør.arbeidsgiverOppdrag.linjer)
        assertEquals(emptyList<Utbetalingslinje>(), annullering.inspektør.personOppdrag.linjer)
    }
}
