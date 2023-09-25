package no.nav.helse.spleis

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import java.util.UUID
import javax.sql.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.serde.api.serializePersonForSporing
import no.nav.helse.serde.serialize
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao

internal fun Application.spannerApi(dataSource: DataSource) {
    val hendelseDao = HendelseDao(dataSource)
    val personDao = PersonDao(dataSource)

    routing {
        authenticate {
            get("/api/person-json") {
                withContext(Dispatchers.IO) {
                    val ident = fnr(personDao)
                    val person = personDao.hentPersonFraFnr(ident) ?: throw NotFoundException("Kunne ikke finne person for fødselsnummer")
                    call.respond(
                        person.deserialize(
                            jurist = MaskinellJurist()
                        ) { hendelseDao.hentAlleHendelser(ident) }.serialize().json
                    )
                }
            }

            get("/api/hendelse-json/{hendelse}") {
                withContext(Dispatchers.IO) {
                    val hendelseId = call.parameters["hendelse"] ?: throw IllegalArgumentException("Kall Mangler hendelse referanse")

                    val meldingsReferanse = try {
                        UUID.fromString(hendelseId)
                    } catch (e: IllegalArgumentException) {
                        throw BadRequestException("meldingsreferanse bør/skal være en UUID")
                    }

                    val hendelse =
                        hendelseDao.hentHendelse(meldingsReferanse) ?: throw NotFoundException("Kunne ikke finne hendelse for hendelsereferanse = ${hendelseId}")


                    call.respondText(hendelse, ContentType.Application.Json)
                }
            }
        }
    }
}

internal fun Application.sporingApi(dataSource: DataSource) {
    val hendelseDao = HendelseDao(dataSource)
    val personDao = PersonDao(dataSource)

    routing {
        authenticate {
            get("/api/vedtaksperioder") {
                withContext(Dispatchers.IO) {
                    val fnr =  fnr(personDao)
                    val person = personDao.hentPersonFraFnr(fnr) ?: throw NotFoundException("Kunne ikke finne person for fødselsnummer")
                    call.respond(
                        serializePersonForSporing(
                            person.deserialize(
                                jurist = MaskinellJurist()
                            ) { hendelseDao.hentAlleHendelser(fnr) }
                        )
                    )
                }
            }
        }
    }
}

private fun PipelineContext<Unit, ApplicationCall>.fnr(personDao: PersonDao): Long {
    val fnr = call.request.header("fnr")?.toLong()
    if (fnr != null) {
        return fnr
    }
    val aktorid = call.request.header("aktorId")?.toLong() ?: throw BadRequestException("Mangler fnr eller aktorId i headers")
    return personDao.hentFødselsnummer(aktorid) ?: throw NotFoundException("Fant ikke aktør-ID")
}
