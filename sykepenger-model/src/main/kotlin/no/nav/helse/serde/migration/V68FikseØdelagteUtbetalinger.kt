package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.LoggerFactory

internal class V68FikseØdelagteUtbetalinger : JsonMigration(version = 68) {
    override val description: String = "Fikser ødelagt utbetaling"
    private val log = LoggerFactory.getLogger("tjenestekall")

    private val fikser = mapOf<String, (JsonNode) -> Unit>(
        "53132067-543c-49d2-883b-04113bdb06cd" to ::fikseCase1,
        "0a00d06b-60bd-42d9-9e59-b070bfb96ef7" to ::fikseCase2,
        "cc274bd6-2874-480a-92f1-401e3d8f8015" to ::fikseCase3,
        "cf09fc55-b793-4203-a872-502479da1017" to ::fikseCase4,
        "5fc09ab9-5239-46aa-8ed2-cc4229d305d5" to ::fikseCase5,
        "f7e61fce-38f9-4cee-b1ec-a7e30a5e31a6" to ::fikseCase6,
        "d372e621-e1ef-4e0c-a335-965d9d8d1f7c" to ::fikseCase7,
        "26b97a8e-e424-4cdf-bb8b-13fb01ae9296" to ::fikseCase8,
    )

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode
            .path("arbeidsgivere")
            .forEach { arbeidsgiver ->
                arbeidsgiver
                    .path("utbetalinger")
                    .onEach { utbetaling ->
                        val id = utbetaling.path("id").asText()
                        fikser[id]?.also { fiks ->
                            log.info("Fikser ødelagt utbetaling {}", keyValue("utbetalingId", id))
                            fiks(utbetaling)
                        }
                    }
            }
    }

    private fun fikseCase1(utbetaling: JsonNode) {
        val oppdrag = utbetaling.path("arbeidsgiverOppdrag")
        val fagsystemId = oppdrag.path("fagsystemId").asText()
        val linjer = oppdrag.path("linjer")
        linjer as ArrayNode
        linjer.addObject().also { linje ->
            linje.put("fom", "2020-09-19")
            linje.put("tom", "2020-09-21")
            linje.put("dagsats", 2339)
            linje.put("lønn", 2850)
            linje.put("grad", 100)
            linje.put("refFagsystemId", fagsystemId)
            linje.put("delytelseId", 2)
            linje.put("refDelytelseId", 1)
            linje.put("endringskode", "NY")
            linje.put("klassekode", "SPREFAG-IOP")
            linje.putNull("datoStatusFom")
            linje.putNull("statuskode")
        }
    }

    private fun fikseCase2(utbetaling: JsonNode) {
        val oppdrag = utbetaling.path("arbeidsgiverOppdrag")
        val fagsystemId = oppdrag.path("fagsystemId").asText()
        val linjer = oppdrag.path("linjer")
        linjer as ArrayNode
        linjer.addObject().also { linje ->
            linje.put("fom", "2020-09-19")
            linje.put("tom", "2020-09-22")
            linje.put("dagsats", 1871)
            linje.put("lønn", 2595)
            linje.put("grad", 80)
            linje.put("refFagsystemId", fagsystemId)
            linje.put("delytelseId", 2)
            linje.put("refDelytelseId", 1)
            linje.put("endringskode", "NY")
            linje.put("klassekode", "SPREFAG-IOP")
            linje.putNull("datoStatusFom")
            linje.putNull("statuskode")
        }
    }

    private fun fikseCase3(utbetaling: JsonNode) {
        val oppdrag = utbetaling.path("arbeidsgiverOppdrag")
        val fagsystemId = oppdrag.path("fagsystemId").asText()
        val linjer = oppdrag.path("linjer")
        linjer as ArrayNode
        linjer.addObject().also { linje ->
            linje.put("fom", "2020-09-19")
            linje.put("tom", "2020-09-21")
            linje.put("dagsats", 2339)
            linje.put("lønn", 2727)
            linje.put("grad", 100)
            linje.put("refFagsystemId", fagsystemId)
            linje.put("delytelseId", 2)
            linje.put("refDelytelseId", 1)
            linje.put("endringskode", "NY")
            linje.put("klassekode", "SPREFAG-IOP")
            linje.putNull("datoStatusFom")
            linje.putNull("statuskode")
        }
    }

    private fun fikseCase4(utbetaling: JsonNode) {
        val oppdrag = utbetaling.path("arbeidsgiverOppdrag")
        val fagsystemId = oppdrag.path("fagsystemId").asText()
        val linjer = oppdrag.path("linjer")
        linjer as ArrayNode
        linjer.addObject().also { linje ->
            linje.put("fom", "2020-09-19")
            linje.put("tom", "2020-09-22")
            linje.put("dagsats", 2339)
            linje.put("lønn", 2529)
            linje.put("grad", 100)
            linje.put("refFagsystemId", fagsystemId)
            linje.put("delytelseId", 2)
            linje.put("refDelytelseId", 1)
            linje.put("endringskode", "NY")
            linje.put("klassekode", "SPREFAG-IOP")
            linje.putNull("datoStatusFom")
            linje.putNull("statuskode")
        }
    }

    private fun fikseCase5(utbetaling: JsonNode) {
        val oppdrag = utbetaling.path("arbeidsgiverOppdrag")
        val fagsystemId = oppdrag.path("fagsystemId").asText()
        val linjer = oppdrag.path("linjer")
        linjer as ArrayNode
        linjer.addObject().also { linje ->
            linje.put("fom", "2020-09-19")
            linje.put("tom", "2020-09-22")
            linje.put("dagsats", 2339)
            linje.put("lønn", 3000)
            linje.put("grad", 100)
            linje.put("refFagsystemId", fagsystemId)
            linje.put("delytelseId", 2)
            linje.put("refDelytelseId", 1)
            linje.put("endringskode", "NY")
            linje.put("klassekode", "SPREFAG-IOP")
            linje.putNull("datoStatusFom")
            linje.putNull("statuskode")
        }
    }

    private fun fikseCase6(utbetaling: JsonNode) {
        val oppdrag = utbetaling.path("arbeidsgiverOppdrag")
        val fagsystemId = oppdrag.path("fagsystemId").asText()
        val linjer = oppdrag.path("linjer")
        linjer as ArrayNode
        linjer.addObject().also { linje ->
            linje.put("fom", "2020-09-19")
            linje.put("tom", "2020-09-21")
            linje.put("dagsats", 1170)
            linje.put("lønn", 3611)
            linje.put("grad", 50)
            linje.put("refFagsystemId", fagsystemId)
            linje.put("delytelseId", 2)
            linje.put("refDelytelseId", 1)
            linje.put("endringskode", "NY")
            linje.put("klassekode", "SPREFAG-IOP")
            linje.putNull("datoStatusFom")
            linje.putNull("statuskode")
        }
    }

    private fun fikseCase7(utbetaling: JsonNode) {
        val oppdrag = utbetaling.path("arbeidsgiverOppdrag")
        val fagsystemId = oppdrag.path("fagsystemId").asText()
        val linjer = oppdrag.path("linjer")
        linjer as ArrayNode
        linjer.addObject().also { linje ->
            linje.put("fom", "2020-09-19")
            linje.put("tom", "2020-09-21")
            linje.put("dagsats", 1403)
            linje.put("lønn", 3121)
            linje.put("grad", 60)
            linje.put("refFagsystemId", fagsystemId)
            linje.put("delytelseId", 3)
            linje.put("refDelytelseId", 2)
            linje.put("endringskode", "NY")
            linje.put("klassekode", "SPREFAG-IOP")
            linje.putNull("datoStatusFom")
            linje.putNull("statuskode")
        }
    }

    private fun fikseCase8(utbetaling: JsonNode) {
        val oppdrag = utbetaling.path("arbeidsgiverOppdrag")
        val fagsystemId = oppdrag.path("fagsystemId").asText()
        val linjer = oppdrag.path("linjer")
        linjer as ArrayNode
        linjer.addObject().also { linje ->
            linje.put("fom", "2020-09-19")
            linje.put("tom", "2020-09-21")
            linje.put("dagsats", 2339)
            linje.put("lønn", 3301)
            linje.put("grad", 100)
            linje.put("refFagsystemId", fagsystemId)
            linje.put("delytelseId", 2)
            linje.put("refDelytelseId", 1)
            linje.put("endringskode", "NY")
            linje.put("klassekode", "SPREFAG-IOP")
            linje.putNull("datoStatusFom")
            linje.putNull("statuskode")
        }
    }
}
