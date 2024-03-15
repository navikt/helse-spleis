package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import no.nav.helse.serde.serdeObjectMapper

internal class V232AvsluttetUtenUtbetalingLåstePerioder: JsonMigration(232) {

    override val description = "låser ned alle perioder i Avsluttet Uten Utbetaling"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val perioder = arbeidsgiver.path("vedtaksperioder")
                .filter { it.path("tilstand").asText() == "AVSLUTTET_UTEN_UTBETALING" }
                .map { it.path("fom").asText() to it.path("tom").asText() }

            val låstePerioder = arbeidsgiver.path("sykdomshistorikk").path(0).path("beregnetSykdomstidslinje").path("låstePerioder")
            if (låstePerioder is ArrayNode) {
                val nyListe = låstePerioder
                    .map { it.path("fom").asText() to it.path("tom").asText() }
                    .plus(perioder)
                    .sortedBy { (fom, _) -> LocalDate.parse(fom) }
                    .map { (fom, tom) ->
                        serdeObjectMapper.createObjectNode().apply {
                            put("fom", fom)
                            put("tom", tom)
                        }
                    }

                låstePerioder.removeAll()
                låstePerioder.addAll(nyListe)
            }
        }
    }
}
