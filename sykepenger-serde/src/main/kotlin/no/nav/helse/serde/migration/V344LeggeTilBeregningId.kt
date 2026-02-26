package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal interface UuidGenerator {
    fun generate(tidsstempel: kotlin.time.Instant): UUID
    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    object UuidV7BasertPåTidsstempelGenerator: UuidGenerator {
        override fun generate(tidsstempel: kotlin.time.Instant) = Uuid.generateV7NonMonotonicAt(tidsstempel).toJavaUuid()
    }
}

internal class V344LeggeTilBeregningId(private val uuidGenerator: UuidGenerator = UuidGenerator.UuidV7BasertPåTidsstempelGenerator) : JsonMigration(344) {

    override val description = "Legger til beregningId på alle behandlinger"

    override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
        jsonNode.path("arbeidsgivere").forEach { arbeidsgiver ->
            arbeidsgiver.path("vedtaksperioder").forEach { periode ->
                migrerVedtaksperiode(periode)
            }
            arbeidsgiver.path("forkastede").forEach { forkastet ->
                migrerVedtaksperiode(forkastet.path("vedtaksperiode"))
            }
        }
    }

    private fun migrerVedtaksperiode(vedtaksperiode: JsonNode) {
        vedtaksperiode.path("behandlinger").forEach { behandling ->
            var trengerNy = true
            var nesteBeregningId: String? = null
            behandling.path("endringer").forEach { endring ->
                val denneUtbetalingId = endring.path("utbetalingId").takeUnless { it.isNull || it.isMissingNode }?.asText()
                endring as ObjectNode
                if (trengerNy) {
                    val tidsstempel = LocalDateTime.parse(endring.path("tidsstempel").asText()).toKotlinInstant()
                    nesteBeregningId = uuidGenerator.generate(tidsstempel).toString()
                }
                endring.put("beregningId", nesteBeregningId!!)
                trengerNy = denneUtbetalingId != null
            }
        }
    }

    companion object {
        fun LocalDateTime.toKotlinInstant() = atZone(ZoneId.systemDefault()).toInstant().toKotlinInstant()
    }
}
