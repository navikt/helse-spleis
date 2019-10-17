package no.nav.helse.person.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.hendelse.*
import no.nav.helse.sykdomstidslinje.objectMapper
import java.util.*

class Person(val aktørId: String) : SakskompleksObserver {
    private val arbeidsgivere = mutableMapOf<String, Arbeidsgiver>()

    private val personObservers = mutableListOf<PersonObserver>()
    fun håndterNySøknad(søknad: NySykepengesøknad) {
        finnEllerOpprettArbeidsgiver(søknad).håndterNySøknad(søknad)
    }

    fun håndterSendtSøknad(søknad: SendtSykepengesøknad) {
        finnEllerOpprettArbeidsgiver(søknad).håndterSendtSøknad(søknad)
    }

    fun håndterInntektsmelding(inntektsmelding: Inntektsmelding) {
        finnEllerOpprettArbeidsgiver(inntektsmelding).håndterInntektsmelding(inntektsmelding)
    }

    fun håndterSykepengehistorikk(sykepengehistorikk: Sykepengehistorikk) {
        finnArbeidsgiver(sykepengehistorikk)?.håndterSykepengehistorikk(sykepengehistorikk)
    }

    override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {
        personObservers.forEach {
            it.personEndret(this)
        }
    }


    fun addObserver(observer: PersonObserver) {
        personObservers.add(observer)
        arbeidsgivere.values.forEach { it.addObserver(observer) }
    }

    private fun finnArbeidsgiver(hendelse: Sykdomshendelse) =
            hendelse.organisasjonsnummer()?.let { arbeidsgivere[it] }

    private fun finnEllerOpprettArbeidsgiver(hendelse: Sykdomshendelse) =
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

        fun håndterNySøknad(søknad: NySykepengesøknad) {
            if (saker.none { it.håndterNySøknad(søknad) }) {
                nyttSakskompleks().håndterNySøknad(søknad)
            }
        }

        fun håndterSendtSøknad(søknad: SendtSykepengesøknad) {
            if (saker.none { it.håndterSendtSøknad(søknad) }) {
                nyttSakskompleks().håndterSendtSøknad(søknad)
            }
        }

        fun håndterInntektsmelding(inntektsmelding: Inntektsmelding) {
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

    fun toJson(): String {
        return objectMapper.writeValueAsString(jsonRepresentation())
    }

    private fun jsonRepresentation(): PersonJson {
        return PersonJson(
            aktørId = aktørId,
            arbeidsgivere = arbeidsgivere.map { it.value.jsonRepresentation() }
        )
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

interface PersonObserver : SakskompleksObserver {
    fun personEndret(person: Person) {}
}
