package no.nav.helse.person

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal interface GenerasjonObserver {

    fun avsluttetUtenVedtak(
        hendelse: IAktivitetslogg,
        generasjonId: UUID,
        tidsstempel: LocalDateTime,
        periode: Periode,
        dokumentsporing: Set<UUID>
    )
    fun vedtakIverksatt(
        hendelse: IAktivitetslogg,
        generasjonId: UUID,
        tidsstempel: LocalDateTime,
        periode: Periode,
        dokumentsporing: Set<UUID>,
        utbetalingId: UUID,
        vedtakFattetTidspunkt: LocalDateTime
    )
}