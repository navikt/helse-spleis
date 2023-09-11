package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V266FerieUtenSykmeldingTilArbeidIkkeGjenopptatt : JsonMigration(version = 266) {

    override val description = "Endre navn på dagtype FerieUtenSykmelding til ArbeidIkkeGjenopptatt"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            arbeidsgiver.path("sykdomshistorikk").renameFerieUtenSykmeldingTilArbeidIkkeGjenopptatt()
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                vedtaksperiode.path("sykdomstidslinje").renameFerieUtenSykmeldingTilArbeidIkkeGjenopptatt()
                vedtaksperiode.path("utbetalinger").renameFerieUtenSykmeldingTilArbeidIkkeGjenopptatt()
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                forkastet.path("vedtaksperiode").path("sykdomstidslinje").renameFerieUtenSykmeldingTilArbeidIkkeGjenopptatt()
                forkastet.path("vedtaksperiode").path("utbetalinger").renameFerieUtenSykmeldingTilArbeidIkkeGjenopptatt()
            }
        }
    }

    private companion object {
        private fun JsonNode.renameFerieUtenSykmeldingTilArbeidIkkeGjenopptatt() {
            when (this) {
                is ObjectNode -> fields().forEach { (key, value) ->
                    if (value.isTextual && value.asText() == "FERIE_UTEN_SYKMELDINGDAG") put(key, "ARBEID_IKKE_GJENOPPTATT_DAG")
                    else value.renameFerieUtenSykmeldingTilArbeidIkkeGjenopptatt()
                }
                is ArrayNode -> forEach { it.renameFerieUtenSykmeldingTilArbeidIkkeGjenopptatt() }
            }
        }
    }
}