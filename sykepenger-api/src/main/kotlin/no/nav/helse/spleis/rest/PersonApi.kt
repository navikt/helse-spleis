package no.nav.helse.spleis.rest

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.navikt.tbd_libs.result_object.getOrThrow
import com.github.navikt.tbd_libs.retry.retryBlocking
import com.github.navikt.tbd_libs.speed.SpeedClient
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.MeterRegistry
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.spleis.SpekematClient
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.hentPersonSnapshot
import no.nav.helse.spleis.rest.dto.Person
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.helse.spleis.rest.PersonApi")
private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")
private val fødselsnummerRegex = "\\d{11}".toRegex()

/**
 * REST-varianten av personoppslaget. Lever side om side med `POST /graphql` og skal etter hvert
 * erstatte det.
 */
internal fun Application.personApi(
    speedClient: SpeedClient,
    spekematClient: SpekematClient,
    hendelseDao: HendelseDao,
    personDao: PersonDao,
    meterRegistry: MeterRegistry
) {
    routing {
        authenticate {
            post("/api/person") {
                val request = call.receive<PersonRequest>()
                val ident = request.fødselsnummer
                if (ident == null || !ident.matches(fødselsnummerRegex)) throw BadRequestException("fødselsnummer må være 11 siffer")

                val callId = call.callId ?: UUID.randomUUID().toString()
                val person = withContext(Dispatchers.IO) {
                    try {
                        hentPerson(speedClient, spekematClient, personDao, hendelseDao, ident, callId, meterRegistry)
                    } catch (err: Exception) {
                        logger.error("callId=$callId Kunne ikke bygge personsnapshot, sjekk tjenestekall-indeksen!")
                        sikkerlogger.error(
                            "callId=$callId {} Kunne ikke bygge personsnapshot: ${err.javaClass.simpleName} - ${err.message}",
                            keyValue("fødselsnummer", ident),
                            err
                        )
                        throw err
                    }
                } ?: throw NotFoundException("Kunne ikke finne person for fødselsnummer")

                call.respond(person)
            }
        }
    }
}

private fun hentPerson(
    speedClient: SpeedClient,
    spekematClient: SpekematClient,
    personDao: PersonDao,
    hendelseDao: HendelseDao,
    ident: String,
    callId: String,
    meterRegistry: MeterRegistry
): Person? {
    val snapshot = hentPersonSnapshot(spekematClient, personDao, hendelseDao, ident, callId, meterRegistry) ?: return null
    val (_, aktørId) = retryBlocking { speedClient.hentFødselsnummerOgAktørId(ident, callId).getOrThrow() }
    return mapTilPerson(snapshot.person, ident, aktørId, snapshot.hendelser)
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class PersonRequest(
    val fødselsnummer: String?
)
