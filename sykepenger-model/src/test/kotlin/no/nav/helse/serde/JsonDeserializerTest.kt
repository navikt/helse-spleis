package no.nav.helse.serde

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.mapping.JsonDagType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.streams.toList

internal class JsonDeserializerTest {
    private val aktørId = "AKTID"
    private val fødselsnummer = "FNR"
    private val organisasjonsnummer = "ORGNR"

    private val arbeidsgiverId = UUID.randomUUID().toString()
    private val inntektsmeldingHendelseId = UUID.randomUUID().toString()
    private val vedtaksperiodeId = UUID.randomUUID().toString()

    private inline fun <reified T> Any.privatProp(fieldName: String): T =
        this::class.memberProperties.first { it.name == fieldName }.apply {
            isAccessible = true
        }.call(this) as T

    @Test
    internal fun test1() {
        val result = SerialisertPerson(enkelPersonJson()).deserialize()

        assertEquals(aktørId, result.privatProp("aktørId"))
        assertEquals(fødselsnummer, result.privatProp("fødselsnummer"))
        val arbeidsgivere = result.privatProp<MutableList<Arbeidsgiver>>("arbeidsgivere")
        assertEquals(1, arbeidsgivere.size)
        val arbeidsgiver = arbeidsgivere.find { it.organisasjonsnummer() == organisasjonsnummer }
        assertNotNull(arbeidsgiver)
        assertEquals(organisasjonsnummer, arbeidsgiver!!.privatProp("organisasjonsnummer"))
        val inntekter = arbeidsgiver.privatProp<Inntekthistorikk>("inntekthistorikk")
            .privatProp<MutableList<Inntekthistorikk.Inntekt>>("inntekter")
        assertEquals(1, inntekter.size)
        assertEquals(inntektsmeldingHendelseId, inntekter.first().hendelseId.toString())
    }

    private val førsteFraværsdag = LocalDate.now().minusDays(18)

    private fun enkelPersonJson(tilstand: TilstandType = TilstandType.TIL_INFOTRYGD): String {

        val tidslinje = førsteFraværsdag.datesUntil(LocalDate.now().minusDays(2))
            .map {
                mapOf(
                    "dagen" to it.toString(),
                    "type" to JsonDagType.SYKEDAG_SØKNAD
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
                    "aktivitetslogger" to mapOf(
                        "aktiviteter" to listOf(
                            mapOf(
                                "alvorlighetsgrad" to "INFO",
                                "melding" to "Behandler sykmelding",
                                "tidsstempel" to "2020-01-29 22:45:38.876"
                            )
                        )
                    ),
                    "aktivitetslogg" to mapOf(
                        "aktiviteter" to listOf(
                            mapOf(
                                "kontekster" to listOf(
                                    mapOf(
                                        "kontekstType" to "NySøknad",
                                        "melding" to "Ny søknad"
                                    )
                                ),
                                "alvorlighetsgrad" to "INFO",
                                "melding" to "Behandler ny søknad",
                                "tidsstempel" to "2020-01-29 22:45:38.876"
                            )
                        )
                    ),
                    "arbeidsgivere" to listOf(
                        mapOf(
                            "utbetalingstidslinjer" to emptyList<Map<String, Any>>(),
                            "organisasjonsnummer" to organisasjonsnummer,
                            "id" to arbeidsgiverId,
                            "inntekter" to listOf(
                                mapOf(
                                    "fom" to "2020-01-01",
                                    "hendelseId" to inntektsmeldingHendelseId,
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
                                            "hendelseId" to inntektsmeldingHendelseId,
                                            "hendelseSykdomstidslinje" to tidslinje,
                                            "beregnetSykdomstidslinje" to tidslinje

                                        )
                                    ),
                                    "tilstand" to tilstand.toString(),
                                    "utbetalingslinjer" to emptyList<Map<String, Any>>(),
                                    "aktivitetslogger" to mapOf(
                                        "aktiviteter" to emptyList<Any>()
                                    )
                                )
                            ),
                            "aktivitetslogger" to mapOf(
                                "aktiviteter" to emptyList<Any>()
                            )
                        )
                    )
                )
            ).also { println(it) }
    }
}
