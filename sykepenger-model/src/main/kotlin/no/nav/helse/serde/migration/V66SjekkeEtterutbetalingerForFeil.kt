package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V66SjekkeEtterutbetalingerForFeil : JsonMigration(version = 66) {
    override val description: String = "Sjekker etterutbetalinger for feil"
    private val log = LoggerFactory.getLogger("tjenestekall")

    override fun doMigration(jsonNode: ObjectNode) {
        /* migrering gjort */
    }
}
