package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V153FjerneSykmeldingsdager : JsonMigration(version = 153) {
    override val description = """
        Fjerne dager med kilde sykmelding fra sykdomstidslinjen. 
        I migrering 150 ble vedtaksperioder som var opprettet basert på sykmelding slettet, men den tilhørende
        sykdomshistorikken ble ikke slettet fra sykdomstidslinjen
    """

    /*
        # For å rydde opp i sykdomshistorikken

        For hver arbeidsgiver:
        1a  Lag en kopi av nyeste sykdomshistorikk[0].beregnetSykdomstidslinje (første elementet er det nyeste)
        1b  Fjern alle dager som har kilde Sykmelding
        1c  Sette periode-objektet til null
            - Når Sykdomstidslinje-klassen initialiseres settes periode til føste frem til siste dag i 'dager'
            - Om vi fjerner noen dager med kilde Sykmelding som ligger på slutten av 'dager' ville den eventuelle
              perioden som lå der blitt feil.
        2a Lage et nytt sykdomshistorikk-element
        2b Generere en ny id & tidsstempel på elementet
        2c Sette hendelseId null
           - Tilsvarende som gjøres når vi forkaster en periode
        2d Legge til en tom hendelseSykdomstidslinje
           - Tilsvarende som gjøres når vi forkaster en periode
        2e Sette beregnetSykdomstidslinje til sykdomstidslinjene som ble laget i punkt 1
        2f Legge til elemetet fremst i sykdomshistorikken (sykdomshistorikk[0])
           - Slik at det etter migreringen er å anse som det nyeste elementet.
           - På denne måten bevarer vi også historikken på sykdomstidslinjer som hadde denne feilen.

        # For å logge mulige kandidater som kan være feilutbetalt

        Ettersom informasjonen i søknaden ikke bare har innvirkningen på dagene i vedtaksperioden isolert,
        men også på skjæringstidspunkt og arbeidsgiverperiode kan alle vedtaksperioder med fom
        etter sykmeldingsdagene som feilaktig lå på sykdomstidslinjen potensielt inneholde feil.

     */

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val fødselsnummer = jsonNode.path("fødselsnummer").asText()

        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            val sykdomshistorikk = arbeidsgiver["sykdomshistorikk"] as ArrayNode
            val sisteSykdomshistorikkElement = sykdomshistorikk.firstOrNull() ?: return@forEach
            val endretSykdomstidslinje = (sisteSykdomshistorikkElement["beregnetSykdomstidslinje"].deepCopy<ObjectNode>())
            val (utenSykmeldingsdager, sykmeldingsdager) = endretSykdomstidslinje
                .path("dager")
                .partition { it.path("kilde").path("type").asText() != "Sykmelding" }
            if (sykmeldingsdager.isEmpty()) return@forEach

            sikkerlogg.info("Fjerner sykmeldingsdager ${sykmeldingsdager.map { it.dagPeriode() }} fra sykdomstidslinjen for {}",
                keyValue("fødselsnummer", fødselsnummer)
            )

            arbeidsgiver.path("vedtaksperioder").filter { vedtaksperiode ->
                val vedtaksperiodeFom = LocalDate.parse(vedtaksperiode.path("fom").asText())
                sykmeldingsdager.any { it.dagFom() < vedtaksperiodeFom }
            }.forEach { vedtaksperiode ->
                sikkerlogg.info("Vedtaksperiode opprettet etter sykmeldingsdager på sykdomstidslinjen for {}, {}, {}",
                    keyValue("fødselsnummer", fødselsnummer),
                    keyValue("vedtaksperiodeId", vedtaksperiode.path("id").asText()),
                    keyValue("tilstand", vedtaksperiode.path("tilstand").asText())
                )
            }

            val nyeDager = serdeObjectMapper.createArrayNode().also { arrayNode ->
                utenSykmeldingsdager.forEach { arrayNode.add(it) }
            }
            endretSykdomstidslinje.putNull("periode")
            endretSykdomstidslinje.replace("dager", nyeDager)

            val nyttSykdomshistorikkElement = serdeObjectMapper.createObjectNode().also { element ->
                element.put("id", "${UUID.randomUUID()}")
                element.putNull("hendelseId")
                element.put("tidsstempel", "${LocalDateTime.now()}")
                element.set<ObjectNode>("hendelseSykdomstidslinje", serdeObjectMapper.createObjectNode().also {
                    it.putArray("låstePerioder")
                    it.putNull("periode")
                    it.putArray("dager")
                })
                element.set<ObjectNode>("beregnetSykdomstidslinje", endretSykdomstidslinje)
            }
            sykdomshistorikk.insert(0, nyttSykdomshistorikkElement)
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        private fun JsonNode.dagFom() =
            path("dato").takeIf { it.isTextual }?.let { LocalDate.parse(it.asText()) } ?: LocalDate.parse(path("fom").asText())
        private fun JsonNode.dagPeriode() =
            path("dato").takeIf { it.isTextual }?.asText() ?: "${path("fom").asText()} - ${path("tom").asText()}"
    }
}