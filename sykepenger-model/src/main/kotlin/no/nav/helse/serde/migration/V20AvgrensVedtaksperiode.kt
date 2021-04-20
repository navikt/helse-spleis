package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V20AvgrensVedtaksperiode : JsonMigration(version = 20) {
    override val description: String = "Legger til eksplisitt avgrensing av vedtaksperioden"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            migrerVedtaksperioder(arbeidsgiver.path("vedtaksperioder").toList())
            migrerVedtaksperioder(arbeidsgiver.path("forkastede").toList())
        }
    }

    private fun migrerVedtaksperioder(perioder: List<JsonNode>) =
        perioder
            .map { it as ObjectNode }
            .forEach(::migrerVedtaksperiode)

    private fun migrerVedtaksperiode(periode: ObjectNode) {
        val sykdomshistorikkElement = periode["sykdomshistorikk"].first()
        val tidslinje = sykdomshistorikkElement["beregnetSykdomstidslinje"]["dager"]

        val fom = tidslinje.first()["dato"]
        val tom = tidslinje.last()["dato"]
        periode.set<ObjectNode>("fom", fom)
        periode.set<ObjectNode>("tom", tom)
    }
}
