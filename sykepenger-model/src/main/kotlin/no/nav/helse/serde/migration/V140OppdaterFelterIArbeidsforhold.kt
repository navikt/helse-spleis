package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V140OppdaterFelterIArbeidsforhold : JsonMigration(version = 140) {
    override val description: String = "Fjerner orgnummer, legger til erAktivt og renamer fom og tom til ansattFom og ansattTom i arbeidsforholdshistorikken"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"]
            .flatMap { it["arbeidsforholdhistorikk"] }
            .flatMap { it["arbeidsforhold"] }
            .map { it as ObjectNode }
            .forEach {
                val ansattFom = it.remove("fom")
                val ansattTom = it.remove("tom")
                it.remove("orgnummer")
                it.set<ObjectNode>("ansattFom", ansattFom)
                it.set<ObjectNode>("ansattTom", ansattTom)
                it.put("erAktivt", true)
            }
    }
}
