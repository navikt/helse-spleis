package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import org.slf4j.LoggerFactory

internal class V312AvsenderOgTidsstempelPåRefusjonsopplysningForDeaktiverteArbeidsforhold: JsonMigration(version = 312) {
    override val description = "legger til avsender og tidsstempel på refusjonsopplysning i deaktiverte arbedisforhold"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val meldinger = meldingerSupplier.hentMeldinger()
        val fnr = jsonNode.path("fødselsnummer").asText()
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { historikkinnslag ->
            historikkinnslag.path("vilkårsgrunnlag").forEach { vilkårsgrunnlag ->
                vilkårsgrunnlag.path("inntektsgrunnlag").path("deaktiverteArbeidsforhold").forEach { arbeidsgiverInntektsopplysning ->
                    val fallbackTidsstempel = LocalDateTime.parse(arbeidsgiverInntektsopplysning.path("inntektsopplysning").path("tidsstempel").asText())
                    arbeidsgiverInntektsopplysning.path("refusjonsopplysninger")
                        .filter { refusjonsopplysning ->
                            refusjonsopplysning.path("tidsstempel").isMissingNode || refusjonsopplysning.path("tidsstempel").isNull
                        }
                        .forEach { refusjonsopplysning ->
                            refusjonsopplysning as ObjectNode
                            val meldingsreferanseId = UUID.fromString(refusjonsopplysning.path("meldingsreferanseId").asText())
                            val hendelse = meldinger[meldingsreferanseId]
                            if (hendelse == null) {
                                refusjonsopplysning.put("avsender", "ARBEIDSGIVER")
                                refusjonsopplysning.put("tidsstempel", "$fallbackTidsstempel")
                                sikkerlogg.info("Fant ikke melding med meldingsreferanse $meldingsreferanseId for person $fnr. Defaulter til ARBEIDSGIVER og tidsstempel for inntektsopplysning")
                            } else {
                                refusjonsopplysning.put("avsender", hendelse.meldingstype.avsender)
                                refusjonsopplysning.put("tidsstempel", "${hendelse.lestDato}")
                            }
                        }
                }
            }
        }
    }

    internal companion object {
        val String.avsender get() = when(this) {
            "INNTEKTSMELDING" -> "ARBEIDSGIVER"
            "OVERSTYRARBEIDSGIVEROPPLYSNINGER",
            "OVERSTYRINNTEKT",
            "MANUELL_SAKSBEHANDLING",
            "GJENOPPLIV_VILKÅRSGRUNNLAG" -> "SAKSBEHANDLER"
            else -> "ARBEIDSGIVER".also { sikkerlogg.info("Mappet meldingstype $this til avsender ARBEIDSGIVER") }
        }

        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
