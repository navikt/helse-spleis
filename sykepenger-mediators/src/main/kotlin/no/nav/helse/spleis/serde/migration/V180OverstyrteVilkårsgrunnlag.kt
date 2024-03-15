package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V180OverstyrteVilkårsgrunnlag: JsonMigration(180) {
    override val description = "Sporer opp opprinnelig deaktiverte inntekter"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {}
}