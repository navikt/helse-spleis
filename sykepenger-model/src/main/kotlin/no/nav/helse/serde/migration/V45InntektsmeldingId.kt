package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.sykdomstidslinje.erRettFør
import java.time.LocalDate
import java.util.*

internal class V45InntektsmeldingId : JsonMigration(version = 45) {
    override val description: String = "setter inntektsmeldingId på tidligere førstegangsbehandlinger, og deres forlengelser"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            settInntektsmeldingerId(arbeidsgiver.path("vedtaksperioder"))
        }
    }

    private fun settInntektsmeldingerId(perioder: JsonNode) {
        if (perioder.size() == 1) {
            sjekkFørstegangsperiode(perioder.first())
            return
        }
        perioder.zipWithNext { periodeFør, periodeEtter ->
            periodeFør as ObjectNode

            val inntektsmeldingId = sjekkFørstegangsperiode(periodeFør)

            if (!periodeEtter.hasNonNull("inntektsmeldingId") && inntektsmeldingId != null && periodeEtter.erPotensiellForlengelse(periodeFør)) {
                settInntektsmeldingId(periodeEtter, inntektsmeldingId)
            }
        }
    }

    private fun sjekkFørstegangsperiode(jsonNode: JsonNode): UUID? {
        var inntektsmeldingId = jsonNode.path("inntektsmeldingId").takeIf(JsonNode::isTextual)?.let { UUID.fromString(it.textValue()) }
        if (inntektsmeldingId == null && jsonNode.erPotensiellFørstegangsbehandling()) {
            inntektsmeldingId = jsonNode.finnInntektsmeldingId()?.also { id ->
                settInntektsmeldingId(jsonNode, id)
            }
        }
        return inntektsmeldingId
    }

    private fun settInntektsmeldingId(jsonNode: JsonNode, id: UUID) {
        jsonNode as ObjectNode
        jsonNode.put("inntektsmeldingId", "$id")

        val hendelseIder = jsonNode.withArray("hendelseIder")
        if (hendelseIder.none { UUID.fromString(it.asText()) == id })
            hendelseIder.add("$id")
    }

    private fun JsonNode.finnInntektsmeldingId() =
        path("sykdomshistorikk").firstOrNull { hendelse ->
            hendelse.path("hendelseSykdomstidslinje").path("dager").any { dag ->
                dag.path("kilde").path("type").asText() == "Inntektsmelding"
            }
        }?.path("hendelseId")?.let { UUID.fromString(it.asText()) }

    private fun JsonNode.erPotensiellFørstegangsbehandling() =
        !erInfotrygdforlengelse() && path("hendelseIder").size() == 3

    private fun JsonNode.erPotensiellForlengelse(periodeFør: JsonNode) =
            !erInfotrygdforlengelse() && periodeFør.path("tom").asLocalDate().erRettFør(this.path("fom").asLocalDate())

    private fun JsonNode.erInfotrygdforlengelse() = path("forlengelseFraInfotrygd").asText() == "JA"
    private fun JsonNode.asLocalDate(): LocalDate =
        asText().let { LocalDate.parse(it) }
}
