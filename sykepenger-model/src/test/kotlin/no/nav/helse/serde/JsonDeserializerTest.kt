package no.nav.helse.serde

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Inntekthistorikk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

internal class JsonDeserializerTest {

    @Test
    fun testPrivateConstructorReflectionStuff() {
        val c = Arbeidsgiver::class.java.getDeclaredConstructor(
            String::class.java,
            UUID::class.java,
            Inntekthistorikk::class.java
        )
        c.isAccessible = true
        val uuid = UUID.randomUUID()
        val ih = Inntekthistorikk()
        val arb: Arbeidsgiver = c.newInstance("999888777", uuid, ih)
        println(arb)
    }

    private val aktørId = "AKTID"
    private val fødselsnummer = "FNR"
    private val organisasjonsnummer = "ORGNR"

    private val arbeidsgiverId = UUID.randomUUID().toString()
    private val inntektsmeldingHendelseId = UUID.randomUUID().toString()

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
        val inntekter = arbeidsgiver.privatProp<Inntekthistorikk>("inntekthistorikk")
            .privatProp<MutableList<Inntekthistorikk.Inntekt>>("inntekter")
        assertEquals(1, inntekter.size)
        assertEquals(inntektsmeldingHendelseId, inntekter.first().hendelse.hendelseId().toString())
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
                                "hendelse" to inntektsmeldingHendelseId,
                                "beløp" to 30000.0
                            )
                        )
                    )
                ),
                "hendelser" to listOf(
                    mapOf(
                        "type" to "Inntektsmelding",
                        "tidspunkt" to "2020-01-01T00:00:00",
                        "data" to mapOf(
                            "hendelseId" to inntektsmeldingHendelseId
                        )
                    )
                )
            )
        )
}
