package no.nav.helse.serde

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.InntektHistorie
import org.junit.jupiter.api.Test
import java.util.*

internal class JsonDeserializerTest {

    @Test
    fun testPrivateConstructorReflectionStuff() {
        val c = Arbeidsgiver::class.java.getDeclaredConstructor(String::class.java, UUID::class.java, InntektHistorie::class.java)
        c.isAccessible = true
        val uuid = UUID.randomUUID()
        val ih = InntektHistorie()
        val arb:Arbeidsgiver = c.newInstance("999888777", uuid, ih)
        println(arb)
    }

    @Test
    fun test1() {
        val json = enkelTidslinjeJson()
        val visitableJson = JsonVisitable(json)
        val modelBuilder = ModelBuilder()
        visitableJson.accept(modelBuilder)
        //println(modelBuilder.result)
    }

    private fun enkelTidslinjeJson() =
        jacksonObjectMapper().writeValueAsString(
        mapOf( "tidslinje" to listOf( mapOf(
            "type" to "FERIEDAG",
            "dato" to "2020-01-01"
        ))))




}
