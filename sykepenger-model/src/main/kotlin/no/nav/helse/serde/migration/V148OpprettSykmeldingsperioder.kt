package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V148OpprettSykmeldingsperioder : JsonMigration(version = 148) {
    override val description: String = "Opprett sykmeldingsperioder-array på arbeidsgiver nivå"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .map { it as ObjectNode }
            .forEach { it.putArray("sykmeldingsperioder") }
    }
}
