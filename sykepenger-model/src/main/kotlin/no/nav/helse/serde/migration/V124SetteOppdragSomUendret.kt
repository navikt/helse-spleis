package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

internal class V124SetteOppdragSomUendret : JsonMigration(version = 124) {

    override val description = "Setter Oppdrag uten endringer til UEND"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            arbeidsgiver["utbetalinger"].forEach { utbetaling ->
                val arbeidsgiverOppdrag = utbetaling["arbeidsgiverOppdrag"] as ObjectNode
                val personOppdrag = utbetaling["personOppdrag"] as ObjectNode
                settUEND(arbeidsgiverOppdrag)
                settUEND(personOppdrag)
            }
        }
    }

    private fun settUEND(oppdrag: ObjectNode) {
        val linjer = oppdrag.path("linjer")
        if (linjer.isEmpty) return // tomme oppdrag vil ha endringskode "NY" som default
        val siste = linjer.last().path("endringskode").asText()
        if (siste != uendret) return
        logger.info("migrerte endringskode for oppdrag ${oppdrag.path("fagsystemId").asText()} (tidsstempel=${oppdrag.path("tidsstempel").asText()})")
        oppdrag.put("endringskode", uendret)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger("tjenestekall")
        private const val uendret = "UEND"
    }
}
