package no.nav.helse.serde

import no.nav.helse.serde.reflection.InntektReflect
import org.junit.jupiter.api.Test

internal class JsonBuilderTest {

    @Test
    fun test2(){
        val inntektReflect = InntektReflect()
        inntektReflect.getSome()
    }
}
