package no.nav.helse.spleis

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.dto.tilSpannerPersonDto
import no.nav.helse.etterlevelse.Subsumsjonslogg.Companion.EmptyLog
import no.nav.helse.person.Person
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.sporing.serializePersonForSporing
import java.util.UUID

internal fun Application.spannerApi(
    hendelseDao: HendelseDao,
    personDao: PersonDao,
) {
    routing {
        authenticate {
            post("/api/person-json") {
                val request = call.receive<PersonRequest>()
                withContext(Dispatchers.IO) {
                    val serialisertPerson =
                        personDao.hentPersonFraFnr(request.fødselsnummer.toLong())
                            ?: throw NotFoundException("Kunne ikke finne person for fødselsnummer")
                    val dto = serialisertPerson.tilPersonDto()
                    val person = Person.gjenopprett(EmptyLog, dto)
                    call.respond(person.dto().tilSpannerPersonDto())
                }
            }

            get("/api/hendelse-json/{hendelse}") {
                withContext(Dispatchers.IO) {
                    val hendelseId = call.parameters["hendelse"] ?: throw IllegalArgumentException("Kall Mangler hendelse referanse")

                    val meldingsReferanse =
                        try {
                            UUID.fromString(hendelseId)
                        } catch (_: IllegalArgumentException) {
                            throw BadRequestException("meldingsreferanse bør/skal være en UUID")
                        }

                    val hendelse =
                        hendelseDao.hentHendelse(meldingsReferanse)
                            ?: throw NotFoundException("Kunne ikke finne hendelse for hendelsereferanse = $hendelseId")

                    call.respondText(hendelse, ContentType.Application.Json)
                }
            }
        }
    }
}

internal fun Application.sporingApi(
    hendelseDao: HendelseDao,
    personDao: PersonDao,
) {
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
    val fødselsnummer: String,
)
