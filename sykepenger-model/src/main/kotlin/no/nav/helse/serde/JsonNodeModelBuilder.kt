package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.person.Person
import no.nav.helse.serde.reflection.create.ReflectionCreationHelper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())

class JsonNodeModelBuilder(json: String) {
    companion object {
        private val reflector = ReflectionCreationHelper()
    }

    fun result(): Person =
        PersonParser().parse(rootNode)



    private val rootNode = objectMapper.readTree(json)

    interface Parser<T> {
        fun parse(jsonNode: JsonNode): T
    }

    private class PersonParser : Parser<Person> {
        override fun parse(jsonNode: JsonNode): Person {
            val hendelser = jsonNode["hendelser"]
                .map { HendelseParser().parse(it) }
            val hendelseMap = hendelser
                .map { it.hendelseId().toString() to it }.toMap()
            val arbeidsgivere = jsonNode["arbeidsgivere"]
                .map { ArbeidsgiverParser(hendelseMap).parse(it) }

            val person = Person(
                fødselsnummer = jsonNode["fødselsnummer"].asText(),
                aktørId = jsonNode["aktørId"].asText()
            )

            val personArbeidsgivere = person.privatProp<MutableMap<String, Arbeidsgiver>>("arbeidsgivere")
            personArbeidsgivere.putAll(arbeidsgivere.map { it.organisasjonsnummer() to it }.toMap())

            return person

        }

        private class HendelseParser : Parser<ArbeidstakerHendelse> {
            override fun parse(jsonNode: JsonNode): ArbeidstakerHendelse {
                val type = jsonNode["type"].asText()
                val data = jsonNode["data"]
                return when (type) {
                    "Inntektsmelding" -> InntektsmeldingParser().parse(data)
                    else -> error("Unknown hendelsetype $type")
                }
            }

            private class InntektsmeldingParser :
                Parser<ModelInntektsmelding> {
                override fun parse(jsonNode: JsonNode): ModelInntektsmelding = ModelInntektsmelding(
                    hendelseId = jsonNode["hendelseId"].asUUID(),
                    refusjon = RefusjonParser().parse(jsonNode["refusjon"]),
                    orgnummer = jsonNode["orgnummer"].asText(),
                    fødselsnummer = jsonNode["fødselsnummer"].asText(),
                    aktørId = jsonNode["aktørId"].asText(),
                    mottattDato = jsonNode["mottattDato"].asDateTime(),
                    førsteFraværsdag = jsonNode["førsteFraværsdag"].asDate(),
                    beregnetInntekt = jsonNode["beregnetInntekt"].asDouble(),
                    aktivitetslogger = Aktivitetslogger(),
                    originalJson = "{}",
                    arbeidsgiverperioder = jsonNode["arbeidsgiverperioder"]
                        .asOptional()
                        ?.map { Periode(it["fom"].asDate(), it["tom"].asDate()) } ?: listOf(),
                    ferieperioder = jsonNode["ferieperioder"]
                        .asOptional()
                        ?.map { Periode(it["fom"].asDate(), it["tom"].asDate()) } ?: listOf()
                )

                private class RefusjonParser : Parser<ModelInntektsmelding.Refusjon> {
                    override fun parse(jsonNode: JsonNode) = ModelInntektsmelding.Refusjon(
                        opphørsdato = jsonNode["opphørsdato"].asOptional()?.asDate(),
                        beløpPrMåned = jsonNode["beløpPrMåned"].asDouble(),
                        endringerIRefusjon = jsonNode["endringerIRefusjon"].asOptional()?.map(JsonNode::asDate)
                    )
                }
            }
        }

        private class ArbeidsgiverParser(
            private val hendelser: Map<String, ArbeidstakerHendelse>
        ) : Parser<Arbeidsgiver> {
            override fun parse(jsonNode: JsonNode): Arbeidsgiver {
                val arbeidsgiverData = objectMapper.convertValue<ArbeidsgiverData>(jsonNode)
                val inntektHistorie = Inntekthistorikk()

                /*jsonNode["inntekter"].forEach {
                    inntektHistorie.add(
                        fom = it["fom"].asDate(),
                        hendelse = hendelser[it["hendelse"].asText()] as ModelInntektsmelding,
                        beløp = it["beløp"].decimalValue()
                    )
                }

                return reflector.lagArbeidsgiver(
                    organisasjonsnummer = jsonNode["organisasjonsnummer"].asText(),
                    id = jsonNode["id"].asUUID(),
                    inntektHistorie = inntektHistorie
                )*/
                arbeidsgiverData.inntekter.forEach {
                    inntektHistorie.add(it.fom, hendelser[it.hendelse.toString()] as ModelInntektsmelding, it.beløp)
                }

                return reflector.lagArbeidsgiver(
                    organisasjonsnummer = arbeidsgiverData.organisasjonsnummer,
                    id = arbeidsgiverData.id,
                    inntekthistorikk = inntektHistorie
                )
            }

            private data class ArbeidsgiverData(
                val organisasjonsnummer: String,
                val id: UUID,
                val inntekter: List<InntektData>
            ) {
                data class InntektData(
                    val fom: LocalDate,
                    val hendelse: UUID,
                    val beløp: BigDecimal
                )
            }
        }
    }
}

private fun JsonNode?.asOptional() = if (this?.isNull == false) this else null
private fun JsonNode.asUUID() = UUID.fromString(asText())
private fun JsonNode.asDate() = LocalDate.parse(asText())
private fun JsonNode.asDateTime() = LocalDateTime.parse(asText())
