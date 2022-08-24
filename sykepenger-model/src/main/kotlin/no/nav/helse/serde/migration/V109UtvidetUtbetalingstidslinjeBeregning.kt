package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.person.AktivitetsloggObserver
import org.slf4j.LoggerFactory

internal class V109UtvidetUtbetalingstidslinjeBeregning : JsonMigration(version = 109) {
    override val description: String = "UtbetalingstidslinjeBeregning peker på innslag i inntektshistorikk og vilkårsgrunnlag-historikk"
    private val NULLUUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    override fun doMigration(
        jsonNode: ObjectNode,
        meldingerSupplier: MeldingerSupplier,
        observer: AktivitetsloggObserver
    ) {
        val aktørId = jsonNode["aktørId"].asText()
        val vilkårsgrunnlagHistorikkInnslagId = jsonNode["vilkårsgrunnlagHistorikk"].firstOrNull()?.path("id")?.asText()
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            val beregnetUtbetalingstidslinjer = arbeidsgiver["beregnetUtbetalingstidslinjer"]
            if (beregnetUtbetalingstidslinjer.isEmpty) return@forEach
            val inntektshistorikkInnslagId = arbeidsgiver["inntektshistorikk"].firstOrNull()?.path("id")?.asText()
            beregnetUtbetalingstidslinjer.forEach {
                val beregning = it as ObjectNode
                val faktiskVilkårsgrunnlagHistorikkInnslagId =
                    if (vilkårsgrunnlagHistorikkInnslagId != null) vilkårsgrunnlagHistorikkInnslagId
                    else {
                        sikkerlogg.info("Person med {} har utbetalingstidslinjeberegning men mangler vilkårsgrunnlag.", keyValue("aktørId", aktørId))
                        NULLUUID.toString()
                    }
                val faktiskInntektshistorikkInnslagId =
                    if (inntektshistorikkInnslagId != null) inntektshistorikkInnslagId
                    else {
                        sikkerlogg.info("Person med {} har utbetalingstidslinjeberegning men mangler inntektshistorikk.", keyValue("aktørId", aktørId))
                        NULLUUID.toString()
                    }
                beregning.put("vilkårsgrunnlagHistorikkInnslagId", faktiskVilkårsgrunnlagHistorikkInnslagId)
                beregning.put("inntektshistorikkInnslagId", faktiskInntektshistorikkInnslagId)
            }
        }
    }
}
