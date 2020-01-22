package no.nav.helse.serde

internal interface ObjectVisitor {
    fun preVisitArrayField(name: String) {}
    fun postVisitArrayField() {}
    fun preVisitObjectField(name: String) {}
    fun postVisitObjectField() {}
    fun visitStringField(name: String, value: String) {}
    fun visitBooleanField(name: String, value: Boolean) {}
    fun visitNumberField(name: String, value: Number) {}
}

internal interface ArrayVisitor {
    fun preVisitArray() {}
    fun postVisitArray() {}
    fun preVisitObject() {}
    fun postVisitObject() {}
    fun visitString(value: String) {}
    fun visitBoolean(value: Boolean) {}
    fun visitNumber(value: Number) {}
}

internal interface StructureVisitor : ObjectVisitor, ArrayVisitor
