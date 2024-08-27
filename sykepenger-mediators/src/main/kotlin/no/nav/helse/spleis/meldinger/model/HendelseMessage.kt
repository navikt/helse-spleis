package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.hendelser.Periode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import no.nav.helse.somPersonidentifikator
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.db.HendelseRepository
import org.slf4j.Logger

internal abstract class HendelseMessage(private val packet: JsonMessage) {
    internal val id: UUID = UUID.fromString(packet["@id"].asText())
    private val navn = packet["@event_name"].asText()
    protected val opprettet = packet["@opprettet"].asLocalDateTime()
    internal open val skalDuplikatsjekkes = true

    protected abstract val fødselsnummer: String

    internal abstract fun behandle(mediator: IHendelseMediator, context: MessageContext)

    internal fun lagreMelding(repository: HendelseRepository) {
        repository.lagreMelding(this, fødselsnummer.somPersonidentifikator(), id, toJson())
    }

    internal fun logReplays(logger: Logger, size: Int) {
        logger.info("som følge av $navn id=$id sendes $size meldinger for replay for fnr=$fødselsnummer")
    }

    internal fun logOutgoingMessages(logger: Logger, size: Int) {
        logger.info("som følge av $navn id=$id sendes $size meldinger på rapid for fnr=$fødselsnummer")
    }

    internal fun logRecognized(insecureLog: Logger, safeLog: Logger) {
        insecureLog.info("gjenkjente {} med id={}", this::class.simpleName, id)
        safeLog.info("gjenkjente {} med id={} for fnr={}:\n{}", this::class.simpleName, id, fødselsnummer, toJson())
    }

    internal fun logDuplikat(logger: Logger) {
        logger.warn("Har mottatt duplikat {} med id={} for fnr={}", this::class.simpleName, id, fødselsnummer)
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

