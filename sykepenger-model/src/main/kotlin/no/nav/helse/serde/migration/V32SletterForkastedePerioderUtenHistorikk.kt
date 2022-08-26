package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V32SletterForkastedePerioderUtenHistorikk : JsonMigration(version = 32) {
    private val log = LoggerFactory.getLogger("SletterForkastedePerioderUtenHistorikk")
    override val description: String = "Sletter forkastede perioder som mangler historikk"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val arbeidsgivere = jsonNode.path("arbeidsgivere")

        // Check for trashed vedtaksperioder with no history. These can not be migrated and must be removed.
        arbeidsgivere.forEach {
            val arbeidsgiver = it as ObjectNode
            val _forkastede = arbeidsgiver.path("forkastede").filter { forkastetPeriode ->
                val harInnhold = !(forkastetPeriode.path("sykdomshistorikk").isEmpty)
                if (!harInnhold) {
                    log.warn("Fant tom sykdomshistorikk på vedtaksperiode: ${forkastetPeriode.path("id").asText()}")
                }
                harInnhold
            }
            arbeidsgiver.putArray("forkastede").addAll(_forkastede)
        }
    }
}
