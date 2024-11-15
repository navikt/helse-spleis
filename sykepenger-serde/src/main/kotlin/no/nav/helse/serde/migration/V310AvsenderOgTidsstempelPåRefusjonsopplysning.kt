package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID
import org.slf4j.LoggerFactory

internal class V310AvsenderOgTidsstempelPåRefusjonsopplysning: JsonMigration(version = 310) {
    override val description = "legger til avsender og tidsstempel på refusjonsopplysning i inntektsgrunnlaget"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val meldinger = meldingerSupplier.hentMeldinger()
        val fnr = jsonNode.path("fødselsnummer").asText()
        val ukjenteMeldingsreferanseIder = mutableSetOf<UUID>()
        jsonNode.path("vilkårsgrunnlagHistorikk").forEach { historikkinnslag ->
            historikkinnslag.path("vilkårsgrunnlag").forEach { vilkårsgrunnlag ->
                vilkårsgrunnlag.path("inntektsgrunnlag").path("arbeidsgiverInntektsopplysninger").forEach { arbeidsgiverInntektsopplysning ->
                    arbeidsgiverInntektsopplysning.path("refusjonsopplysninger")
                        .filter { refusjonsopplysning ->
                            refusjonsopplysning.path("tidsstempel").isMissingNode || refusjonsopplysning.path("tidsstempel").isNull
                        }
                        .forEach { refusjonsopplysning ->
                            refusjonsopplysning as ObjectNode
                            val meldingsreferanseId = UUID.fromString(refusjonsopplysning.path("meldingsreferanseId").asText())
                            val hendelse = meldinger[meldingsreferanseId]
                            if (hendelse == null) {
                                ukjenteMeldingsreferanseIder.add(meldingsreferanseId)
                            } else {
                                refusjonsopplysning.put("avsender", hendelse.meldingstype.avsender)
                                refusjonsopplysning.put("tidsstempel", "${hendelse.lestDato}")
                            }
                        }
                }
            }
        }

        if (ukjenteMeldingsreferanseIder.isEmpty()) return sikkerlogg.info("Migrert inn komplette refusjonsopplysninger for $fnr")
        sikkerlogg.info("Fant ${ukjenteMeldingsreferanseIder.size} ukjente meldingsreferanser for $fnr: ${ukjenteMeldingsreferanseIder.joinToString()}")
    }

    internal companion object {
        val String.avsender get() = when(this) {
            "INNTEKTSMELDING" -> "ARBEIDSGIVER"
            "OVERSTYRARBEIDSGIVEROPPLYSNINGER",
            "OVERSTYRINNTEKT",
            "GJENOPPLIV_VILKÅRSGRUNNLAG" -> "SAKSBEHANDLER"
            else -> error("Forventet ikke meldingstype $this")
        }

        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
