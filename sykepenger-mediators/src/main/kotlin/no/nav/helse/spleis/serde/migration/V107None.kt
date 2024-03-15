package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V107None() : JsonMigration(version = 107) {
    override val description: String = "Slette arbeidsforhold som kan ha blitt lagret på feil orgnummer"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {}
}
