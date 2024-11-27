package no.nav.helse.person

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal interface BehandlingObserver {

    fun avsluttetUtenVedtak(
        aktivitetslogg: IAktivitetslogg,
        behandlingId: UUID,
        tidsstempel: LocalDateTime,
        periode: Periode,
        dokumentsporing: Set<UUID>
    )

    fun vedtakIverksatt(
        aktivitetslogg: IAktivitetslogg,
        vedtakFattetTidspunkt: LocalDateTime,
        behandling: Behandlinger.Behandling
    )

    fun vedtakAnnullert(aktivitetslogg: IAktivitetslogg, behandlingId: UUID)
    fun behandlingLukket(behandlingId: UUID)
    fun behandlingForkastet(behandlingId: UUID, hendelse: Hendelse)

    fun nyBehandling(
        id: UUID,
        periode: Periode,
        meldingsreferanseId: UUID,
        innsendt: LocalDateTime,
        registert: LocalDateTime,
        avsender: Avsender,
        type: PersonObserver.BehandlingOpprettetEvent.Type,
        søknadIder: Set<UUID>
    )

    fun utkastTilVedtak(
        utkastTilVedtak: PersonObserver.UtkastTilVedtakEvent
    )
}
