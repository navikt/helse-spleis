package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V110SletteArbeidsforhold() : JsonMigration(version = 110) {
    override val description: String = "Slette arbeidsforhold som kan ha blitt lagret på feil orgnummer"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .forEach { (it as ObjectNode).putArray("arbeidsforholdhistorikk") }
    }
}
