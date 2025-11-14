package no.nav.helse.spleis.e2e.behandlinger

import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.aktivitetslogg.Varselkode
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

            assertBehandlingEventRekkefølge(listOf(
                EventSubscription.VedtaksperiodeOpprettet::class,
                EventSubscription.BehandlingOpprettetEvent::class
            ))

            val behandlingOpprettetEvent = observatør.behandlingOpprettetEventer.last()
            inspektør(1.vedtaksperiode).behandlinger.also { behandlinger ->
                val behandlingId = behandlinger.single().id
                val forventetBehandlingEvent = EventSubscription.BehandlingOpprettetEvent(
                    yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                    vedtaksperiodeId = 1.vedtaksperiode,
                    søknadIder = setOf(søknadId),
                    behandlingId = behandlingId,
                    type = EventSubscription.BehandlingOpprettetEvent.Type.Søknad,
                    fom = 1.januar,
                    tom = 20.januar,
                    kilde = EventSubscription.BehandlingOpprettetEvent.Kilde(
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
            assertEquals(EventSubscription.BehandlingOpprettetEvent.Type.Søknad, førsteEvent.type)
            assertEquals(EventSubscription.BehandlingOpprettetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                søknadIder = setOf(førsteEvent.kilde.meldingsreferanseId),
                behandlingId = andreEvent.behandlingId,
                type = EventSubscription.BehandlingOpprettetEvent.Type.Revurdering,
                fom = 1.januar,
                tom = 31.januar,
                kilde = EventSubscription.BehandlingOpprettetEvent.Kilde(
                    meldingsreferanseId = andreEvent.kilde.meldingsreferanseId,
                    innsendt = andreEvent.kilde.innsendt,
                    registert = andreEvent.kilde.registert,
                    avsender = Avsender.SYKMELDT
                )
            ), andreEvent)

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
            assertEquals(EventSubscription.BehandlingOpprettetEvent.Type.Søknad, førsteEvent.type)
            assertEquals(EventSubscription.BehandlingOpprettetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                søknadIder = setOf(førsteEvent.kilde.meldingsreferanseId),
                behandlingId = andreEvent.behandlingId,
                type = EventSubscription.BehandlingOpprettetEvent.Type.Omgjøring,
                fom = 1.januar,
                tom = 16.januar,
                kilde = EventSubscription.BehandlingOpprettetEvent.Kilde(
                    meldingsreferanseId = andreEvent.kilde.meldingsreferanseId,
                    innsendt = andreEvent.kilde.innsendt,
                    registert = andreEvent.kilde.registert,
                    avsender = Avsender.ARBEIDSGIVER
                )
            ), andreEvent)
        }
    }

    @Test
    fun annullering() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            håndterAnnullering(1.vedtaksperiode)
            håndterUtbetalt()
            håndterUtbetalt()

            val behandlingOpprettetEventer = observatør.behandlingOpprettetEventer
            assertEquals(4, behandlingOpprettetEventer.size)
            assertEquals(EventSubscription.BehandlingOpprettetEvent.Type.Søknad, behandlingOpprettetEventer[0].type)
            assertEquals(EventSubscription.BehandlingOpprettetEvent.Type.Søknad, behandlingOpprettetEventer[1].type)
            assertEquals(EventSubscription.BehandlingOpprettetEvent.Type.Revurdering, behandlingOpprettetEventer[2].type)
            assertEquals(EventSubscription.BehandlingOpprettetEvent.Type.Revurdering, behandlingOpprettetEventer[3].type)

            assertBehandlingEventRekkefølge(listOf(
                EventSubscription.VedtaksperiodeOpprettet::class,
                EventSubscription.BehandlingOpprettetEvent::class,
                EventSubscription.BehandlingLukketEvent::class,
                EventSubscription.AvsluttetMedVedtakEvent::class,
                EventSubscription.VedtaksperiodeOpprettet::class,
                EventSubscription.BehandlingOpprettetEvent::class,
                EventSubscription.BehandlingLukketEvent::class,
                EventSubscription.AvsluttetMedVedtakEvent::class,

                EventSubscription.BehandlingOpprettetEvent::class,
                EventSubscription.BehandlingOpprettetEvent::class,
                EventSubscription.BehandlingForkastetEvent::class,
                EventSubscription.VedtaksperiodeAnnullertEvent::class,
                EventSubscription.VedtaksperiodeForkastetEvent::class,
                EventSubscription.BehandlingForkastetEvent::class,
                EventSubscription.VedtaksperiodeAnnullertEvent::class,
                EventSubscription.VedtaksperiodeForkastetEvent::class
            ))

            assertEquals(EventSubscription.BehandlingOpprettetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                søknadIder = setOf(behandlingOpprettetEventer[0].kilde.meldingsreferanseId),
                behandlingId = inspektørForkastet(1.vedtaksperiode).behandlinger.last().id,
                type = EventSubscription.BehandlingOpprettetEvent.Type.Revurdering,
                fom = 1.januar,
                tom = 31.januar,
                kilde = behandlingOpprettetEventer[2].kilde
            ), behandlingOpprettetEventer[2])

            val vedtaksperiodeForkastetEventer = observatør.behandlingForkastetEventer
            assertEquals(2, vedtaksperiodeForkastetEventer.size)
            assertEquals(EventSubscription.BehandlingForkastetEvent(
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 1.vedtaksperiode,
                behandlingId = inspektørForkastet(1.vedtaksperiode).behandlinger.last().id,
                automatiskBehandling = false
            ), vedtaksperiodeForkastetEventer[0])

            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `søknaden som forkastes på direkten`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), utenlandskSykmelding = true)
            val behandlingOpprettet = observatør.behandlingOpprettetEventer.single()
            assertEquals(EventSubscription.BehandlingOpprettetEvent.Type.Søknad, behandlingOpprettet.type)
        }
    }

    @Test
    fun `anmoder en periode om forkasting`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterAnmodningOmForkasting(1.vedtaksperiode)

            assertBehandlingEventRekkefølge(listOf(
                EventSubscription.VedtaksperiodeOpprettet::class,
                EventSubscription.BehandlingOpprettetEvent::class,
                EventSubscription.BehandlingLukketEvent::class,
                EventSubscription.BehandlingOpprettetEvent::class,
                EventSubscription.BehandlingForkastetEvent::class,
                EventSubscription.VedtaksperiodeForkastetEvent::class
            ))

            val behandlingOpprettet = observatør.behandlingOpprettetEventer
            assertEquals(2, behandlingOpprettet.size)
            assertEquals(EventSubscription.BehandlingOpprettetEvent.Type.Søknad, behandlingOpprettet[0].type)
            assertEquals(EventSubscription.BehandlingOpprettetEvent.Type.Omgjøring, behandlingOpprettet[1].type)
            val behandlingForkastetEvent = observatør.behandlingForkastetEventer.single()
            assertEquals(1.vedtaksperiode, behandlingForkastetEvent.vedtaksperiodeId)
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

            assertBehandlingEventRekkefølge(listOf(
                EventSubscription.VedtaksperiodeOpprettet::class,
                EventSubscription.BehandlingOpprettetEvent::class,
                EventSubscription.BehandlingLukketEvent::class,
                EventSubscription.BehandlingOpprettetEvent::class,
                EventSubscription.AvsluttetMedVedtakEvent::class,
                EventSubscription.BehandlingForkastetEvent::class,
                EventSubscription.VedtaksperiodeAnnullertEvent::class,
                EventSubscription.VedtaksperiodeForkastetEvent::class
            ))
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

            assertBehandlingEventRekkefølge(listOf(
                EventSubscription.VedtaksperiodeOpprettet::class,
                EventSubscription.BehandlingOpprettetEvent::class,
                EventSubscription.BehandlingLukketEvent::class,
                EventSubscription.AvsluttetMedVedtakEvent::class,
                EventSubscription.BehandlingOpprettetEvent::class,
                EventSubscription.BehandlingLukketEvent::class,
                EventSubscription.BehandlingOpprettetEvent::class,
                EventSubscription.AvsluttetMedVedtakEvent::class,
                EventSubscription.BehandlingForkastetEvent::class,
                EventSubscription.VedtaksperiodeAnnullertEvent::class,
                EventSubscription.VedtaksperiodeForkastetEvent::class
            ))
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

            assertBehandlingEventRekkefølge(listOf(
                EventSubscription.VedtaksperiodeOpprettet::class,
                EventSubscription.BehandlingOpprettetEvent::class,
                EventSubscription.BehandlingLukketEvent::class,
                EventSubscription.BehandlingOpprettetEvent::class,
                EventSubscription.AvsluttetMedVedtakEvent::class,
                EventSubscription.BehandlingForkastetEvent::class,
                EventSubscription.VedtaksperiodeAnnullertEvent::class,
                EventSubscription.VedtaksperiodeForkastetEvent::class
            ))
        }
    }

    private fun assertBehandlingEventRekkefølge(rekkefølge: List<KClass<out EventSubscription.Event>>) {
        assertEquals(rekkefølge, testperson.behandlingevents().map { it::class })
    }
}
