package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V239KompensereManglendeOriginalInntekt: JsonMigration(239) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "kobler saksbehandlerinntektene til det den overstyrer, når tidligere vilkårsgrunnlag mangler"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val skjæringstidspunktMedInntekter = mutableMapOf<LocalDate, MutableMap<String, MutableList<Pair<UUID, String>>>>()
        val aktørId = jsonNode.path("aktørId").asText()

        val arbeidsgiverinntekter = jsonNode.path("arbeidsgivere").associate { arbeidsgiver ->
            val orgnummer: String = arbeidsgiver.path("organisasjonsnummer").asText()
            val inntekter = arbeidsgiver.path("inntektshistorikk").groupBy({ inntekt ->
                inntekt.path("dato").dato()
            }) { inntekt ->
                Inntektsmelding(
                    UUID.fromString(inntekt.path("id").asText()),
                    inntekt.path("dato").dato(),
                    inntekt.path("hendelseId").asText(),
                    inntekt.path("beløp").asDouble(),
                    LocalDateTime.parse(inntekt.path("tidsstempel").asText())
                )
            }
            orgnummer to inntekter
        }
        val nyeInnslag = mutableListOf<Pair<UUID, JsonNode>>()
        jsonNode.path("vilkårsgrunnlagHistorikk")
            .reversed()
            .forEach { innslag ->
                val nyeElementer = mutableListOf<JsonNode>()
                innslag.path("vilkårsgrunnlag")
                    .filter { elementet -> elementet.path("type").asText() == "Vilkårsprøving" }
                    .forEach { elementet ->
                        val vilkårsgrunnlagId = elementet.path("vilkårsgrunnlagId").asText()
                        val skjæringstidspunkt = elementet.path("skjæringstidspunkt").dato()
                        val sykepengegrunnlag = elementet.path("sykepengegrunnlag")

                        val nyeInntekter = mutableMapOf<String, Inntektsmelding>()
                        sykepengegrunnlag.path("arbeidsgiverInntektsopplysninger").forEach { opplysning ->
                            migrerOpplysning(aktørId, skjæringstidspunktMedInntekter, vilkårsgrunnlagId, skjæringstidspunkt, opplysning, arbeidsgiverinntekter, nyeInntekter)
                        }
                        sykepengegrunnlag.path("deaktiverteArbeidsforhold").forEach { opplysning ->
                            migrerOpplysning(aktørId, skjæringstidspunktMedInntekter, vilkårsgrunnlagId, skjæringstidspunkt, opplysning, arbeidsgiverinntekter, mutableMapOf())
                        }

                        if (nyeInntekter.isNotEmpty()) {
                            logg(aktørId, "Må opprette et nytt, tidligere, innslag med originale inntekter for $skjæringstidspunkt")
                            val nyttElement = elementet.deepCopy<JsonNode>() as ObjectNode
                            nyttElement.put("vilkårsgrunnlagId", UUID.randomUUID().toString())
                            nyeInntekter.map { (orgnummer, inntektsmelding) ->
                                orgnummer to serdeObjectMapper.createObjectNode().apply {
                                    put("id", inntektsmelding.id.toString())
                                    put("dato", inntektsmelding.dato.toString())
                                    put("hendelseId", inntektsmelding.hendelseId)
                                    put("beløp", inntektsmelding.beløp)
                                    put("kilde", "INNTEKTSMELDING")
                                    put("tidsstempel", inntektsmelding.tidsstempel.toString())
                                }
                            }.forEach { (orgnummer, nyInntektsopplysning) ->
                                (nyttElement
                                    .path("sykepengegrunnlag")
                                    .path("arbeidsgiverInntektsopplysninger")
                                    .first { it.path("orgnummer").asText() == orgnummer }
                                    as ObjectNode).replace("inntektsopplysning", nyInntektsopplysning)
                            }

                            nyeElementer.add(nyttElement)
                        }
                    }

                if (nyeElementer.isNotEmpty()) {
                    logg(aktørId, "Lager nytt, eldre, innslag med ${nyeElementer.size} fiksede elementer")
                    val nyttInnslag = innslag.deepCopy<JsonNode>() as ObjectNode
                    nyttInnslag.put("id", UUID.randomUUID().toString())
                    val eksisterendeElementer = nyttInnslag.path("vilkårsgrunnlag").filterNot { elementet ->
                        val dato = elementet.path("skjæringstidspunkt").dato()
                        nyeElementer.any { nyttElement ->
                            nyttElement.path("skjæringstidspunkt").dato() == dato
                        }
                    }

                    val elementer= nyttInnslag.path("vilkårsgrunnlag") as ArrayNode
                    elementer.removeAll()
                    elementer.addAll(eksisterendeElementer + nyeElementer)
                    nyeInnslag.add(UUID.fromString(innslag.path("id").asText()) to nyttInnslag)
                }
            }

        if (nyeInnslag.isNotEmpty()) {
            logg(aktørId, "Legger til ${nyeInnslag.size} nye innslag")
            val vilkårsgrunnlagHistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode
            nyeInnslag.forEach { (forrigeInnslagId, nyttInnslag) ->
                val nåværendeInnslag = vilkårsgrunnlagHistorikk.deepCopy().map { it }.toMutableList()
                val index = nåværendeInnslag.indexOfFirst { innslag ->
                    UUID.fromString(innslag.path("id").asText()) == forrigeInnslagId
                }
                if (index == -1) {
                    logg(aktørId, "Fant ikke index til innslaget med id $forrigeInnslagId!")
                } else {
                    nåværendeInnslag.add(index + 1, nyttInnslag)
                    vilkårsgrunnlagHistorikk.removeAll()
                    vilkårsgrunnlagHistorikk.addAll(nåværendeInnslag)
                }
            }
        }
    }

    private fun migrerOpplysning(
        aktørId: String,
        skjæringstidspunktMedInntekter: MutableMap<LocalDate, MutableMap<String, MutableList<Pair<UUID, String>>>>,
        vilkårsgrunnlagId: String,
        skjæringstidspunkt: LocalDate,
        opplysning: JsonNode,
        arbeidsgiverinntekter: Map<String, Map<LocalDate, List<Inntektsmelding>>>,
        nyeInntekter: MutableMap<String, Inntektsmelding>
    ) {
        val orgnummer = opplysning.path("orgnummer").asText()
        val inntektopplysning = opplysning.path("inntektsopplysning")
        val inntektId = UUID.fromString(inntektopplysning.path("id").asText())
        val kilde = inntektopplysning.path("kilde").asText()
        val inntektdato = inntektopplysning.path("dato").dato()
        val tidsstempel = LocalDateTime.parse(inntektopplysning.path("tidsstempel").asText())

        if (kilde != "SAKSBEHANDLER") {
            skjæringstidspunktMedInntekter
                .getOrPut(skjæringstidspunkt) { mutableMapOf() }
                .getOrPut(orgnummer) { mutableListOf() }
                .add(0, inntektId to kilde)

            return
        }
        if (inntektopplysning.hasNonNull("overstyrtInntektId"))
            return logg(aktørId, "Saksbehandlerinntekt er allerede migrert for orgnr $orgnummer ved skjæringstidspunkt $skjæringstidspunkt for vilkårsgrunnlagId=$vilkårsgrunnlagId")

        val inntekter = forsøkFinneInntektHosArbeidsgiver(nyeInntekter, skjæringstidspunktMedInntekter, orgnummer, skjæringstidspunkt, inntektdato, tidsstempel, arbeidsgiverinntekter) ?: return logg(aktørId, "Har ikke inntekter for skjæringstidspunkt $skjæringstidspunkt for vilkårsgrunnlagId=$vilkårsgrunnlagId")
        val inntekterForAG = inntekter[orgnummer]
            ?: return logg(aktørId, "Har ikke inntekter for orgnr $orgnummer ved skjæringstidspunkt $skjæringstidspunkt for vilkårsgrunnlagId=$vilkårsgrunnlagId")
        val forrigeInntektId = inntekterForAG.firstOrNull()
            ?: return logg(aktørId, "Har ikke noen tidligere inntekter for orgnr $orgnummer ved skjæringstidspunkt $skjæringstidspunkt for vilkårsgrunnlagId=$vilkårsgrunnlagId")

        logg(aktørId, "Migrerer saksbehandlerinntekt til å peke på ${forrigeInntektId.first} (kilde=${forrigeInntektId.second}) for orgnr $orgnummer ved skjæringstidspunkt $skjæringstidspunkt for vilkårsgrunnlagId=$vilkårsgrunnlagId")
        (inntektopplysning as ObjectNode).put("overstyrtInntektId", forrigeInntektId.first.toString())
    }

    private fun forsøkFinneInntektHosArbeidsgiver(
        nyeInntekter: MutableMap<String, Inntektsmelding>,
        skjæringstidspunktMedInntekter: MutableMap<LocalDate, MutableMap<String, MutableList<Pair<UUID, String>>>>,
        orgnummer: String,
        skjæringstidspunkt: LocalDate,
        dato: LocalDate,
        tidsstempel: LocalDateTime,
        arbeidsgiverinntekter: Map<String, Map<LocalDate, List<Inntektsmelding>>>
    ): Map<String, List<Pair<UUID, String>>>? {
        val ret = skjæringstidspunktMedInntekter[skjæringstidspunkt]
        if (ret != null) return ret
        val inntekt = arbeidsgiverinntekter[orgnummer]?.let { arbeidsgiver ->
            arbeidsgiver[dato]?.firstOrNull { inntektsmelding -> inntektsmelding.tidsstempel < tidsstempel }
                ?: arbeidsgiver[skjæringstidspunkt]?.firstOrNull { inntektsmelding -> inntektsmelding.tidsstempel < tidsstempel }
        } ?: return null

        skjæringstidspunktMedInntekter
            .getOrPut(skjæringstidspunkt) { mutableMapOf() }
            .getOrPut(orgnummer) { mutableListOf() }
            .add(0, inntekt.id to "INNTEKTSMELDING")

        nyeInntekter[orgnummer] = inntekt

        return skjæringstidspunktMedInntekter[skjæringstidspunkt]
    }

    private fun logg(aktørId: String, melding: String) {
        sikkerlogg.info("[V239] {} $melding", keyValue("aktørId", aktørId))
    }
    private fun JsonNode.dato() = LocalDate.parse(asText())
    private class Inntektsmelding(
        val id: UUID,
        val dato: LocalDate,
        val hendelseId: String,
        val beløp: Double,
        val tidsstempel: LocalDateTime
    )
}