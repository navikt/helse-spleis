package no.nav.helse.sak

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.saksbehandling.ManuellSaksbehandlingHendelse
import no.nav.helse.hendelser.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import java.util.*

private const val CURRENT_SKJEMA_VERSJON=2

class Sak(val aktørId: String) : SakskompleksObserver {
    private val arbeidsgivere = mutableMapOf<String, Arbeidsgiver>()
    private var skjemaVersjon = CURRENT_SKJEMA_VERSJON

    private val sakObservers = mutableListOf<SakObserver>()
    fun håndterNySøknad(nySøknadHendelse: NySøknadHendelse) {
        if (!nySøknadHendelse.kanBehandles()) {
            throw UtenforOmfangException("kan ikke behandle ny søknad", nySøknadHendelse)
        }
        finnEllerOpprettArbeidsgiver(nySøknadHendelse).håndterNySøknad(nySøknadHendelse)
    }

    fun håndterSendtSøknad(sendtSøknadHendelse: SendtSøknadHendelse) {
        if (!sendtSøknadHendelse.kanBehandles()) {
            throw UtenforOmfangException("kan ikke behandle sendt søknad", sendtSøknadHendelse)
        }
        finnEllerOpprettArbeidsgiver(sendtSøknadHendelse).håndterSendtSøknad(sendtSøknadHendelse)
    }

    fun håndterInntektsmelding(inntektsmeldingHendelse: InntektsmeldingHendelse) {
        if (!inntektsmeldingHendelse.kanBehandles()) {
            invaliderAlleSaker(inntektsmeldingHendelse)
            throw UtenforOmfangException("kan ikke behandle inntektsmelding", inntektsmeldingHendelse)
        }
        finnEllerOpprettArbeidsgiver(inntektsmeldingHendelse).håndterInntektsmelding(inntektsmeldingHendelse)
    }

    fun håndterSykepengehistorikk(sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
        finnArbeidsgiver(sykepengehistorikkHendelse)?.håndterSykepengehistorikk(sykepengehistorikkHendelse)
    }

    fun håndterManuellSaksbehandling(manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse) {
        finnArbeidsgiver(manuellSaksbehandlingHendelse)?.håndterManuellSaksbehandling(manuellSaksbehandlingHendelse)
    }

    override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {
        sakObservers.forEach {
            it.sakEndret(SakObserver.SakEndretEvent(
                    aktørId = aktørId,
                    sykdomshendelse = event.sykdomshendelse,
                    memento = memento()
            ))
        }
    }

    fun addObserver(observer: SakObserver) {
        sakObservers.add(observer)
        arbeidsgivere.values.forEach { it.addObserver(observer) }
    }

    private fun invaliderAlleSaker(inntektsmeldingHendelse: InntektsmeldingHendelse) {
        arbeidsgivere.forEach { (_, arbeidsgiver) ->
            arbeidsgiver.invaliderSaker(inntektsmeldingHendelse)
        }
    }

    private fun finnArbeidsgiver(hendelse: ArbeidstakerHendelse) =
            hendelse.organisasjonsnummer().let { arbeidsgivere[it] }

    private fun finnEllerOpprettArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let { orgnr ->
            arbeidsgivere.getOrPut(orgnr) {
                arbeidsgiver(orgnr)
            }
        }

    private fun arbeidsgiver(organisasjonsnummer: String) =
        Arbeidsgiver(organisasjonsnummer, UUID.randomUUID()).also {
            it.addObserver(this)
            sakObservers.forEach { sakObserver ->
                it.addObserver(sakObserver)
            }
        }

    internal inner class Arbeidsgiver(val organisasjonsnummer: String, val id: UUID) {

        internal constructor(arbeidsgiverJson: ArbeidsgiverJson): this(arbeidsgiverJson.organisasjonsnummer, arbeidsgiverJson.id) {
            saker.addAll(arbeidsgiverJson.saker.map { Sakskompleks.fromJson(it) })
        }
        private val saker = mutableListOf<Sakskompleks>()

        private val sakskompleksObservers = mutableListOf<SakskompleksObserver>()

        fun håndterNySøknad(nySøknadHendelse: NySøknadHendelse) {
            if (saker.none { it.håndterNySøknad(nySøknadHendelse) }) {
                nyttSakskompleks().håndterNySøknad(nySøknadHendelse)
            }
        }

        fun håndterSendtSøknad(sendtSøknadHendelse: SendtSøknadHendelse) {
            if (saker.none { it.håndterSendtSøknad(sendtSøknadHendelse) }) {
                nyttSakskompleks().håndterSendtSøknad(sendtSøknadHendelse)
            }
        }

        fun håndterInntektsmelding(inntektsmeldingHendelse: InntektsmeldingHendelse) {
            if (saker.none { it.håndterInntektsmelding(inntektsmeldingHendelse) }) {
                nyttSakskompleks().håndterInntektsmelding(inntektsmeldingHendelse)
            }
        }

        internal fun håndterSykepengehistorikk(sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
            saker.forEach { it.håndterSykepengehistorikk(sykepengehistorikkHendelse) }
        }

        internal fun håndterManuellSaksbehandling(manuellSaksbehandlingHendelse: ManuellSaksbehandlingHendelse) {
            saker.forEach { it.håndterManuellSaksbehandling(manuellSaksbehandlingHendelse) }
        }

        internal fun invaliderSaker(hendelse: ArbeidstakerHendelse) {
            saker.forEach { it.invaliderSak(hendelse) }
        }

        fun addObserver(observer: SakskompleksObserver) {
            sakskompleksObservers.add(observer)
            saker.forEach { it.addSakskompleksObserver(observer) }
        }

        private fun nyttSakskompleks(): Sakskompleks {
            return Sakskompleks(UUID.randomUUID(), aktørId, organisasjonsnummer).also {
                sakskompleksObservers.forEach(it::addSakskompleksObserver)
                saker.add(it)
            }
        }

        internal fun jsonRepresentation(): ArbeidsgiverJson {
            return ArbeidsgiverJson(
                    organisasjonsnummer = organisasjonsnummer,
                    saker = saker.map { it.jsonRepresentation() },
                    id = id
            )
        }

    }

    private fun memento() =
            Memento(objectMapper.writeValueAsString(jsonRepresentation()))

    private fun jsonRepresentation(): SakJson {
        return SakJson(
                aktørId = aktørId,
                skjemaVersjon = skjemaVersjon,
                arbeidsgivere = arbeidsgivere.map { it.value.jsonRepresentation() }
        )
    }

    data class Memento(private val json: String) {
        override fun toString() = json
    }

    override fun toString() = memento().toString()

    internal data class ArbeidsgiverJson(
            val organisasjonsnummer: String,
            val saker: List<Sakskompleks.SakskompleksJson>,
            val id: UUID
    )

    private data class SakJson(
            val aktørId: String,
            val skjemaVersjon: Int,
            val arbeidsgivere: List<ArbeidsgiverJson>
    )

    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): Sak {
            val sakJson: SakJson = objectMapper.readValue(json)
            if (sakJson.skjemaVersjon < CURRENT_SKJEMA_VERSJON){
                throw SakskjemaForGammelt(sakJson.aktørId, sakJson.skjemaVersjon, CURRENT_SKJEMA_VERSJON)
            }
            return Sak(sakJson.aktørId)
                    .apply {
                        arbeidsgivere.putAll(sakJson.arbeidsgivere
                                .map {
                                    it.organisasjonsnummer to Arbeidsgiver(it).also { arbeidsgiver ->
                                        arbeidsgiver.addObserver(this)
                                    }
                                })
                    }
        }
    }
}
