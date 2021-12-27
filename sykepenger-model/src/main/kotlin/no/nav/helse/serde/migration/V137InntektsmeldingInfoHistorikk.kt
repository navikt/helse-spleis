package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class V137InntektsmeldingInfoHistorikk : JsonMigration(version = 137) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override val description = "Migrerer alle inntektsmeldinginfo til arbeidsgivernivå"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            val historikkElementer = mutableMapOf<LocalDate, MutableList<Map<String, String>>>()
            arbeidsgiver.path("forkastede").forEach { forkasting ->
                migrer(historikkElementer, forkasting.path("vedtaksperiode"))
            }
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrer(historikkElementer, vedtaksperiode)
            }

            (arbeidsgiver as ObjectNode).putArray("inntektsmeldingInfo").also { elementer ->
                historikkElementer.forEach { (dato, inntektsmeldinginfoer) ->
                    elementer.addObject().also { historikkElement ->
                        historikkElement.put("dato", "$dato")
                        historikkElement.putArray("inntektsmeldinger").also { arrayNode ->
                            inntektsmeldinginfoer.forEach { detaljer ->
                                arrayNode.addObject().also { obj ->
                                    obj.put("id", detaljer.getValue("id"))
                                    if (detaljer.containsKey("arbeidsforholdId")) {
                                        obj.put("arbeidsforholdId", detaljer.getValue("arbeidsforholdId"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun migrer(historikkElementer: MutableMap<LocalDate, MutableList<Map<String, String>>>, vedtaksperiode: JsonNode) {
        val info = vedtaksperiode.path("inntektsmeldingInfo")
        val skjæringstidspunktnode = vedtaksperiode.path("skjæringstidspunktFraInfotrygd")?.takeIf { it.isTextual } ?:
            vedtaksperiode.path("skjæringstidspunkt")
        if (info.isMissingNode || info.isNull) return
        if (skjæringstidspunktnode.isMissingNode || skjæringstidspunktnode.isNull) return sikkerlogg.warn("Finner ikke skjæringstidspunkt på vedtaksperiode {} med inntektsmeldinginfo", keyValue("vedtaksperiodeId", vedtaksperiode.path("id").asText()))

        val inntektsmeldingId = info.path("id").asText()
        val arbeidsforholdId = info.path("arbeidsforholdId").takeIf { it.isTextual }?.asText()
        val skjæringstidspunkt = LocalDate.parse(skjæringstidspunktnode.asText())

        val medSammeDato = historikkElementer.getOrPut(skjæringstidspunkt) { mutableListOf() }

        if (medSammeDato.any { it["id"] == inntektsmeldingId && it["arbeidsforholdId"] == arbeidsforholdId }) return
        medSammeDato.add(mutableMapOf(
            "id" to inntektsmeldingId
        ).apply {
            compute("arbeidsforholdId") { _, _ -> arbeidsforholdId }
        })
    }
}
