package no.nav.helse.person.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.hendelse.Inntektsmelding
import no.nav.helse.hendelse.NySykepengesøknad
import no.nav.helse.hendelse.SendtSykepengesøknad
import no.nav.helse.hendelse.Sykdomshendelse
import no.nav.helse.sykdomstidslinje.objectMapper
import java.util.*

class Person(val aktørId: String) : SakskompleksObserver {
    private val arbeidsgivere = mutableMapOf<String, Arbeidsgiver>()

    private val personObservers = mutableListOf<PersonObserver>()
    fun håndterNySøknad(søknad: NySykepengesøknad) {
        findOrCreateArbeidsgiver(søknad).håndterNySøknad(søknad)
    }

    fun håndterSendtSøknad(søknad: SendtSykepengesøknad) {
        findOrCreateArbeidsgiver(søknad).håndterSendtSøknad(søknad)
    }

    fun håndterInntektsmelding(inntektsmelding: Inntektsmelding) {
        findOrCreateArbeidsgiver(inntektsmelding).håndterInntektsmelding(inntektsmelding)
    }

    override fun sakskompleksChanged(event: SakskompleksObserver.StateChangeEvent) {
        personObservers.forEach {
            it.personEndret(this)
        }
    }


    fun addObserver(observer: PersonObserver) {
        personObservers.add(observer)
        arbeidsgivere.values.forEach { it.addObserver(observer) }
    }

    private fun findOrCreateArbeidsgiver(hendelse: Sykdomshendelse) =
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
        internal val saker = mutableListOf<Sakskompleks>()
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


        fun jsonRepresentation(): ArbeidsgiverJson {
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

    fun jsonRepresentation(): PersonJson {
        return PersonJson(
            aktørId = aktørId,
            arbeidsgivere = arbeidsgivere.map { it.value.jsonRepresentation() }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Person

        if (aktørId != other.aktørId) return false

        return true
    }

    override fun hashCode(): Int {
        return aktørId.hashCode()
    }

    data class ArbeidsgiverJson(
        val organisasjonsnummer: String,
        val saker: List<Sakskompleks.SakskompleksJson>,
        val id: UUID
    )

    data class PersonJson(
        val aktørId: String,
        val arbeidsgivere: List<ArbeidsgiverJson>
    )

    companion object {
        fun fromJson(json: String): Person {
            val personJson: PersonJson = objectMapper.readValue(json)
            return Person(personJson.aktørId)
                .apply {
                    arbeidsgivere.putAll(personJson.arbeidsgivere
                        .map { it.organisasjonsnummer to fromArbeidsgiverJson(it).also { arbeidsgiver ->
                            //arbeidsgiver.addObserver(this)
                        } })
                }
        }

        private fun Person.fromArbeidsgiverJson(arbeidsgiverJson: ArbeidsgiverJson): Arbeidsgiver {
            return Arbeidsgiver(
                organisasjonsnummer = arbeidsgiverJson.organisasjonsnummer,
                id = arbeidsgiverJson.id
            ).apply {
                saker.addAll(arbeidsgiverJson.saker.map { Sakskompleks.fromJson(it) })
            }
        }
    }
}

interface PersonObserver : SakskompleksObserver {
    fun personEndret(person: Person) {}
}
