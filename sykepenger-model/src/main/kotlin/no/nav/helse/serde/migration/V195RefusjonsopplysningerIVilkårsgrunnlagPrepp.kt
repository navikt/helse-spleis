package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode

internal class V195RefusjonsopplysningerIVilkårsgrunnlagPrepp : JsonMigration(version = 195) {
    override val description: String = "Legger inn tomme refusjonsopplysninger på arbeidsgiverinntektsopplysninger og deaktiverte arbeidsforhold på sykepengegrunnlaget"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["vilkårsgrunnlagHistorikk"]
            .flatMap { it["vilkårsgrunnlag"] }
            .forEach { vilkårsgrunnlag ->
                val sykepengegrunnlag = vilkårsgrunnlag["sykepengegrunnlag"]
                sykepengegrunnlag.path("arbeidsgiverInntektsopplysninger").forEach {
                    it as ObjectNode
                    it.putArray("refusjonsopplysninger")
                }
                sykepengegrunnlag.path("deaktiverteArbeidsforhold").forEach {
                    it as ObjectNode
                    it.putArray("refusjonsopplysninger")
                }
            }
    }
}
