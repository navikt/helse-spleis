package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.AktivitetsloggObserver

internal class V27CachetSykdomstidslinjePåVedtaksperiode : JsonMigration(version = 27) {
    override val description: String = "Kopierer nyeste beregnet sykdomstidslinje i sykdomshistorikken til vedtaksperioden"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        for (arbeidsgiver in jsonNode["arbeidsgivere"]) {
            kopierSykdomstidslinjeFraHistorikk(arbeidsgiver["vedtaksperioder"])
            kopierSykdomstidslinjeFraHistorikk(arbeidsgiver["forkastede"])
        }
    }

    private fun kopierSykdomstidslinjeFraHistorikk(perioder: JsonNode) {
        for (periode in perioder) {
            periode as ObjectNode
            periode.set<ObjectNode>("sykdomstidslinje", periode["sykdomshistorikk"].first()["beregnetSykdomstidslinje"])
        }
    }
}
