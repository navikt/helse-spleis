package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class JsonVisitableTest {

    @Test
    fun testJsonVisitable() {
        val json = JsonVisitable(enkelTidslinjeJson())
        val rec = mutableListOf<Pair<String, String>>()
        json.accept(Gjestebok(rec))
        assertEquals(listOf(
            "object" to "START",
            "arrayField" to "tidslinje",
            "object" to "START",
            "string" to "type=FERIEDAG",
            "string" to "dato=2020-01-01",
            "object" to "END",
            "arrayField" to "END",
            "object" to "END"
            ), rec)
    }

    private fun enkelTidslinjeJson() =
        jacksonObjectMapper().convertValue(
            mapOf( "tidslinje" to listOf( mapOf(
                "type" to "FERIEDAG",
                "dato" to "2020-01-01"
            ))), JsonNode::class.java)
}

private class Gjestebok(private val rec : MutableList<Pair<String, String>>) : StructureVisitor {
    override fun preVisitArrayField(name: String) {
        rec += "arrayField" to name
    }
    override fun postVisitArrayField() {
        rec += "arrayField" to "END"
    }
    override fun preVisitObjectField(name: String) {
        rec += "objectField" to name
    }
    override fun preVisitObject() {
        rec += "object" to "START"
    }
    override fun postVisitObject() {
        rec += "object" to "END"
    }
    override fun visitStringField(name: String, value: String) {
        rec += "string" to "$name=$value"
    }
}

