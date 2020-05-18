package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V14NettoBeløpIVedtaksperiode : JsonMigration(version = 14) {
    override val description = "Legger til netto beløp i vedtaksperiode"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                periode as ObjectNode
                periode.put("personNettoBeløp", 0)
                periode.put("arbeidsgiverNettoBeløp", 0)
            }
        }
    }
}
