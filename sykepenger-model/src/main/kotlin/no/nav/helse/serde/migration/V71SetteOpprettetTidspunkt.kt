package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class V71SetteOpprettetTidspunkt : JsonMigration(version = 71) {
    private companion object {
        private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        private val førstedato = LocalDateTime.of(2020, 4, 15, 15, 0, 0)
    }
    override val description: String = "Sette opprettet-tidspunkt for Person"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val tidligste = jsonNode
            .path("aktivitetslogg")
            .path("aktiviteter")
            .minOfOrNull { LocalDateTime.parse(it.path("tidsstempel").asText(), tidsstempelformat) } ?: førstedato

        jsonNode.put("opprettet", "$tidligste")
    }
}
