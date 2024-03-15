package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V209SpissetVilkårsgrunnlagKopiMedAnnetSkjæringstidspunkt : JsonMigration(version = 209) {
    override val description = "[utført]"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {}
}