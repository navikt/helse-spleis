package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode


internal class V1EndreKunArbeidsgiverSykedagEnum : JsonMigration(version = 1) {

    override val description = "Endrer KUN_ARBEIDSGIVER_SYKEDAG-enum til FORELDET_SYKEDAG fordi Dag-klassen har fått nytt navn"

    private val hendelsetidslinjeKey = "hendelseSykdomstidslinje"
    private val beregnetTidslinjeKey = "beregnetSykdomstidslinje"
    private val dagtypeKey = "type"
    private val foreldetSykedagtype = "FORELDET_SYKEDAG"
    private val kunArbeidsgiverSykedagtype = "KUN_ARBEIDSGIVER_SYKEDAG"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                periode.path("sykdomshistorikk").forEach { historikkElement ->
                    migrerTidslinje(historikkElement.path(beregnetTidslinjeKey))
                    migrerTidslinje(historikkElement.path(hendelsetidslinjeKey))
                }
            }
        }
    }

    private fun migrerTidslinje(tidslinje: JsonNode) {
        tidslinje.forEach { dag ->
            if (dag[dagtypeKey].textValue() == kunArbeidsgiverSykedagtype) {
                (dag as ObjectNode).put(dagtypeKey, foreldetSykedagtype)
            }
        }
    }
}
