package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory

internal class V268ForkasteVilkårsgrunnlagUtenInntekter: JsonMigration(268) {

    override val description = "forkaster vilkårsgrunnlag som har tom liste med arbeidsgiveropplysninger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        val vilkårsgrunnlaghistorikk = jsonNode.path("vilkårsgrunnlagHistorikk") as ArrayNode
        if (vilkårsgrunnlaghistorikk.isEmpty) return

        val nyesteInnslag = vilkårsgrunnlaghistorikk.path(0) as ObjectNode
        val innslagkopi = nyesteInnslag.deepCopy()
        val grunnlag = nyesteInnslag.path("vilkårsgrunnlag") as ArrayNode
        val noenFjernet = grunnlag.removeAll { element ->
            element.path("sykepengegrunnlag").path("arbeidsgiverInntektsopplysninger").isEmpty
        }

        if (noenFjernet) {
            nyesteInnslag.put("opprettet", LocalDateTime.now().toString())
            nyesteInnslag.put("id", UUID.randomUUID().toString())
            vilkårsgrunnlaghistorikk.insert(1, innslagkopi)
            sikkerlogg.info("V268 {} fjerner vilkårsgrunnlag som har tom liste med arbeidsgiverInntektsopplysninger",
                kv("aktørId", jsonNode.path("aktørId").asText()))
        }
    }

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
