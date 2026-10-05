package no.nav.helse.serde.migration

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode

internal class V336PerioderUtenNavAnsvarPåBehandling : JsonMigration(336) {
    override val description = "Renamer arbeidsgiverperiode til periodeUtenNavAnsvar på Behandling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("perioderUtenNavAnsvar").forEach { periodeUtenNavAnsvar ->
                periodeUtenNavAnsvar as ObjectNode
                periodeUtenNavAnsvar.set("dagerUtenNavAnsvar", periodeUtenNavAnsvar.path("arbeidsgiverperiode").deepCopy())
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
                endring.set("dagerUtenNavAnsvar", endring.path("arbeidsgiverperiode").deepCopy())
                endring.remove("arbeidsgiverperiode")
            }
        }
    }
}
