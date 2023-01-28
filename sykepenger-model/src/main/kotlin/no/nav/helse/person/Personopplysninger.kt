package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.Personidentifikator
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Alder.Companion.alder

class Personopplysninger internal constructor(
    private val personidentifikator: Personidentifikator,
    private val aktørId: String,
    private val alder: Alder
) {
    constructor(
        personidentifikator: Personidentifikator,
        aktørId: String,
        fødselsdato: LocalDate
    ) : this(personidentifikator, aktørId, fødselsdato.alder)

    internal fun nyPerson(jurist: MaskinellJurist) = Person(
        aktørId = aktørId,
        personidentifikator = personidentifikator,
        alder = alder,
        jurist = jurist
    )
}