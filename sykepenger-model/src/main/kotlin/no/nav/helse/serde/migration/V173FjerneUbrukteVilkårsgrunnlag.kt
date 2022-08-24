package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.person.AktivitetsloggObserver
import no.nav.helse.serde.serdeObjectMapper
import org.slf4j.LoggerFactory

internal class V173FjerneUbrukteVilkårsgrunnlag : JsonMigration(version = 173) {
    override val description = """Fjerner ubrukte vilkårsgrunnlaginnslag"""

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier, observer: AktivitetsloggObserver) {
        val aktørId = jsonNode.path("aktørId").asText()
        val brukteInnslag = jsonNode.path("arbeidsgivere").flatMap { arbeidsgiver ->
            arbeidsgiver.path("beregnetUtbetalingstidslinjer").map { beregning ->
                beregning.path("vilkårsgrunnlagHistorikkInnslagId").asText()
            }
        }.toSet()
        val ubrukteVilkårsgrunnlag = mutableSetOf<String>()
        jsonNode.path("vilkårsgrunnlagHistorikk").forEachIndexed { index, innslag ->
            if (index == 0) return@forEachIndexed // hopper over første element, da vi alltid vil beholde dette
            val id = innslag.path("id").asText()
            if (id !in brukteInnslag) ubrukteVilkårsgrunnlag.add(id)
        }
        if (ubrukteVilkårsgrunnlag.isEmpty()) return
        sikkerlogg.info("fjerner {} ubrukte vilkårsgrunnlag-innslag for person {}:\n{}", ubrukteVilkårsgrunnlag.size, keyValue("aktørId", aktørId), ubrukteVilkårsgrunnlag)
        val kopi = jsonNode.path("vilkårsgrunnlagHistorikk").filterNot { innslag ->
            innslag.path("id").asText() in ubrukteVilkårsgrunnlag
        }
        jsonNode.replace("vilkårsgrunnlagHistorikk", serdeObjectMapper.createArrayNode().addAll(kopi))
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}