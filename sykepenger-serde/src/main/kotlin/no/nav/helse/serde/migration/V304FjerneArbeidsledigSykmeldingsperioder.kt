package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V304FjerneArbeidsledigSykmeldingsperioder: JsonMigration(version = 304) {
    override val description = "lagrer egenmeldingsdager på vedtaksperiode"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere")
            .singleOrNull { it.path("organisasjonsnummer").asText() == "ARBEIDSLEDIG" }
            ?.let { arbeidsledig ->
                arbeidsledig as ObjectNode
                arbeidsledig.putArray("sykmeldingsperioder")
            }
    }
}