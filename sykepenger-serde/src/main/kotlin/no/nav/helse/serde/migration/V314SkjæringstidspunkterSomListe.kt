package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V314SkjæringstidspunkterSomListe : JsonMigration(version = 314) {
    override val description = "Legger til liste av skjæringstidspunkter på behandling"

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

    private fun migrerEndring(endring: ObjectNode) {
        val skjæringstidspunkt = endring.path("skjæringstidspunkt").asText()
        endring.putArray("skjæringstidspunkter").apply {
            add(skjæringstidspunkt)
        }
    }
}
