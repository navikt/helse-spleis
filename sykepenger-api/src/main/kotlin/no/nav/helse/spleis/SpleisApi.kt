package no.nav.helse.spleis

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.features.BadRequestException
import io.ktor.features.NotFoundException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.pipeline.PipelineContext
import io.prometheus.client.Histogram
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.person.Person
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.serde.api.PersonDTO
import no.nav.helse.serde.api.serializePersonForSporing
import no.nav.helse.serde.serialize
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.dto.håndterPerson
import org.slf4j.LoggerFactory

private object ApiMetrikker {
    private val responstid = Histogram
        .build("person_snapshot_api", "Metrikker for henting av speil-snapshot")
        .labelNames("operasjon")
        .register()

    fun målDatabase(block: () -> SerialisertPerson?): SerialisertPerson? = responstid.labels("hent_person").time(block)

    fun målDeserialisering(block: () -> Person): Person = responstid.labels("deserialiser_person").time(block)

    fun målByggSnapshot(block: () -> PersonDTO): PersonDTO = responstid.labels("bygg_snapshot").time(block)
}

internal fun Application.spesialistApi(dataSource: DataSource, authProviderName: String) {

    val hendelseDao = HendelseDao(dataSource)
    val personDao = PersonDao(dataSource)

    val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    routing {
        authenticate(authProviderName) {
            get("/api/person-snapshot") {
                withContext(Dispatchers.IO) {
                    val fnr = call.request.header("fnr")!!.toLong()
                    sikkerLogg.info("Serverer person-snapshot for fødselsnummer $fnr")
                    try {
                        ApiMetrikker.målDatabase { personDao.hentPersonFraFnr(fnr) }
                            ?.let {
                                ApiMetrikker.målDeserialisering {
                                    it.deserialize(
                                        jurist = MaskinellJurist(),
                                        meldingerSupplier = { hendelseDao.hentAlleHendelser(fnr) })
                                }
                            }
                            ?.let { ApiMetrikker.målByggSnapshot { håndterPerson(it, hendelseDao) } }
                            ?.let { call.respond(it) }
                            ?: call.respond(HttpStatusCode.NotFound, "Resource not found")
                    } catch (e: RuntimeException) {
                        sikkerLogg.error("Feil ved servering av person-json for person: $fnr", e)
                        call.respond(HttpStatusCode.InternalServerError, "Feil ved servering av person-json. Sjekk sikkerlogg i spleis-api")
                        throw e
                    }
                }
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
                withContext(Dispatchers.IO) {
                    val fnr = fnr(personDao)
                    val person = personDao.hentPersonFraFnr(fnr) ?: throw NotFoundException("Kunne ikke finne person for fødselsnummer")
                    call.respond(
                        person.deserialize(
                            jurist = MaskinellJurist(),
                            meldingerSupplier = { hendelseDao.hentAlleHendelser(fnr) }
                        ).serialize().json
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

internal fun Application.sporingApi(dataSource: DataSource, authProviderName: String) {
    val hendelseDao = HendelseDao(dataSource)
    val personDao = PersonDao(dataSource)

    routing {
        authenticate(authProviderName) {
            get("/api/vedtaksperioder") {
                withContext(Dispatchers.IO) {
                    val fnr = fnr(personDao)
                    val person = personDao.hentPersonFraFnr(fnr) ?: throw NotFoundException("Kunne ikke finne person for fødselsnummer")
                    call.respond(
                        serializePersonForSporing(
                            person.deserialize(
                                jurist = MaskinellJurist(),
                                meldingerSupplier = { hendelseDao.hentAlleHendelser(fnr) })
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
