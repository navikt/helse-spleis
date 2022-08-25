package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.LoggerFactory

internal class V172LoggeUbrukteVilkårsgrunnlag : JsonMigration(version = 172) {
    override val description = """Logger ubrukte vilkårsgrunnlaginnslag"""

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        val brukteInnslag = jsonNode.path("arbeidsgivere").flatMap { arbeidsgiver ->
            arbeidsgiver.path("beregnetUtbetalingstidslinjer").map { beregning ->
                beregning.path("vilkårsgrunnlagHistorikkInnslagId").asText()
            }
        }.toSet()
        val ubrukteVilkårsgrunnlag = mutableSetOf<String>()
        jsonNode.path("vilkårsgrunnlagHistorikk").forEachIndexed { index, innslag ->
            if (index == 0) return // hopper over første element, da vi alltid vil beholde dette
            val id = innslag.path("id").asText()
            if (id !in brukteInnslag) ubrukteVilkårsgrunnlag.add(id)
        }
        if (ubrukteVilkårsgrunnlag.isEmpty()) return
        sikkerlogg.info("person {} har {} ubrukte vilkårsgrunnlag-innslag", keyValue("aktørId", aktørId), ubrukteVilkårsgrunnlag.size)
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}