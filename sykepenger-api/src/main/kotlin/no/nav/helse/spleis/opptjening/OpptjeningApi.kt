package no.nav.helse.spleis.opptjening

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.person.Person
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.objectMapper

@JsonIgnoreProperties(ignoreUnknown = true)
private data class PersonRequest(
    val fødselsnummer: String
)

data class Opptjeningsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
)
data class OpptjeningsvurderingDto(
    val opptjeningsvurderingId: UUID,
    val opptjeningsperiode: Opptjeningsperiode,
    val skjæringstidspunkt: LocalDate,
    val opptjeningOk: Boolean,
    val typeGrunnlag: String,
) {
    companion object {
        fun fraVilkårsgrunnlag(dto: VilkårsgrunnlagUtDto): OpptjeningsvurderingDto {
            return when(dto) {
                is VilkårsgrunnlagUtDto.Spleis -> dto.opptjening!!.let { opptjening ->
                    OpptjeningsvurderingDto(
                        opptjeningsvurderingId = dto.opptjeningsvurderingId,
                        opptjeningsperiode = Opptjeningsperiode(
                            fom = opptjening.opptjeningsperiode.fom,
                            tom = opptjening.opptjeningsperiode.tom
                        ),
                        skjæringstidspunkt = dto.skjæringstidspunkt,
                        opptjeningOk = opptjening.erOppfylt,
                        typeGrunnlag = "SPLEIS"
                    )
                }
                is VilkårsgrunnlagUtDto.Infotrygd -> throw RuntimeException("Infotrygd")
            }
        }
    }
}

data class OpptjeningsvurderingerRespons(
    val opptjeningsvurderinger: List<OpptjeningsvurderingDto>,
)

internal fun Application.opptjeningApi(personDao: PersonDao) {
    routing {
        authenticate {
            post("/api/opptjeningsvurderinger") {
                val request = call.receive<PersonRequest>()
                withContext(Dispatchers.IO) {
                    val serialisertPerson = personDao.hentPersonFraFnr(request.fødselsnummer.toLong()) ?: throw NotFoundException("Kunne ikke finne person for fødselsnummer")
                    val innDto = serialisertPerson.tilPersonDto()
                    val person = Person.gjenopprett(EmptyLog, innDto)
                    val dto = person.dto()
                    val opptjeningsvurderinger = dto.vilkårsgrunnlagHistorikk.historikk.flatMap { it.vilkårsgrunnlag }.map {
                        OpptjeningsvurderingDto.fraVilkårsgrunnlag(it)
                    }.distinctBy { it.opptjeningsvurderingId }

                    call.respondText(
                        objectMapper.writeValueAsString(OpptjeningsvurderingerRespons(opptjeningsvurderinger)),
                        ContentType.Application.Json
                    )

                }
            }
        }
    }
}
