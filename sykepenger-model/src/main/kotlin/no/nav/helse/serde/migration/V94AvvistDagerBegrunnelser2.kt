package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.helse.serde.serdeObjectMapper

internal class V94AvvistDagerBegrunnelser2  : JsonMigration(version = 94) {
    override val description: String = "Flytter 'begrunnelse' på AvvistDag-er på beregnetUtbetalingstidslinjer til 'begrunnelser' som er en liste"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere")
            .forEach { arbeidsgiver ->
                arbeidsgiver.path("beregnetUtbetalingstidslinjer")
                    .forEach { beregnetUtbetalingstidslinje ->
                        beregnetUtbetalingstidslinje.path("utbetalingstidslinje").path("dager")
                            .forEach {
                                if (it["type"].asText() == "AvvistDag") {
                                    val begrunnelser = listOf((it as ObjectNode).remove("begrunnelse"))
                                    it.set<ObjectNode>("begrunnelser", serdeObjectMapper.convertValue<ArrayNode>(begrunnelser))
                                }
                            }
                    }
                arbeidsgiver.path("utbetalinger")
                    .forEach { utbetaling ->
                        utbetaling.path("utbetalingstidslinje").path("dager")
                            .forEach {
                                if (it["type"].asText() == "AvvistDag") {
                                    val begrunnelser = listOf((it as ObjectNode).remove("begrunnelse"))
                                    it.set<ObjectNode>("begrunnelser", serdeObjectMapper.convertValue<ArrayNode>(begrunnelser))
                                }
                            }
                    }
            }
    }
}
