package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V322YrkesaktivitetstypePåArbeidsgiver : JsonMigration(322) {
    override val description = "Setter yrkesaktivitetstype på arbeidsgiver til ARBEIDSTAKER for alle arbeidsgivere"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            (arbeidsgiver as ObjectNode).put("yrkesaktivitetstype", arbeidsgiver.finnYrkesaktivitetstype())
        }
    }

    private fun ObjectNode.finnYrkesaktivitetstype(): String = when (this["organisasjonsnummer"].asText().uppercase()) {
        "SELVSTENDIG" -> "SELVSTENDIG"
        "ARBEIDSLEDIG" -> "ARBEIDSLEDIG"
        "FRILANS" -> "FRILANS"
        else -> "ARBEIDSTAKER"
    }
}
