package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper
import java.time.LocalDateTime
import java.util.*

internal class V90HendelsekildeTidsstempel : JsonMigration(version = 90) {
    override val description: String = "Setter tidsstempel på hendelsekilde"
    private val tidsstempler = mutableMapOf<UUID, LocalDateTime>()

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val elementtidsstempler = arbeidsgiver.path("sykdomshistorikk")
                .filter { element -> element.hasNonNull("hendelseId") }
                .associate { element -> UUID.fromString(element.path("hendelseId").asText()) to LocalDateTime.parse(element.path("tidsstempel").asText()) }
            arbeidsgiver.path("sykdomshistorikk").forEach { element ->
                val elementtidsstempel = LocalDateTime.parse(element.path("tidsstempel").asText())
                migrerSykdomstidslinje(meldingerSupplier, elementtidsstempler, elementtidsstempel, element.path("hendelseSykdomstidslinje"))
                migrerSykdomstidslinje(meldingerSupplier, elementtidsstempler, elementtidsstempel, element.path("beregnetSykdomstidslinje"))
            }

            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrerSykdomstidslinje(meldingerSupplier, vedtaksperiode.path("sykdomstidslinje"))
            }

            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerSykdomstidslinje(meldingerSupplier, forkastet.path("vedtaksperiode").path("sykdomstidslinje"))
            }
        }
    }

    private fun migrerSykdomstidslinje(meldingerSupplier: MeldingerSupplier, elementtidsstempler: Map<UUID, LocalDateTime>, elementtidsstempel: LocalDateTime, tidslinje: JsonNode) {
        tidslinje.path("dager").forEach { dag ->
            migrerKilde(meldingerSupplier, elementtidsstempler, elementtidsstempel, dag.path("kilde") as ObjectNode)
        }
    }

    private fun migrerSykdomstidslinje(meldingerSupplier: MeldingerSupplier, tidslinje: JsonNode) {
        tidslinje.path("dager").forEach { dag ->
            val id = UUID.fromString(dag.path("kilde").path("id").asText())
            val type = dag.path("kilde").path("type").asText()
            migrerKilde(dag.path("kilde") as ObjectNode, finnTidsstempel(meldingerSupplier, id, type))
        }
    }

    private fun migrerKilde(meldingerSupplier: MeldingerSupplier, elementtidsstempler: Map<UUID, LocalDateTime>, elementtidsstempel: LocalDateTime, kilde: ObjectNode) {
        val id = UUID.fromString(kilde.path("id").asText())
        val type = kilde.path("type").asText()
        migrerKilde(kilde, finnTidsstempel(meldingerSupplier, elementtidsstempler, elementtidsstempel, id, type))
    }

    private fun migrerKilde(kilde: ObjectNode, tidsstempel: LocalDateTime) {
        kilde.put("tidsstempel", tidsstempel.toString())
    }

    private fun finnTidsstempel(meldingerSupplier: MeldingerSupplier, elementtidsstempler: Map<UUID, LocalDateTime>, elementtidsstempel: LocalDateTime, id: UUID, type: String): LocalDateTime {
        if (id in tidsstempler) return tidsstempler.getValue(id)
        val tidsstempel = meldingerSupplier.hentMeldinger()[id]?.let {
            tidsstempelFraMelding(type, serdeObjectMapper.readTree(it))?.let { LocalDateTime.parse(it) }
        } ?: elementtidsstempler[id] ?: elementtidsstempel
        tidsstempler[id] = tidsstempel
        return tidsstempel
    }

    private fun finnTidsstempel(meldingerSupplier: MeldingerSupplier, id: UUID, type: String): LocalDateTime {
        if (id in tidsstempler) return tidsstempler.getValue(id)
        val tidsstempel = meldingerSupplier.hentMeldinger()[id]?.let {
            tidsstempelFraMelding(type, serdeObjectMapper.readTree(it))?.let { LocalDateTime.parse(it) }
        } ?: LocalDateTime.now()
        tidsstempler[id] = tidsstempel
        return tidsstempel
    }

    private fun tidsstempelFraMelding(type: String, melding: JsonNode): String? {
        return when (type) {
            "Inntektsmelding" -> melding.path("mottattDato").takeIf(JsonNode::isTextual)?.asText() ?: melding.path("@opprettet").asText()
            "Sykmelding", "Søknad" -> melding.path("sykmeldingSkrevet").asText()
            else -> null
        }
    }
}
