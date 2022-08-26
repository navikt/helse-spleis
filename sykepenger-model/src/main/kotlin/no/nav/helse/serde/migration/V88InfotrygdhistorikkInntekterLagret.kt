package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime

internal class V88InfotrygdhistorikkInntekterLagret : JsonMigration(version = 88) {
    override val description: String = "Infotrygdhistorikk med statslønn"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val hendelseIder = jsonNode.path("arbeidsgivere").flatMap { arbeidsgiver ->
            arbeidsgiver.path("inntektshistorikk").flatMap { innslag ->
                innslag.path("inntektsopplysninger").filter { opplysning ->
                    opplysning.hasNonNull("hendelseId")
                }.map { opplysning ->
                    opplysning.path("hendelseId").asText() to LocalDateTime.parse(opplysning.path("tidsstempel").asText())
                }
            }
        }.toMap()

        jsonNode.path("infotrygdhistorikk").forEach { historikkelement ->
            val id = historikkelement.path("id").asText()
            val tidsstempel = hendelseIder[id]
            (historikkelement as ObjectNode).put("lagretVilkårsgrunnlag", tidsstempel != null)
            historikkelement.put("lagretInntekter", tidsstempel != null)

            if (tidsstempel != null) {
                historikkelement.path("inntekter").forEach { inntekt ->
                    (inntekt as ObjectNode).put("lagret", "$tidsstempel")
                }
            }
        }
    }
}
