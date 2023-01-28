package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.Personidentifikator
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

    internal fun <T> brukPersonOpplysninger(visitor: (id: Personidentifikator, aktørId: String, alder: Alder) -> T): T = visitor(personidentifikator, aktørId, alder)
}