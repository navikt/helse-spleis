package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode

internal class JsonVisitable(private val json: JsonNode) {

    internal fun accept(visitor: StructureVisitor) {
        accept(visitor, json)
    }

    private fun accept(visitor: StructureVisitor, json: JsonNode) {
        when {
            json.isArray -> {
                visitor.preVisitArray()
                json.forEach {
                    accept(visitor, it)
                }
                visitor.postVisitArray()
            }
            json.isObject -> {
                visitor.preVisitObject()
                json.fieldNames().forEach { name ->
                    accept(visitor, name, json[name])
                }
                visitor.postVisitObject()
            }
            json.isTextual -> visitor.visitString(json.textValue())
            json.isNumber -> visitor.visitNumber(json.numberValue())
            json.isBoolean -> visitor.visitBoolean(json.booleanValue())
            else -> error("Håndterer ikke ${json.nodeType}")
        }
    }

    private fun accept(visitor: StructureVisitor, name: String, json: JsonNode) {
        when {
            json.isArray -> {
                visitor.preVisitArrayField(name)
                json.forEach {
                    accept(visitor, it)
                }
                visitor.postVisitArrayField()
            }
            json.isObject -> {
                visitor.preVisitObjectField(name)
                json.fieldNames().forEach { fieldName ->
                    accept(visitor, fieldName, json[fieldName])
                }
                visitor.postVisitObjectField()
            }
            json.isTextual -> visitor.visitStringField(name, json.textValue())
            json.isNumber -> visitor.visitNumberField(name, json.numberValue())
            json.isBoolean -> visitor.visitBooleanField(name, json.booleanValue())
            else -> error("Håndterer ikke ${json.nodeType}")
        }
    }

}
