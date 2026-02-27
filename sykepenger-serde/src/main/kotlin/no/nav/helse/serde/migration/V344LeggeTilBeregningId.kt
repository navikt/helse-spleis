package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import no.nav.helse.serde.serdeObjectMapper

internal interface UuidGenerator {
    fun generate(tidsstempel: Instant): UUID
    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    object UuidV7BasertPåTidsstempelGenerator: UuidGenerator {
        override fun generate(tidsstempel: Instant) = Uuid.generateV7NonMonotonicAt(tidsstempel).toJavaUuid()
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
            var forrigeUtbetalingId: String? = null
            var gjeldendeBeregningId: String? = null
            behandling.path("endringer").forEach { endring ->
                val denneUtbetalingId = endring.path("utbetalingId").takeUnless { it.isNull || it.isMissingNode }?.asText()
                if (gjeldendeBeregningId == null || (forrigeUtbetalingId != null && forrigeUtbetalingId != denneUtbetalingId)) {
                    val tidsstempel = endring.path("tidsstempel").localDateTime().toKotlinInstant()
                    gjeldendeBeregningId = uuidGenerator.generate(tidsstempel).toString()
                }
                endring as ObjectNode
                endring.put("beregningId", gjeldendeBeregningId)
                forrigeUtbetalingId = denneUtbetalingId
            }
        }
    }

    companion object {
        fun JsonNode.localDateTime() = serdeObjectMapper.readValue<LocalDateTime>(toString())
        fun LocalDateTime.toKotlinInstant() = atZone(ZoneId.systemDefault()).toInstant().toKotlinInstant()
    }
}
