package no.nav.helse.unit.person

import no.nav.helse.TestConstants.søknad
import no.nav.helse.person.domain.Person
import no.nav.helse.søknad.domain.Sykepengesøknad
import org.junit.jupiter.api.Test

internal class PersonTest {


    @Test internal fun `first event`() {
        val p = Person()
        p.add(søknad)
    }
}