package no.nav.helse.spleis.e2e.behandlinger

import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.BehandlingView.TilstandView.ANNULLERT_PERIODE
import no.nav.helse.person.BehandlingView.TilstandView.TIL_INFOTRYGD
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType
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
            val forventetBehandlingEvent = PersonObserver.BehandlingForkastetEvent(
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
    fun `uberegnet behandling forkastes manuelt`() {
        a1 {
            tilGodkjenning(januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false, automatiskBehandling = false)
            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            val sisteBehandling = inspektørForkastet(1.vedtaksperiode).behandlinger.single()
            val forventetBehandlingId = sisteBehandling.id
            val forventetBehandlingEvent = PersonObserver.BehandlingForkastetEvent(
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
            val sisteBehandling = inspektørForkastet(1.vedtaksperiode).behandlinger.last()
            val forventetBehandlingId = sisteBehandling.id
            val forventetBehandlingEvent = PersonObserver.BehandlingForkastetEvent(
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
    fun `annullering oppretter ny behandling som forkastes`() {
        a1 {
            nyttVedtak(januar)
            håndterAnnullering(inspektør.utbetalinger(1.vedtaksperiode).single().inspektør.utbetalingId)
            håndterUtbetalt()
            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            val sisteBehandling = inspektørForkastet(1.vedtaksperiode).behandlinger.last()
            val forventetBehandlingId = sisteBehandling.id
            val forventetBehandlingEvent = PersonObserver.BehandlingForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = forventetBehandlingId,
                automatiskBehandling = false
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(ANNULLERT_PERIODE, sisteBehandling.tilstand)
            assertForventetFeil(
                forklaring = "Automatisk behandling settes av utbetalingshendelsen, og den er jo automatisk. Men det er egentlig annulleringen som forkaster den, så det burde være false",
                nå = { assertEquals(forventetBehandlingEvent.copy(automatiskBehandling = true), behandlingForkastetEvent) },
                ønsket = { assertEquals(forventetBehandlingEvent, behandlingForkastetEvent) }
            )

            val behandlingOpprettetEventer = observatør.behandlingOpprettetEventer
            assertEquals(2, behandlingOpprettetEventer.size)
            val sisteBehandlingOpprettet = behandlingOpprettetEventer.last()
            assertEquals(sisteBehandling.id, sisteBehandlingOpprettet.behandlingId)
            assertEquals(PersonObserver.BehandlingOpprettetEvent.Type.Revurdering, sisteBehandlingOpprettet.type)
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
            håndterAnnullering(inspektør.utbetalinger(1.vedtaksperiode).last().inspektør.utbetalingId)
            håndterUtbetalt()
            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            val sisteBehandling = inspektørForkastet(1.vedtaksperiode).behandlinger.last()
            val forventetBehandlingId = sisteBehandling.id
            val forventetBehandlingEvent = PersonObserver.BehandlingForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = forventetBehandlingId,
                automatiskBehandling = false
            )
            assertTilstand(1.vedtaksperiode, TilstandType.TIL_INFOTRYGD)
            assertEquals(ANNULLERT_PERIODE, sisteBehandling.tilstand)
            assertForventetFeil(
                forklaring = "Automatisk behandling settes av utbetalingshendelsen, og den er jo automatisk. Men det er egentlig annulleringen som forkaster den, så det burde være false",
                nå = { assertEquals(forventetBehandlingEvent.copy(automatiskBehandling = true), behandlingForkastetEvent) },
                ønsket = { assertEquals(forventetBehandlingEvent, behandlingForkastetEvent) }
            )

            val behandlingOpprettetEventer = observatør.behandlingOpprettetEventer
            assertEquals(2, behandlingOpprettetEventer.size)
            val sisteBehandlingOpprettet = behandlingOpprettetEventer.last()
            assertEquals(sisteBehandling.id, sisteBehandlingOpprettet.behandlingId)
            assertEquals(PersonObserver.BehandlingOpprettetEvent.Type.Revurdering, sisteBehandlingOpprettet.type)
        }
    }
}
