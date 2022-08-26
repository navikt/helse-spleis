package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V142DeaktiverteArbeidsforholdPåSykepengegrunnlag : JsonMigration(version = 142) {
    override val description: String = "Legger inn deaktiverte arbeidsforhold på sykepengegrunnlag"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["vilkårsgrunnlagHistorikk"]
            .flatMap { it["vilkårsgrunnlag"] }
            .forEach {
                it["sykepengegrunnlag"].withArray<ObjectNode>("deaktiverteArbeidsforhold")
            }
    }
}
