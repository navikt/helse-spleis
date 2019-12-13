package no.nav.helse.behov

import io.prometheus.client.Counter
import org.slf4j.LoggerFactory

internal object BehovProbe {

    private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

    private val mottattBehovCounter = Counter.build("mottatt_behov_totals", "Antall behov mottatt")
            .labelNames("type")
            .register()

    fun mottattBehov(behov: Behov) {
        sikkerLogg.info(behov.toJson())
        behov.behovType().forEach {
            mottattBehovCounter.labels(it).inc()
        }
    }
}
