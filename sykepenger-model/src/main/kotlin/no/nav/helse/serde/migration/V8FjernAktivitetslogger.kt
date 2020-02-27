package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V8FjernAktivitetslogger : JsonMigration(version = 8) {

    override val description = "Legger til tom aktivitetslogg i person"

    private val aktivitetsloggerKey = "aktivitetslogger"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.remove(aktivitetsloggerKey)
        fjernArbeidsgiver(jsonNode)
    }

    private fun fjernArbeidsgiver(personNode: ObjectNode) {
        personNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            (arbeidsgiver as ObjectNode).remove(aktivitetsloggerKey)
            fjernVedtaksperiode(arbeidsgiver)
        }
    }

    private fun fjernVedtaksperiode(arbeidsgiverNode: ObjectNode) {
        arbeidsgiverNode.path("vedtaksperioder").forEach { vedtaksperiode ->
            (vedtaksperiode as ObjectNode).remove(aktivitetsloggerKey)
        }
    }
}
