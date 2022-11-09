package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.slf4j.LoggerFactory

internal class V194RefusjonsopplysningerIVilkårsgrunnlagDryRun: JsonMigration(version = 194) {

    override val description =
        "Logger vilkårsgrunnlag før og etter det blir lagt til refusjonsopplysninger"

    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val aktørId = jsonNode.path("aktørId").asText()
        try {
            val vilkårsgrunnlagHistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode
            val gjeldendeVilkårsgrunnlagInnslag = vilkårsgrunnlagHistorikk.firstOrNull() ?: return
            val vilkårsgrunnlagFør = gjeldendeVilkårsgrunnlagInnslag.path("vilkårsgrunnlag").takeUnless { it.isEmpty } ?: return
            val vilkårsgrunnlagEtter = RefusjonsopplysningerIVilkårsgrunnlag.vilkårsgrunnlagMedRefusjonsopplysninger(jsonNode) ?: return

            sikkerlogg.info("Hadde oppdatert vilkårsgrunnlag for {}:\nfra\n\t$vilkårsgrunnlagFør\ntil\n\t$vilkårsgrunnlagEtter", keyValue("aktørId", aktørId))

            if ("${vilkårsgrunnlagFør.map { it.path("sykepengegrunnlag")}}".contains("IKKE_RAPPORTERT")){
                sikkerlogg.info("Sykepengegrunnlag med ikke rapportert inntekt for {}", keyValue("aktørId", aktørId))
            }
        } catch (e: Exception) {
            sikkerlogg.error("Feil ved migrering 194 for {}", keyValue("aktørId", aktørId), e)
        }
    }
}