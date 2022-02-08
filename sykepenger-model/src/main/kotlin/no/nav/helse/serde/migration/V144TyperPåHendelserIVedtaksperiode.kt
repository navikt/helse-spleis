package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper
import java.util.*

internal class V144TyperPåHendelserIVedtaksperiode : JsonMigration(version = 144) {
    override val description: String = "Legger til typer på hendelses-ider i vedtaksperioden"

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


    fun List<JsonNode>.migrerInnHendelseNavn(meldinger: Map<UUID, Pair<Navn, Json>>) {
        this.map { it as ObjectNode }
            .forEach {
                val hendelser: ObjectNode = it.remove("hendelseIder").fold(serdeObjectMapper.createObjectNode()) { acc, idNode ->
                    val type = meldinger[UUID.fromString(idNode.asText())]?.let { (type, _) -> type } ?: "UKJENT"
                    acc.put(idNode.asText(), type)
                }
                it.set<ObjectNode>("hendelseIder", hendelser)
            }
    }
}
