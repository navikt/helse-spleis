package no.nav.helse.spleis

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.PersonObserver
import org.slf4j.LoggerFactory

object VedtaksperiodeProbe : PersonObserver {

    private val log = LoggerFactory.getLogger(VedtaksperiodeProbe::class.java)

    override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {}

    override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
        log.info(
            "vedtaksperiode endret {}, {}, {}",
            keyValue("vedtaksperiodeId", "${event.vedtaksperiodeId}"),
            keyValue("tilstand", event.gjeldendeTilstand.name),
            keyValue("forrigeTilstand", event.forrigeTilstand.name)
        )
    }

    override fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {
        log.info(
            "mottok påminnelse for vedtaksperiode: ${påminnelse.vedtaksperiodeId}",
            keyValue("påminnelsenr", "${påminnelse.antallGangerPåminnet()}"),
            keyValue("påminnelsestidspunkt", påminnelse.påminnelsestidspunkt().toString()),
            keyValue("vedtaksperiodeId", påminnelse.vedtaksperiodeId),
            keyValue("tilstand", påminnelse.tilstand().name),
            keyValue("tilstandsendringstidspunkt", påminnelse.tilstandsendringstidspunkt().toString()),
            keyValue("nestePåminnelsestidspunkt", påminnelse.nestePåminnelsestidspunkt().toString())
        )
    }
}
