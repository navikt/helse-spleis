package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime

internal class V11EgenHendelsedagForSykHelgedag() : JsonMigration(version = 11) {

    override val description = "Kalkulerer riktig hendelsetype for alle Sykhelgedager."

    private val tidsstempelKey = "tidsstempel"
    private val hendelsetidslinjeKey = "hendelseSykdomstidslinje"
    private val beregnetTidslinjeKey = "beregnetSykdomstidslinje"
    private val dagenKey = "dagen"
    private val dagtypeKey = "type"

    private val sykHelgedagType = "SYK_HELGEDAG"
    private val sykHelgedagSøknad = "SYK_HELGEDAG_SØKNAD"
    private val sykHelgedagSykmelding = "SYK_HELGEDAG_SYKMELDING"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                val sykdomshistorikk = periode.path("sykdomshistorikk")
                val sykHelgedager = finnForekomsterAvSykhelgedag(sykdomshistorikk)

                sykdomshistorikk.forEach { historikkElement ->
                    migrerHendelseTidslinje(historikkElement.path(hendelsetidslinjeKey))

                    val hendelsetidsstempel = LocalDateTime.parse(historikkElement[tidsstempelKey].asText())
                    migrerTidslinje(hendelsetidsstempel, sykHelgedager, historikkElement.path(beregnetTidslinjeKey))
                }
            }
        }
    }

    private fun finnForekomsterAvSykhelgedag(sykdomshistorikk: JsonNode): Map<LocalDate, List<Pair<Hendelsetype, LocalDateTime>>> {
        val sykHelgedager = mutableListOf<Triple<LocalDate, Hendelsetype, LocalDateTime>>()
        sykdomshistorikk.forEach { historikkElement ->
            val hendelsetidsstempel = LocalDateTime.parse(historikkElement[tidsstempelKey].asText())
            val hendelseTidslinje = historikkElement.path(hendelsetidslinjeKey)
            val hendelsetype = gjettHendelsetype(hendelseTidslinje)

            hendelseTidslinje.forEach { dag ->
                if (dag[dagtypeKey].textValue() == sykHelgedagType) {
                    sykHelgedager.add(
                        Triple(
                            LocalDate.parse(dag[dagenKey].asText()),
                            hendelsetype,
                            hendelsetidsstempel
                        )
                    )
                }
            }
        }

        return sykHelgedager.groupBy { (dagen, _, _) -> dagen }
            .mapValues { (_, value) ->
                value.map { (if (sykdomshistorikk.size() == 1) Hendelsetype.Sykmelding else it.second) to it.third }
            }
    }

    private fun Map<LocalDate, List<Pair<Hendelsetype, LocalDateTime>>>.finnSisteDag(dagen: LocalDate, hendelsetidsstempel: LocalDateTime) =
        this.getValue(dagen)
            .filter { (_, dato) -> dato <= hendelsetidsstempel }
            .maxBy { (_, dato) -> dato }?.first
            ?: error("Fant ikke hendelse for $dagen")

    private fun gjettHendelsetype(tidslinje: JsonNode): Hendelsetype {
        val dagtyper = tidslinje.map { it[dagtypeKey].textValue() }
        if (SykmeldingDagType.SYKEDAG_SYKMELDING.name in dagtyper) return Hendelsetype.Sykmelding
        if (dagtyper.any { it in SøknadDagType.values().map(Enum<*>::name) }) return Hendelsetype.Søknad
        if (dagtyper.all { it == sykHelgedagType }) return Hendelsetype.Sykmelding // guess
        return Hendelsetype.Inntektsmelding
    }

    private fun migrerHendelseTidslinje(tidslinje: JsonNode) {
        val hendelsetype = gjettHendelsetype(tidslinje)
        if (hendelsetype == Hendelsetype.Inntektsmelding) return

        val nyVerdi = verdiForHendelse(hendelsetype)

        tidslinje.forEach { dag ->
            if (dag[dagtypeKey].textValue() != sykHelgedagType) return@forEach
            (dag as ObjectNode).put(dagtypeKey, nyVerdi)
        }
    }

    private fun migrerTidslinje(hendelsetidsstempel: LocalDateTime, sykHelgedager: Map<LocalDate, List<Pair<Hendelsetype, LocalDateTime>>>, tidslinje: JsonNode) {
        tidslinje.forEach { dag ->
            if (dag[dagtypeKey].textValue() != sykHelgedagType) return@forEach
            val hendelseForDag = sykHelgedager.finnSisteDag(LocalDate.parse(dag[dagenKey].asText()), hendelsetidsstempel)
            (dag as ObjectNode).put(dagtypeKey, verdiForHendelse(hendelseForDag))
        }
    }

    private fun verdiForHendelse(hendelsetype: Hendelsetype) =
        when (hendelsetype) {
            Hendelsetype.Søknad -> sykHelgedagSøknad
            Hendelsetype.Sykmelding -> sykHelgedagSykmelding
            else -> error("Forventet ikke $hendelsetype")
        }

    private enum class Hendelsetype {
        Inntektsmelding,
        Søknad,
        Sykmelding
    }

    private enum class SøknadDagType {
        ARBEIDSDAG_SØKNAD,
        EGENMELDINGSDAG_SØKNAD,
        FERIEDAG_SØKNAD,
        PERMISJONSDAG_SØKNAD,
        STUDIEDAG,
        SYKEDAG_SØKNAD,
        UTENLANDSDAG
    }

    private enum class SykmeldingDagType {
        SYKEDAG_SYKMELDING
    }
}
