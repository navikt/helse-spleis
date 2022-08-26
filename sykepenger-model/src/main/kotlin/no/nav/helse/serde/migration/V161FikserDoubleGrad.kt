package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlin.math.roundToInt

internal class V161FikserDoubleGrad : JsonMigration(version = 161) {
    override val description = "Spisset migrering for å runde av alle grader med desimal. Grad oppgis bare som heltall i søknad, sykmelding og overstyringer." +
            "Desimalet skyldes unøyaktigheter med doubles."


    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("utbetalinger").forEach { utbetaling ->
                migrerUtbetalingstidslinje(utbetaling.path("utbetalingstidslinje"))
            }
            arbeidsgiver.path("beregnetUtbetalingstidslinjer").forEach { beregnetUtbetalingstidslinje ->
                migrerUtbetalingstidslinje(beregnetUtbetalingstidslinje.path("utbetalingstidslinje"))
            }
            arbeidsgiver.path("vedtaksperioder").forEach { vedtaksperiode ->
                migrerUtbetalingstidslinje(vedtaksperiode.path("utbetalingstidslinje"))
            }
        }
    }

    private fun migrerUtbetalingstidslinje(tidslinje: JsonNode) {
        tidslinje.path("dager")
            .forEach { dag ->
                val gradFør = dag.path("grad").asDouble()
                val gradEtter = gradFør.roundToInt().toDouble()
                if (gradFør != gradEtter) (dag as ObjectNode).put("grad", gradEtter)
            }
    }
}