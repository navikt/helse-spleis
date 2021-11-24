package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.utbetalingstidslinje.decodeUtbetalingsreferanse
import org.slf4j.LoggerFactory
import java.util.*

internal class V125KorrelasjonsIdPåUtbetaling : JsonMigration(version = 125) {

    override val description = "Setter korrelasjonsId på utbetalinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode["arbeidsgivere"].forEach { arbeidsgiver ->
            val erstatningsIder = mutableMapOf<String, UUID>()
            arbeidsgiver["utbetalinger"].forEach { utbetaling ->
                val fagsystemId = utbetaling.path("arbeidsgiverOppdrag").path("fagsystemId").asText()
                val korrelasjonsId =
                    try { decodeUtbetalingsreferanse(fagsystemId) }
                    catch (err: Exception) {
                        logger.warn("Klarte ikke å dekode fagsystemId til uuid; genererer ny random uuid for fagsystem=$fagsystemId")
                        erstatningsIder.getOrPut(fagsystemId) { UUID.randomUUID() }
                    }
                utbetaling as ObjectNode
                utbetaling.put("korrelasjonsId", "$korrelasjonsId")
            }
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger("tjenestekall")
    }
}
