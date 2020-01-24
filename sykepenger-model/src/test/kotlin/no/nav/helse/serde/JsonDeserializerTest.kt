package no.nav.helse.serde

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.InntektHistorie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
    private val organisasjonsnummer = "ORGNR"

    private val arbeidsgiverId = UUID.randomUUID().toString()
    private val hendelseId = UUID.randomUUID().toString()

    @Test
    fun test1() {
        val result = ModelBuilder(enkelPersonJson()).result()

        assertEquals(aktørId, result.privatProp("aktørId"))
        assertEquals(fødselsnummer, result.privatProp("fødselsnummer"))
        val arbeidsgivere = result.privatProp<MutableMap<String, Arbeidsgiver>>("arbeidsgivere")
        assertEquals(1, arbeidsgivere.size)
        val arbeidsgiver = arbeidsgivere[organisasjonsnummer]
        assertNotNull(arbeidsgiver)
        assertEquals(organisasjonsnummer, arbeidsgiver!!.privatProp("organisasjonsnummer"))
        val inntekter = arbeidsgiver.privatProp<InntektHistorie>("inntektHistorie")
            .privatProp<MutableList<InntektHistorie.Inntekt>>("inntekter")
        assertEquals(1, inntekter.size)
        //assertEquals(hendelseId, inntekter.first().hendelse.hendelseId())
    }

    private fun enkelPersonJson() =
        jacksonObjectMapper().writeValueAsString(
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
                ),
                "hendelser" to mapOf(
                    hendelseId to mapOf<String, Any?>(
                        "id" to hendelseId,
                        "type" to "ArbeidstakerHendelse"
                    )
                )
            )
        )
}

private inline fun <reified T> Any.privatProp(fieldName: String): T =
    this::class.memberProperties.first { it.name == fieldName }.apply {
        isAccessible = true
    }.call(this) as T



