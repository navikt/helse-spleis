package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V152SlettGamleSykmeldingsperioder : JsonMigration(version = 152) {
    override val description: String = "Sletter gamle sykmeldingsperioder som blokkerer for gjenopptaBehandlingNy og " +
            "som gjør at en periode kan være stuck i AvventerBlokkerendePeriode"

    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    /*
        Strategi:
        Vi sletter sykmeldingsperioder litt ulikt i disse 2 tilfellene:

        1. Sykmeldingsperioder som som ligger lagret på samme arbeidsgiver som den søknaden treffer
            Vi sletter alt som ligger frem til og med søknad.periode().endInclusive, beholder alt etter

        2. Sykmeldingsperioder som ligger lagret på en annen arbeidsgiver enn den søknaden treffer
            Vi sletter alt som ligger frem til og med søknad.periode().endInclusive.minusDays(1), beholder alt etter

        Etter migrering V150 vil alle vedtaksperioder være i en state der de har mottatt søknad
        Vi bruker vedtaksperiode.tom som basis for sletting av gamle sykmeldingsperioder

        Algoritme:
            1. For hver arbeidsgiver A
            2. Gå gjennom alle vedtaksperioder for A og finn siste dato vedtaksperiode.tom
            3. For alle sykmeldingsperioder SP for A
                hvis SP.tom <= dato  --> slett SP
                hvis SP.fom > dato --> behold SP
                hvis ikke --> SP(fom, tom) blir kutta til SP(dato+1, tom)
            4. For alle arbeidsgivere gå gjennom
                hvis SP.tom <= dato-1  --> slett SP
                hvis SP.fom > dato-1 --> behold SP
                hvis ikke --> SP(fom, tom) blir kutta til SP(dato, tom)

     */

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fødselsnummer = jsonNode.fødselsnummer()
        jsonNode["arbeidsgivere"]
            .forEach { arbeidsgiverNode ->
                val nyesteTom = arbeidsgiverNode.hentAlleVedtaksperiodeToms().maxOfOrNull { it } ?: return@forEach
                arbeidsgiverNode.slettSykmeldingsperioderForSammeArbeidsgiver(nyesteTom, fødselsnummer)
                jsonNode.slettSykmeldingsperioderPåTversAvArbeidsgivere(nyesteTom, fødselsnummer)
            }
    }

    private fun JsonNode.slettSykmeldingsperioderForSammeArbeidsgiver(
        nyesteTom: LocalDate,
        fødselsnummer: String
    ) {
        val sykmeldingsperioder = this.hentSykmeldingsperioder()
        if (sykmeldingsperioder.isEmpty()) return
        val orgnummer = this.orgnummer()
        val nyeSykmeldingsperioder = sykmeldingsperioder.beholdSykmeldingsperioderEtter(nyesteTom)
        if (nyeSykmeldingsperioder != sykmeldingsperioder) {
            this.erstattSykmeldingsperioder(nyeSykmeldingsperioder)
            sikkerLogg.info(
                "Migrerte sykmeldingsperioder på samme arbeidsgiver med {}, {}, gamleSykmeldingsperioder=${sykmeldingsperioder}, nyeSykmeldingsperioder=${nyeSykmeldingsperioder}, {}",
                StructuredArguments.keyValue("fodselsnummer", fødselsnummer),
                StructuredArguments.keyValue("orgnummer", orgnummer),
                StructuredArguments.keyValue("nyesteTom", nyesteTom)
            )
        }
    }

    private fun JsonNode.slettSykmeldingsperioderPåTversAvArbeidsgivere(
        nyesteTom: LocalDate,
        fødselsnummer: String
    ) {
        this["arbeidsgivere"]
            .forEach { arbeidsgiverNode ->
                val sykmeldingsperioder = arbeidsgiverNode.hentSykmeldingsperioder()
                if (sykmeldingsperioder.isEmpty()) return@forEach
                val orgnummer = arbeidsgiverNode.orgnummer()
                val nyeSykmeldingsperioder =
                    sykmeldingsperioder.beholdSykmeldingsperioderEtter(nyesteTom.minusDays(1))
                if (nyeSykmeldingsperioder != sykmeldingsperioder) {
                    arbeidsgiverNode.erstattSykmeldingsperioder(nyeSykmeldingsperioder)
                    sikkerLogg.info(
                        "Migrerte sykmeldingsperioder på annen arbeidsgiver med {}, {}, gamleSykmeldingsperioder=${sykmeldingsperioder}, nyeSykmeldingsperioder=${nyeSykmeldingsperioder}, {}",
                        StructuredArguments.keyValue("fodselsnummer", fødselsnummer),
                        StructuredArguments.keyValue("orgnummer", orgnummer),
                        StructuredArguments.keyValue("nyesteTom", nyesteTom)
                    )
                }
            }
    }
}


private fun List<Periode>.beholdSykmeldingsperioderEtter(nyesteTom: LocalDate): List<Periode> {
    return this.mapNotNull { it.beholdDagerEtterDato(nyesteTom) }
}

private fun Periode.beholdDagerEtterDato(cutoff: LocalDate): Periode? = when {
    endInclusive <= cutoff -> null
    start > cutoff -> this
    else -> cutoff.plusDays(1) til endInclusive
}

private fun JsonNode.erstattSykmeldingsperioder(perioder: List<Periode>) {
    val nyeSykmeldingsperiodeNoder = perioder.map { periode ->
        serdeObjectMapper.createObjectNode().also {
            it.put("fom", periode.start.toString())
            it.put("tom", periode.endInclusive.toString())
        }
    }
    this as ObjectNode
    this.remove("sykmeldingsperioder")
    this.putArray("sykmeldingsperioder").addAll(nyeSykmeldingsperiodeNoder)
}

private fun JsonNode.orgnummer() = this["organisasjonsnummer"].asText()

private fun JsonNode.fødselsnummer() = this["fødselsnummer"].asText()

private fun JsonNode.hentAlleVedtaksperiodeToms() =
    this["vedtaksperioder"].map { LocalDate.parse(it["tom"].asText()) }

private fun JsonNode.hentSykmeldingsperioder() =
    this["sykmeldingsperioder"].map {
        Periode(
            fom = LocalDate.parse(it["fom"].asText()),
            tom = LocalDate.parse(it["tom"].asText())
        )
    }
