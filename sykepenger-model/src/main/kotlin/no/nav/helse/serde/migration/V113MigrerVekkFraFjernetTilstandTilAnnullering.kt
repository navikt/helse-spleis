package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V113MigrerVekkFraFjernetTilstandTilAnnullering : JsonMigration(version = 113) {
    override val description: String = "Det ligger igjen 3 vedtaksperioder med status til_annullering etter at tilstanden ble fjernet"

    val vedtaksperiodeIder = listOf(
        "39cee3ad-3fb3-4a53-a980-0f1cdfb5e5ec",
        "ccecefca-20a6-40d2-a2e5-7c4971fa8243",
        "c68f4dfc-39f4-41aa-adec-4851f14b9d9c"
    )

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere")
            .flatMap { arbeidsgiver -> arbeidsgiver.path("vedtaksperioder") }
            .filter { it["tilstand"]?.asText() == "TIL_ANNULLERING" }
            .filter { vedtaksperiodeIder.contains(it["id"]?.asText()) }
            .forEach { utbetaling ->
                utbetaling as ObjectNode
                utbetaling.put("tilstand", "AVSLUTTET")
            }
    }

}
