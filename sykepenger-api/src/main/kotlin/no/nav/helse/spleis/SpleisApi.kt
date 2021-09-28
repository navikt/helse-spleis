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
import no.nav.helse.person.Person
import no.nav.helse.serde.api.PersonDTO
import no.nav.helse.serde.api.hendelseReferanserForPerson
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.serde.serialize
import no.nav.helse.spleis.HendelseDTO.*
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.HendelseDao.Meldingstype.*
import no.nav.helse.spleis.dao.PersonDao
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import no.nav.helse.serde.api.v2.InntektsmeldingDTO as SerdeInntektsmeldingDTO
import no.nav.helse.serde.api.v2.SykmeldingDTO as SerdeSykmeldingDTO
import no.nav.helse.serde.api.v2.SøknadArbeidsgiverDTO as SerdeSøknadArbeidsgiverDTO
import no.nav.helse.serde.api.v2.SøknadNavDTO as SerdeSøknadNavDTO

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

private fun håndterPerson(person: Person, hendelseDao: HendelseDao): PersonDTO {
    val hendelseReferanser = hendelseReferanserForPerson(person)
    val hendelser = hendelseDao.hentHendelser(hendelseReferanser).map { (type, hendelseJson) ->
        when (type) {
            NY_SØKNAD -> NySøknadDTO(objectMapper.readTree(hendelseJson))
            SENDT_SØKNAD_NAV -> SendtSøknadNavDTO(objectMapper.readTree(hendelseJson))
            SENDT_SØKNAD_ARBEIDSGIVER -> SendtSøknadArbeidsgiverDTO(objectMapper.readTree(hendelseJson))
            INNTEKTSMELDING -> InntektsmeldingDTO(objectMapper.readTree(hendelseJson))
        }
    }.mapHendelseDTO()
    return serializePersonForSpeil(person, hendelser)
}

private fun List<HendelseDTO>.mapHendelseDTO() = map {
    when (it) {
        is NySøknadDTO -> mapNySøknad(it)
        is SendtSøknadNavDTO -> mapSendtSøknad(it)
        is SendtSøknadArbeidsgiverDTO -> mapSendtSøknad(it)
        is InntektsmeldingDTO -> mapInntektsmelding(it)
    }
}

private fun mapSendtSøknad(sendtSøknadNavDTO: SendtSøknadNavDTO) = SerdeSøknadNavDTO(
    sendtSøknadNavDTO.hendelseId,
    sendtSøknadNavDTO.fom,
    sendtSøknadNavDTO.tom,
    sendtSøknadNavDTO.rapportertdato,
    sendtSøknadNavDTO.sendtNav
)

private fun mapSendtSøknad(sendtSøknadArbeidsgiverDTO: SendtSøknadArbeidsgiverDTO) = SerdeSøknadArbeidsgiverDTO(
    sendtSøknadArbeidsgiverDTO.hendelseId,
    sendtSøknadArbeidsgiverDTO.fom,
    sendtSøknadArbeidsgiverDTO.tom,
    sendtSøknadArbeidsgiverDTO.rapportertdato,
    sendtSøknadArbeidsgiverDTO.sendtArbeidsgiver
)

private fun mapNySøknad(nySøknadDTO: NySøknadDTO) = SerdeSykmeldingDTO(
    nySøknadDTO.hendelseId,
    nySøknadDTO.fom,
    nySøknadDTO.tom,
    nySøknadDTO.rapportertdato
)

private fun mapInntektsmelding(inntektsmeldingDTO: InntektsmeldingDTO) = SerdeInntektsmeldingDTO(
    inntektsmeldingDTO.hendelseId,
    inntektsmeldingDTO.mottattDato,
    inntektsmeldingDTO.beregnetInntekt.toDouble()
)

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
