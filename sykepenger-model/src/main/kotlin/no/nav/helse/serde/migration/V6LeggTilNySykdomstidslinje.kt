package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.serde.mapping.JsonDagType
import no.nav.helse.serde.migration.V6LeggTilNySykdomstidslinje.JsonDagTypePreV10.*
import java.util.*

internal class V6LeggTilNySykdomstidslinje : JsonMigration(version = 6) {
    override val description = "Legger til nye sykdomstidslinjer under sykdomshistorikk"

    private val hendelsetidslinjeKey = "hendelseSykdomstidslinje"
    private val beregnetTidslinjeKey = "beregnetSykdomstidslinje"
    private val nyHendelsetidslinjeKey = "nyHendelseSykdomstidslinje"
    private val nyBeregnetTidslinjeKey = "nyBeregnetSykdomstidslinje"
    private val ubestemtKilde = "Ingen"
    private val inntektsmelding = "Inntektsmelding"
    private val søknad = "Søknad"
    private val sykmelding = "Sykmelding"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                val hendelser = collectHendelseIds(periode)
                periode.path("sykdomshistorikk").forEach { historikkElement ->
                    (historikkElement as ObjectNode).replace(
                        nyBeregnetTidslinjeKey,
                        konverterSykdomstidslinje(
                            historikkElement.withArray(beregnetTidslinjeKey),
                            historikkElement["tidsstempel"],
                            hendelser
                        )
                    )
                    historikkElement.replace(
                        nyHendelsetidslinjeKey,
                        konverterSykdomstidslinje(
                            historikkElement.withArray(hendelsetidslinjeKey),
                            historikkElement["tidsstempel"],
                            hendelser
                        )
                    )
                }
            }
        }
    }

    private fun collectHendelseIds(
        periode: JsonNode
    ): MutableMap<String, UUID> {
        val hendelser = mutableMapOf<String, UUID>()
        periode.path("sykdomshistorikk").forEach { historikkElement ->
            val hendelsetype = historikkElement[hendelsetidslinjeKey]
                .map { kildeForDag(it["type"].asText()) }.find { it != ubestemtKilde }
            val hendelseId = UUID.fromString(historikkElement["hendelseId"].asText())
            hendelsetype?.also {
                hendelser[hendelsetype] = hendelseId
            }
        }
        return hendelser
    }

    private fun konverterSykdomstidslinje(
        sykdomstidslinje: ArrayNode,
        tidsstempelNode: JsonNode,
        hendelser: MutableMap<String, UUID>
    ): JsonNode {
        val nyeDager = sykdomstidslinje.map { dag -> nyDag(dag, hendelser) }
        val nySykdomstidslinjeNode = jacksonObjectMapper().createObjectNode()
        nySykdomstidslinjeNode.put("id", UUID.randomUUID().toString())
        nySykdomstidslinjeNode.replace("tidsstempel", tidsstempelNode)
        nySykdomstidslinjeNode.replace(
            "dager",
            jacksonObjectMapper().convertValue(nyeDager, ArrayNode::class.java)
        )
        nySykdomstidslinjeNode.replace("låstePerioder", jacksonObjectMapper().createArrayNode())
        return nySykdomstidslinjeNode
    }

    private fun nyDag(dag: JsonNode, hendelser: MutableMap<String, UUID>): JsonNode {
        val dagNode = jacksonObjectMapper().createObjectNode()
        dagNode.put("type", konverterDagtype(dag["type"].asText()).toString())
        if (dagNode.isProblemdag()) {
            dagNode.put("melding", "Konvertert dag")
        }
        dagNode.replace("dato", dag["dagen"])

        val grad = dag["grad"]?.asDouble() ?: if (dagNode.isArbeidsgiverdag()) 100.0 else null
        grad?.also { dagNode.put("grad", it) }

        val kilde = kilde(dag["type"].asText(), hendelser)
        dagNode.replace("kilde", kilde)
        return dagNode
    }

    private fun ObjectNode.isProblemdag() = this["type"].asText() == "PROBLEMDAG"
    private fun ObjectNode.isArbeidsgiverdag() = this["type"].asText() == "ARBEIDSGIVERDAG"

    private fun kilde(dagtype: String, hendelser: MutableMap<String, UUID>): JsonNode {
        val kilde = jacksonObjectMapper().createObjectNode()
        val type = kildeForDag(dagtype)
        val id = hendelser[type]?.toString() ?: "00000000-0000-0000-0000-000000000000"
        kilde.put("type", type)
        kilde.put("id", id)
        return kilde
    }

    private fun kildeForDag(dagtype: String) = when (valueOf(dagtype)) {
        ARBEIDSDAG_INNTEKTSMELDING,
        EGENMELDINGSDAG_INNTEKTSMELDING,
        FRISK_HELGEDAG_INNTEKTSMELDING,
        FERIEDAG_INNTEKTSMELDING -> inntektsmelding
        EGENMELDINGSDAG_SØKNAD,
        FORELDET_SYKEDAG,
        ARBEIDSDAG_SØKNAD,
        FERIEDAG_SØKNAD,
        PERMISJONSDAG_SØKNAD,
        SYKEDAG_SØKNAD,
        SYK_HELGEDAG_SØKNAD,
        STUDIEDAG,
        UTENLANDSDAG,
        FRISK_HELGEDAG_SØKNAD -> søknad
        SYKEDAG_SYKMELDING,
        SYK_HELGEDAG_SYKMELDING -> sykmelding
        IMPLISITT_DAG,
        PERMISJONSDAG_AAREG,
        UBESTEMTDAG -> ubestemtKilde
    }

    private fun konverterDagtype(dagType: String) = when (valueOf(dagType)) {
        ARBEIDSDAG_INNTEKTSMELDING,
        ARBEIDSDAG_SØKNAD -> JsonDagType.ARBEIDSDAG
        EGENMELDINGSDAG_INNTEKTSMELDING,
        EGENMELDINGSDAG_SØKNAD -> JsonDagType.ARBEIDSGIVERDAG
        FERIEDAG_INNTEKTSMELDING,
        FERIEDAG_SØKNAD -> JsonDagType.FERIEDAG
        FRISK_HELGEDAG_INNTEKTSMELDING,
        FRISK_HELGEDAG_SØKNAD -> JsonDagType.FRISK_HELGEDAG
        IMPLISITT_DAG -> JsonDagType.UKJENT_DAG
        FORELDET_SYKEDAG -> JsonDagType.FORELDET_SYKEDAG
        PERMISJONSDAG_SØKNAD,
        PERMISJONSDAG_AAREG -> JsonDagType.PERMISJONSDAG
        STUDIEDAG -> JsonDagType.STUDIEDAG
        SYKEDAG_SYKMELDING,
        SYKEDAG_SØKNAD -> JsonDagType.SYKEDAG
        SYK_HELGEDAG_SYKMELDING,
        SYK_HELGEDAG_SØKNAD -> JsonDagType.SYK_HELGEDAG
        UBESTEMTDAG -> JsonDagType.PROBLEMDAG
        UTENLANDSDAG -> JsonDagType.UTENLANDSDAG
    }

    private enum class JsonDagTypePreV10 {
        ARBEIDSDAG_INNTEKTSMELDING,
        ARBEIDSDAG_SØKNAD,
        EGENMELDINGSDAG_INNTEKTSMELDING,
        EGENMELDINGSDAG_SØKNAD,
        FERIEDAG_INNTEKTSMELDING,
        FERIEDAG_SØKNAD,
        FRISK_HELGEDAG_INNTEKTSMELDING,
        FRISK_HELGEDAG_SØKNAD,
        FORELDET_SYKEDAG,
        IMPLISITT_DAG,
        PERMISJONSDAG_SØKNAD,
        PERMISJONSDAG_AAREG,
        STUDIEDAG,
        SYKEDAG_SYKMELDING,
        SYKEDAG_SØKNAD,
        SYK_HELGEDAG_SYKMELDING,
        SYK_HELGEDAG_SØKNAD,
        UBESTEMTDAG,
        UTENLANDSDAG
    }
}
