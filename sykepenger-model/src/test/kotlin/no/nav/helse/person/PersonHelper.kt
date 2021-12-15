package no.nav.helse.person

import no.nav.helse.Organisasjonsnummer
import org.junit.jupiter.api.Assertions
import java.util.*

internal fun Person.arbeidsgiver(orgnummer: Organisasjonsnummer): Arbeidsgiver {
    var _arbeidsgiver: Arbeidsgiver? = null
    accept(object : PersonVisitor {
        override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
            if (organisasjonsnummer == orgnummer.toString()) _arbeidsgiver = arbeidsgiver
        }
    })
    return _arbeidsgiver ?: Assertions.fail { "Fant ikke arbeidsgiver $orgnummer" }
}
