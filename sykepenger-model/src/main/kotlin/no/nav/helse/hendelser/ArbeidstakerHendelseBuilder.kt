package no.nav.helse.hendelser

import no.nav.helse.person.ArbeidstakerHendelse

interface ArbeidstakerHendelseBuilder {
    fun build(json: String): ArbeidstakerHendelse?
}
