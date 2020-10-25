package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

internal class V16StatusIUtbetaling : JsonMigration(version = 16) {

    override val description = "Legger til statusfelt på utbetaling"

    override fun doMigration(jsonNode: ObjectNode) {
        val meldinger = jsonNode["aktivitetslogg"]["aktiviteter"].map {
            it["melding"].textValue() to LocalDateTime.parse(
                it["tidsstempel"].textValue(),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            )
        }.toMap()
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            arbeidsgiver["utbetalinger"].forEach { utbetaling ->
                utbetaling as ObjectNode
                utbetaling.put("status", "IKKE_UTBETALT")
            }
            arbeidsgiver["vedtaksperioder"].forEach { vedtaksperiode ->
                val dager = vedtaksperiode["utbetalingstidslinje"]["dager"].map { it["dato"].textValue() }
                val fom = dager.minOrNull()
                val tom = dager.maxOrNull()

                arbeidsgiver["utbetalinger"]
                    .filter { utbetaling ->
                        utbetaling["utbetalingstidslinje"]["dager"].map { it["dato"].textValue() }
                            .containsAll(listOf(fom, tom))
                    }
                    .forEach { utbetaling ->
                        val time = LocalDateTime.parse(utbetaling["tidsstempel"].textValue())
                        val status =
                            if (vedtaksperiode["tilstand"].textValue() == "AVSLUTTET") "UTBETALT"
                            else if (vedtaksperiode["tilstand"].textValue() == "TIL_INFOTRYGD" && "Invaliderer vedtaksperiode: ${vedtaksperiode["id"].textValue()} på grunn av annullering" in meldinger) {
                                if (meldinger["Invaliderer vedtaksperiode: ${vedtaksperiode["id"].textValue()} på grunn av annullering"]
                                        ?.until(time, ChronoUnit.SECONDS)?.absoluteValue?.let { it == 0L } == true
                                ) "ANNULLERT" else "UTBETALT"
                            } else "IKKE_UTBETALT"

                        utbetaling as ObjectNode
                        utbetaling.put("status", status)
                    }
            }
        }
    }
}
