package no.nav.helse.inntektsmelding

import io.prometheus.client.Counter
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.sakskompleks.domain.Sakskompleks
import org.slf4j.LoggerFactory

class InntektsmeldingProbe {

    companion object {
        private val log = LoggerFactory.getLogger(InntektsmeldingProbe::class.java)

        val innteksmeldingerMottattCounterName = "inntektsmeldinger_mottatt_totals"
        val innteksmeldingKobletTilSakCounterName = "inntektsmelding_koblet_til_sak_totals"
        val manglendeSakskompleksForInntektsmeldingCounterName = "manglende_sakskompleks_for_inntektsmelding_totals"

        private val inntektsmeldingMottattCounter = Counter.build(innteksmeldingerMottattCounterName, "Antall inntektsmeldinger mottatt")
                .register()

        private val inntektsmeldingKobletTilSakCounter = Counter.build(innteksmeldingKobletTilSakCounterName, "Antall inntektsmeldinger vi har mottatt som ble koblet til et sakskompleks")
            .register()

        private val manglendeSakskompleksForInntektsmeldingCounter = Counter.build(manglendeSakskompleksForInntektsmeldingCounterName, "Antall inntektsmeldinger vi har mottatt som vi ikke klarer å koble til et sakskompleks")
                .register()
    }

    fun mottattInntektsmelding(inntektsmelding: Inntektsmelding) {
        log.info("mottok inntektsmelding med id=${inntektsmelding.inntektsmeldingId} " +
                "for arbeidstaker med aktørId = ${inntektsmelding.arbeidstakerAktorId} " +
                "fra arbeidsgiver med virksomhetsnummer ${inntektsmelding.virksomhetsnummer} " +
                "evt. arbeidsgiverAktørId = ${inntektsmelding.arbeidsgiverAktorId}")
        inntektsmeldingMottattCounter.inc()
    }

    fun inntektsmeldingKobletTilSakskompleks(inntektsmelding: Inntektsmelding, sak: Sakskompleks) {
        log.info("Inntektsmelding med id ${inntektsmelding.inntektsmeldingId} ble koblet til sakskompleks med id ${sak.id}")
        inntektsmeldingKobletTilSakCounter.inc()
    }

    fun inntektmeldingManglerSakskompleks(inntektsmelding: Inntektsmelding) {
        log.error("Mottok inntektsmelding med id ${inntektsmelding.inntektsmeldingId}, men klarte ikke finne et et tilhørende sakskompleks :(")
        manglendeSakskompleksForInntektsmeldingCounter.inc()
    }

}
