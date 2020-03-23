package no.nav.helse.spleis

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import no.nav.helse.person.Person
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.spleis.HendelseDTO.*
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.HendelseDao.Meldingstype.*
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.dao.UtbetalingDao
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

internal fun Application.spleisApi(dataSource: DataSource) {
    val hendelseDao = HendelseDao(dataSource)
    val utbetalingDao = UtbetalingDao(dataSource)
    val personDao = PersonDao(dataSource)

    routing {
        authenticate {
            get("/api/utbetaling/{utbetalingsreferanse}") {
                utbetalingDao.hentUtbetaling(call.parameters["utbetalingsreferanse"]!!)
                    ?.let { personDao.hentPersonAktørId(it.aktørId) }
                    ?.let { call.respond(serializePersonForSpeil(it)) }
                    ?: call.respond(HttpStatusCode.NotFound, "Resource not found")
            }

            get("/api/person/{aktørId}") {
                personDao.hentPersonAktørId(call.parameters["aktørId"]!!)
                    ?.let { håndterPerson(it, hendelseDao) }
                    ?.let { call.respond(it) }
                    ?: call.respond(HttpStatusCode.NotFound, "Resource not found")
            }

            get("/api/person/fnr/{fnr}") {
                personDao.hentPerson(call.parameters["fnr"]!!)
                    ?.let { håndterPerson(it, hendelseDao) }
                    ?.let { call.respond(it) }
                    ?: call.respond(HttpStatusCode.NotFound, "Resource not found")
            }
        }
    }
}

private fun håndterPerson(person: Person, hendelseDao: HendelseDao): ObjectNode {
    val (personJson, hendelseReferanser) = serializePersonForSpeil(person)
    val hendelser = hendelseDao.hentHendelser(hendelseReferanser).mapNotNull {
        when (it.first) {
            NY_SØKNAD -> NySøknadDTO(objectMapper.readTree(it.second))
            SENDT_SØKNAD -> SendtSøknadDTO(objectMapper.readTree(it.second))
            INNTEKTSMELDING -> InntektsmeldingDTO(objectMapper.readTree(it.second))
            PÅMINNELSE -> null
            YTELSER -> null
            VILKÅRSGRUNNLAG -> null
            MANUELL_SAKSBEHANDLING -> null
            UTBETALING -> null
        }
    }.map { objectMapper.valueToTree<JsonNode>(it) }

    return personJson.apply { putArray("hendelser").addAll(hendelser) }
}

sealed class HendelseDTO(val type: String, val hendelseId: String) {

    class NySøknadDTO(json: JsonNode) : HendelseDTO("NY_SØKNAD", json["@id"].asText()) {
        val rapportertdato: LocalDateTime = LocalDateTime.parse(json["@opprettet"].asText())
        val fom: LocalDate = LocalDate.parse(json["fom"].asText())
        val tom: LocalDate = LocalDate.parse(json["tom"].asText())
    }

    class SendtSøknadDTO(json: JsonNode) : HendelseDTO("SENDT_SØKNAD", json["@id"].asText()) {
        val rapportertdato: LocalDateTime = LocalDateTime.parse(json["@opprettet"].asText())
        val sendtNav: LocalDateTime = LocalDateTime.parse(json["@opprettet"].asText())
        val fom: LocalDate = LocalDate.parse(json["fom"].asText())
        val tom: LocalDate = LocalDate.parse(json["tom"].asText())
    }

    class InntektsmeldingDTO(json: JsonNode) : HendelseDTO("INNTEKTSMELDING", json["@id"].asText()) {
        val beregnetInntekt: Number = json["beregnetInntekt"].asDouble()
        val førsteFraværsdag: LocalDate = LocalDate.parse(json["foersteFravaersdag"].asText())
        val mottattDato: LocalDateTime = LocalDateTime.parse(json["@opprettet"].asText())
    }
}


