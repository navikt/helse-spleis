package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.serde.migration.BrukteVilkårsgrunnlag.brukteVilkårsgrunnlag
import org.slf4j.LoggerFactory

internal class V176LoggingAvForkastOgFlyttVilkårsgrunnlag : JsonMigration(version = 176) {

    override val description =
        "Logger migreringslogikk for å: " +
            "1. forkaste vilkårsgrunnlag som ikke er brukt av en vedtaksperiode" +
            "2. flytte vilkårsgrunnlag til skjæringstidspunkt, f.eks. fordi det var lagret på første utbetalingsdag i infotrygd"

    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        try {
            brukteVilkårsgrunnlag(jsonNode)
        } catch (e: Exception) {
            sikkerlogg.error("Feil ved utleding av relevante vilkårsgrunnlag for aktørId=${jsonNode.path("aktørId").asText()}", e)
        }
    }
}