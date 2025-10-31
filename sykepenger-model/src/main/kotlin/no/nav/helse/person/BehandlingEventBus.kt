package no.nav.helse.person

import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingEventBus(
    private val eventBus: EventBus,
    private val yrkesaktivitetstype: Behandlingsporing.Yrkesaktivitet,
    private val vedtaksperiodeId: UUID,
    private val tidligereSøknadIder: Set<MeldingsreferanseId>
) {
    fun behandlingLukket(
        behandlingId: UUID
    ) {
        eventBus.behandlingLukket(
            EventSubscription.BehandlingLukketEvent(
                yrkesaktivitetssporing = yrkesaktivitetstype,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId
            )
        )
    }

    fun behandlingForkastet(
        behandlingId: UUID,
        automatiskBehandling: Boolean
    ) {
        eventBus.behandlingForkastet(
            EventSubscription.BehandlingForkastetEvent(
                yrkesaktivitetssporing = yrkesaktivitetstype,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                automatiskBehandling = automatiskBehandling
            )
        )
    }

    fun nyBehandling(
        id: UUID,
        periode: Periode,
        meldingsreferanseId: MeldingsreferanseId,
        innsendt: LocalDateTime,
        registert: LocalDateTime,
        avsender: Avsender,
        type: EventSubscription.BehandlingOpprettetEvent.Type,
        søknadIder: Set<MeldingsreferanseId>
    ) {
        val event = EventSubscription.BehandlingOpprettetEvent(
            yrkesaktivitetssporing = yrkesaktivitetstype,
            vedtaksperiodeId = vedtaksperiodeId,
            søknadIder = (tidligereSøknadIder + søknadIder).map { it.id }.toSet(),
            behandlingId = id,
            fom = periode.start,
            tom = periode.endInclusive,
            type = type,
            kilde = EventSubscription.BehandlingOpprettetEvent.Kilde(meldingsreferanseId.id, innsendt, registert, avsender)
        )
        eventBus.nyBehandling(event)
    }
}
