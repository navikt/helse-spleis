package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.helse.serde.serdeObjectMapper

internal class V90AvvistDagerBegrunnelser : JsonMigration(version = 90) {
    override val description: String = "Flytter 'begrunnelse' på AvvistDag-er til 'begrunnelser' som er en liste"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere")
            .forEach { arbeidsgiver ->
                arbeidsgiver.path("vedtaksperioder")
                    .forEach { vedtaksperiode ->
                        vedtaksperiode.path("utbetalingstidslinje").path("dager")
                            .forEach {
                                if (it["type"].asText() == "AvvistDag") {
                                    val begrunnelser = listOf((it as ObjectNode).remove("begrunnelse"))
                                    it.set<ObjectNode>("begrunnelser", serdeObjectMapper.convertValue<ArrayNode>(begrunnelser))
                                }
                            }
                    }
                arbeidsgiver.path("forkastede")
                    .forEach { forkastet ->
                        forkastet.path("vedtaksperiode").path("utbetalingstidslinje").path("dager")
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
