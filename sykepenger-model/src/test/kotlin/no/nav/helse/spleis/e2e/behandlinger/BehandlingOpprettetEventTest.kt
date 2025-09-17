package no.nav.helse.spleis.e2e.behandlinger

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_ANNULLERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BehandlingOpprettetEventTest : AbstractDslTest() {

    @Test
    fun `event om opprettet behandling`() {
        a1 {
            val søknadId = UUID.randomUUID()
            val registrert = LocalDateTime.now()
            val innsendt = registrert.minusHours(2)
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), søknadId = søknadId, sendtTilNAVEllerArbeidsgiver = innsendt, registrert = registrert)
            assertVarsel(Varselkode.RV_SØ_2, 1.vedtaksperiode.filter())
            val behandlingOpprettetEvent = observatør.behandlingOpprettetEventer.last()
            inspektør(1.vedtaksperiode).behandlinger.also { behandlinger ->
                val behandlingId = behandlinger.single().id
                val forventetBehandlingEvent = PersonObserver.BehandlingOpprettetEvent(
                    yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                    vedtaksperiodeId = 1.vedtaksperiode,
                    søknadIder = setOf(søknadId),
                    behandlingId = behandlingId,
                    type = PersonObserver.BehandlingOpprettetEvent.Type.Søknad,
                    fom = 1.januar,
                    tom = 20.januar,
                    kilde = PersonObserver.BehandlingOpprettetEvent.Kilde(
                        meldingsreferanseId = søknadId,
                        innsendt = innsendt,
                        registert = registrert,
                        avsender = Avsender.SYKMELDT
                    )
                )
                assertEquals(forventetBehandlingEvent, behandlingOpprettetEvent)
            }
        }
    }

    @Test
    fun revurdering() {
        a1 {
            nyttVedtak(januar, 100.prosent)
            håndterSøknad(Sykdom(1.januar, 31.januar, 80.prosent))
            val behandlingOpprettetEventer = observatør.behandlingOpprettetEventer
            assertEquals(2, behandlingOpprettetEventer.size)
            val førsteEvent = behandlingOpprettetEventer.first()
            val andreEvent = behandlingOpprettetEventer.last()
            assertEquals(PersonObserver.BehandlingOpprettetEvent.Type.Søknad, førsteEvent.type)
            assertEquals(PersonObserver.BehandlingOpprettetEvent.Type.Revurdering, andreEvent.type)
        }
    }

    @Test
    fun omgjøring() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterInntektsmelding(listOf(25.desember(2017) til 9.januar), beregnetInntekt = INNTEKT)
            val behandlingOpprettetEventer = observatør.behandlingOpprettetEventer
            assertEquals(2, behandlingOpprettetEventer.size)
            val førsteEvent = behandlingOpprettetEventer.first()
            val andreEvent = behandlingOpprettetEventer.last()
            assertEquals(PersonObserver.BehandlingOpprettetEvent.Type.Søknad, førsteEvent.type)
            assertEquals(PersonObserver.BehandlingOpprettetEvent.Type.Omgjøring, andreEvent.type)
        }
    }

    @Test
    fun annullering() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            håndterAnnullering(inspektør.utbetaling(0).utbetalingId)
            håndterUtbetalt()
            val behandlingOpprettetEventer = observatør.behandlingOpprettetEventer
            assertEquals(4, behandlingOpprettetEventer.size)
            assertEquals(PersonObserver.BehandlingOpprettetEvent.Type.Søknad, behandlingOpprettetEventer[0].type)
            assertEquals(PersonObserver.BehandlingOpprettetEvent.Type.Søknad, behandlingOpprettetEventer[1].type)
            assertEquals(PersonObserver.BehandlingOpprettetEvent.Type.Revurdering, behandlingOpprettetEventer[2].type)
            assertEquals(PersonObserver.BehandlingOpprettetEvent.Type.Revurdering, behandlingOpprettetEventer[3].type)

            val vedtaksperiodeForkastetEventer = observatør.behandlingForkastetEventer
            assertEquals(1, vedtaksperiodeForkastetEventer.size)
            assertEquals(1.vedtaksperiode, vedtaksperiodeForkastetEventer[0].vedtaksperiodeId)
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(2.vedtaksperiode, TIL_ANNULLERING)
        }
    }

    @Test
    fun `søknaden som forkastes på direkten`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), utenlandskSykmelding = true)
            val behandlingOpprettet = observatør.behandlingOpprettetEventer.single()
            assertEquals(PersonObserver.BehandlingOpprettetEvent.Type.Søknad, behandlingOpprettet.type)
        }
    }

    @Test
    fun `anmoder en periode om forkasting`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterAnmodningOmForkasting(1.vedtaksperiode)
            val behandlingOpprettet = observatør.behandlingOpprettetEventer
            assertEquals(2, behandlingOpprettet.size)
            assertEquals(PersonObserver.BehandlingOpprettetEvent.Type.Søknad, behandlingOpprettet[0].type)
            assertEquals(PersonObserver.BehandlingOpprettetEvent.Type.Omgjøring, behandlingOpprettet[1].type)
            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            assertEquals(1.vedtaksperiode, behandlingForkastetEvent.vedtaksperiodeId)
        }
    }
}
