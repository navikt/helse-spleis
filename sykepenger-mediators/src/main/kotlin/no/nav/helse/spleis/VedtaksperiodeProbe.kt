package no.nav.helse.spleis

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.person.EventSubscription
import org.slf4j.LoggerFactory

object VedtaksperiodeProbe : EventSubscription {

    private val log = LoggerFactory.getLogger(VedtaksperiodeProbe::class.java)

    override fun vedtaksperiodeEndret(event: EventSubscription.VedtaksperiodeEndretEvent) {
        log.info(
            "vedtaksperiode endret {}, {}, {}",
            keyValue("vedtaksperiodeId", event.vedtaksperiodeId),
            keyValue("tilstand", event.gjeldendeTilstand.name),
            keyValue("forrigeTilstand", event.forrigeTilstand.name)
        )
    }

    override fun utbetalingEndret(event: EventSubscription.UtbetalingEndretEvent) {
        log.info(
            "utbetaling endret {}, {}, {}, {}",
            keyValue("utbetalingId", event.utbetalingId),
            keyValue("korrelasjonsId", event.korrelasjonsId),
            keyValue("status", event.gjeldendeStatus),
            keyValue("forrigeStatus", event.forrigeStatus)
        )
    }

    override fun vedtaksperiodePåminnet(event: EventSubscription.VedtaksperiodePåminnetEvent) {
        log.debug(
            "mottok påminnelse for vedtaksperiode: $event.vedtaksperiodeId",
            keyValue("påminnelsenr", "${event.antallGangerPåminnet}"),
            keyValue("påminnelsestidspunkt", event.påminnelsestidspunkt.toString()),
            keyValue("event.vedtaksperiodeId", event.vedtaksperiodeId),
            keyValue("tilstand", event.tilstand.name),
            keyValue("tilstandsendringstidspunkt", event.tilstandsendringstidspunkt.toString()),
            keyValue("nestePåminnelsestidspunkt", event.nestePåminnelsestidspunkt.toString())
        )
    }
}
