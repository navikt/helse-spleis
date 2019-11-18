package no.nav.helse.person

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

class Person(val aktørId: String) : SakskompleksObserver {
    private val arbeidsgivere = mutableMapOf<String, Arbeidsgiver>()
    private var skjemaVersjon = CURRENT_SKJEMA_VERSJON

    private val personObservers = mutableListOf<PersonObserver>()
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
        personObservers.forEach {
            it.personEndret(PersonObserver.PersonEndretEvent(
                    aktørId = aktørId,
                    sykdomshendelse = event.sykdomshendelse,
                    memento = memento()
            ))
        }
    }

    fun addObserver(observer: PersonObserver) {
        personObservers.add(observer)
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
            personObservers.forEach { personObserver ->
                it.addObserver(personObserver)
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

    private fun jsonRepresentation(): PersonJson {
        return PersonJson(
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

    private data class PersonJson(
            val aktørId: String,
            val skjemaVersjon: Int,
            val arbeidsgivere: List<ArbeidsgiverJson>
    )

    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): Person {
            val personJson: PersonJson = objectMapper.readValue(json)
            if (personJson.skjemaVersjon < CURRENT_SKJEMA_VERSJON){
                throw PersonskjemaForGammelt(personJson.aktørId, personJson.skjemaVersjon, CURRENT_SKJEMA_VERSJON)
            }
            return Person(personJson.aktørId)
                    .apply {
                        arbeidsgivere.putAll(personJson.arbeidsgivere
                                .map {
                                    it.organisasjonsnummer to Arbeidsgiver(it).also { arbeidsgiver ->
                                        arbeidsgiver.addObserver(this)
                                    }
                                })
                    }
        }
    }
}
