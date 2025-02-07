package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import java.time.LocalDate
import java.util.UUID

internal class V318FjerneSykNavSomSykdomstidslinjedagtype : JsonMigration(version = 318) {
    override val description = "fjerner syknav som sykdomstidslinjedagtype"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val sykNavDager = buildMap<LocalDate, MutableList<Dag>> {
                arbeidsgiver.path("sykdomshistorikk").forEach { element ->
                    (
                        element
                            .path("beregnetSykdomstidslinje")
                            .path("dager") as ArrayNode
                    )
                        .datoer()
                        .map { (dato, dag) ->
                            if (dag.path("type").asText() == "SYKEDAG_NAV") {
                                putIfAbsent(dato, mutableListOf())
                            }
                            this[dato]?.add(dag.somDag())
                        }
                }
            }

            arbeidsgiver.path("sykdomshistorikk").forEachIndexed { index, element ->
                migrerHendelseSykdomstidslinje(element.path("hendelseSykdomstidslinje"))
                migrerSykdomstidslinje(sykNavDager, element.path("beregnetSykdomstidslinje"))
            }
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrerVedtaksperiode(sykNavDager, vedtaksperiode)
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerVedtaksperiode(sykNavDager, forkastet.path("vedtaksperiode"))
            }
        }
    }

    private fun migrerVedtaksperiode(sykNavDager: Map<LocalDate, List<Dag>>, vedtaksperiode: JsonNode) {
        vedtaksperiode.path("behandlinger").forEach {
            migrerBehandling(sykNavDager, it)
        }
    }

    private fun migrerBehandling(sykNavDager: Map<LocalDate, List<Dag>>, behandling: JsonNode) {
        behandling.path("endringer").forEach { migrerEndring(sykNavDager, it) }
    }

    private fun migrerEndring(sykNavDager: Map<LocalDate, List<Dag>>, endring: JsonNode) {
        migrerSykdomstidslinje(sykNavDager, endring.path("sykdomstidslinje"))
    }

    private fun migrerSykdomstidslinje(sykNavDager: Map<LocalDate, List<Dag>>, sykdomstidslinje: JsonNode) {
        val endretSykdomstidslinje = (
            sykdomstidslinje
                .deepCopy<ObjectNode>()
                .path("dager") as ArrayNode
            )
            .datoer()
            .map { (dato, dag) ->
                if (dag.path("type").asText() != "SYKEDAG_NAV") {
                    dag
                } else {
                    val kilde = dag.path("kilde").somKilde()
                    val nyDagtype = when {
                        kilde.type.lowercase().contains("inntektsmelding") -> {
                            val dagFørSykNav = sykNavDager[dato]?.let { historikk ->
                                // finner det siste historikk-innslaget som har registrert dagen med samme kilde
                                val indeksTilSiste = historikk.indexOfLast { historiskDag -> historiskDag.kilde.id == kilde.id }
                                // finner dagtypen forut før endringen
                                historikk.drop(indeksTilSiste + 1).firstOrNull { historiskDag ->
                                    historiskDag.dagtype != "SYKEDAG_NAV"
                                }
                            }
                            if (dagFørSykNav == null) {
                                "ARBEIDSGIVERDAG"
                            } else {
                                dag.replace("kilde", dagFørSykNav.node.path("kilde"))
                                dagFørSykNav.dagtype
                            }
                        }
                        else -> "SYKEDAG"
                    }
                    dag.put("type", nyDagtype)
                }
            }

        val nySykdomstidslinje = (sykdomstidslinje as ObjectNode)
        val dagerListe = (nySykdomstidslinje.path("dager") as ArrayNode)
        dagerListe.removeAll()
        dagerListe.addAll(endretSykdomstidslinje)
    }

    private fun migrerHendelseSykdomstidslinje(sykdomstidslinje: JsonNode) {
        sykdomstidslinje
            .path("dager")
            .filter { it.path("type").asText() == "SYKEDAG_NAV" }
            .map { dag ->
                val kilde = dag.path("kilde").path("type").asText()
                val nyDagtype = when {
                    kilde.lowercase().contains("inntektsmelding") -> "ARBEIDSGIVERDAG"
                    else -> "SYKEDAG"
                }
                (dag as ObjectNode).put("type", nyDagtype)
            }
    }

    private data class Dag(
        val dagtype: String,
        val kilde: Kilde,
        val node: JsonNode
    ) {
        data class Kilde(
            val type: String,
            val id: UUID
        )
    }

    private fun JsonNode.somDag() =
        Dag(
            dagtype = this.path("type").asText(),
            kilde = this.path("kilde").somKilde(),
            node = this
        )
    private fun JsonNode.somKilde() =
        Dag.Kilde(
            type = this.path("type").asText(),
            id = this.path("id").asText().uuid
        )

    private fun ArrayNode.datoer() = flatMap { it.dagTilDatoer() }

    private fun JsonNode.dagTilDatoer(): List<Pair<LocalDate, ObjectNode>> {
        val dato = (this.path("dato") as? TextNode)?.asText()?.dato
        val interval = dato?.let { dato..dato } ?: (this.path("fom").asText().dato .. this.path("tom").asText().dato)
        return interval.start.datesUntil(interval.endInclusive.plusDays(1)).map { dato ->
            dato to this.deepCopy<ObjectNode>().apply {
                putNull("fom")
                putNull("tom")
                put("dato", dato.toString())
            }
        }.toList()
    }
}
