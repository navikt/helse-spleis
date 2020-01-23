package no.nav.helse.serde

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.InntektHistorie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

internal class JsonDeserializerTest {

    @Test
    fun testPrivateConstructorReflectionStuff() {
        val c = Arbeidsgiver::class.java.getDeclaredConstructor(
            String::class.java,
            UUID::class.java,
            InntektHistorie::class.java
        )
        c.isAccessible = true
        val uuid = UUID.randomUUID()
        val ih = InntektHistorie()
        val arb: Arbeidsgiver = c.newInstance("999888777", uuid, ih)
        println(arb)
    }

    private val aktørId = "AKTID"
    private val fødselsnummer = "FNR"

    private val uuid1 = UUID.randomUUID().toString()

    @Test
    fun test1() {
        val result = ModelBuilder(enkelPersonJson()).result()

        assertEquals(aktørId, result.privatProp("aktørId"))
        assertEquals(fødselsnummer, result.privatProp("fødselsnummer"))
    }


    private fun enkelPersonJson() =
        jacksonObjectMapper().writeValueAsString(
            mapOf(
                "person" to mapOf(
                    "aktørId" to aktørId,
                    "fødselsnummer" to fødselsnummer,
                    "arbeidsgivere" to emptyList<Map<String, Any?>>()
                ),
                "hendelser" to mapOf<String, Any?>()
            )
        )
}

/*
    private fun enkelPersonJson() =
        jacksonObjectMapper().writeValueAsString(
            listOf(
                mapOf(
                    hendelseId to mapOf<String, Any?>(
                        "id" to hendelseId,
                        "type" to "ArbeidstakerHendelse"
                    )
                ),
                mapOf(
                    "aktørId" to aktørId,
                    "fødselsnummer" to fødselsnummer,
                    "arbeidsgivere" to listOf(
                        mapOf(
                            "organisasjonsnummer" to organisasjonsnummer,
                            "id" to arbeidsgiverId,
                            "inntekter" to listOf(
                                mapOf(
                                    "fom" to "2020-01-01",
                                    "hendelse" to hendelseId,
                                    "beløp" to 30000.0
                                )
                            )
                        )
                    )
                )
            )
        )
 */

private fun Any.privatProp(fieldName: String): Any? =
    this::class.memberProperties.first { it.name == fieldName }.apply {
        isAccessible = true
    }.call(this)



