package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.*

internal class V139EndreTommeInntektsopplysningerTilIkkeRapportert : JsonMigration(version = 139) {
    override val description = "Migrerer tomme inntektsopplysninger i vilkårsgrunnlaget til å være en IkkeRapportert inntekt⁉"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["vilkårsgrunnlagHistorikk"]
            .flatMap { it["vilkårsgrunnlag"] }
            .forEach { vilkårsgrunnlag ->
                val skjæringstidspunkt = vilkårsgrunnlag["skjæringstidspunkt"]
                vilkårsgrunnlag["sykepengegrunnlag"]["arbeidsgiverInntektsopplysninger"]
                    .map { it["inntektsopplysning"] as ObjectNode }
                    .filter { it.isEmpty }
                    .forEach {
                        it.put("id", UUID.randomUUID().toString())
                        it.set<ObjectNode>("dato", skjæringstidspunkt.deepCopy())
                        it.put("kilde", "IKKE_RAPPORTERT")
                        it.put("tidsstempel", LocalDateTime.now().toString())
                    }
            }
    }
}
