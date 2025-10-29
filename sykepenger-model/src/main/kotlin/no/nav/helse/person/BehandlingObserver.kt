package no.nav.helse.person

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal interface BehandlingObserver {

    fun avsluttetUtenVedtak(
        eventBus: EventBus,
        aktivitetslogg: IAktivitetslogg,
        behandlingId: UUID,
        tidsstempel: LocalDateTime,
        periode: Periode,
        dekkesAvArbeidsgiverperioden: Boolean,
        dokumentsporing: Set<UUID>
    )

    fun vedtakIverksatt(
        eventBus: EventBus,
        aktivitetslogg: IAktivitetslogg,
        vedtakFattetTidspunkt: LocalDateTime,
        behandling: Behandlinger.Behandling
    )

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

    fun utkastTilVedtak(
        eventBus: EventBus,
        utkastTilVedtak: EventSubscription.UtkastTilVedtakEvent
    )
}
