package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V136MigrereTilstanderPåForkastede : JsonMigration(version = 136) {
    private companion object {
        private val gamle = setOf(
            "TIL_ANNULLERING",
            "AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING",
            "UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_GAP",
            "UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_FORLENGELSE",
            "AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP",
            "AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP",
            "AVVENTER_VILKÅRSPRØVING_GAP",
            "AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD",
            "AVVENTER_UFERDIG_GAP",
            "AVVENTER_UFERDIG_FORLENGELSE",
            "AVVENTER_UTBETALINGSGRUNNLAG",
            "AVVENTER_HISTORIKK_REVURDERING",
            "AVVENTER_GAP"
        )
    }

    override val description = "Migrerer tilstander på forkastede perioder"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("forkastede").forEach { forkasting ->
                migrer(forkasting.path("vedtaksperiode") as ObjectNode)
            }
        }
    }

    private fun migrer(vedtaksperiode: ObjectNode) {
        if (vedtaksperiode.path("tilstand").asText() !in gamle) return
        vedtaksperiode.put("tilstand", "TIL_INFOTRYGD")
    }
}
