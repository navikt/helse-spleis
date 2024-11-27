package no.nav.helse.person

import no.nav.helse.inspectors.inspektør
import org.junit.jupiter.api.Assertions.fail

internal fun Person.arbeidsgiver(orgnummer: String): Arbeidsgiver = this.inspektør.arbeidsgiver(orgnummer) ?: fail { "Fant ikke arbeidsgiver $orgnummer" }
