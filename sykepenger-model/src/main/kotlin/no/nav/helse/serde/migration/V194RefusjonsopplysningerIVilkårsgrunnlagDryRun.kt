package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V194RefusjonsopplysningerIVilkårsgrunnlagDryRun: JsonMigration(version = 194) {
    override val description = "[utført]"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {}
}