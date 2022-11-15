package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.til
import org.slf4j.LoggerFactory

internal class V198GjenoppliveTidligereForkastet: JsonMigration(version = 198) {
    override val description = """Gjenoppliver tidligere forkastet vedtaksperioder som fortsatt har aktiv utbetaling"""

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val orgnr = arbeidsgiver.path("organisasjonsnummer").asText()

            val utbetalinger = arbeidsgiver.path("utbetalinger")
                .groupBy { utbetaling -> utbetaling.path("korrelasjonsId").asText() }
                .mapValues { (_, utbetalinger) -> utbetalinger.sortedBy { LocalDateTime.parse(it.path("tidsstempel").asText()) } }

            arbeidsgiver.path("forkastede").forEach { forkastetPeriode ->
                val vedtaksperiode = forkastetPeriode.path("vedtaksperiode")
                val tilstand = vedtaksperiode.path("tilstand").asText()
                val vedtaksperiodensPeriode = LocalDate.parse(vedtaksperiode.path("fom").asText()) til LocalDate.parse(vedtaksperiode.path("tom").asText())

                // potensiell kandidat for gjenoppliving
                if (tilstand in setOf("AVSLUTTET", "AVSLUTTET_UTEN_UTBETALING")) {
                    val utbetaling = utbetalinger.firstNotNullOfOrNull { (_, utbetalinger) ->
                        utbetalinger
                            .filterNot {
                                it.path("status").asText() in setOf("GODKJENT_UTEN_UTBETALING", "FORKASTET", "IKKE_GODKJENT")
                            }
                            .lastOrNull()
                            ?.takeUnless { utbetaling ->
                                utbetaling.path("status").asText() == "ANNULLERT"
                            }
                            ?.takeIf { sisteUtbetaling ->
                                val fom = LocalDate.parse(sisteUtbetaling.path("fom").asText())
                                val tom = LocalDate.parse(sisteUtbetaling.path("tom").asText())
                                val utbetalingensPeriode = fom til tom

                                vedtaksperiodensPeriode in utbetalingensPeriode
                            }
                    }

                    if (utbetaling != null) {
                        val fom = LocalDate.parse(utbetaling.path("fom").asText())
                        val tom = LocalDate.parse(utbetaling.path("tom").asText())
                        val utbetalingensPeriode = fom til tom

                        sikkerLogg.info("{}-{} Kandidat for gjennopplivelse {} ($vedtaksperiodensPeriode) fordi {} ({}, {}) har periode $utbetalingensPeriode",
                            keyValue("aktørId", aktørId),
                            keyValue("organisasjonsnummer", orgnr),
                            keyValue("vedtaksperiodeId", vedtaksperiode.path("id").asText()),
                            keyValue("utbetalingId", utbetaling.path("id").asText()),
                            keyValue("type", utbetaling.path("type").asText()),
                            keyValue("status", utbetaling.path("status").asText())
                        )
                    }
                }
            }
        }
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}