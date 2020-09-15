package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V37InntektshistorikkVol2 : JsonMigration(version = 37) {
    override val description: String = "Vi migrerer inntektshistorikk fra v1 -> v2"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            (arbeidsgiver as ObjectNode).withArray("inntektshistorikk").removeAll()

            val inntekter =
                arbeidsgiver.path("inntekter")
                    .sortedByDescending { it["tidsstempel"].asText() }
                    .map { inntekt ->
                        inntekt.deepCopy<ObjectNode>()
                            .apply {
                                set<ObjectNode>("dato", inntekt["fom"])
                                remove("fom")
                            }
                    }

            inntekter.fold(inntekter) { acc, _ ->
                arbeidsgiver.withArray("inntektshistorikk")
                    .add(JsonNodeFactory.instance.objectNode()
                        .also { innslag ->
                            innslag.withArray("inntektsopplysninger")
                                .addAll(acc.sortedBy { it["tidsstempel"].asText() })
                        })
                acc.drop(1)
            }
        }
    }
}
