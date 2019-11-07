package no.nav.helse.behov

import io.prometheus.client.Counter
import org.slf4j.LoggerFactory

internal class BehovProbe {

    companion object {
        private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

        private val mottattBehovCounter = Counter.build("mottatt_behov_totals", "Antall behov mottatt")
                .labelNames("type")
                .register()
    }

    fun mottattBehov(behov: Behov) {
        sikkerLogg.info(behov.toJson())
        mottattBehovCounter.labels(behov.behovType()).inc()
    }
}
