package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til

internal abstract class KildeSykmeldingTilSøknad(versjon: Int): JsonMigration(versjon) {

    override val description = "Endrer kilde Sykmelding til Søknad for gamle perioder som er gjenopplivet i V203-V208"

    abstract fun perioderSomSkalEndres(): Map<Periode, UUID>

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            val dager = arbeidsgiver["sykdomshistorikk"].firstOrNull()?.path("beregnetSykdomstidslinje")?.path("dager") ?: return@forEach
            val kilder = dager.map { it.kildeId }.toSet()
            if (kilder.intersect(perioderSomSkalEndres().values.toSet()).isEmpty()) return@forEach

            dager
                .filter { it.kildeSykmelding }
                .filter { it.periode in perioderSomSkalEndres().keys }
                .forEach { dag ->
                    val kilde = dag.path("kilde") as ObjectNode
                    kilde.put("type", "Søknad")
                    kilde.put("id", "${perioderSomSkalEndres().getValue(dag.periode)}")
                }
        }
    }

    internal companion object {
        internal val String.dato get() = LocalDate.parse(this)
        internal val String.uuid get() = UUID.fromString(this)

        private val JsonNode.kildeId get() = UUID.fromString(path("kilde").path("id").asText())
        private val JsonNode.kildeSykmelding get() = path("kilde").path("type").asText() == "Sykmelding"
        private val JsonNode.periode get() = when (has("fom") && has("tom")) {
            true -> LocalDate.parse(path("fom").asText()) til LocalDate.parse(path("tom").asText())
            false -> LocalDate.parse(path("dato").asText()).somPeriode()
        }
    }
}