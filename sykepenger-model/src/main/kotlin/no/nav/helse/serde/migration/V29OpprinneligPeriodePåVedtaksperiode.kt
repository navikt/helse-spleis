package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class V29OpprinneligPeriodePåVedtaksperiode : JsonMigration(version = 29) {
    override val description: String = "Setter opprinneligPeriode = periode dersom nøkkel ikke eksisterer"

    override fun doMigration(jsonNode: ObjectNode) {
        for (arbeidsgiver in jsonNode["arbeidsgivere"]) {
            kopierHendelseIderFraHistorikk(arbeidsgiver["vedtaksperioder"])
            kopierHendelseIderFraHistorikk(arbeidsgiver["forkastede"])
        }
    }

    private fun kopierHendelseIderFraHistorikk(perioder: JsonNode) {
        for (periode in perioder) {
            periode as ObjectNode
            val datoer = finnSykmeldingFomOgTom(periode)
                periode.put("sykmeldingFom", datoer.first.toString())
                periode.put("sykmeldingTom", datoer.second.toString())
        }
    }

    private fun finnSykmeldingFomOgTom(periode: JsonNode): Pair<LocalDate, LocalDate> {
        val sykmeldingTidslinje = periode["sykdomshistorikk"].last().get("hendelseSykdomstidslinje").get("dager")
        return Pair(
            LocalDate.parse(sykmeldingTidslinje.first().get("dato").asText()),
            LocalDate.parse(sykmeldingTidslinje.last().get("dato").asText())
        )
    }
}
