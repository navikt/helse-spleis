package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.serdeObjectMapper
import java.time.LocalDate

internal class V313OppryddingIUbrukteRefusjonsopplysninger : JsonMigration(version = 313) {
    override val description = "Fjerner ubrukte refusjonsopplysninger tidligere enn siste vedtaksperiode på arbeidsgiveren"

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
    ) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver as ObjectNode
            val sisteVedtaksperiode = arbeidsgiver.path("vedtaksperioder").lastOrNull() ?: return@forEach
            val sisteTomPåVedtaksperiode =
                sisteVedtaksperiode.path("behandlinger").last().path("endringer").last().path("tom").asText().let {
                    LocalDate.parse(it)
                }
            val ubrukteRefusjonsopplysninger = arbeidsgiver.path("ubrukteRefusjonsopplysninger") as ObjectNode
            val ubrukteRefusjonsopplysningerEtterMigrering = serdeObjectMapper.createObjectNode()
            ubrukteRefusjonsopplysninger.fields().forEach { (dato, refusjonstidslinjer) ->
                val futiristiske =
                    refusjonstidslinjer
                        .path("perioder")
                        .deepCopy<JsonNode>()
                        .filter { periode -> periode.path("tom").dato().isAfter(sisteTomPåVedtaksperiode) }
                        .map { periode ->
                            (periode as ObjectNode)
                            val fom = periode.path("fom").dato()
                            periode.put("fom", maxOf(fom, sisteTomPåVedtaksperiode.plusDays(1)).toString())
                        }
                if (futiristiske.isNotEmpty()) {
                    ubrukteRefusjonsopplysningerEtterMigrering.putObject(dato).putArray("perioder").apply {
                        this.addAll(futiristiske)
                    }
                }
            }
            arbeidsgiver.replace("ubrukteRefusjonsopplysninger", ubrukteRefusjonsopplysningerEtterMigrering)
        }
    }

    private companion object {
        private fun JsonNode.dato(): LocalDate = this.asText().let { LocalDate.parse(it) }
    }
}
