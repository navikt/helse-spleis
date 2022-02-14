package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import java.util.*

internal class V143DummyMigrerHendelseInnPåVedtaksperiode : JsonMigration(version = 143) {
    override val description: String = "Legger til typer på hendelses-ider i vedtaksperioden"

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val meldinger: Map<UUID, Pair<Navn, Json>> = meldingerSupplier.hentMeldinger()
        jsonNode["arbeidsgivere"]
            .flatMap { it["vedtaksperioder"] }
            .migrerInnHendelseNavn(meldinger)

        jsonNode["arbeidsgivere"]
            .flatMap { it["forkastede"] }
            .map { it["vedtaksperiode"] }
            .migrerInnHendelseNavn(meldinger)
    }


    private fun List<JsonNode>.migrerInnHendelseNavn(meldinger: Map<UUID, Pair<Navn, Json>>) {
        this.map { it as ObjectNode }
            .forEach {
                val vedtaksperiodeId = UUID.fromString(it["id"].asText())
                it["hendelseIder"].forEach { id ->
                    val hendelse =
                        meldinger[UUID.fromString(id.asText())]
                            ?.let { (type, _) -> type }
                            ?: return sikkerlogg.warn("Fant ikke hendelse i spleisdb for id $id. VedtaksperiodeId: $vedtaksperiodeId")

                    hendelseTypeTilSporing(hendelse, id.asText(), vedtaksperiodeId)
                }
            }
    }

    private fun hendelseTypeTilSporing(hendelseType: String, id: String, vedtaksperiodeId: UUID) {
        if (hendelseType !in hendelsetyper)
            sikkerlogg.warn("Forventet ikke å finne hendelsetype $hendelseType i hendelseIder i vedtaksperiode-noden. Id: $id, VedtaksperiodeId: $vedtaksperiodeId")
    }

    private val hendelsetyper = listOf(
        "NY_SØKNAD",
        "SENDT_SØKNAD_ARBEIDSGIVER",
        "SENDT_SØKNAD_NAV",
        "INNTEKTSMELDING",
        "OVERSTYRTIDSLINJE",
        "OVERSTYRINNTEKT",
        "OVERSTYRARBEIDSFORHOLD",
    )
}
