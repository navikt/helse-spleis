package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.til
import org.slf4j.LoggerFactory

internal class V225SondereTrøbleteUtbetalinger: JsonMigration(225) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "sonderer json etter trøblete utbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val utbetalinger = arbeidsgiver.path("utbetalinger")
                .groupBy { utbetaling ->
                    utbetaling.path("korrelasjonsId").asText()
                }

            val trøbleteKandidater = utbetalinger.mapNotNull { (_, utbetalinger) ->
                val kandidat = utbetalinger.size == 1 && utbetalinger.single().path("status").asText() == "GODKJENT_UTEN_UTBETALING"
                utbetalinger.firstOrNull()?.takeIf { kandidat }
            }

            val trøbleteUtbetalinger = trøbleteKandidater.filter { kandidat ->
                val periode = LocalDate.parse(kandidat.path("fom").asText()) til LocalDate.parse(kandidat.path("tom").asText())
                utbetalinger.any { (korrelasjonsId, utbetalinger) ->
                    korrelasjonsId != kandidat.path("korrelasjonsId").asText() && utbetalinger.any { other ->
                        val otherPeriode = LocalDate.parse(other.path("fom").asText()) til LocalDate.parse(other.path("tom").asText())
                        other.path("status").asText() != "FORKASTET" && periode.overlapperMed(otherPeriode)
                    }
                }
            }

            if (trøbleteUtbetalinger.isNotEmpty()) {
                sikkerlogg.info("{} har {} trøblete utbetalinger på arbeidsgiver {}: {}",
                    keyValue("aktørId", jsonNode.path("aktørId").asText()),
                    trøbleteUtbetalinger.size,
                    keyValue("organisasjonsnummer", arbeidsgiver.path("organisasjonsnummer").asText()),
                    trøbleteUtbetalinger.joinToString { it.path("id").asText() }
                )
            }
        }
    }
}
