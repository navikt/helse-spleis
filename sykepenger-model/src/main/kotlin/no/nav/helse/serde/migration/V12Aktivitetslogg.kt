package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.helse.serde.serdeObjectMapper

internal class V12Aktivitetslogg : JsonMigration(version = 12) {

    override val description = "Lager en liste av kontekster på aktivitetslogg-nivå"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktivitetslogg = jsonNode["aktivitetslogg"] as ObjectNode
        val kontekster = mutableSetOf<JsonNode>()
        val aktiviteter = aktivitetslogg["aktiviteter"].onEach { aktivitet ->
            kontekster.addAll(aktivitet["kontekster"])
        }
        val kontekstList = kontekster.toList()

        aktivitetslogg.set<ObjectNode>(
            "kontekster",
            serdeObjectMapper.convertValue<ArrayNode>(kontekstList)
        )

        aktiviteter.forEach { aktivitet ->
            aktivitet as ObjectNode
            val indices = aktivitet["kontekster"].map { kontekstList.indexOf(it) }
            val array = serdeObjectMapper.convertValue<ArrayNode>(indices)
            aktivitet.set<ObjectNode>("kontekster", array)
        }
    }
}
