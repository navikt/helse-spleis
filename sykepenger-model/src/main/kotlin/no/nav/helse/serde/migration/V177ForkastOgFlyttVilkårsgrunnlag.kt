package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.serde.migration.BrukteVilkårsgrunnlag.brukteVilkårsgrunnlag
import no.nav.helse.serde.serdeObjectMapper

internal class V177ForkastOgFlyttVilkårsgrunnlag: JsonMigration(version = 177) {

    override val description =
        "Rydder opp i vilkårsgrunnlag ved å:" +
                "1. forkaste vilkårsgrunnlag som ikke er brukt av en vedtaksperiode" +
                "2. flytte vilkårsgrunnlag til skjæringstidspunkt, f.eks. fordi det var lagret på første utbetalingsdag i infotrygd"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val brukteVilkårsgrunnlag = brukteVilkårsgrunnlag(jsonNode) ?: return
        val vilkårsgrunnlagHistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode
        val nyttInnslag = serdeObjectMapper.createObjectNode().apply {
            put("id", "${UUID.randomUUID()}")
            put("opprettet", "${LocalDateTime.now()}")
            putArray("vilkårsgrunnlag").addAll(brukteVilkårsgrunnlag)
        }

        vilkårsgrunnlagHistorikk.insert(0, nyttInnslag)
    }
}