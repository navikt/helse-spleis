package no.nav.helse.inntektsmelding

import io.prometheus.client.Counter
import org.slf4j.LoggerFactory

class InntektsmeldingProbe {

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

        private val innteksmeldingerMottattCounterName = "inntektsmeldinger_mottatt_totals"

        private val inntektsmeldingMottattCounter = Counter.build(innteksmeldingerMottattCounterName, "Antall inntektsmeldinger mottatt")
                .register()
    }

    fun mottattInntektsmelding(inntektsmelding: Inntektsmelding) {
        sikkerLogg.info("${inntektsmelding.jsonNode}")
        inntektsmeldingMottattCounter.inc()
    }

}
