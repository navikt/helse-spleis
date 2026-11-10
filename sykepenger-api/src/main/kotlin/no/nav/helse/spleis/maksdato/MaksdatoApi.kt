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

@JsonIgnoreProperties(ignoreUnknown = true)
private data class MaksdatoRequest(
    val fødselsnummer: String,
    val infotrygdperioder: List<InfotrygdperiodeRequest>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class InfotrygdperiodeRequest(
    val fom: LocalDate,
    val tom: LocalDate,
    val type: String,
    val organisasjonsnummer: String?,
) {
    // samme tolkning som Sykepengehistorikk-behovet i UtbetalingshistorikkMessage
    fun tilInfotrygdperiode(): Infotrygdperiode? {
        if (fom > tom) return null
        val orgnr = organisasjonsnummer ?: ""
        return when (type) {
            "UTBETALING", "REDUKSJON_MEDLEM" -> PersonUtbetalingsperiode(orgnr, fom, tom)
            "ARB_REF", "REDUKSJON_ARB_REF" -> ArbeidsgiverUtbetalingsperiode(orgnr, fom, tom)
            "FERIE" -> Friperiode(fom, tom)
            else -> null
        }
    }
}

private data class MaksdatoResponse(
    val gjenståendeDager: Int,
    val vurdertTilOgMed: LocalDate,
)

internal fun Application.maksdatoApi(personDao: PersonDao) {
    routing {
        authenticate {
            post("/api/maksdato") {
                val request = call.receive<MaksdatoRequest>()
                if (request.infotrygdperioder.isEmpty()) throw BadRequestException("infotrygdperioder må ha minst én periode")
                val infotrygdperioder = request.infotrygdperioder.mapNotNull { it.tilInfotrygdperiode() }

                withContext(Dispatchers.IO) {
                    val serialisertPerson =
                        personDao.hentPersonFraFnr(request.fødselsnummer.toLong())
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
