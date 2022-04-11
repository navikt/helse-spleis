package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper

internal class V149ResetLåstePerioder : JsonMigration(version = 149) {
    override val description: String = "Resetter alle låste perioder til å kun inneholde alle avsluttede perioder"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .forEach { arbeidsgiverNode ->
                val avsluttedeVedtaksperioder = arbeidsgiverNode["vedtaksperioder"].filter { it["tilstand"].asText() == "AVSLUTTET" }
                val periodenoder = avsluttedeVedtaksperioder.map { vedtaksperiodeNode ->
                    serdeObjectMapper.createObjectNode().also {
                        it.put("fom", vedtaksperiodeNode["fom"].asText())
                        it.put("tom", vedtaksperiodeNode["tom"].asText())
                    }
                }
                val sisteSykdomshistorikkinnslag = arbeidsgiverNode["sykdomshistorikk"].lastOrNull() ?: return@forEach
                (sisteSykdomshistorikkinnslag["beregnetSykdomstidslinje"] as ObjectNode).also {
                    it.putArray("låstePerioder").addAll(periodenoder)
                }
            }
    }
}
