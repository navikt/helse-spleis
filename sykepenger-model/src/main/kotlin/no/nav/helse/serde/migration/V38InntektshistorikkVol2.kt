package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate

internal class V38InntektshistorikkVol2 : JsonMigration(version = 38) {
    override val description: String = "Andre forsøk på å migrere inntektshistorikk. Må shifte FOM med en dag for å " +
        "korrigere for at vi tidligere har brukt feil dato"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            (arbeidsgiver as ObjectNode).withArray("inntektshistorikk").removeAll()

            val inntekter =
                arbeidsgiver.path("inntekter")
                    .sortedByDescending { it["tidsstempel"].asText() }
                    .map { inntekt ->
                        inntekt.deepCopy<ObjectNode>()
                            .apply {
                                put("dato", LocalDate.parse(inntekt["fom"].asText()).plusDays(1).toString())
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
