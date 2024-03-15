package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V29LeggerTilInntektsKildeType : JsonMigration(version = 29) {
    override val description: String = "Legger til type felt i inntektskilden for å kunne differensiere mellom dem."

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("inntekter").forEach { inntekt ->
                inntekt as ObjectNode
                inntekt.put("kilde", "INNTEKTSMELDING")
            }
        }
    }
}
