package no.nav.helse.spleis

import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonskjemaForGammelt
import no.nav.helse.person.VedtaksperiodeObserver.StateChangeEvent
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

    private val utenforOmfangCounter =
        Counter.build("utenfor_omfang_totals", "Antall ganger en hendelse er utenfor omfang")
            .labelNames("hendelse")
            .register()

    private val vedtaksperiodePåminnetCounter =
        Counter.build("vedtaksperiode_paminnet_totals", "Antall ganger en vedtaksperiode er blitt påminnet")
            .labelNames("tilstand")
            .register()

    private val personskjemaForGammeltCounter =
        Counter.build("personskjema_for_gammelt_totals", "fordelinger av for gamle versjoner av person")
            .labelNames("skjemaVersjon")
            .register()

    override fun vedtaksperiodeTrengerLøsning(event: Behov) {
        event.behovType().forEach { behovCounter.labels(it, event.hendelsetype().name).inc() }
    }

    override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {}

    fun forGammelSkjemaversjon(err: PersonskjemaForGammelt) {
        personskjemaForGammeltCounter
            .labels("${err.skjemaVersjon}")
            .inc()
    }

    override fun vedtaksperiodeEndret(event: StateChangeEvent) {
        tilstandGauge.labels(event.forrigeTilstand.name).dec()
        tilstandGauge.labels(event.gjeldendeTilstand.name).inc()

        tilstandCounter.labels(
            event.forrigeTilstand.name,
            event.gjeldendeTilstand.name,
            event.sykdomshendelse.hendelsetype().name
        ).inc()

        log.info(
            "vedtaksperiode endret {}, {}, {}, {}",
            keyValue("vedtaksperiodeId", "${event.id}"),
            keyValue("hendelse", event.sykdomshendelse.hendelsetype().name),
            keyValue("tilstand", event.gjeldendeTilstand.name),
            keyValue("forrigeTilstand", event.forrigeTilstand.name)
        )
    }

    override fun vedtaksperiodePåminnet(påminnelse: Påminnelse) {
        log.info(
            "mottok påminnelse for vedtaksperiode: ${påminnelse.vedtaksperiodeId()}",
            keyValue("påminnelsenr", "${påminnelse.antallGangerPåminnet}"),
            keyValue("påminnelsestidspunkt", påminnelse.påminnelsestidspunkt.toString()),
            keyValue("vedtaksperiodeId", påminnelse.vedtaksperiodeId()),
            keyValue("tilstand", påminnelse.tilstand.name),
            keyValue("tilstandsendringstidspunkt", påminnelse.tilstandsendringstidspunkt.toString()),
            keyValue("nestePåminnelsestidspunkt", påminnelse.nestePåminnelsestidspunkt.toString())
        )

        vedtaksperiodePåminnetCounter
            .labels(påminnelse.tilstand.toString())
            .inc()
    }

    fun utenforOmfang(hendelse: ArbeidstakerHendelse) {
        utenforOmfangCounter.labels(hendelse.hendelsetype().name).inc()
    }
}
