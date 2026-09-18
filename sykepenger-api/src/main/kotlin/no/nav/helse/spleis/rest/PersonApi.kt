package no.nav.helse.spleis.rest

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.navikt.tbd_libs.result_object.getOrThrow
import com.github.navikt.tbd_libs.retry.retryBlocking
import com.github.navikt.tbd_libs.speed.SpeedClient
import io.ktor.http.HttpStatusCode
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
import kotlinx.io.IOException
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.spleis.SpekematClient
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.hentPersonSnapshot
import no.nav.helse.spleis.rest.dto.ApiPerson
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.helse.spleis.rest.PersonApi")
private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")
private val fødselsnummerRegex = "\\d{11}".toRegex()

/**
 * REST-endepunktet for personoppslag.
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
                withContext(Dispatchers.IO) {
                    try {
                        val person = hentPerson(speedClient, spekematClient, personDao, hendelseDao, ident, callId, meterRegistry)
                            ?: throw NotFoundException("Kunne ikke finne person for fødselsnummer")

                        call.respond(person)
                    }
                    catch (err: IOException) {
                        logger.warn("callId=$callId Kunne ikke bygge personsnapshot, se i Team Logs for detaljer.")
                        sikkerlogger.warn(
                            "callId=$callId {} Kunne ikke bygge personsnapshot: ${err.javaClass.simpleName} - ${err.message}",
                            keyValue("fødselsnummer", ident),
                            err
                        )
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                    catch (err: Exception) {
                        logger.error("callId=$callId Kunne ikke bygge personsnapshot, se i Team Logs for detaljer.")
                        sikkerlogger.error(
                            "callId=$callId {} Kunne ikke bygge personsnapshot: ${err.javaClass.simpleName} - ${err.message}",
                            keyValue("fødselsnummer", ident),
                            err
                        )
                        throw err
                    }
                }
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
): ApiPerson? {
    val snapshot = hentPersonSnapshot(spekematClient, personDao, hendelseDao, ident, callId, meterRegistry) ?: return null
    val (_, aktørId) = retryBlocking { speedClient.hentFødselsnummerOgAktørId(ident, callId).getOrThrow() }
    return mapTilPerson(snapshot.person, ident, aktørId, snapshot.hendelser)
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class PersonRequest(
    val fødselsnummer: String?
)
