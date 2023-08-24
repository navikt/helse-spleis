package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import no.nav.helse.forrigeDag

internal class V257FikseOpptjeningsperiode : JsonMigration(version = 257) {
    override val description = "justerer opptjeningsperioden til å starte dagen før skjæringstidspunktet"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { innslag ->
            innslag.path("vilkårsgrunnlag").forEach { element ->
                val skjæringstidspunkt = LocalDate.parse(element.path("skjæringstidspunkt").asText())
                val opptjening = element.path("opptjening")
                if (opptjening is ObjectNode) {
                    val fom = LocalDate.parse(opptjening.path("opptjeningFom").asText())
                    val forrigeDag = skjæringstidspunkt.forrigeDag
                    opptjening.put("opptjeningFom", minOf(fom, forrigeDag).toString())
                    opptjening.put("opptjeningTom", forrigeDag.toString())
                }
            }
        }
    }
}