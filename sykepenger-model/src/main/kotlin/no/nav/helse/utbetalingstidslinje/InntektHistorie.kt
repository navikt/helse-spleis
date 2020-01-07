package no.nav.helse.utbetalingstidslinje

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.RawValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Grunnbeløp.Companion.`6G`
import no.nav.helse.hendelser.Inntektsmelding
import java.time.LocalDate

internal class InntektHistorie {
    private class Inntekt(val fom: LocalDate, val kilde: String, val dagsats: Int)

    private val inntekter = mutableListOf<Inntekt>()

    fun add(dagen: LocalDate, kilde: String, dagsats: Int) {
        inntekter.add(Inntekt(dagen, kilde, dagsats))
    }

    fun add(inntektsmelding: Inntektsmelding) {
        inntekter.add(Inntekt(
            inntektsmelding.førsteFraværsdag,
            "Inntektsmelding",
            inntektsmelding.dagsats(inntektsmelding.førsteFraværsdag, `6G`)
        ))
    }

    fun inntekt(dagen: LocalDate) = inntekter.sortedBy { it.fom }.last { dagen.isAfter(it.fom) }.dagsats

    internal class Memento internal constructor(
        internal val inntekter: List<Inntekt>
    ) {

        fun state(): JsonNode {
            return objectMapper.createObjectNode().also {
                it.set<ArrayNode>("inntekter", inntekter.fold(objectMapper.createArrayNode()) { result, current ->
                    result.addRawValue(RawValue(current.state()))
                })
            }
        }

        internal class Inntekt(val fom: LocalDate, val kilde: String, val dagsats: Int) {
            internal companion object {
                fun fromJsonNode(json: JsonNode) = Inntekt(
                    fom = LocalDate.parse(json["fom"].textValue()),
                    kilde = json["kilde"].asText(),
                    dagsats = json["dagsats"].asInt()
                )
            }

            fun state(): JsonNode = objectMapper.convertValue<ObjectNode>(
                mapOf("fom" to this.fom, "kilde" to this.kilde, "dagsats" to this.dagsats)
            )
        }

        internal companion object {
            val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)


            fun fromJsonNode(json: JsonNode): Memento {
                return Memento(
                    inntekter = json["inntekter"].map {
                        Inntekt.fromJsonNode(it)
                    }
                )
            }
        }
    }

    internal companion object {
        fun restore(memento: Memento) = InntektHistorie().apply {
            memento.inntekter.forEach { this.add(it.fom, it.kilde, it.dagsats) }
        }
    }

    internal fun memento() = Memento(inntekter.map { Memento.Inntekt(it.fom, it.kilde, it.dagsats) })

}
