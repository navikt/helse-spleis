package no.nav.helse.spleis

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import io.ktor.util.logging.Logger
import java.time.LocalDateTime
import java.util.UUID
import kotlin.collections.set
import no.nav.helse.spleis.EnBehovslytterSomSerOmBehovErLike.UtgåendeBehovsmelding.Companion.utgåendeBehov

internal interface Behovslytter {
    fun behovsmeldingFraEventBus(json: String )
    fun behovsmeldingFraAktivitetslogg(json: String)
}

internal object EnIkkeAgerendeBehovslytter: Behovslytter {
    override fun behovsmeldingFraEventBus(json: String) {}
    override fun behovsmeldingFraAktivitetslogg(json: String) {}
}

internal class EnBehovslytterSomSerOmBehovErLike(private val sikkerLogg: Logger) : Behovslytter {
    private val utgåendeBehovsmeldingerFraEventBus = mutableMapOf<Set<String>, UtgåendeBehovsmelding>()

    override fun behovsmeldingFraEventBus(json: String) {
        try {
            val utgåendeBehovsmelding = json.utgåendeBehov()
            check(utgåendeBehovsmeldingerFraEventBus[utgåendeBehovsmelding.identifikator] == null) { "[EnBehovslytterSomSerOmBehovErLike] Har allerede registrert utgående behovsmelding fra EventBus med identifikator ${utgåendeBehovsmelding.identifikator}" }
            utgåendeBehovsmeldingerFraEventBus[utgåendeBehovsmelding.identifikator] = utgåendeBehovsmelding
        } catch (exception: Exception) {
            sikkerLogg.warn("[EnBehovslytterSomSerOmBehovErLike] Feil ved oversetting av behov sendt fra EventBus", exception)
        }
    }

    override fun behovsmeldingFraAktivitetslogg(json: String) {
        try {
            val fraAktivitetslogg = json.utgåendeBehov()
            val fraEventBus = utgåendeBehovsmeldingerFraEventBus[fraAktivitetslogg.identifikator] ?: return sikkerLogg.info("[EnBehovslytterSomSerOmBehovErLike] Behovsmelding med behovene ${fraAktivitetslogg.identifikator} sendes ikke fra EventBus ennå.")
            check(fraAktivitetslogg.json == fraEventBus.json) { "Disse beohvene er jo ikke like!\n\t${fraAktivitetslogg.json}\n\t${fraEventBus.json}" }
        } catch (exception: Exception) {
            sikkerLogg.warn("[EnBehovslytterSomSerOmBehovErLike] Feil ved sjekking av om behov sendt fra Aktivitetslogg er like behov sendt fra EventBus", exception)
        }
    }

    private data class UtgåendeBehovsmelding(
        val identifikator: Set<String>,
        val json: ObjectNode,
    ) {
        companion object {
            val mapper = jacksonObjectMapper()
            fun String.utgåendeBehov(): UtgåendeBehovsmelding {
                val json = (mapper.readTree(this) as ObjectNode)
                UUID.fromString(json.path("@id").asText())
                UUID.fromString(json.path("@behovId").asText())
                LocalDateTime.parse(json.path("@opprettet").asText())

                json.remove(setOf(
                    "@opprettetUTC",                    // Sendes kun fra EventBus
                    "@id",                              // Genererte ID'er/tidsstempl sjekket ovenfor
                    "@behovId",
                    "@opprettet",
                    "system_participating_services",    // R&R-greier
                    "system_read_count"
                ))

                val etterspurteBehov = json.path("@behov").map { it.asText() }.toSet()
                if (etterspurteBehov == setOf("Sykepengehistorikk")) {
                    // Akkurat dette behovet sendes både fra personnivå (kun fødselsnummer), men også fra enkelte tilstander, disse 4 ekstra parameterne bruker ikke sparkel-sykepengeperioder så gjør ikke noe at de ei sendes
                    json.remove(setOf("organisasjonsnummer", "yrkesaktivitetstype", "vedtaksperiodeId", "behandlingId"))
                }

                // For utbetalinger så kan det sendes flere like behov, men forskjellig fagområder (vanlig utbetaling) og/eller flere utbetalinger på samme fagområde med forskjellige mottakere (feriepenger)
                val identifikator = etterspurteBehov + setOfNotNull(json.fagområdeOrNull(), json.mottakerOrNull())
                return UtgåendeBehovsmelding(identifikator, json)
            }

            private fun JsonNode.mottakerOrNull() =
                (path("Utbetaling").path("mottaker").takeUnless { it.isMissingOrNull() } ?: path("Feriepengeutbetaling").path("mottaker").takeUnless { it.isMissingOrNull() })?.asText()

            private fun JsonNode.fagområdeOrNull() =
                (path("Utbetaling").path("fagområde").takeUnless { it.isMissingOrNull() } ?: path("Feriepengeutbetaling").path("fagområde").takeUnless { it.isMissingOrNull() })?.asText()
        }
    }
}
