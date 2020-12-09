package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.til
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

internal class V65SjekkeEtterutbetalingerForFeil : JsonMigration(version = 65) {
    override val description: String = "Sjekker etterutbetalinger for feil"
    private val log = LoggerFactory.getLogger("tjenestekall")

    override fun doMigration(jsonNode: ObjectNode) {
        val aktørId = jsonNode.path("aktørId").asText()
        jsonNode
            .path("arbeidsgivere")
            .forEach { arbeidsgiver ->
                val utbetalinger = arbeidsgiver
                    .path("utbetalinger")
                    .map { it }
                    .aktive()

                arbeidsgiver
                    .path("utbetalinger")
                    .filter { utbetaling -> utbetaling.path("type").asText() == "ETTERUTBETALING" }
                    .forEach { sjekkEtterutbetaling(aktørId, utbetalinger, it) }
            }
    }

    private fun sjekkEtterutbetaling(aktørId: String, utbetalinger: Map<String, JsonNode>, utbetaling: JsonNode) {
        val fagsystemId = utbetaling.path("arbeidsgiverOppdrag").path("fagsystemId").asText()
        val forrigeUtbetaling = utbetalinger[fagsystemId] ?: return log.error("Finner ikke forrige utbetaling på {} for {}",
            keyValue("fagsystemId", fagsystemId), keyValue("aktørId", aktørId))

        if (forrigeUtbetaling.path("type").asText() == "ANNULLERING") return
        val forrigePeriode = forrigeUtbetaling.minsteFom() til forrigeUtbetaling.størsteTom()
        val dennePeriode = utbetaling.minsteFom() til utbetaling.størsteTom()

        if (forrigePeriode == dennePeriode) return
        log.error("Utbetaling {} med {} for {} har endret fom/tom fra $forrigePeriode til $dennePeriode!",
            keyValue("utbetalingId", utbetaling.path("id").asText()),
            keyValue("fagsystemId", fagsystemId), keyValue("aktørId", aktørId)
        )
    }

    private companion object {
        fun JsonNode.minsteFom() =
            path("arbeidsgiverOppdrag")
                .path("linjer")
                .minOf { LocalDate.parse(it.path("fom").asText()) }

        fun JsonNode.størsteTom() =
            path("arbeidsgiverOppdrag")
                .path("linjer")
                .maxOf { LocalDate.parse(it.path("tom").asText()) }

        fun List<JsonNode>.aktive() =
            this.groupBy { it.path("arbeidsgiverOppdrag").path("fagsystemId") }
                .map { (_, utbetalinger) -> utbetalinger.kronologisk() }
                .sortedBy { LocalDateTime.parse(it.first().path("tidsstempel").asText()) }
                .mapNotNull {
                    it.lastOrNull { it.erUtbetalingEllerAnnullering() && it.erAktiv() }?.let {
                        it.path("arbeidsgiverOppdrag").path("fagsystemId").asText() to it
                    }
                }.toMap()

        fun JsonNode.erUtbetalingEllerAnnullering() =
            this.path("type").asText() in listOf("UTBETALING", "ANNULLERING")

        fun JsonNode.erAktiv() =
            this.path("status").asText() in listOf("UTBETALT", "ANNULLERT", "GODKJENT", "SENDT", "OVERFØRT", "UTBETALING_FEILET")

        fun List<JsonNode>.kronologisk() = this.sortedBy {
            LocalDateTime.parse(it.path("tidsstempel").asText())
        }
    }
}
