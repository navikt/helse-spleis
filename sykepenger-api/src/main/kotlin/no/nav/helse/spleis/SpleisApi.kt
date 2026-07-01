package no.nav.helse.spleis

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.dto.tilSpannerPersonDto
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.person.Person
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.dao.SendtDao
import no.nav.helse.spleis.dao.SendtDao.Companion.responseJson
import no.nav.helse.spleis.sporing.serializePersonForSporing

internal fun Application.spannerApi(
    hendelseDao: HendelseDao,
    personDao: PersonDao,
    sendtDao: SendtDao
) {
    routing {
        authenticate {
            post("/api/person-json") {
                val request = call.receive<PersonRequest>()
                withContext(Dispatchers.IO) {
                    val serialisertPerson = personDao.hentPersonFraFnr(request.fødselsnummer.toLong()) ?: throw NotFoundException("Kunne ikke finne person for fødselsnummer")
                    val dto = serialisertPerson.tilPersonDto()
                    val person = Person.gjenopprett(EmptyLog, dto)
                    call.respond(person.dto().tilSpannerPersonDto())
                }
            }

            get("/api/hendelse-json/{hendelse}") {
                withContext(Dispatchers.IO) {
                    val hendelseId = call.parameters["hendelse"] ?: throw IllegalArgumentException("Kall Mangler hendelse referanse")

                    val meldingsReferanse = try {
                        UUID.fromString(hendelseId)
                    } catch (_: IllegalArgumentException) {
                        throw BadRequestException("meldingsreferanse bør/skal være en UUID")
                    }

                    val hendelse =
                        hendelseDao.hentHendelse(meldingsReferanse) ?: throw NotFoundException("Kunne ikke finne hendelse for hendelsereferanse = $hendelseId")

                    call.respondText(hendelse, ContentType.Application.Json)
                }
            }

            get("/api/sendte-meldinger/{forarsaketAv}") {
                withContext(Dispatchers.IO) {
                    val hendelseId = call.parameters["forarsaketAv"] ?: throw IllegalArgumentException("Kall Mangler forarsaketAv")

                    val forarsaketAv = try {
                        UUID.fromString(hendelseId)
                    } catch (_: IllegalArgumentException) {
                        throw BadRequestException("forarsaketAv skal være en UUID")
                    }

                    val sendteMeldinger = sendtDao.sendteMeldinger(forarsaketAv)

                    call.respondText(sendteMeldinger.responseJson(), ContentType.Application.Json)
                }
            }
        }
    }
}

internal fun Application.sporingApi(personDao: PersonDao) {
    routing {
        authenticate {
            get("/api/vedtaksperioder") {
                withContext(Dispatchers.IO) {
                    val fnr = call.request.header("fnr")?.toLong() ?: throw BadRequestException("mangler fnr")
                    val person = personDao.hentPersonFraFnr(fnr) ?: throw NotFoundException("Kunne ikke finne person for fødselsnummer")
                    val dto = person.tilPersonDto()
                    call.respond(serializePersonForSporing(Person.gjenopprett(EmptyLog, dto)))
                }
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class PersonRequest(
    val fødselsnummer: String
)
