package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.ForkastetÅrsak

internal class V35ÅrsakTilForkasting : JsonMigration(version = 35) {
    override val description: String = "Legger til årsak-felt i forkastede-listen"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        for (arbeidsgiver in jsonNode["arbeidsgivere"]) {
            arbeidsgiver as ObjectNode
            arbeidsgiver.replace("forkastede", leggTilÅrsak(arbeidsgiver["forkastede"]))
        }
    }

    private fun leggTilÅrsak(perioder: JsonNode) =
        perioder.map { periode ->
            jacksonObjectMapper().createObjectNode().apply {
                put("årsak", ForkastetÅrsak.UKJENT.name)
                replace("vedtaksperiode", periode)
            }
        }.let { jacksonObjectMapper().convertValue(it, ArrayNode::class.java) }
}
