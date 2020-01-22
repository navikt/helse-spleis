package no.nav.helse.serde

import no.nav.helse.PersonInStateTestHelper
import no.nav.helse.person.InntektHistorie
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.reflection.InntektReflect
import org.junit.jupiter.api.Test

internal class JsonBuilderTest {

    @Test
    fun test1() {
        val testPerson = PersonInStateTestHelper().personInState(TilstandType.TIL_UTBETALING)
        val jsonBuilder = JsonBuilder()
        testPerson.accept(jsonBuilder)

        println(jsonBuilder.toString())
    }

    @Test
    fun test2(){
        val inntektReflect = InntektReflect()
        inntektReflect.getSome()
    }
}
