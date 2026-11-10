package no.nav.helse.spleis.maksdato

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.person.Person
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.spleis.dao.PersonDao
import java.time.LocalDate

private val fødselsnummerRegex = "\\d{11}".toRegex()

@JsonIgnoreProperties(ignoreUnknown = true)
private data class MaksdatoRequest(
    val fødselsnummer: String?,
    val infotrygdperioder: List<InfotrygdperiodeRequest?>?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class InfotrygdperiodeRequest(
    val fom: String?,
    val tom: String?,
    val type: String?,
    val organisasjonsnummer: String?,
)

private data class MaksdatoResponse(
    val gjenståendeDager: Int,
    val vurdertTilOgMed: LocalDate,
)

internal fun Application.maksdatoApi(personDao: PersonDao) {
    routing {
        authenticate {
            post("/api/maksdato") {
                val request = call.receive<MaksdatoRequest>()
                val fødselsnummer =
                    request.fødselsnummer?.takeIf { it.matches(fødselsnummerRegex) }
                        ?: throw BadRequestException("fødselsnummer må være 11 siffer")
                val infotrygdperioder =
                    request.infotrygdperioder
                        ?.takeIf { it.isNotEmpty() }
                        ?.mapNotNull { periode ->
                            (periode ?: throw BadRequestException("infotrygdperioder kan ikke inneholde null")).tilInfotrygdperiode()
                        }
                        ?: throw BadRequestException("infotrygdperioder må ha minst én periode")

                withContext(Dispatchers.IO) {
                    val serialisertPerson =
                        personDao.hentPersonFraFnr(fødselsnummer.toLong())
                            ?: throw NotFoundException("Fant ikke person")
                    val person = Person.gjenopprett(EmptyLog, serialisertPerson.tilPersonDto())
                    val beregnet =
                        person.beregnGjenståendeSykedager(infotrygdperioder)
                            ?: throw NotFoundException("Fant ingen utbetalte dager å beregne gjenstående sykedager fra")
                    call.respond(MaksdatoResponse(beregnet.gjenståendeDager, beregnet.vurdertTilOgMed))
                }
            }
        }
    }
}

// samme tolkning som Sykepengehistorikk-behovet (UtbetalingshistorikkMessage): typer som ikke gir dager, og perioder med tom før fom, blir utelatt
private fun InfotrygdperiodeRequest.tilInfotrygdperiode(): Infotrygdperiode? {
    val fom = fom.tilDato("fom")
    val tom = tom.tilDato("tom")
    if (fom > tom) return null
    val orgnr = organisasjonsnummer ?: ""
    return when (type ?: throw BadRequestException("type mangler")) {
        "UTBETALING", "REDUKSJON_MEDLEM" -> PersonUtbetalingsperiode(orgnr, fom, tom)
        "ARB_REF", "REDUKSJON_ARB_REF" -> ArbeidsgiverUtbetalingsperiode(orgnr, fom, tom)
        "FERIE" -> Friperiode(fom, tom)
        else -> null
    }
}

private fun String?.tilDato(felt: String): LocalDate =
    this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: throw BadRequestException("$felt må være en dato")
