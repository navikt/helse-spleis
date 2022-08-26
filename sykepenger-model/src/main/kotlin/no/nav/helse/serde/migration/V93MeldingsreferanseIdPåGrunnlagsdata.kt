package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V93MeldingsreferanseIdPåGrunnlagsdata : JsonMigration(version = 93) {
    override val description: String =
        "Legger til meldingsreferanseId på grunnlagsdata. Bruker hendelsesId-en som er lagret på sammenligningsgrunnlaget for samme skjæringstidspunkt."

    private val log = LoggerFactory.getLogger("tjenestekall")

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("vilkårsgrunnlagHistorikk")
            .forEach { vilkårsgrunnlag ->
                if (vilkårsgrunnlag["type"].asText() != "Vilkårsprøving") return@forEach
                val skjæringstidspunkt = vilkårsgrunnlag.path("skjæringstidspunkt").asText()
                var sammenligningsgrunnlag: JsonNode? = null
                run loop@{
                    jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
                        sammenligningsgrunnlag = arbeidsgiver.path("inntektshistorikk").firstOrNull()?.path("inntektsopplysninger")
                            ?.filterNot { it.path("skatteopplysninger").isMissingNode }
                            ?.flatMap { it.path("skatteopplysninger") }
                            ?.find { it["kilde"].asText() == "SKATT_SAMMENLIGNINGSGRUNNLAG" && it["dato"].asText() == skjæringstidspunkt }
                        if (sammenligningsgrunnlag != null) return@loop
                    }
                }
                if (sammenligningsgrunnlag == null) log.info("Meldingsreferanse på vilkårsgrunnlag er null. AktørId: ${jsonNode["aktørId"].asText()}")
                (vilkårsgrunnlag as ObjectNode).set<JsonNode>("meldingsreferanseId", sammenligningsgrunnlag?.path("hendelseId"))
            }
    }
}
