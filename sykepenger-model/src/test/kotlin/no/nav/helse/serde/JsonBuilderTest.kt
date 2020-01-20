package no.nav.helse.serde

import no.nav.helse.PersonInStateTestHelper
import no.nav.helse.person.TilstandType
import org.junit.jupiter.api.Test

internal class JsonBuilderTest {

    @Test
    fun test1() {
        val testPerson = PersonInStateTestHelper().personInState(TilstandType.TIL_UTBETALING)
        val jsonBuilder = JsonBuilder()
        testPerson.accept(jsonBuilder)

        println(jsonBuilder.toString())
    }
}
