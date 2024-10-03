package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V307RefusjonstidslinjePåBehandlingsendring: JsonMigration(version = 307) {
    override val description = "lager en tom refusjonstidslinje på behandlingsendring"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                migrerVedtaksperiode(periode)
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerVedtaksperiode(forkastet.path("vedtaksperiode"))
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        vedtaksperiode.path("behandlinger").forEach { behandling ->
            behandling.path("endringer").forEach { endring ->
                migrerEndring(endring as ObjectNode)
            }
        }
    }

    private fun migrerEndring(behandling: ObjectNode) {
        behandling.putObject("refusjonstidslinje").apply {
            putArray("perioder")
        }
    }
}
