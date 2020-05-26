package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

internal class V17ForkastedePerioder : JsonMigration(version = 17) {

    override val description = "Legger til forkastede perioder"

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            val index = arbeidsgiver["vedtaksperioder"].indexOfLast { vedtaksperiode ->
                vedtaksperiode["tilstand"].textValue() == "TIL_INFOTRYGD"
            }
            arbeidsgiver.withArray<ArrayNode>("forkastede")

            if (index < 0) return@forEach

            val forkastede = arbeidsgiver["vedtaksperioder"].toList().subList(0, index + 1)
            val perioder = arbeidsgiver["vedtaksperioder"].toList().run { subList(index + 1, size) }

            arbeidsgiver.withArray<ArrayNode>("vedtaksperioder").apply {
                removeAll()
                addAll(perioder)
            }
            arbeidsgiver.withArray<ArrayNode>("forkastede").addAll(forkastede)
        }
    }
}
