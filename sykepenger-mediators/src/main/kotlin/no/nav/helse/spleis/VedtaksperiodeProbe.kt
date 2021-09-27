package no.nav.helse.spleis

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.PersonObserver
import org.slf4j.LoggerFactory

object VedtaksperiodeProbe : PersonObserver {

    private val log = LoggerFactory.getLogger(VedtaksperiodeProbe::class.java)

    override fun personEndret(hendelseskontekst: Hendelseskontekst) {}

    override fun vedtaksperiodeEndret(hendelseskontekst: Hendelseskontekst, event: PersonObserver.VedtaksperiodeEndretEvent) {
        log.info(
            "vedtaksperiode endret {}, {}, {}",
            keyValue("vedtaksperiodeId", hendelseskontekst.vedtaksperiodeId()),
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

    override fun vedtaksperiodePåminnet(hendelseskontekst: Hendelseskontekst, påminnelse: Påminnelse) {
        log.debug(
            "mottok påminnelse for vedtaksperiode: ${hendelseskontekst.vedtaksperiodeId()}",
            keyValue("påminnelsenr", "${påminnelse.antallGangerPåminnet()}"),
            keyValue("påminnelsestidspunkt", påminnelse.påminnelsestidspunkt().toString()),
            keyValue("vedtaksperiodeId", hendelseskontekst.vedtaksperiodeId()),
            keyValue("tilstand", påminnelse.tilstand().name),
            keyValue("tilstandsendringstidspunkt", påminnelse.tilstandsendringstidspunkt().toString()),
            keyValue("nestePåminnelsestidspunkt", påminnelse.nestePåminnelsestidspunkt().toString())
        )
    }

    private fun Hendelseskontekst.vedtaksperiodeId(): String {
        val keyValues = mutableMapOf<String, String>()
        appendTo(keyValues::set)
        return keyValues["vedtaksperiodeId"]!!
    }
}
