package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Periode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.db.HendelseRepository
import org.slf4j.Logger
import java.util.*

internal abstract class HendelseMessage(private val packet: JsonMessage) {
    internal val id: UUID = UUID.fromString(packet["@id"].asText())
    private val navn = packet["@event_name"].asText()
    protected val opprettet = packet["@opprettet"].asLocalDateTime()

    protected abstract val fødselsnummer: String

    internal abstract fun behandle(mediator: IHendelseMediator)

    internal fun lagreMelding(repository: HendelseRepository) {
        repository.lagreMelding(this, fødselsnummer, id, toJson())
    }

    internal fun logReplays(logger: Logger, size: Int) {
        logger.info("som følge av $navn id=$id sendes $size meldinger for replay for fnr=$fødselsnummer")
    }

    internal fun logOutgoingMessages(logger: Logger, size: Int) {
        logger.info("som følge av $navn id=$id sendes $size meldinger på rapid for fnr=$fødselsnummer")
    }

    internal fun logRecognized(logger: Logger) {
        logger.info("gjenkjente melding id={} for fnr={} som {}:\n{}", id, fødselsnummer, this::class.simpleName, toJson())
    }

    internal fun secureDiagnosticinfo() = mapOf(
        "fødselsnummer" to fødselsnummer
    )

    internal fun tracinginfo() = additionalTracinginfo(packet) + mapOf(
        "event_name" to navn,
        "id" to id,
        "opprettet" to opprettet
    )

    protected open fun additionalTracinginfo(packet: JsonMessage): Map<String, Any> = emptyMap()

    internal fun toJson() = packet.toJson()
}

internal fun asPeriode(jsonNode: JsonNode) =
    Periode(jsonNode.path("fom").asLocalDate(), jsonNode.path("tom").asLocalDate())

