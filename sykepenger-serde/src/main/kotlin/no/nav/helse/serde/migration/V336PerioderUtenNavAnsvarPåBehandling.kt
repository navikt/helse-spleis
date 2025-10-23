package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V336PerioderUtenNavAnsvarPåBehandling : JsonMigration(336) {
    override val description = "Renamer arbeidsgiverperiode til periodeUtenNavAnsvar på Behandling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("perioderUtenNavAnsvar").forEach { periodeUtenNavAnsvar ->
                periodeUtenNavAnsvar as ObjectNode
                periodeUtenNavAnsvar.set<ArrayNode>("dagerUtenNavAnsvar", periodeUtenNavAnsvar.path("arbeidsgiverperiode").deepCopy<ArrayNode>())
                periodeUtenNavAnsvar.remove("arbeidsgiverperiode")
            }
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrerVedtaksperiode(vedtaksperiode)
            }
            arbeidsgiver.path("forkastede").forEach { vedtaksperiode ->
                migrerVedtaksperiode(vedtaksperiode.path("vedtaksperiode"))
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        vedtaksperiode.path("behandlinger").forEach { behandling ->
            behandling.path("endringer").forEach { endring ->
                endring as ObjectNode
                endring.set<ArrayNode>("dagerUtenNavAnsvar", endring.path("arbeidsgiverperiode").deepCopy<ArrayNode>())
                endring.remove("arbeidsgiverperiode")
            }
        }
    }
}
