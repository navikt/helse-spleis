package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.migration.V1FjernHendelsetypeEnumFraDag.JsonDagtype.*
import no.nav.helse.serde.migration.V1FjernHendelsetypeEnumFraDag.JsonDagtypeNy.*
import no.nav.helse.serde.migration.V1FjernHendelsetypeEnumFraDag.JsonHendelsetype.*

internal class V1FjernHendelsetypeEnumFraDag : JsonMigration(version = 1) {
    override val description = "Fjerner hendelseType fra JSON til Dag, og erstatter med nye sammensatte enum-typer"

    private val hendelsetypeKey = "hendelseType"
    private val dagtypeKey = "type"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                periode.path("sykdomshistorikk").forEach { sykdomshistorikk ->
                    migrerTidslinje(sykdomshistorikk.path("hendelseSykdomstidslinje"))
                    migrerTidslinje(sykdomshistorikk.path("beregnetSykdomstidslinje"))
                }
            }
        }
    }

    private fun migrerTidslinje(tidslinje: JsonNode) {
        tidslinje.forEach { dag ->
            if (dag.has(hendelsetypeKey)) {
                if (dag[dagtypeKey].textValue() in JsonDagtype.values().map(Enum<*>::name)) {
                    (dag as ObjectNode).put(
                        dagtypeKey,
                        migrerDagtype(
                            JsonHendelsetype.valueOf(dag[hendelsetypeKey].textValue()),
                            JsonDagtype.valueOf(dag[dagtypeKey].textValue())
                        ).name
                    )
                }
                (dag as ObjectNode).remove(hendelsetypeKey)
            }
        }
    }

    private fun migrerDagtype(hendelsetype: JsonHendelsetype, dagtype: JsonDagtype) =
        when (dagtype) {
            ARBEIDSDAG -> when (hendelsetype) {
                Søknad -> ARBEIDSDAG_SØKNAD
                Inntektsmelding -> ARBEIDSDAG_INNTEKTSMELDING
                else -> kanIkkeMigrere(hendelsetype, dagtype)
            }
            EGENMELDINGSDAG -> when (hendelsetype) {
                Søknad -> EGENMELDINGSDAG_SØKNAD
                Inntektsmelding -> EGENMELDINGSDAG_INNTEKTSMELDING
                else -> kanIkkeMigrere(hendelsetype, dagtype)
            }
            FERIEDAG -> when (hendelsetype) {
                Søknad -> FERIEDAG_SØKNAD
                Inntektsmelding -> FERIEDAG_INNTEKTSMELDING
                else -> kanIkkeMigrere(hendelsetype, dagtype)
            }
            PERMISJONSDAG -> when (hendelsetype) {
                Søknad -> PERMISJONSDAG_SØKNAD
                else -> kanIkkeMigrere(hendelsetype, dagtype)
            }
            SYKEDAG -> when (hendelsetype) {
                Søknad -> SYKEDAG_SØKNAD
                Sykmelding -> SYKEDAG_SYKMELDING
                else -> kanIkkeMigrere(hendelsetype, dagtype)
            }
        }

    private fun kanIkkeMigrere(hendelsetype: JsonHendelsetype, dagtype: JsonDagtype): Nothing {
        error("støtter ikke $dagtype for $hendelsetype")
    }

    private enum class JsonHendelsetype {
        Sykmelding,
        Søknad,
        Inntektsmelding
    }

    private enum class JsonDagtype {
        ARBEIDSDAG,
        EGENMELDINGSDAG,
        FERIEDAG,
        PERMISJONSDAG,
        SYKEDAG
    }

    private enum class JsonDagtypeNy {
        ARBEIDSDAG_INNTEKTSMELDING,
        ARBEIDSDAG_SØKNAD,
        EGENMELDINGSDAG_INNTEKTSMELDING,
        EGENMELDINGSDAG_SØKNAD,
        FERIEDAG_INNTEKTSMELDING,
        FERIEDAG_SØKNAD,
        PERMISJONSDAG_SØKNAD,
        SYKEDAG_SYKMELDING,
        SYKEDAG_SØKNAD
    }
}
