package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID
import no.nav.helse.Toggle

internal class V150MigrerVedtaksperioderTilNyTilstandsflyt : JsonMigration(version = 150) {
    override val description: String = "Migrerer vedtaksperiodetilstander til ny flyt"

    init {
        require(Toggle.NyTilstandsflyt.enabled) {
            "Er du helt sikker på at du vil slette og endre alle vedtaksperiodetilstander? ;)"
        }
    }

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

    private val tilstanderUtenMottatSøknad = arrayOf(
        "AVVENTER_SØKNAD_UFERDIG_FORLENGELSE",
        "AVVENTER_SØKNAD_FERDIG_FORLENGELSE",
        "AVVENTER_SØKNAD_FERDIG_GAP",
        "AVVENTER_SØKNAD_UFERDIG_GAP",
        "MOTTATT_SYKMELDING_FERDIG_FORLENGELSE",
        "MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE",
        "MOTTATT_SYKMELDING_FERDIG_GAP",
        "MOTTATT_SYKMELDING_UFERDIG_GAP"
    )

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {}

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: JsonMigrationObserver
    ) {
        jsonNode["arbeidsgivere"]
            .forEach { arbeidsgiverNode ->
                val vedtaksperioder = arbeidsgiverNode["vedtaksperioder"] as ArrayNode
                vedtaksperioder.slettIkkeMottattSøknadTilstander(observer)
                vedtaksperioder.oppdaterTilstander()

                val forkastedeVedtaksperioder = arbeidsgiverNode["forkastede"]
                    .map { forkastetVedtaksperiodeNode -> forkastetVedtaksperiodeNode["vedtaksperiode"] }
                forkastedeVedtaksperioder.oppdaterTilstander()
                forkastedeVedtaksperioder.oppdaterForkastedeIkkeMottattSøknadTilstander()
            }
    }

    private fun ArrayNode.slettIkkeMottattSøknadTilstander(observer: JsonMigrationObserver) {
        val iterator = iterator()
        while (iterator.hasNext()) {
            val vedtaksperiodeNode = iterator.next()
            if (vedtaksperiodeNode["tilstand"].asText() in tilstanderUtenMottatSøknad) {
                iterator.remove()
                observer.vedtaksperiodeSlettet(UUID.fromString(vedtaksperiodeNode["id"].asText()), vedtaksperiodeNode)
            }
        }
    }

    private fun Iterable<JsonNode>.oppdaterTilstander() = forEach { vedtaksperiodeNode ->
        val vedtaksperiode = vedtaksperiodeNode as ObjectNode
        when (vedtaksperiodeNode["tilstand"].asText()) {
            in inntektsmeldingTilstander -> {
                vedtaksperiode.put("tilstand", "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK")
            }
            in avventerBlokkerendeTilstander -> {
                vedtaksperiode.put("tilstand", "AVVENTER_BLOKKERENDE_PERIODE")
            }
        }
    }

    private fun Iterable<JsonNode>.oppdaterForkastedeIkkeMottattSøknadTilstander() = forEach { vedtaksperiodeNode ->
        val vedtaksperiode = vedtaksperiodeNode as ObjectNode
        val tilstand = vedtaksperiodeNode["tilstand"].asText()
        if (tilstand in tilstanderUtenMottatSøknad) {
            /*
            For forkastede perioder ønsker vi å sette tilstanden til TIL_INFOTRYGD i tilfelle det finnes gamle
            perioder som ikke fikk denne tilstanden da de ble forkastet.
            */
            vedtaksperiode.put("tilstand", "TIL_INFOTRYGD")
        }
    }
}
