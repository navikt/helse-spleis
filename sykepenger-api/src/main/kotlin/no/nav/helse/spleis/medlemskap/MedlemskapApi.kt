package no.nav.helse.spleis.medlemskap

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.helse.dto.MedlemskapsvurderingDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagInnslagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlaghistorikkUtDto
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.person.Person
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.objectMapper
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
private data class PersonRequest(
    val fødselsnummer: String,
)

internal data class Medlemskapsvurdering(
    val opprettet: LocalDateTime,
    val vilkårsgrunnlag: VilkårsgrunnlagUtDto,
)

internal fun medlemskapsvurderinger(historikk: List<VilkårsgrunnlagInnslagUtDto>): List<Medlemskapsvurdering> {
    fun tidligstOpprettet(vilkårsgrunnlag: VilkårsgrunnlagUtDto) =
        historikk
            .filter { innslag -> innslag.vilkårsgrunnlag.any { it.medlemskapsvurderingId == vilkårsgrunnlag.medlemskapsvurderingId } }
            .minOf { it.opprettet }

    return historikk
        .flatMap { it.vilkårsgrunnlag }
        .distinctBy { it.medlemskapsvurderingId }
        .map { Medlemskapsvurdering(tidligstOpprettet(it), it) }
}

internal fun Application.medlemskapApi(personDao: PersonDao) {
    fun vilkårsgrunnlagHistorikk(fødselsnummer: String): VilkårsgrunnlaghistorikkUtDto {
        val serialisertPerson = personDao.hentPersonFraFnr(fødselsnummer.toLong()) ?: return VilkårsgrunnlaghistorikkUtDto(emptyList())
        val utDto = Person.gjenopprett(EmptyLog, serialisertPerson.tilPersonDto()).dto()
        return utDto.vilkårsgrunnlagHistorikk
    }

    fun somJson(
        opprettet: LocalDateTime,
        vilkårsgrunnlag: VilkårsgrunnlagUtDto,
    ) = objectMapper.createObjectNode().apply {
        put("medlemskapsvurderingId", vilkårsgrunnlag.medlemskapsvurderingId.toString())
        put("opprettet", opprettet.toString())
        put("skjæringstidspunkt", vilkårsgrunnlag.skjæringstidspunkt.toString())
        when (vilkårsgrunnlag) {
            is VilkårsgrunnlagUtDto.Spleis -> {
                put("kilde", "SPLEIS")
                put(
                    "medlemskapstatus",
                    when (vilkårsgrunnlag.medlemskapstatus) {
                        MedlemskapsvurderingDto.Ja -> "JA"
                        MedlemskapsvurderingDto.Nei -> "NEI"
                        MedlemskapsvurderingDto.VetIkke -> "VET_IKKE"
                        MedlemskapsvurderingDto.UavklartMedBrukerspørsmål -> "UAVKLART_MED_BRUKERSPORSMAAL"
                    },
                )
            }

            is VilkårsgrunnlagUtDto.Infotrygd -> {
                put("kilde", "INFOTRYGD")
            }
        }
    }

    routing {
        authenticate {
            post("/api/medlemskapsvurderinger") {
                val request = call.receive<PersonRequest>()

                withContext(Dispatchers.IO) {
                    val vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk(request.fødselsnummer)

                    val medlemskapsvurderinger =
                        medlemskapsvurderinger(vilkårsgrunnlagHistorikk.historikk)
                            .map { somJson(it.opprettet, it.vilkårsgrunnlag) }

                    val response =
                        objectMapper
                            .createObjectNode()
                            .apply {
                                putArray("medlemskapsvurderinger").addAll(medlemskapsvurderinger)
                            }.toString()

                    call.respondText(response, ContentType.Application.Json)
                }
            }
        }
    }
}
