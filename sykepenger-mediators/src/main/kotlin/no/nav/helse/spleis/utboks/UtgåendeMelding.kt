package no.nav.helse.spleis.utboks

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
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
    val opprettet = Instant.parse(json.path("@opprettetUTC").asText())

    init {
        check(id.version() == 7) { "id må være en UUIDv7" }
        val fødselnummerFraJson = json.path("fødselsnummer").takeUnless { it.isMissingOrNull() }?.asText()
        if (fødselnummerFraJson != null) {
            check(key == fødselnummerFraJson) { "Når det er satt fødselsnummer i meldingen må den keyes på fødselsnummeret!" }
        }
    }

    enum class Mottaker { RAPID, SUBSUMSJON }

    constructor(key: String?, json: String, mottaker: Mottaker) : this(key, objectmapper.readValue<ObjectNode>(json), mottaker)

    companion object {
        private val objectmapper = jacksonObjectMapper()

        @OptIn(ExperimentalUuidApi::class)
        private fun nyUuidv7() = Uuid.generateV7().toString()

        private class Tidsstempler() {
            val oslo: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault())
            val zoned = oslo.atZone(ZoneId.systemDefault())
            val utc = zoned.toInstant()
        }

        private fun standardfelter(personidentifikator: Personidentifikator?, tidsstempler: Tidsstempler = Tidsstempler()): Map<String, Any> {
            val tidsstempler = mapOf(
                "@opprettet" to "${tidsstempler.oslo}",
                "@opprettetUTC" to "${tidsstempler.utc}"
            )
            return when (personidentifikator) {
                null -> tidsstempler
                else -> tidsstempler + ("fødselsnummer" to personidentifikator.toString())
            }
        }

        private fun ny(personidentifikator: Personidentifikator?, eventName: String, innhold: Map<String, Any>, mottaker: Mottaker): UtgåendeMelding {
            val innholdMedStandardfelter = innhold + standardfelter(personidentifikator)
            val json = JsonMessage.newMessage(eventName, innholdMedStandardfelter) { nyUuidv7() }.toJson()
            return UtgåendeMelding(
                key = personidentifikator?.toString(),
                json = json,
                mottaker = mottaker
            )
        }

        fun nyRapidmelding(personidentifikator: Personidentifikator, eventName: String, innhold: Map<String, Any>) = ny(personidentifikator, eventName, innhold, Mottaker.RAPID)
        fun nyRapidmelding(eventName: String, innhold: Map<String, Any>) = ny(null, eventName, innhold, Mottaker.RAPID)

        fun nySubsumsjonsmelding(personidentifikator: Personidentifikator, innhold: (id: String, tidsstempel: ZonedDateTime) -> Map<String, Any>): UtgåendeMelding {
            val id = nyUuidv7()
            val tidsstempler = Tidsstempler()
            val innholdMedStandardfelter = innhold(id, tidsstempler.zoned) + standardfelter(personidentifikator, tidsstempler)
            val json = JsonMessage.newMessage("subsumsjon", innholdMedStandardfelter) { id }.toJson()
            return UtgåendeMelding(
                key = personidentifikator.toString(),
                json = json,
                mottaker = Mottaker.SUBSUMSJON
            )
        }

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
