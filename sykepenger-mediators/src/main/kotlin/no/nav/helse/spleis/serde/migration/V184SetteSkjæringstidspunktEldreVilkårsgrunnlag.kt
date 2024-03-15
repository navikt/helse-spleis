package no.nav.helse.spleis.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDate
import java.util.UUID
import org.slf4j.LoggerFactory

internal class V184SetteSkjæringstidspunktEldreVilkårsgrunnlag: JsonMigration(184) {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
    override val description = "Setter skjæringstidspunktene som ble satt i V178, men på eldre innslag"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        val vilkårsgrunnlagskjæringstidspunkt = mutableMapOf<UUID, LocalDate>()
        jsonNode
            .path("vilkårsgrunnlagHistorikk")
            .forEach { innslag ->
                innslag.path("vilkårsgrunnlag").forEach { vilkårsgrunnlag ->
                    val id = UUID.fromString(vilkårsgrunnlag.path("vilkårsgrunnlagId").asText())
                    val skjæringstidspunktForNåværendeGrunnlag = LocalDate.parse(vilkårsgrunnlag.path("skjæringstidspunkt").asText())
                    val skjæringstidspunkt = vilkårsgrunnlagskjæringstidspunkt[id]
                    if (skjæringstidspunkt == null) {
                        vilkårsgrunnlagskjæringstidspunkt[id] = skjæringstidspunktForNåværendeGrunnlag
                    } else if (skjæringstidspunkt != skjæringstidspunktForNåværendeGrunnlag) {
                        sikkerlogg.info("overskriver eldre skjæringstidspunkt=$skjæringstidspunktForNåværendeGrunnlag til $skjæringstidspunkt for vilkårsgrunnlagId=$id for aktørId=$aktørId")
                        (vilkårsgrunnlag as ObjectNode).put("skjæringstidspunkt", skjæringstidspunkt.toString())
                    }
                }
            }
    }
}