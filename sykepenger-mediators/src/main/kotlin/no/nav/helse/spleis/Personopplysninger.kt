package no.nav.helse.spleis

import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.Personidentifikator
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.person.Person

internal class Personopplysninger internal constructor(
    private val personidentifikator: Personidentifikator,
    private val aktørId: String,
    private val alder: Alder,
    private val dødsdato: LocalDate?
) {
    constructor(
        personidentifikator: Personidentifikator,
        aktørId: String,
        fødselsdato: LocalDate,
        dødsdato: LocalDate?
    ) : this(personidentifikator, aktørId, fødselsdato.alder, dødsdato)

    fun person(jurist: MaskinellJurist) = Person(aktørId, personidentifikator, alder, dødsdato, jurist)
}