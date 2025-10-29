package no.nav.helse.spleis.testhelpers

import java.util.UUID
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.EventSubscription.VedtaksperiodeEndretEvent
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spleis.IdInnhenter
import org.junit.jupiter.api.fail

internal class TestObservatør : EventSubscription {
    lateinit var sisteVedtaksperiode: UUID

    private val tilstandsendringer = mutableMapOf<UUID, MutableList<TilstandType>>()
    private val utbetalteVedtaksperioder = mutableListOf<UUID>()
    private val vedtaksperioder = mutableMapOf<String, MutableSet<UUID>>()
    private val vedtaksperiodeendringer = mutableMapOf<UUID, MutableList<VedtaksperiodeEndretEvent>>()
    private val ventendeReplays = mutableListOf<Pair<String, UUID>>()
    fun hendelseider(vedtaksperiodeId: UUID) =
        vedtaksperiodeendringer[vedtaksperiodeId]?.last()?.hendelser ?: fail { "VedtaksperiodeId $vedtaksperiodeId har ingen hendelser tilknyttet" }

    fun sisteVedtaksperiode() = IdInnhenter { orgnummer -> vedtaksperioder.getValue(orgnummer).last() }
    fun vedtaksperiode(orgnummer: String, indeks: Int) = vedtaksperioder.getValue(orgnummer).toList()[indeks]

    fun ventendeReplays() = ventendeReplays.toList().also {
        ventendeReplays.clear()
    }

    override fun nyBehandling(event: EventSubscription.BehandlingOpprettetEvent) {
        sisteVedtaksperiode = event.vedtaksperiodeId
    }

    override fun inntektsmeldingReplay(event: EventSubscription.TrengerInntektsmeldingReplayEvent) {
        ventendeReplays.add(event.opplysninger.yrkesaktivitetssporing.somOrganisasjonsnummer to event.opplysninger.vedtaksperiodeId)
    }

    override fun vedtaksperiodeEndret(event: VedtaksperiodeEndretEvent) {
        vedtaksperiodeendringer.getOrPut(event.vedtaksperiodeId) { mutableListOf(event) }.add(event)
        vedtaksperioder.getOrPut(event.yrkesaktivitetssporing.somOrganisasjonsnummer) { mutableSetOf() }.add(event.vedtaksperiodeId)
        if (event.gjeldendeTilstand != event.forrigeTilstand) {
            tilstandsendringer.getOrPut(event.vedtaksperiodeId) { mutableListOf(TilstandType.START) }.add(event.gjeldendeTilstand)
        }
        if (event.gjeldendeTilstand == TilstandType.AVSLUTTET) utbetalteVedtaksperioder.add(event.vedtaksperiodeId)
    }
}
