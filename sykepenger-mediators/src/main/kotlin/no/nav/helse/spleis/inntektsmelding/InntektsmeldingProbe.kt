package no.nav.helse.spleis.inntektsmelding

import io.prometheus.client.Counter
import no.nav.helse.hendelser.inntektsmelding.Inntektsmelding
import org.slf4j.LoggerFactory

object InntektsmeldingProbe {

    private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

    private val innteksmeldingerMottattCounterName = "inntektsmeldinger_mottatt_totals"

    private val inntektsmeldingMottattCounter = Counter.build(innteksmeldingerMottattCounterName, "Antall inntektsmeldinger mottatt")
            .register()

    fun mottattInntektsmelding(inntektsmelding: Inntektsmelding) {
        sikkerLogg.info("${inntektsmelding.jsonNode}")
        inntektsmeldingMottattCounter.inc()
    }

}
