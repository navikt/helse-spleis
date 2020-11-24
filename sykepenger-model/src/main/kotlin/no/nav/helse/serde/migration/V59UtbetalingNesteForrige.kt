package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V59UtbetalingNesteForrige : JsonMigration(version = 59) {
    override val description: String = "Utvider Utbetaling med forrige/neste-pekere"

    override fun doMigration(jsonNode: ObjectNode) {
        val fagsystemIder = mutableMapOf<String, MutableList<ObjectNode>>()
        jsonNode
            .path("arbeidsgivere")
            .forEach { arbeidsgiver ->
                arbeidsgiver
                    .path("utbetalinger")
                    .map { it as ObjectNode }
                    .forEach {
                        val fagsystemId = it.path("arbeidsgiverOppdrag").path("fagsystemId").asText()
                        fagsystemIder.getOrPut(fagsystemId) { mutableListOf() }.add(it)
                    }
            }

        fagsystemIder.forEach { (fagsystemId, utbetalinger) ->
            utbetalinger.zipWithNext { venstre, høyre ->
                venstre.put("neste", høyre.path("id").asText())
                høyre.put("forrige", venstre.path("id").asText())
            }
        }
    }
}

