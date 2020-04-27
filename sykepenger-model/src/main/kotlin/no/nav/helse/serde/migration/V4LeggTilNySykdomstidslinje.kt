package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.serde.mapping.JsonDagType.*
import no.nav.helse.serde.mapping.NyJsonDagType
import java.util.*

internal class V4LeggTilNySykdomstidslinje : JsonMigration(version = 4) {
    override val description = "Legger til nye sykdomstidslinjer under sykdomshistorikk"

    private val hendelsetidslinjeKey = "hendelseSykdomstidslinje"
    private val beregnetTidslinjeKey = "beregnetSykdomstidslinje"
    private val nyHendelsetidslinjeKey = "nyHendelseSykdomstidslinje"
    private val nyBeregnetTidslinjeKey = "nyBeregnetSykdomstidslinje"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                periode.path("sykdomshistorikk").forEach { historikkElement ->
                    (historikkElement as ObjectNode).set<JsonNode>(
                        nyBeregnetTidslinjeKey,
                        konverterSykdomstidslinje(
                            historikkElement.withArray(beregnetTidslinjeKey),
                            historikkElement["tidsstempel"]
                        )
                    )
                    (historikkElement as ObjectNode).set<JsonNode>(
                        nyHendelsetidslinjeKey,
                        konverterSykdomstidslinje(
                            historikkElement.withArray(hendelsetidslinjeKey),
                            historikkElement["tidsstempel"]
                        )
                    )
                }
            }
        }
    }

    private fun konverterSykdomstidslinje(sykdomstidslinje: ArrayNode, tidsstempelNode: JsonNode): JsonNode {
        val nyeDager = sykdomstidslinje.map { dag -> nyDag(dag) }
        val nySykdomstidslinjeNode = jacksonObjectMapper().createObjectNode()
        nySykdomstidslinjeNode.put("id", UUID.randomUUID().toString())
        nySykdomstidslinjeNode.set<JsonNode>("tidsstempel", tidsstempelNode)
        nySykdomstidslinjeNode.set<ArrayNode>("dager", jacksonObjectMapper().convertValue(nyeDager, ArrayNode::class.java))
        nySykdomstidslinjeNode.set<ArrayNode>("låstePerioder", jacksonObjectMapper().createArrayNode())
        return nySykdomstidslinjeNode
    }

    private fun nyDag(dag: JsonNode): JsonNode {
        val dagNode = jacksonObjectMapper().createObjectNode()
        dagNode.put("type", konverterDagtype(dag["type"].asText()).toString())
        if (dagNode["type"].asText() == "PROBLEMDAG") {
            dagNode.put("melding", "Konvertert dag")
        }
        dagNode.set<JsonNode>("dato", dag["dagen"])
        dag["grad"]?.also { dagNode.set<JsonNode>("grad", it) }
        val kilde = kilde(dag["type"].asText())
        dagNode.set<JsonNode>("kilde", kilde)
        return dagNode
    }

    private fun kilde(dagtype: String): JsonNode {
        val kilde = jacksonObjectMapper().createObjectNode()
        val type = when (valueOf(dagtype)) {
            ARBEIDSDAG_INNTEKTSMELDING,
            EGENMELDINGSDAG_INNTEKTSMELDING,
            FRISK_HELGEDAG_INNTEKTSMELDING,
            FERIEDAG_INNTEKTSMELDING -> "Inntektsmelding"
            EGENMELDINGSDAG_SØKNAD,
            ARBEIDSDAG_SØKNAD,
            FERIEDAG_SØKNAD,
            PERMISJONSDAG_SØKNAD,
            SYKEDAG_SØKNAD,
            SYK_HELGEDAG_SØKNAD,
            FRISK_HELGEDAG_SØKNAD -> "Søknad"
            SYKEDAG_SYKMELDING,
            SYK_HELGEDAG_SYKMELDING -> "Sykmelding"
            IMPLISITT_DAG,
            FORELDET_SYKEDAG,
            PERMISJONSDAG_AAREG,
            STUDIEDAG,
            UBESTEMTDAG,
            UTENLANDSDAG -> "Konvertert dag"
        }
        kilde.put("type", type)
        kilde.put("id", "00000000-0000-0000-0000-000000000000")
        return kilde
    }

    private fun konverterDagtype(dagType: String) = when (valueOf(dagType)) {
        ARBEIDSDAG_INNTEKTSMELDING,
        ARBEIDSDAG_SØKNAD -> NyJsonDagType.ARBEIDSDAG
        EGENMELDINGSDAG_INNTEKTSMELDING,
        EGENMELDINGSDAG_SØKNAD -> NyJsonDagType.ARBEIDSGIVERDAG
        FERIEDAG_INNTEKTSMELDING,
        FERIEDAG_SØKNAD -> NyJsonDagType.FERIEDAG
        FRISK_HELGEDAG_INNTEKTSMELDING,
        FRISK_HELGEDAG_SØKNAD -> NyJsonDagType.FRISK_HELGEDAG
        IMPLISITT_DAG -> NyJsonDagType.UKJENT_DAG
        FORELDET_SYKEDAG -> NyJsonDagType.FORELDET_SYKEDAG
        PERMISJONSDAG_SØKNAD,
        PERMISJONSDAG_AAREG -> NyJsonDagType.PERMISJONSDAG
        STUDIEDAG -> NyJsonDagType.STUDIEDAG
        SYKEDAG_SYKMELDING,
        SYKEDAG_SØKNAD -> NyJsonDagType.SYKEDAG
        SYK_HELGEDAG_SYKMELDING,
        SYK_HELGEDAG_SØKNAD -> NyJsonDagType.SYK_HELGEDAG
        UBESTEMTDAG -> NyJsonDagType.PROBLEMDAG
        UTENLANDSDAG -> NyJsonDagType.UTENLANDSDAG
    }
}
