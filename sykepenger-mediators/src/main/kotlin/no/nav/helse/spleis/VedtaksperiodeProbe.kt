package no.nav.helse.spleis

import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.PersonObserver
import org.slf4j.LoggerFactory

object VedtaksperiodeProbe : PersonObserver {

    private val log = LoggerFactory.getLogger(VedtaksperiodeProbe::class.java)

    private val behovCounter = Counter.build("behov_totals", "Antall behov opprettet")
        .labelNames("behovType", "hendelsetype")
        .register()

    private val tilstandCounter = Counter.build(
        "vedtaksperiode_tilstander_totals",
        "Fordeling av tilstandene periodene er i, og hvilken tilstand de kom fra"
    )
        .labelNames("forrigeTilstand", "tilstand", "hendelse")
        .register()

    private val tilstandGauge = Gauge.build(
        "gjeldende_vedtaksperiode_tilstander",
        "Gjeldende tilstander"
    )
        .labelNames("tilstand")
        .register()

    private val vedtaksperiodePåminnetCounter =
        Counter.build("vedtaksperiode_paminnet_totals", "Antall ganger en vedtaksperiode er blitt påminnet")
            .labelNames("tilstand")
            .register()

    override fun vedtaksperiodeTrengerLøsning(behov: Behov) {
        behov.behovType().forEach { behovCounter.labels(it, behov.hendelsetype().name).inc() }
    }

    override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {}

    override fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
        tilstandGauge.labels(event.forrigeTilstand.name).dec()
        tilstandGauge.labels(event.gjeldendeTilstand.name).inc()

        tilstandCounter.labels(
            event.forrigeTilstand.name,
            event.gjeldendeTilstand.name,
            event.sykdomshendelse.hendelsestype().name
        ).inc()

        log.info(
            "vedtaksperiode endret {}, {}, {}, {}",
            keyValue("vedtaksperiodeId", "${event.id}"),
            keyValue("hendelse", event.sykdomshendelse.hendelsestype().name),
            keyValue("tilstand", event.gjeldendeTilstand.name),
            keyValue("forrigeTilstand", event.forrigeTilstand.name)
        )
    }

    override fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {
        log.info(
            "mottok påminnelse for vedtaksperiode: ${påminnelse.vedtaksperiodeId()}",
            keyValue("påminnelsenr", "${påminnelse.antallGangerPåminnet()}"),
            keyValue("påminnelsestidspunkt", påminnelse.påminnelsestidspunkt().toString()),
            keyValue("vedtaksperiodeId", påminnelse.vedtaksperiodeId()),
            keyValue("tilstand", påminnelse.tilstand().name),
            keyValue("tilstandsendringstidspunkt", påminnelse.tilstandsendringstidspunkt().toString()),
            keyValue("nestePåminnelsestidspunkt", påminnelse.nestePåminnelsestidspunkt().toString())
        )

        vedtaksperiodePåminnetCounter
            .labels(påminnelse.tilstand().toString())
            .inc()
    }
}
