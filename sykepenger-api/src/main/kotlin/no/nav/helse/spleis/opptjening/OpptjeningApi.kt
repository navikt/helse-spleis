package no.nav.helse.spleis.opptjening

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
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlaghistorikkUtDto
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.person.Person
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.objectMapper

@JsonIgnoreProperties(ignoreUnknown = true)
private data class PersonRequest(
    val fødselsnummer: String
)

internal fun Application.opptjeningApi(personDao: PersonDao) {

    fun vilkårsgrunnlagHistorikk(fødselsnummer: String): VilkårsgrunnlaghistorikkUtDto {
        val serialisertPerson = personDao.hentPersonFraFnr(fødselsnummer.toLong()) ?: return VilkårsgrunnlaghistorikkUtDto(emptyList())
        val utDto = Person.gjenopprett(EmptyLog, serialisertPerson.tilPersonDto()).dto()
        return utDto.vilkårsgrunnlagHistorikk
    }

    fun somJson(vilkårsgrunnlag: VilkårsgrunnlagUtDto) = objectMapper.createObjectNode().apply {
        put("opptjeningsvurderingId", vilkårsgrunnlag.opptjeningsvurderingId.toString())
        put("skjæringstidspunkt", vilkårsgrunnlag.skjæringstidspunkt.toString())
        when (vilkårsgrunnlag) {
            is VilkårsgrunnlagUtDto.Spleis -> {
                put("kilde", "SPLEIS")
                val arbeidstakerOpptjening = vilkårsgrunnlag.opptjening ?: error("TODO: Bør fikse at opptjening ikke er nullable for Spleis, det er de aldri i praksis")
                put("oppfylt", arbeidstakerOpptjening.erOppfylt)
                when (val opptjeningsperiode = arbeidstakerOpptjening.reellOpptjeningsperiode) {
                    null -> putNull("opptjeningsperiode")
                    else -> putObject("opptjeningsperiode").apply {
                        put("fom", opptjeningsperiode.fom.toString())
                        put("tom", opptjeningsperiode.tom.toString())
                    }
                }
                put("antallDager", arbeidstakerOpptjening.opptjeningsdager.takeUnless { arbeidstakerOpptjening.reellOpptjeningsperiode == null } ?: 0)
                putArray("arbeidsforhold").apply {
                    arbeidstakerOpptjening.arbeidsforhold.forEach arbeidsforhold@{ arbeidsforhold ->
                        val ansettelsesperioder = arbeidsforhold.ansattPerioder.filterNot { it.deaktivert }.takeUnless { it.isEmpty() } ?: return@arbeidsforhold
                        addObject().apply {
                            put("organisasjonsnummer", arbeidsforhold.orgnummer)
                            putArray("ansettelsesperioder").apply {
                                ansettelsesperioder.forEach { ansettelsesperiode ->
                                    addObject().apply {
                                        put("fom", ansettelsesperiode.ansattFom.toString())
                                        when (val tom = ansettelsesperiode.ansattTom) {
                                            null -> putNull("tom")
                                            else -> put("tom", tom.toString())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is VilkårsgrunnlagUtDto.Infotrygd -> {
                put("kilde", "INFOTRYGD")
            }
        }
    }

    routing {
        authenticate {
            post("/api/opptjeningsvurderinger") {
                val request = call.receive<PersonRequest>()

                withContext(Dispatchers.IO) {
                    val vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk(request.fødselsnummer)

                    val opptjeningsvurderinger = vilkårsgrunnlagHistorikk
                        .historikk
                        .flatMap { it.vilkårsgrunnlag }
                        .distinctBy { it.opptjeningsvurderingId }
                        .map(::somJson)

                    val response = objectMapper.createObjectNode().apply {
                        putArray("opptjeningsvurderinger").addAll(opptjeningsvurderinger)
                    }.toString()

                    call.respondText(response, ContentType.Application.Json)
                }
            }
        }
    }
}
