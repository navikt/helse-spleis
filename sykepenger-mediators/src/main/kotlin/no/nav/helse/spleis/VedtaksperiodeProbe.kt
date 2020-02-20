package no.nav.helse.spleis

import io.prometheus.client.Counter
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.behov.BehovType
import no.nav.helse.hendelser.HendelseObserver
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.PersonObserver
import org.slf4j.LoggerFactory

object VedtaksperiodeProbe : PersonObserver, HendelseObserver {

    private val log = LoggerFactory.getLogger(VedtaksperiodeProbe::class.java)

    private val behovCounter = Counter.build("behov_totals", "Antall behov opprettet")
        .labelNames("behovType")
        .register()

    override fun onBehov(behov: BehovType) {
        behovCounter.labels(behov.navn).inc()
    }

    override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {}

    override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
        log.info(
            "vedtaksperiode endret {}, {}, {}, {}",
            keyValue("vedtaksperiodeId", "${event.id}"),
            keyValue("hendelse", event.sykdomshendelse.hendelsetype()),
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

    private fun ArbeidstakerHendelse.hendelsetype() = this::class.simpleName ?: "UNKNOWN"
}
