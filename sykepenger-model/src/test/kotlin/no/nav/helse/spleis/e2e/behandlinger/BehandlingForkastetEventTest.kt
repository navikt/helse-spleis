package no.nav.helse.spleis.e2e.behandlinger

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.selvstendig
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.person.BehandlingView.TilstandView.ANNULLERT_PERIODE
import no.nav.helse.person.BehandlingView.TilstandView.TIL_INFOTRYGD
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_ANNULLERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BehandlingForkastetEventTest : AbstractDslTest() {

    @Test
    fun `uberegnet behandling forkastes`() {
        a1 {
            tilGodkjenning(januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            val sisteBehandling = inspektørForkastet(1.vedtaksperiode).behandlinger.single()
            val forventetBehandlingId = sisteBehandling.id
            val forventetBehandlingEvent = EventSubscription.BehandlingForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = forventetBehandlingId,
                automatiskBehandling = true
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(TIL_INFOTRYGD, sisteBehandling.tilstand)
            assertEquals(forventetBehandlingEvent, behandlingForkastetEvent)
        }
    }

    @Test
    fun `uberegnet selvstendig behandling forkastes`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            val sisteBehandling = inspektørForkastet(1.vedtaksperiode).behandlinger.single()
            val forventetBehandlingId = sisteBehandling.id
            val forventetBehandlingEvent = EventSubscription.BehandlingForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Selvstendig,
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = forventetBehandlingId,
                automatiskBehandling = true
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(TIL_INFOTRYGD, sisteBehandling.tilstand)
            assertEquals(forventetBehandlingEvent, behandlingForkastetEvent)
        }
    }

    @Test
    fun `uberegnet behandling forkastes manuelt`() {
        a1 {
            tilGodkjenning(januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false, automatiskBehandling = false)
            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            val sisteBehandling = inspektørForkastet(1.vedtaksperiode).behandlinger.single()
            val forventetBehandlingId = sisteBehandling.id
            val forventetBehandlingEvent = EventSubscription.BehandlingForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = forventetBehandlingId,
                automatiskBehandling = false
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(TIL_INFOTRYGD, sisteBehandling.tilstand)
            assertEquals(forventetBehandlingEvent, behandlingForkastetEvent)
        }
    }

    @Test
    fun `behandling uten vedtak forkastes`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            val behandlinger = inspektørForkastet(1.vedtaksperiode).behandlinger
            val sisteBehandling = behandlinger.last()
            val forventetBehandlingId = sisteBehandling.id
            val forventetBehandlingEvent = EventSubscription.BehandlingForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = forventetBehandlingId,
                automatiskBehandling = true
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(2, behandlinger.size)
            assertEquals(TIL_INFOTRYGD, sisteBehandling.tilstand)
            assertEquals(forventetBehandlingEvent, behandlingForkastetEvent)
        }
    }

    @Test
    fun `bestridelse av sykdom - auu`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterInntektsmelding(
                emptyList(),
                førsteFraværsdag = 1.januar,
                begrunnelseForReduksjonEllerIkkeUtbetalt = "BetvilerArbeidsufoerhet"
            )
            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            val behandlinger = inspektørForkastet(1.vedtaksperiode).behandlinger
            val sisteBehandling = behandlinger.last()
            val forventetBehandlingId = sisteBehandling.id
            val forventetBehandlingEvent = EventSubscription.BehandlingForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = forventetBehandlingId,
                automatiskBehandling = false
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(2, behandlinger.size)
            assertEquals(TIL_INFOTRYGD, sisteBehandling.tilstand)
            assertEquals(forventetBehandlingEvent, behandlingForkastetEvent)
        }
    }

    @Test
    fun `delvis overlappende sykdom - auu`() {
        a1 {
            håndterSøknad(Sykdom(11.januar, 16.januar, 100.prosent))
            håndterSøknad(Sykdom(10.januar, 15.januar, 100.prosent))
            assertVarsler(listOf(Varselkode.RV_SØ_13), 1.vedtaksperiode.filter())

            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            val behandlinger = inspektørForkastet(2.vedtaksperiode).behandlinger
            val sisteBehandling = behandlinger.last()
            val forventetBehandlingId = sisteBehandling.id
            val forventetBehandlingEvent = EventSubscription.BehandlingForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 2.vedtaksperiode,
                behandlingId = forventetBehandlingId,
                automatiskBehandling = false
            )
            assertTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET_UTEN_UTBETALING)
            assertTilstand(2.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(1, behandlinger.size)
            assertEquals(TIL_INFOTRYGD, sisteBehandling.tilstand)
            assertEquals(forventetBehandlingEvent, behandlingForkastetEvent)
        }
    }

    @Test
    fun `utenlandsk sykmelding`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), utenlandskSykmelding = true)

            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            val behandlinger = inspektørForkastet(1.vedtaksperiode).behandlinger
            val sisteBehandling = behandlinger.last()
            val forventetBehandlingId = sisteBehandling.id
            val forventetBehandlingEvent = EventSubscription.BehandlingForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = forventetBehandlingId,
                automatiskBehandling = false
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(1, behandlinger.size)
            assertEquals(TIL_INFOTRYGD, sisteBehandling.tilstand)
            assertEquals(forventetBehandlingEvent, behandlingForkastetEvent)
        }
    }

    @Test
    fun `annullering av siste periode og vedtaksperioder med samme utbetaling`() {
        medJSONPerson("/personer/to_vedtak_samme_fagsystem_id.json", 334)

        a1 {
            håndterAnnullering(2.vedtaksperiode)
            assertVarsel(Varselkode.RV_RV_7, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)

            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterUtbetalt()

            assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVSLUTTET)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, AVVENTER_ANNULLERING, TilstandType.TIL_INFOTRYGD)

            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            val behandlinger = inspektørForkastet(2.vedtaksperiode).behandlinger
            val sisteBehandling = behandlinger.last()
            val forventetBehandlingId = sisteBehandling.id
            val forventetBehandlingEvent = EventSubscription.BehandlingForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 2.vedtaksperiode,
                behandlingId = forventetBehandlingId,
                automatiskBehandling = false
            )
            assertEquals(2, behandlinger.size)
            assertEquals(ANNULLERT_PERIODE, sisteBehandling.tilstand)
            assertEquals(forventetBehandlingEvent, behandlingForkastetEvent)
        }
    }

    @Test
    fun `annullering oppretter ny behandling som forkastes`() {
        a1 {
            nyttVedtak(januar)
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            val sisteBehandling = inspektørForkastet(1.vedtaksperiode).behandlinger.last()
            val forventetBehandlingId = sisteBehandling.id
            val forventetBehandlingEvent = EventSubscription.BehandlingForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = forventetBehandlingId,
                automatiskBehandling = false
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(ANNULLERT_PERIODE, sisteBehandling.tilstand)
            assertEquals(forventetBehandlingEvent, behandlingForkastetEvent)

            val behandlingOpprettetEventer = observatør.behandlingOpprettetEventer
            assertEquals(2, behandlingOpprettetEventer.size)
            val sisteBehandlingOpprettet = behandlingOpprettetEventer.last()
            assertEquals(sisteBehandling.id, sisteBehandlingOpprettet.behandlingId)
            assertEquals(EventSubscription.BehandlingOpprettetEvent.Type.Revurdering, sisteBehandlingOpprettet.type)
        }
    }

    @Test
    fun `annullering av åpnet revurdering endrer behandling som forkastes`() {
        a1 {
            nyttVedtak(januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_UT_23, 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            val sisteBehandling = inspektørForkastet(1.vedtaksperiode).behandlinger.last()
            val forventetBehandlingId = sisteBehandling.id
            val forventetBehandlingEvent = EventSubscription.BehandlingForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = forventetBehandlingId,
                automatiskBehandling = false
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(ANNULLERT_PERIODE, sisteBehandling.tilstand)
            assertEquals(forventetBehandlingEvent, behandlingForkastetEvent)

            val behandlingOpprettetEventer = observatør.behandlingOpprettetEventer
            assertEquals(2, behandlingOpprettetEventer.size)
            val sisteBehandlingOpprettet = behandlingOpprettetEventer.last()
            assertEquals(sisteBehandling.id, sisteBehandlingOpprettet.behandlingId)
            assertEquals(EventSubscription.BehandlingOpprettetEvent.Type.Revurdering, sisteBehandlingOpprettet.type)
        }
    }
}
