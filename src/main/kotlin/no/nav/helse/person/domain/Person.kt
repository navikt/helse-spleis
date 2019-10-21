package no.nav.helse.person.domain

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.hendelse.*
import java.util.*

class Person(val aktørId: String) : SakskompleksObserver {
    private val arbeidsgivere = mutableMapOf<String, Arbeidsgiver>()

    private val personObservers = mutableListOf<PersonObserver>()
    fun håndterNySøknad(søknad: NySøknadOpprettet) {
        finnEllerOpprettArbeidsgiver(søknad).håndterNySøknad(søknad)
    }

    fun håndterSendtSøknad(søknad: SendtSøknadMottatt) {
        finnEllerOpprettArbeidsgiver(søknad).håndterSendtSøknad(søknad)
    }

    fun håndterInntektsmelding(inntektsmelding: InntektsmeldingMottatt) {
        finnEllerOpprettArbeidsgiver(inntektsmelding).håndterInntektsmelding(inntektsmelding)
    }

    fun håndterSykepengehistorikk(sykepengehistorikk: Sykepengehistorikk) {
        finnArbeidsgiver(sykepengehistorikk)?.håndterSykepengehistorikk(sykepengehistorikk)
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

    private fun finnArbeidsgiver(hendelse: DokumentMottattHendelse) =
            hendelse.organisasjonsnummer()?.let { arbeidsgivere[it] }

    private fun finnEllerOpprettArbeidsgiver(hendelse: DokumentMottattHendelse) =
        hendelse.organisasjonsnummer()?.let { orgnr ->
            arbeidsgivere.getOrPut(orgnr) {
                arbeidsgiver(orgnr)
            }
        } ?: throw UtenforOmfangException("dokument mangler virksomhetsnummer", hendelse)

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

        fun håndterNySøknad(søknad: NySøknadOpprettet) {
            if (saker.none { it.håndterNySøknad(søknad) }) {
                nyttSakskompleks().håndterNySøknad(søknad)
            }
        }

        fun håndterSendtSøknad(søknad: SendtSøknadMottatt) {
            if (saker.none { it.håndterSendtSøknad(søknad) }) {
                nyttSakskompleks().håndterSendtSøknad(søknad)
            }
        }

        fun håndterInntektsmelding(inntektsmelding: InntektsmeldingMottatt) {
            if (saker.none { it.håndterInntektsmelding(inntektsmelding) }) {
                nyttSakskompleks().håndterInntektsmelding(inntektsmelding)
            }
        }

        internal fun håndterSykepengehistorikk(sykepengehistorikk: Sykepengehistorikk) {
            saker.forEach { it.håndterSykepengehistorikk(sykepengehistorikk) }
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

    internal fun memento() =
            Memento(objectMapper.writeValueAsString(jsonRepresentation()))

    private fun jsonRepresentation(): PersonJson {
        return PersonJson(
            aktørId = aktørId,
            arbeidsgivere = arbeidsgivere.map { it.value.jsonRepresentation() }
        )
    }

    data class Memento(private val json: String) {
        override fun toString() = json
    }

    internal data class ArbeidsgiverJson(
        val organisasjonsnummer: String,
        val saker: List<Sakskompleks.SakskompleksJson>,
        val id: UUID
    )

    private data class PersonJson(
        val aktørId: String,
        val arbeidsgivere: List<ArbeidsgiverJson>
    )

    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): Person {
            val personJson: PersonJson = objectMapper.readValue(json)
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
