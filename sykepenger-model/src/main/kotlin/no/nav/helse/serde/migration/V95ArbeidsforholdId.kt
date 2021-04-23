package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V95ArbeidsforholdId : JsonMigration(version = 95) {
    override val description = "Flytter inntektsmeldingId og legger til arbeidsforholdId inn i InntektsmeldingInfo"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
    }
}
