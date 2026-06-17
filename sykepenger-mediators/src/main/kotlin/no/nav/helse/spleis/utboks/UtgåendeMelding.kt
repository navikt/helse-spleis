package no.nav.helse.spleis.utboks

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.collections.associate
import kotlin.collections.plus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.spleis.Behov

data class UtgåendeMelding(
    val key: String?,
    val json: ObjectNode,
    val mottaker: Mottaker
) {
    val id = UUID.fromString(json.path("@id").asText())
    val eventName = json.path("@event_name").asText()

    init {
        check(id.version() == 7) { "id må være en UUIDv7" }
        val fødselnummerFraJson = json.path("fødselsnummer").takeUnless { it.isMissingOrNull() }?.asText()
        if (fødselnummerFraJson != null) {
            check(key == fødselnummerFraJson) { "Når det er satt fødselsnummer i meldingen må den keyes på fødselsnummeret!" }
        }
    }

    enum class Mottaker { RAPID, SUBSUMSJON }

    internal constructor(key: String?, json: String, mottaker: Mottaker) : this(key, objectmapper.readValue<ObjectNode>(json), mottaker)

    companion object {
        private val objectmapper = jacksonObjectMapper()

        @OptIn(ExperimentalUuidApi::class)
        private fun nyUuidv7() = Uuid.generateV7().toString()
        private fun standardfelter(personidentifikator: Personidentifikator): Map<String, Any> {
            val opprettet = LocalDateTime.now(ZoneId.systemDefault())
            val opprettetUTC = opprettet.atZone(ZoneId.systemDefault()).toInstant()
            return mapOf(
                "fødselsnummer" to "$personidentifikator",
                "@opprettet" to "$opprettet",
                "@opprettetUTC" to "$opprettetUTC"
            )
        }

        private fun ny(personidentifikator: Personidentifikator, eventName: String, innhold: Map<String, Any>, mottaker: Mottaker): UtgåendeMelding {
            val id = nyUuidv7()
            val innholdMedStandardfelter = innhold + standardfelter(personidentifikator)
            val json = JsonMessage.newMessage(eventName, innholdMedStandardfelter) { id }.toJson()
            return UtgåendeMelding(
                key = personidentifikator.toString(),
                json = json,
                mottaker = mottaker
            )
        }

        fun nyRapidmelding(personidentifikator: Personidentifikator, eventName: String, innhold: Map<String, Any>) = ny(personidentifikator, eventName, innhold, Mottaker.RAPID)
        fun nySubsumsjonsmelding(personidentifikator: Personidentifikator, innhold: Map<String, Any>) = ny(personidentifikator, "subsumsjon", innhold, Mottaker.SUBSUMSJON)
        fun nyttBehov(
            personidentifikator: Personidentifikator,
            meldingsreferanseId: MeldingsreferanseId, // TODO: Dette feltet er sendt på alle behov, men tror ingen sparkel-apper bruker det, må sjekkes opp i!
            behov: List<Behov>,
            extra: Map<String, Any> = emptyMap()
        ): UtgåendeMelding {
            val extraMedStandardfelter = extra + standardfelter(personidentifikator) + ("meldingsreferanseId" to meldingsreferanseId.id)
            val json = JsonMessage.newNeed(
                behov = behov.map { it.type.utgåendeNavn },
                map = behov.associate { it.type.utgåendeNavn to it.input } + extraMedStandardfelter,
                randomIdGenerator = { nyUuidv7() }
            ).toJson()

            return UtgåendeMelding(
                key = personidentifikator.toString(),
                json = json,
                mottaker = Mottaker.RAPID
            )
        }
    }
}
