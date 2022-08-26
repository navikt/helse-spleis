package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper

internal class V95ArbeidsforholdId : JsonMigration(version = 95) {
    override val description = "Flytter inntektsmeldingId og legger til arbeidsforholdId inn i InntektsmeldingInfo"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val arbeidsgivere = jsonNode["arbeidsgivere"]
        arbeidsgivere.flatMap { it["vedtaksperioder"] }.flyttInntektsmeldingId()
        arbeidsgivere.flatMap { it["forkastede"] }.map { it["vedtaksperiode"] }.flyttInntektsmeldingId()
    }

    fun Iterable<JsonNode>.flyttInntektsmeldingId() = forEach {
        it as ObjectNode
        val id = it.remove("inntektsmeldingId")
        if (!id.isNull) {
            it.putObject("inntektsmeldingInfo")
                .set<ObjectNode>("id", id)
                .set<ObjectNode>("arbeidsforholdId", serdeObjectMapper.nullNode())
        }
    }
}
