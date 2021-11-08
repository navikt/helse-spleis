package no.nav.helse.spleis

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.helse.serde.serialize
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.dto.håndterPerson
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

internal fun Application.spesialistApi(dataSource: DataSource, authProviderName: String) {
    val hendelseDao = HendelseDao(dataSource)
    val personDao = PersonDao(dataSource)

    routing {
        authenticate(authProviderName) {
            get("/api/person-snapshot") {
                val fnr = call.request.header("fnr")!!.toLong()
                personDao.hentPersonFraFnr(fnr)
                    ?.deserialize { hendelseDao.hentAlleHendelser(fnr) }
                    ?.let { håndterPerson(it, hendelseDao) }
                    ?.let { call.respond(it) }
                    ?: call.respond(HttpStatusCode.NotFound, "Resource not found")
            }
        }
    }
}

internal fun Application.spannerApi(dataSource: DataSource, authProviderName: String) {
    val hendelseDao = HendelseDao(dataSource)
    val personDao = PersonDao(dataSource)

    routing {
        authenticate(authProviderName) {
            get("/api/person-json") {
                val fnr = fnr(personDao)

                val person = personDao.hentPersonFraFnr(fnr) ?: throw NotFoundException("Kunne ikke finne person for fødselsnummer")

                call.respond(person.deserialize { hendelseDao.hentAlleHendelser(fnr) }.serialize().json)
            }

            get("/api/hendelse-json/{hendelse}") {
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

private fun PipelineContext<Unit, ApplicationCall>.fnr(personDao: PersonDao): Long {
    val fnr = call.request.header("fnr")?.toLong()
    if (fnr != null) {
        return fnr
    }
    val aktorid = call.request.header("aktorId")?.toLong() ?: throw BadRequestException("Mangler fnr eller aktorId i headers")
    return personDao.hentFødselsnummer(aktorid) ?: throw NotFoundException("Fant ikke aktør-ID")
}

sealed class HendelseDTO(val type: String, val hendelseId: String) {

    class NySøknadDTO(json: JsonNode) : HendelseDTO("NY_SØKNAD", json["@id"].asText()) {
        val rapportertdato: LocalDateTime = LocalDateTime.parse(json["@opprettet"].asText())
        val fom: LocalDate = LocalDate.parse(json["fom"].asText())
        val tom: LocalDate = LocalDate.parse(json["tom"].asText())
    }

    class SendtSøknadNavDTO(json: JsonNode) : HendelseDTO("SENDT_SØKNAD_NAV", json["@id"].asText()) {
        val rapportertdato: LocalDateTime = LocalDateTime.parse(json["@opprettet"].asText())
        val sendtNav: LocalDateTime = LocalDateTime.parse(json["sendtNav"].asText())
        val fom: LocalDate = LocalDate.parse(json["fom"].asText())
        val tom: LocalDate = LocalDate.parse(json["tom"].asText())
    }

    class SendtSøknadArbeidsgiverDTO(json: JsonNode) : HendelseDTO("SENDT_SØKNAD_ARBEIDSGIVER", json["@id"].asText()) {
        val rapportertdato: LocalDateTime = LocalDateTime.parse(json["@opprettet"].asText())
        val sendtArbeidsgiver: LocalDateTime = LocalDateTime.parse(json["sendtArbeidsgiver"].asText())
        val fom: LocalDate = LocalDate.parse(json["fom"].asText())
        val tom: LocalDate = LocalDate.parse(json["tom"].asText())
    }

    class InntektsmeldingDTO(json: JsonNode) : HendelseDTO("INNTEKTSMELDING", json["@id"].asText()) {
        val beregnetInntekt: Number = json["beregnetInntekt"].asDouble()
        val førsteFraværsdag: LocalDate? = json.path("foersteFravaersdag").takeIf(JsonNode::isTextual)?.let { LocalDate.parse(it.asText()) }
        val mottattDato: LocalDateTime = LocalDateTime.parse(json["@opprettet"].asText())
    }
}
