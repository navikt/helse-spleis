package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.person.AktivitetsloggObserver
import org.slf4j.LoggerFactory

internal class V150MigrerVedtaksperioderTilNyTilstandsflyt : JsonMigration(version = 150) {
    override val description: String = "Migrerer vedtaksperiodetilstander til ny flyt"
    private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    private val inntektsmeldingTilstander = arrayOf(
        "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
        "AVVENTER_INNTEKTSMELDING_UFERDIG_GAP",
        "AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE",
        "AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE"
    )

    private val avventerBlokkerendeTilstander = arrayOf(
        "AVVENTER_UFERDIG",
        "AVVENTER_ARBEIDSGIVERE"
    )

    private val tilstanderUtenMottattSøknad = arrayOf(
        "AVVENTER_SØKNAD_UFERDIG_FORLENGELSE",
        "AVVENTER_SØKNAD_FERDIG_FORLENGELSE",
        "AVVENTER_SØKNAD_FERDIG_GAP",
        "AVVENTER_SØKNAD_UFERDIG_GAP",
        "MOTTATT_SYKMELDING_FERDIG_FORLENGELSE",
        "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE",
        "MOTTATT_SYKMELDING_FERDIG_GAP",
        "MOTTATT_SYKMELDING_UFERDIG_GAP"
    )

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        val fødselsnummer = jsonNode["fødselsnummer"].asText()
        jsonNode["arbeidsgivere"]
            .forEach { arbeidsgiverNode ->
                val vedtaksperioder = arbeidsgiverNode["vedtaksperioder"] as ArrayNode
                vedtaksperioder.slettIkkeMottattSøknadTilstander(fødselsnummer)
                vedtaksperioder.oppdaterTilstander(
                    fødselsnummer = fødselsnummer,
                    forkastet = false
                )

                val forkastedeVedtaksperioder = arbeidsgiverNode["forkastede"]
                    .map { forkastetVedtaksperiodeNode -> forkastetVedtaksperiodeNode["vedtaksperiode"] }
                forkastedeVedtaksperioder.oppdaterTilstander(
                    fødselsnummer = fødselsnummer,
                    forkastet = true
                )
                forkastedeVedtaksperioder.oppdaterForkastedeIkkeMottattSøknadTilstander(fødselsnummer)
            }
    }

    private fun ArrayNode.slettIkkeMottattSøknadTilstander(fødselsnummer: String) {
        val iterator = iterator()
        while (iterator.hasNext()) {
            val vedtaksperiodeNode = iterator.next()
            if (vedtaksperiodeNode.tilstand() in tilstanderUtenMottattSøknad) {
                iterator.remove()
                val vedtaksperiodeId = vedtaksperiodeNode.vedtaksperiodeId()
                val gammelTilstand = vedtaksperiodeNode.tilstand()
                sikkerLogg.info(
                    "Slettet vedtaksperiode med {}, {}, {}",
                    StructuredArguments.keyValue("fodselsnummer", fødselsnummer),
                    StructuredArguments.keyValue("vedtaksperiodeId", vedtaksperiodeId),
                    StructuredArguments.keyValue("gammelTilstand", gammelTilstand)
                )
            }
        }
    }

    private fun Iterable<JsonNode>.oppdaterTilstander(
        fødselsnummer: String,
        forkastet: Boolean
    ) = forEach { vedtaksperiodeNode ->
        val vedtaksperiode = vedtaksperiodeNode as ObjectNode
        val gammelTilstand = vedtaksperiodeNode.tilstand()
        val vedtaksperiodeId = vedtaksperiodeNode.vedtaksperiodeId()

        val nyTilstand = when (gammelTilstand) {
            in inntektsmeldingTilstander -> "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK"
            in avventerBlokkerendeTilstander -> "AVVENTER_BLOKKERENDE_PERIODE"
            else -> return@forEach
        }
        sikkerLogg.info(
            "Migrerte vedtaksperiode med {}, {}, {}, {}, {}",
            StructuredArguments.keyValue("fodselsnummer", fødselsnummer),
            StructuredArguments.keyValue("vedtaksperiodeId", vedtaksperiodeId),
            StructuredArguments.keyValue("gammelTilstand", gammelTilstand),
            StructuredArguments.keyValue("nyTilstand", nyTilstand),
            StructuredArguments.keyValue("forkastet", forkastet)
        )
        vedtaksperiode.put("tilstand", nyTilstand)
    }

    private fun Iterable<JsonNode>.oppdaterForkastedeIkkeMottattSøknadTilstander(fødselsnummer: String) =
        forEach { vedtaksperiodeNode ->
            val vedtaksperiode = vedtaksperiodeNode as ObjectNode
            val tilstand = vedtaksperiodeNode.tilstand()
            val vedtaksperiodeId = vedtaksperiodeNode.vedtaksperiodeId()

            if (tilstand in tilstanderUtenMottattSøknad) {
                /*
                For forkastede perioder ønsker vi å sette tilstanden til TIL_INFOTRYGD i tilfelle det finnes gamle
                perioder som ikke fikk denne tilstanden da de ble forkastet.
                */
                vedtaksperiode.put("tilstand", "TIL_INFOTRYGD")
                sikkerLogg.info(
                    "Migrerte vedtaksperiode med {}, {}, {}, {}, {}",
                    StructuredArguments.keyValue("fodselsnummer", fødselsnummer),
                    StructuredArguments.keyValue("vedtaksperiodeId", vedtaksperiodeId),
                    StructuredArguments.keyValue("gammelTilstand", tilstand),
                    StructuredArguments.keyValue("nyTilstand", "TIL_INFOTRYGD"),
                    StructuredArguments.keyValue("forkastet", true)
                )
            }
        }

    private fun JsonNode.tilstand() = this["tilstand"].asText()
    private fun JsonNode.vedtaksperiodeId() = this["id"].asText()

}
