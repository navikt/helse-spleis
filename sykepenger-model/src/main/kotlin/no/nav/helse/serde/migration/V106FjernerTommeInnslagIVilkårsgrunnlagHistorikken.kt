package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.AktivitetsloggObserver

internal class V106FjernerTommeInnslagIVilkårsgrunnlagHistorikken : JsonMigration(version = 106) {
    override val description: String = "Fjerner innslag uten vilkårsgrunnlag som ble opprettet feilaktig av tidligere migrering (105)"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        if (jsonNode["vilkårsgrunnlagHistorikk"] == null) return
        jsonNode.withArray("vilkårsgrunnlagHistorikk")
            .removeAll { innslag ->
                innslag.withArray<JsonNode?>("vilkårsgrunnlag").isEmpty
            }
    }

}
