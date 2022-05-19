package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V158SletterDuplikaterFraInntektshistorikken : JsonMigration(version = 158) {
    override val description = "Spisset migrering for person som gikk i loop og dupliserte innteksthistorikken sin"


    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val vedtaksperiodeId = "9354c0c8-ac43-4c34-b094-aa64fd94be5e"
        val inntektsopplysningId = "a9f09978-cb29-48d4-929f-0e8b6e81de1a"

        val relevantOrgummer = jsonNode.finnOrgnummer(vedtaksperiodeId)?.asText() ?: return
        val arbeidsgiver = jsonNode.finnArbeidsgiver(relevantOrgummer) ?: return
        val inntektsopplysning = arbeidsgiver.finnInntektsopplysning(inntektsopplysningId) ?: return

        arbeidsgiver["inntektshistorikk"].removeAll {
            inntektsopplysning["id"] != it["id"] && inntektsopplysning["inntektsopplysninger"] == it["inntektsopplysninger"]
        }
    }

    private fun ObjectNode.finnOrgnummer(vedtaksperiodeId: String) = this["arbeidsgivere"].firstOrNull { arbeidsgiver ->
        arbeidsgiver["vedtaksperioder"].any { it["id"].asText() == vedtaksperiodeId }
            || arbeidsgiver["forkastede"].map { it["vedtaksperiode"] }.any { it["id"].asText() in vedtaksperiodeId }
    }?.get("organisasjonsnummer")

    private fun ObjectNode.finnArbeidsgiver(orgnummer: String) = this["arbeidsgivere"]
        .firstOrNull { arbeidsgiver -> arbeidsgiver["organisasjonsnummer"].asText() == orgnummer }

    private fun JsonNode.finnInntektsopplysning(inntektsopplysningId: String) = this["inntektshistorikk"].firstOrNull { it["id"].asText() == inntektsopplysningId }
}