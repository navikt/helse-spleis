package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.person.ForlengelseFraInfotrygd

internal class V11LeggeTilForlengelseFraInfotrygd : JsonMigration(version = 11) {

    override val description = "Legger til defaultverdi for \"forlengelseFraInfotrygd\""

    override fun doMigration(jsonNode: ObjectNode) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                (periode as ObjectNode).put("forlengelseFraInfotrygd", ForlengelseFraInfotrygd.IKKE_ETTERSPURT.name)
            }
        }
    }
}
