package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlin.math.roundToInt

internal class V8LeggerTilLønnIUtbetalingslinjer : JsonMigration(version = 8) {
    override val description = "Legger til lønn i utbetalingslinjer basert på dagsats og grad"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            arbeidsgiver["utbetalinger"].forEach { utbetaling ->
                utbetaling["arbeidsgiverOppdrag"]["linjer"].forEach { linje ->
                    linje as ObjectNode
                    val dagsats = linje["dagsats"].asInt()
                    val grad = linje["grad"].asDouble()
                    linje.put("lønn", (dagsats / (grad / 100)).roundToInt())
                }
            }
        }
    }
}
