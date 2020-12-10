package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.til
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

internal class V66SjekkeEtterutbetalingerForFeil : JsonMigration(version = 66) {
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

    private fun sjekkEtterutbetaling(aktørId: String, utbetalinger: Map<String, List<Pair<LocalDateTime, JsonNode>>>, utbetaling: JsonNode) {
        val fagsystemId = utbetaling.path("arbeidsgiverOppdrag").path("fagsystemId").asText()
        val forrigeUtbetaling = utbetalinger.forrigeUtbetaling(fagsystemId, utbetaling.tidsstempel()) ?: return log.error("Finner ikke forrige utbetaling på {} for {}",
            keyValue("fagsystemId", fagsystemId), keyValue("aktørId", aktørId))
        val sisteUtbetaling = requireNotNull(utbetalinger.forrigeUtbetaling(fagsystemId, LocalDateTime.MAX))

        if (sisteUtbetaling.utbetalingtype() == "ANNULLERING") return
        val forrigePeriode = forrigeUtbetaling.minsteFom() til forrigeUtbetaling.størsteTom()
        val dennePeriode = utbetaling.minsteFom() til utbetaling.størsteTom()

        if (forrigePeriode == dennePeriode) return
        log.error("Fant etterutbetaling med feil. Utbetaling {} med {} for {} har endret fom/tom fra <$forrigePeriode> til <$dennePeriode>!" +
            "Forrige utbetaling var {}",
            keyValue("utbetalingId", utbetaling.path("id").asText()),
            keyValue("fagsystemId", fagsystemId), keyValue("aktørId", aktørId),
            keyValue("forrigeUtbetalingId", forrigeUtbetaling.path("id").asText()),
        )
    }

    private companion object {
        fun Map<String, List<Pair<LocalDateTime, JsonNode>>>.forrigeUtbetaling(fagsystemId: String, tidsstempel: LocalDateTime) =
            this[fagsystemId]?.lastOrNull { (opprettet, _) -> opprettet < tidsstempel }?.second

        fun JsonNode.minsteFom() =
            path("arbeidsgiverOppdrag")
                .path("linjer")
                .minOf { LocalDate.parse(it.path("fom").asText()) }

        fun JsonNode.størsteTom() =
            path("arbeidsgiverOppdrag")
                .path("linjer")
                .maxOf { LocalDate.parse(it.path("tom").asText()) }

        fun List<JsonNode>.aktive() =
            this.groupBy { it.path("arbeidsgiverOppdrag").path("fagsystemId").asText() }
                .mapValues { (_, utbetalinger) -> utbetalinger.kronologisk() }
                .mapValues { (_, utbetalinger) -> utbetalinger.filter { it.erAktiv() } }
                .mapValues { (_, utbetalinger) -> utbetalinger.map { it.tidsstempel() to it } }

        fun JsonNode.utbetalingtype() =
            this.path("type").asText()

        fun JsonNode.tidsstempel() =
            LocalDateTime.parse(this.path("tidsstempel").asText())

        fun JsonNode.erAktiv() =
            this.path("status").asText() in listOf("UTBETALT", "ANNULLERT", "GODKJENT", "SENDT", "OVERFØRT", "UTBETALING_FEILET")

        fun List<JsonNode>.kronologisk() = this.sortedBy {
            it.tidsstempel()
        }
    }
}
