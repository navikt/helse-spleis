package no.nav.helse.spleis

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.PersonObserver
import org.slf4j.LoggerFactory
import java.util.*

object VedtaksperiodeProbe : PersonObserver {

    private val log = LoggerFactory.getLogger(VedtaksperiodeProbe::class.java)

    override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {}

    override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretEvent) {
        log.info(
            "vedtaksperiode endret {}, {}, {}",
            keyValue("vedtaksperiodeId", "${event.vedtaksperiodeId}"),
            keyValue("tilstand", event.gjeldendeTilstand.name),
            keyValue("forrigeTilstand", event.forrigeTilstand.name)
        )
    }

    override fun utbetalingEndret(event: PersonObserver.UtbetalingEndretEvent) {
        log.info(
            "utbetaling endret {}, {}, {}",
            keyValue("utbetalingId", "${event.utbetalingId}"),
            keyValue("status", event.gjeldendeStatus),
            keyValue("forrigeStatus", event.forrigeStatus)
        )
    }

    override fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, påminnelse: Påminnelse) {
        log.debug(
            "mottok påminnelse for vedtaksperiode: $vedtaksperiodeId",
            keyValue("påminnelsenr", "${påminnelse.antallGangerPåminnet()}"),
            keyValue("påminnelsestidspunkt", påminnelse.påminnelsestidspunkt().toString()),
            keyValue("vedtaksperiodeId", vedtaksperiodeId),
            keyValue("tilstand", påminnelse.tilstand().name),
            keyValue("tilstandsendringstidspunkt", påminnelse.tilstandsendringstidspunkt().toString()),
            keyValue("nestePåminnelsestidspunkt", påminnelse.nestePåminnelsestidspunkt().toString())
        )
    }
}
