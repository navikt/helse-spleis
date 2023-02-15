package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.til
import org.slf4j.LoggerFactory

internal class V228SondereFeilaktigFomForUtbetaling: JsonMigration(228) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private fun JsonNode.asLocalDate() = LocalDate.parse(asText())
        private fun JsonNode.asOptionalLocalDate() = takeIf { it.isTextual }?.asLocalDate()
    }

    override val description = "sonderer utbetalinger som kan ha en feilaktig fom-dato"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger")
                .forEach { utbetaling ->
                    val utbetalingId = utbetaling.path("id").asText()
                    val utbetalingstidslinjeSisteUkjentDag = utbetaling.path("utbetalingstidslinje").path("dager").lastOrNull { dag ->
                        dag.path("type").asText() == "UkjentDag"
                    }?.let { dag ->
                        when {
                            dag.hasNonNull("dato") -> dag.path("dato").asLocalDate()
                            else -> dag.path("tom").asLocalDate()
                        }
                    }

                    if (utbetalingstidslinjeSisteUkjentDag != null) {
                        val utbetalingensFom = utbetaling.path("fom").asLocalDate()
                        val arbeidsgiveroppdrag = oppdragsperiode(utbetaling.path("arbeidsgiverOppdrag"))
                        val personoppdrag = oppdragsperiode(utbetaling.path("personOppdrag"))
                        val oppdragsperiode = listOfNotNull(arbeidsgiveroppdrag, personoppdrag)?.periode()

                        if (utbetalingensFom != utbetalingstidslinjeSisteUkjentDag) {
                            sikkerlogg.info("utbetaling {} for {} har ulik fom enn siste ukjente dag. " +
                                    "Utbetalingens fom er ${if (utbetalingensFom < utbetalingstidslinjeSisteUkjentDag) "eldre" else "nyere"}. " +
                                    "Vi mener $utbetalingstidslinjeSisteUkjentDag er mer riktig enn $utbetalingensFom. " +
                                    "Oppdragene har tidligste linje ${oppdragsperiode?.start}. " +
                                    "${if (oppdragsperiode != null && oppdragsperiode.start > utbetalingstidslinjeSisteUkjentDag) "Linjene starter etter kalkulert fom" else "Linjene starter før kalkulert fom"}",
                                keyValue("utbetalingId", utbetalingId),
                                keyValue("aktørId", aktørId)
                            )
                        }
                    }
                }
        }
    }

    private fun oppdragsperiode(oppdrag: JsonNode): Periode? {
        val linjeperiode = oppdrag.path("linjer").map { linje ->
            val datoStatusFom = linje.path("datoStatusFom").asOptionalLocalDate()
            val fom = (datoStatusFom ?: linje.path("fom").asLocalDate())
            fom til linje.path("tom").asLocalDate()
        }
        return linjeperiode.periode()
    }
}