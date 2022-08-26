package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V86ForkastetPeriodeIFjernetState : JsonMigration(version = 86) {
    override val description: String = "Flytter forkastede vedtaksperioder som er i AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD til AVSLUTTET_UTEN_UTBETALING"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .flatMap { arbeidsgiver ->
                arbeidsgiver["forkastede"].map { it["vedtaksperiode"] }
            }
            .filter { it["tilstand"].asText() == "AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD" }
            .forEach { (it as ObjectNode).put("tilstand", "AVSLUTTET_UTEN_UTBETALING") }
    }
}
