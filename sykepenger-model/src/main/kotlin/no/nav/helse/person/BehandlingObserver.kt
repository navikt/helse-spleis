package no.nav.helse.person

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal interface BehandlingObserver {

    fun vedtakAnnullert(eventBus: EventBus, aktivitetslogg: IAktivitetslogg, behandlingId: UUID)
    fun behandlingLukket(eventBus: EventBus, behandlingId: UUID)
    fun behandlingForkastet(eventBus: EventBus, behandlingId: UUID, automatiskBehandling: Boolean)

    fun nyBehandling(
        eventBus: EventBus,
        id: UUID,
        periode: Periode,
        meldingsreferanseId: MeldingsreferanseId,
        innsendt: LocalDateTime,
        registert: LocalDateTime,
        avsender: Avsender,
        type: EventSubscription.BehandlingOpprettetEvent.Type,
        søknadIder: Set<MeldingsreferanseId>
    )
}
