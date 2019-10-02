package no.nav.helse.person.domain

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.sakskompleks.domain.SakskompleksObserver
import no.nav.helse.sykdomstidslinje.KildeHendelse
import no.nav.helse.søknad.domain.Sykepengesøknad
import java.util.*

class Person {
    private val arbeidsgivere = mutableMapOf<String, Arbeidsgiver>()
    private val personObservers = mutableListOf<PersonObserver>()

    fun add(søknad: Sykepengesøknad) {
        val arbeidsgiver = findOrCreateArbeidsgiver(søknad)
        arbeidsgiver.add(søknad)
    }

    fun addObserver(observer: PersonObserver) {
        personObservers.add(observer)
        arbeidsgivere.values.forEach { it.addObserver(observer) }
    }

    private fun findOrCreateArbeidsgiver(hendelse: Sykdomshendelse) =
            arbeidsgivere.getOrPut(hendelse.organisasjonsnummer()) {
                Arbeidsgiver(hendelse)
            }

    internal inner class Arbeidsgiver(hendelse: Sykdomshendelse) {
        private val saker = mutableListOf<Sakskompleks>()
        fun add(søknad: Sykepengesøknad) {
            val case = findOrCreateSakskompleks(søknad)
            case.leggTil(søknad)
        }

        fun addObserver(observer: PersonObserver) {
            saker.forEach { it.addObserver(observer) }
        }

        private fun findOrCreateSakskompleks(hendelse: Sykdomshendelse) : Sakskompleks {
            return Sakskompleks(UUID.randomUUID(), hendelse.aktørId()).also {
                personObservers.forEach(it::addObserver)
                saker.add(it)
            }
        }

        private val organisasjonsnummer = hendelse.organisasjonsnummer()
    }
}

interface PersonObserver : SakskompleksObserver {

}

interface Sykdomshendelse : KildeHendelse {
    fun aktørId(): String
    fun organisasjonsnummer(): String

    fun sykdomstidslinje(): Sykdomstidslinje
}
