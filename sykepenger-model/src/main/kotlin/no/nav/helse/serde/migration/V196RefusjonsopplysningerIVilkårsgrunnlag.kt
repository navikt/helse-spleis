package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V196RefusjonsopplysningerIVilkårsgrunnlag: JsonMigration(version = 196) {
    override val description = "[utført]"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {}
}