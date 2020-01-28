package no.nav.helse.serde

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Inntekthistorikk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.streams.toList

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
    private val vedtaksperiodeId = UUID.randomUUID().toString()

    @Test
    fun test1() {
        //val result = ModelBuilder(enkelPersonJson()).result()
        //val result = JsonNodeModelBuilder(enkelPersonJson()).result()
        val result = DataClassModelBuilder(enkelPersonJson()).result()

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

    @Test
    fun test2() {
        val data = parseJson(enkelPersonJson())
        println(data)
    }

    private val førsteFraværsdag = LocalDate.now().minusDays(18)

    private fun enkelPersonJson(): String {

        val tidslinje = førsteFraværsdag.datesUntil(LocalDate.now().minusDays(2))
            .map {
                mapOf(
                    "dagen" to it.toString(),
                    "hendelseId" to inntektsmeldingHendelseId,
                    "type" to "SYKEDAG",
                    "erstatter" to emptyList<Map<String,Any>>()
                )
            }
            .toList()

        return jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .writeValueAsString(
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
                            ),
                            "vedtaksperioder" to listOf(
                                mapOf(
                                    "id" to vedtaksperiodeId,
                                    "maksdato" to LocalDate.now().plusMonths(3),
                                    "godkjentAv" to null,
                                    "utbetalingsreferanse" to null,
                                    "førsteFraværsdag" to førsteFraværsdag,
                                    "inntektFraInntektsmelding" to 30000.00,
                                    "dataForVilkårsvurdering" to null,
                                    "sykdomshistorikk" to listOf(
                                        mapOf(
                                            "tidsstempel" to "2020-01-28T12:01:21.813492",
                                            "hendelseId" to  inntektsmeldingHendelseId,
                                            "hendelseSykdomstidslinje" to tidslinje,
                                            "beregnetSykdomstidslinje" to tidslinje

                                        )),
                                    "tilstand" to "TIL_INFOTRYGD",
                                    "sykdomstidslinje" to tidslinje,
                                    "utbetalingslinjer" to emptyList<Map<String,Any>>()
                                )
                            )
                        )
                    ),
                    "hendelser" to listOf(
                        mapOf(
                            "type" to "Inntektsmelding",
                            "tidspunkt" to "2020-01-01T00:00:00",
                            "data" to mapOf(
                                "hendelseId" to inntektsmeldingHendelseId,
                                "refusjon" to mapOf(
                                    "beløpPrMåned" to 30000.00,
                                    "endringerIRefusjon" to emptyList<Any>()
                                ),
                                "orgnummer" to organisasjonsnummer,
                                "mottattDato" to "2020-01-09T14:02:28",
                                "førsteFraværsdag" to førsteFraværsdag,
                                "beregnetInntekt" to 30000.00,
                                "arbeidsgiverperioder" to listOf(
                                    mapOf(
                                        "fom" to førsteFraværsdag,
                                        "tom" to LocalDate.now().minusDays(2)
                                    )
                                ),
                                "ferieperioder" to emptyList<Map<String, Any>>()
                            )
                        )
                    )
                )
            ).also { println(it) }
    }
}
