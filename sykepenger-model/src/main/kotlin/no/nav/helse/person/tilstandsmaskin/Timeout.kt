package no.nav.helse.person.tilstandsmaskin

import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.ChronoUnit.DAYS
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Påminnelse.Predikat.Flagg
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal sealed interface Timeout {
    fun tidspunkt(tilstandsendringstidspunkt: LocalDateTime): LocalDateTime
    fun håndter(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr?

    data object Ingen: Timeout {
        override fun tidspunkt(tilstandsendringstidspunkt: LocalDateTime) = LocalDateTime.MAX
        override fun håndter(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) = null
    }

    data class Etter(
        private val periode: Period,
        private val tvingFremTimeoutoppførselFlagg: Flagg,
        private val vedTimeout: (vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) -> Revurderingseventyr
    ): Timeout {
        override fun tidspunkt(tilstandsendringstidspunkt: LocalDateTime) = tilstandsendringstidspunkt.plus(periode)

        override fun håndter(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
            val timeoutTidspunkt = tidspunkt(påminnelse.tilstandsendringstidspunkt)
            val nå = LocalDateTime.now()
            val dagerVentet = DAYS.between(påminnelse.tilstandsendringstidspunkt, nå)
            
            if (nå >= timeoutTidspunkt) {
                aktivitetslogg.info("Gir opp å vente i tilstand ${vedtaksperiode.tilstand::class.simpleName} etter $dagerVentet dager. (Timeout for tiltanden er ${periode.days} dager.)")
                return vedTimeout(vedtaksperiode, eventBus, påminnelse, aktivitetslogg)
            }

            if (påminnelse.når(tvingFremTimeoutoppførselFlagg)) {
                aktivitetslogg.info("Gir opp å vente i tilstand ${vedtaksperiode.tilstand::class.simpleName} etter $dagerVentet dager ettersom flagget '$tvingFremTimeoutoppførselFlagg' var satt i påminnelsen. (Timeout for tiltanden er ${periode.days} dager.)")
                return vedTimeout(vedtaksperiode, eventBus, påminnelse, aktivitetslogg)
            }
            return null
        }
    }
}
