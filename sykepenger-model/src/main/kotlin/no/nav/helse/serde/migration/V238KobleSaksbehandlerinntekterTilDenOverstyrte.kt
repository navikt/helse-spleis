package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.LoggerFactory

internal class V238KobleSaksbehandlerinntekterTilDenOverstyrte: JsonMigration(238) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "kobler saksbehandlerinntektene til det den overstyrer"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val skjæringstidspunktMedInntekter = mutableMapOf<LocalDate, MutableMap<String, MutableList<Pair<UUID, String>>>>()
        val aktørId = jsonNode.path("aktørId").asText()

        jsonNode.path("vilkårsgrunnlagHistorikk")
            .reversed()
            .forEach { innslag ->
                innslag.path("vilkårsgrunnlag")
                    .filter { elementet -> elementet.path("type").asText() == "Vilkårsprøving" }
                    .forEach { elementet ->
                        val vilkårsgrunnlagId = elementet.path("vilkårsgrunnlagId").asText()
                        val skjæringstidspunkt = elementet.path("skjæringstidspunkt").dato()
                        val sykepengegrunnlag = elementet.path("sykepengegrunnlag")

                        sykepengegrunnlag.path("arbeidsgiverInntektsopplysninger").forEach { opplysning ->
                            migrerOpplysning(aktørId, skjæringstidspunktMedInntekter, vilkårsgrunnlagId, skjæringstidspunkt, opplysning)
                        }
                        sykepengegrunnlag.path("deaktiverteArbeidsforhold").forEach { opplysning ->
                            migrerOpplysning(aktørId, skjæringstidspunktMedInntekter, vilkårsgrunnlagId, skjæringstidspunkt, opplysning)
                        }
                    }
            }
    }

    private fun migrerOpplysning(
        aktørId: String,
        skjæringstidspunktMedInntekter: MutableMap<LocalDate, MutableMap<String, MutableList<Pair<UUID, String>>>>,
        vilkårsgrunnlagId: String,
        skjæringstidspunkt: LocalDate,
        opplysning: JsonNode
    ) {
        val orgnummer = opplysning.path("orgnummer").asText()
        val inntektopplysning = opplysning.path("inntektsopplysning")
        val inntektId = UUID.fromString(inntektopplysning.path("id").asText())
        val kilde = inntektopplysning.path("kilde").asText()

        if (kilde != "SAKSBEHANDLER") {
            skjæringstidspunktMedInntekter
                .getOrPut(skjæringstidspunkt) { mutableMapOf() }
                .getOrPut(orgnummer) { mutableListOf() }
                .add(0, inntektId to kilde)

            return
        }
        if (inntektopplysning.hasNonNull("overstyrtInntektId"))
            return logg(aktørId, "Saksbehandlerinntekt er allerede migrert for orgnr $orgnummer ved skjæringstidspunkt $skjæringstidspunkt for vilkårsgrunnlagId=$vilkårsgrunnlagId")

        val inntekter = skjæringstidspunktMedInntekter[skjæringstidspunkt]
            ?: return logg(aktørId, "Har ikke inntekter for skjæringstidspunkt $skjæringstidspunkt for vilkårsgrunnlagId=$vilkårsgrunnlagId")
        val inntekterForAG = inntekter[orgnummer]
            ?: return logg(aktørId, "Har ikke inntekter for orgnr $orgnummer ved skjæringstidspunkt $skjæringstidspunkt for vilkårsgrunnlagId=$vilkårsgrunnlagId")
        val forrigeInntektId = inntekterForAG.firstOrNull()
            ?: return logg(aktørId, "Har ikke noen tidligere inntekter for orgnr $orgnummer ved skjæringstidspunkt $skjæringstidspunkt for vilkårsgrunnlagId=$vilkårsgrunnlagId")

        logg(aktørId, "Migrerer saksbehandlerinntekt til å peke på ${forrigeInntektId.first} (kilde=${forrigeInntektId.second}) for orgnr $orgnummer ved skjæringstidspunkt $skjæringstidspunkt for vilkårsgrunnlagId=$vilkårsgrunnlagId")
        (inntektopplysning as ObjectNode).put("overstyrtInntektId", forrigeInntektId.first.toString())
    }

    private fun logg(aktørId: String, melding: String) {
        sikkerlogg.info("[V238] {} $melding", keyValue("aktørId", aktørId))
    }
    private fun JsonNode.dato() = LocalDate.parse(asText())
}