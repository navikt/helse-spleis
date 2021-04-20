package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class V19KlippOverlappendeVedtaksperioder : JsonMigration(version = 19) {
    override val description = "Klipper overlappende vedtaksperioder etter omskriving av sykdomstidslinje"
    private val sykdomshistorikkKey = "sykdomshistorikk"
    private val beregnetSykdomstidslinjeKey = "beregnetSykdomstidslinje"
    private val hendelsetidslinjeKey = "hendelseSykdomstidslinje"
    private val dagerKey = "dager"
    private val datoKey = "dato"
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        var tom: LocalDate? = null
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                val vedtaksperiodeId = periode["id"].asText()
                if (tom != null) {
                    periode[sykdomshistorikkKey].forEach { sykdomshistorikk ->
                        val orginalHendelseTidslinje = (sykdomshistorikk[hendelsetidslinjeKey] as ObjectNode)
                        val hendelseDagerKlippet = orginalHendelseTidslinje[dagerKey].filter {
                            val kilde = it["kilde"]["type"].asText()
                            LocalDate.parse(it[datoKey].asText()).isAfter(tom) || kilde != "Inntektsmelding"
                        }
                        val orginalBeregnetDager = sykdomshistorikk[beregnetSykdomstidslinjeKey] as ObjectNode
                        val beregnetDagerKlippet =
                            orginalBeregnetDager[dagerKey].filter {
                                val kilde = it["kilde"]["type"].asText()
                                LocalDate.parse(it[datoKey].asText()).isAfter(tom) || kilde != "Inntektsmelding"
                            }

                        if (orginalHendelseTidslinje[dagerKey].size() != hendelseDagerKlippet.size || orginalBeregnetDager[dagerKey].size() != beregnetDagerKlippet.size) {
                            log.info("Klippet bort overlappende tidslinjedager i vedtaksperiode: $vedtaksperiodeId")
                        }

                        orginalHendelseTidslinje.replace(
                            dagerKey,
                            jacksonObjectMapper().convertValue(hendelseDagerKlippet, ArrayNode::class.java)
                        )
                        orginalBeregnetDager.replace(
                            dagerKey,
                            jacksonObjectMapper().convertValue(beregnetDagerKlippet, ArrayNode::class.java)
                        )
                    }
                }

                tom =
                    periode[sykdomshistorikkKey].flatMap { it[hendelsetidslinjeKey][dagerKey] }
                        .filter { it["kilde"]["type"].asText() != "Inntektsmelding" }
                        .maxOfOrNull { LocalDate.parse(it[datoKey].asText()) }
                        ?: run {
                            log.error("Kunne ikke migrere vedtaksperiode: $vedtaksperiodeId med tom historikk.")
                            throw IllegalStateException("Kunne ikke migrere vedtaksperiode: $vedtaksperiodeId med tom historikk.")
                        }

                periode[sykdomshistorikkKey].forEach { sykdomshistorikk ->
                    val orginalHendelseTidslinje = (sykdomshistorikk[hendelsetidslinjeKey] as ObjectNode)
                    val hendelseDagerKlippet = orginalHendelseTidslinje[dagerKey].filter {
                        val kilde = it["kilde"]["type"].asText()
                        !LocalDate.parse(it[datoKey].asText()).isAfter(tom) || kilde != "Inntektsmelding"
                    }
                    val orginalBeregnetDager = sykdomshistorikk[beregnetSykdomstidslinjeKey] as ObjectNode
                    val beregnetDagerKlippet =
                        orginalBeregnetDager[dagerKey].filter {
                            val kilde = it["kilde"]["type"].asText()
                            !LocalDate.parse(it[datoKey].asText()).isAfter(tom) || kilde != "Inntektsmelding"
                        }

                    if (orginalHendelseTidslinje[dagerKey].size() != hendelseDagerKlippet.size || orginalBeregnetDager[dagerKey].size() != beregnetDagerKlippet.size) {
                        log.info("Klippet bort inntektsmeldinger som strekker seg ut til høyre utenfor vedtaksperioden: $vedtaksperiodeId")
                    }

                    orginalHendelseTidslinje.replace(
                        dagerKey,
                        jacksonObjectMapper().convertValue(hendelseDagerKlippet, ArrayNode::class.java)
                    )
                    orginalBeregnetDager.replace(
                        dagerKey,
                        jacksonObjectMapper().convertValue(beregnetDagerKlippet, ArrayNode::class.java)
                    )
                }
            }
        }
    }
}
