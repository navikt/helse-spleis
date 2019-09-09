package no.nav.helse.inntektsmelding

import io.prometheus.client.Counter
import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.slf4j.LoggerFactory

class InntektsmeldingProbe {

    companion object {
        private val log = LoggerFactory.getLogger(InntektsmeldingProbe::class.java)

        val innteksmeldingerTotalsCounterName = "inntektsmeldinger_totals"
        val manglendeSakskompleksForInntektsmeldingCounterName = "manglende_sakskompleks_for_inntektsmelding_totals"

        private val inntektsmeldingCounter = Counter.build(innteksmeldingerTotalsCounterName, "Antall inntektsmeldinger mottatt")
                .register()

        private val manglendeSakskompleksForInntektsmeldingCounter = Counter.build(manglendeSakskompleksForInntektsmeldingCounterName, "Antall inntektsmeldinger vi har mottatt som vi ikke klarer å koble til et sakskompleks")
                .register()
    }

    fun mottattInntektsmelding(inntektsmelding: Inntektsmelding) {
        log.info("mottok inntektsmelding med id=${inntektsmelding.inntektsmeldingId} " +
                "for arbeidstaker med aktørId = ${inntektsmelding.arbeidstakerAktorId} " +
                "fra arbeidsgiver med virksomhetsnummer ${inntektsmelding.virksomhetsnummer} " +
                "evt. arbeidsgiverAktørId = ${inntektsmelding.arbeidsgiverAktorId}")
        inntektsmeldingCounter.inc()
    }

    fun inntektsmeldingKobletTilSakskompleks(inntektsmelding: Inntektsmelding, sak: Sakskompleks) {
        log.info("Inntektsmelding med id ${inntektsmelding.inntektsmeldingId} ble koblet til sakskompleks med id ${sak.id}")
    }

    fun inntektmeldingManglerSakskompleks(inntektsmelding: Inntektsmelding) {
        log.error("Mottok inntektsmelding med id ${inntektsmelding.inntektsmeldingId}, men klarte ikke finne et et tilhørende sakskompleks :(")
        manglendeSakskompleksForInntektsmeldingCounter.inc()
    }

}