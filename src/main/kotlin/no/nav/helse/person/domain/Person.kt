package no.nav.helse.person.domain

import no.nav.helse.søknad.domain.Sykepengesøknad

class Person {
    private val arbeidsgivere = mutableMapOf<String, Arbeidsgiver>()

    fun add(søknad: Sykepengesøknad) {
        val arbeidsgiver = findOrCreateArbeidsgiver()
    }

    private fun findOrCreateArbeidsgiver(søknad: Sykepengesøknad): Arbeidsgiver {
        return arbeidsgivere.getOrPut(søknad.organisasjonsnummer()) {
            Arbeidsgiver(søknad)
        }
    }

    internal class Arbeidsgiver(hendelse: Sykdomshendelse) {
        private val organisasjonsnummer = hendelse.organisasjonsnummer()
    }
}

interface Sykdomshendelse {
    fun organisasjonsnummer(): String
}
