package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V296SkjæringstidspunktPåBehandlinger: JsonMigration(version = 296) {
    override val description = "skjæringstidspunkt på behandlinger"

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
        var gjeldendeSkjæringstidspunkt: String = vedtaksperiode.path("skjæringstidspunkt").asText()
        vedtaksperiode.path("behandlinger").reversed().forEach { behandling ->
            val skjæringstidspunkt = behandling.path("endringer").reversed().firstNotNullOfOrNull { endring ->
                endring.path("skjæringstidspunkt").takeIf(JsonNode::isTextual)?.asText()
            }

            if (skjæringstidspunkt != null) {
                gjeldendeSkjæringstidspunkt = skjæringstidspunkt
            }

            behandling.path("endringer").forEach { endring ->
                endring as ObjectNode
                endring.put("skjæringstidspunkt", gjeldendeSkjæringstidspunkt)
            }
        }
    }
}