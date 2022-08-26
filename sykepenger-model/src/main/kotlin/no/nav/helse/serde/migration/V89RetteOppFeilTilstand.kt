package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V89RetteOppFeilTilstand : JsonMigration(version = 89) {
    override val description: String = "Setter riktig tilstand etter en liten bug"

    private val vedtaksperiodeIder = listOf(
        "2d194cb6-a208-4e97-b45a-81de8ed680c4",
        "c400ce20-3568-4813-a488-83e6865f36d0",
        "b30e6003-f4df-4cb0-b08d-ea24008600a1",
    )

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").firstOrNull {
                vedtaksperiodeIder.contains(it.path("id").asText())
            }?.let {
                check(
                    it["tilstand"].asText() == "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE"
                ) { "Vedtaksperioden har blitt behandlet etter at den fikk feil tilstand, trenger oppfølging" }
                (it as ObjectNode).put("tilstand", "MOTTATT_SYKMELDING_UFERDIG_GAP")
            }
        }
    }
}
