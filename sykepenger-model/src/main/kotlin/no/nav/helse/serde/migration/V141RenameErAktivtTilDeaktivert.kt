package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V141RenameErAktivtTilDeaktivert : JsonMigration(version = 141) {
    override val description: String = "Endrer navn på erAktivt til deaktivert i arbeidsforholdhistorikken"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .flatMap { it["arbeidsforholdhistorikk"] }
            .flatMap { it["arbeidsforhold"] }
            .map { it as ObjectNode }
            .forEach {
                it.put("deaktivert", !it.remove("erAktivt").asBoolean())
            }
    }
}
