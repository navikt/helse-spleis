package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
private typealias VedtaksperiodeID = String
private typealias InntektsoppsysningsID = String
internal class V165SletterDuplikaterFraInntektshistorikken : JsonMigration(version = 165) {
    override val description = "Spisset migrering for person som gikk i loop og dupliserte innteksthistorikken sin"

    private val tilfeller: Map<VedtaksperiodeID, InntektsoppsysningsID> = mapOf(
        "9354c0c8-ac43-4c34-b094-aa64fd94be5e" to "a9f09978-cb29-48d4-929f-0e8b6e81de1a",
        "dd4e8eec-7b6e-4f01-9d49-73310b4916ce" to "fc1823d4-0764-4900-a728-aee6da161e33"
    )

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        tilfeller.forEach { (vedtaksperiodeID, inntektsopplysningID) -> migrerEttTilfelle(vedtaksperiodeID, inntektsopplysningID, jsonNode) }
    }

    private fun migrerEttTilfelle(vedtaksperiodeID: VedtaksperiodeID, inntektsoppsysningsID: InntektsoppsysningsID, jsonNode: ObjectNode) {
        val relevantOrgummer = jsonNode.finnOrgnummer(vedtaksperiodeID)?.asText() ?: return
        val arbeidsgiver = jsonNode.finnArbeidsgiver(relevantOrgummer) ?: return
        val inntektsopplysning = arbeidsgiver.finnInntektsopplysning(inntektsoppsysningsID) ?: return

        arbeidsgiver["inntektshistorikk"].removeAll {
            inntektsopplysning["id"] != it["id"] && inntektsopplysning["inntektsopplysninger"] == it["inntektsopplysninger"]
        }
    }

    private fun ObjectNode.finnOrgnummer(vedtaksperiodeId: VedtaksperiodeID) = this["arbeidsgivere"].firstOrNull { arbeidsgiver: JsonNode ->
        arbeidsgiver.harVedtaksperiode(vedtaksperiodeId) || arbeidsgiver.harForkastetVedtaksperiode(vedtaksperiodeId)?:false
    }?.get("organisasjonsnummer")

    private fun ObjectNode.finnArbeidsgiver(orgnummer: String) = this["arbeidsgivere"]
        .firstOrNull { arbeidsgiver -> arbeidsgiver["organisasjonsnummer"].asText() == orgnummer }

    private fun JsonNode.finnInntektsopplysning(inntektsopplysningId: String) = this["inntektshistorikk"].firstOrNull { it["id"].asText() == inntektsopplysningId }
    private fun JsonNode.harVedtaksperiode(vedtaksperiodeId: VedtaksperiodeID) = this["vedtaksperioder"].any { it["id"].asText() == vedtaksperiodeId }
    private fun JsonNode.harForkastetVedtaksperiode(vedtaksperiodeId: VedtaksperiodeID) = this["forkastede"]?.map { it["vedtaksperiode"] }?.any { it["id"].asText() in vedtaksperiodeId }
}