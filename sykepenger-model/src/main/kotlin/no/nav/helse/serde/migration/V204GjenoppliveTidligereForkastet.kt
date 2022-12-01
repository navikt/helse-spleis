package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.hendelser.til
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V204GjenoppliveTidligereForkastet: JsonMigration(version = 204) {
    override val description = """Gjenoppliver tidligere forkastet vedtaksperioder som fortsatt har aktiv utbetaling"""

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()

        val perioder = finnPerioder(jsonNode).takeUnless { it.isEmpty() } ?: return

        sikkerLogg.info("{} gjenoppliver {} perioder=$perioder", keyValue("aktørId", aktørId), perioder.size)

        perioder.forEach { vedtaksperiodeId ->
            val arbeidsgiver = jsonNode.path("arbeidsgivere").first { arbeidsgiver ->
                arbeidsgiver.path("forkastede").any { forkastetPeriode ->
                    val vedtaksperiode = forkastetPeriode.path("vedtaksperiode")
                    vedtaksperiodeId == vedtaksperiode.path("id").asText()
                }
            }

            val forkastede = arbeidsgiver.path("forkastede") as ArrayNode
            val vedtaksperiodensIndeks = forkastede.indexOfFirst { forkastetPeriode ->
                val vedtaksperiode = forkastetPeriode.path("vedtaksperiode")
                vedtaksperiodeId == vedtaksperiode.path("id").asText()
            }
            val vedtaksperioden = forkastede.remove(vedtaksperiodensIndeks).path("vedtaksperiode")

            val sykdomstidslinje = vedtaksperioden.path("sykdomstidslinje").deepCopy<ObjectNode>()
            val fom = vedtaksperioden.path("fom").asText()
            val tom = vedtaksperioden.path("tom").asText()

            val aktivePerioder = arbeidsgiver.path("vedtaksperioder") as ArrayNode
            aktivePerioder.add(vedtaksperioden)

            val sykdomshistorikk = arbeidsgiver.path("sykdomshistorikk") as ArrayNode
            val nyBeregnetSykdomstidslinje = (sykdomshistorikk.path(0).path("beregnetSykdomstidslinje").takeUnless { it.isNull || it.isMissingNode } ?: sykdomstidslinje).deepCopy<ObjectNode>()
            sykdomshistorikk.insert(0, nyttSykdomshistorikkElement(fom, tom, sykdomstidslinje, nyBeregnetSykdomstidslinje))

            // 1. flytte perioden fra forkastet-liste til aktiv-liste
            // 2. sørge for at sykdomstidslinjen til vedtaksperioden er på arbeidsgivers sykdomshistorikk
            // 3. sørge for at vedtaksperiodens fom/tom er i låstePerioder på arbeidsgivers sykdomshistorikk, gitt at vedtaksperioden er avsluttet

        }
    }

    private fun nyttSykdomshistorikkElement(fom: String, tom: String, sykdomstidslinje: ObjectNode, nyBeregnetSykdomstidslinje: ObjectNode): ObjectNode {
        return serdeObjectMapper.createObjectNode().apply {
            put("id", UUID.randomUUID().toString())
            putNull("hendelseId")
            put("tidsstempel", LocalDateTime.now().toString())
            set<ObjectNode>("hendelseSykdomstidslinje", sykdomstidslinje)
            set<ObjectNode>("beregnetSykdomstidslinje", nyBeregnetSykdomstidslinje.apply {
                val låstePerioder = path("låstePerioder") as ArrayNode
                if (låstePerioder.none { låstPeriode -> låstPeriode.path("fom").asText() == fom && låstPeriode.path("tom").asText() == tom }) {
                    låstePerioder.addObject().apply {
                        put("fom", fom)
                        put("tom", tom)
                    }
                }
                putNull("periode")
                val beregnetDager = path("dager") as ArrayNode

                val dagerSomSkalSettesInn = sykdomstidslinje.path("dager")
                    .flatMap { dag ->
                        val dager = dag.deepCopy<ObjectNode>()
                        val fraDag = LocalDate.parse(dager.remove("fom").asText())
                        val tilDag = LocalDate.parse(dager.remove("tom").asText())

                        (fraDag til tilDag).map { dato ->
                            dager.deepCopy().apply {
                                put("fom", dato.toString())
                                put("tom", dato.toString())
                            }
                        }
                    }
                    .filterNot { dagen ->
                        val dagensDato = LocalDate.parse(dagen.path("fom").asText())
                        beregnetDager.any { beregnetDag ->
                            val fraDag = LocalDate.parse(beregnetDag.path("fom").asText())
                            val tilDag = LocalDate.parse(beregnetDag.path("tom").asText())

                            dagensDato in (fraDag til tilDag)
                        }
                    }

                beregnetDager.addAll(dagerSomSkalSettesInn)
            })
        }
    }

    private fun finnPerioder(jsonNode: ObjectNode): Set<String> {
        val vedtaksperioder = mutableSetOf<String>()

        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val utbetalinger = arbeidsgiver.path("utbetalinger")
                .groupBy { utbetaling -> utbetaling.path("korrelasjonsId").asText() }
                .mapValues { (_, utbetalinger) -> utbetalinger.sortedBy { LocalDateTime.parse(it.path("tidsstempel").asText()) } }

            arbeidsgiver.path("forkastede").forEach { forkastetPeriode ->
                val vedtaksperiode = forkastetPeriode.path("vedtaksperiode")
                val tilstand = vedtaksperiode.path("tilstand").asText()
                val vedtaksperiodensPeriode = LocalDate.parse(vedtaksperiode.path("fom").asText()) til LocalDate.parse(vedtaksperiode.path("tom").asText())

                // potensiell kandidat for gjenoppliving
                if (tilstand in setOf("AVSLUTTET", "AVSLUTTET_UTEN_UTBETALING")) {
                    val utbetaling = utbetalinger.firstNotNullOfOrNull { (_, utbetalinger) ->
                        utbetalinger
                            .filterNot {
                                it.path("status").asText() in setOf("GODKJENT_UTEN_UTBETALING", "FORKASTET", "IKKE_GODKJENT")
                            }
                            .lastOrNull()
                            ?.takeUnless { utbetaling -> utbetaling.path("status").asText() == "ANNULLERT" }
                            ?.takeUnless { utbetaling -> utbetaling.path("arbeidsgiverOppdrag").path("linjer").isEmpty && utbetaling.path("personOppdrag").path("linjer").isEmpty  }
                            ?.takeIf { sisteUtbetaling ->
                                val fom = LocalDate.parse(sisteUtbetaling.path("fom").asText())
                                val tom = LocalDate.parse(sisteUtbetaling.path("tom").asText())
                                val utbetalingensPeriode = fom til tom

                                vedtaksperiodensPeriode in utbetalingensPeriode
                            }
                    }

                    if (utbetaling != null) {
                        vedtaksperioder.add(vedtaksperiode.path("id").asText())
                    }
                }
            }
        }
        return vedtaksperioder
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}