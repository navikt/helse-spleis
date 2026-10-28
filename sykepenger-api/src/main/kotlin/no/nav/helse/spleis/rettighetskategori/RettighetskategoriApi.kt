package no.nav.helse.spleis.rettighetskategori

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
import no.nav.helse.dto.deserialisering.YrkesaktivitetstypeDto
import no.nav.helse.dto.serialisering.ArbeidsgiverUtDto
import no.nav.helse.dto.serialisering.PersonUtDto
import no.nav.helse.dto.serialisering.VedtaksperiodeUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.person.Person
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.objectMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
private data class PersonRequest(
    val fødselsnummer: String,
)

internal data class Rettighetskategori(
    val rettighetskategoriId: UUID,
    val skjæringstidspunkt: LocalDate,
    val opprettet: LocalDateTime,
    val kilde: Kilde,
    val harSelvstendigInntekt: Boolean,
    val aktive: List<YrkesaktivitetstypeDto>,
    val forkastede: List<YrkesaktivitetstypeDto>,
) {
    enum class Kilde { SPLEIS, INFOTRYGD }
}

internal fun rettighetskategorier(person: PersonUtDto): List<Rettighetskategori> {
    val historikk = person.vilkårsgrunnlagHistorikk.historikk
    val vilkårsgrunnlag = historikk.flatMap { it.vilkårsgrunnlag }

    fun tidligstOpprettet(rettighetskategoriId: UUID) =
        historikk
            .filter { innslag -> innslag.vilkårsgrunnlag.any { it.rettighetskategoriId == rettighetskategoriId } }
            .minOf { it.opprettet }

    fun yrkesaktivitetstyper(
        skjæringstidspunkt: LocalDate,
        perioder: (ArbeidsgiverUtDto) -> List<VedtaksperiodeUtDto>,
    ) = person.arbeidsgivere
        .filter { arbeidsgiver -> perioder(arbeidsgiver).any { it.skjæringstidspunkt == skjæringstidspunkt } }
        .map { it.yrkesaktivitetstype }

    return vilkårsgrunnlag
        .distinctBy { it.rettighetskategoriId }
        .map {
            Rettighetskategori(
                rettighetskategoriId = it.rettighetskategoriId,
                skjæringstidspunkt = it.skjæringstidspunkt,
                opprettet = tidligstOpprettet(it.rettighetskategoriId),
                kilde =
                    when (it) {
                        is VilkårsgrunnlagUtDto.Spleis -> Rettighetskategori.Kilde.SPLEIS
                        is VilkårsgrunnlagUtDto.Infotrygd -> Rettighetskategori.Kilde.INFOTRYGD
                    },
                harSelvstendigInntekt = it.inntektsgrunnlag.selvstendigInntektsopplysning != null,
                aktive = yrkesaktivitetstyper(it.skjæringstidspunkt) { arbeidsgiver -> arbeidsgiver.vedtaksperioder },
                forkastede =
                    yrkesaktivitetstyper(it.skjæringstidspunkt) { arbeidsgiver ->
                        arbeidsgiver.forkastede.map { forkastet -> forkastet.vedtaksperiode }
                    },
            )
        }
}

internal fun Application.rettighetskategoriApi(personDao: PersonDao) {
    fun person(fødselsnummer: String): PersonUtDto? {
        val serialisertPerson = personDao.hentPersonFraFnr(fødselsnummer.toLong()) ?: return null
        return Person.gjenopprett(EmptyLog, serialisertPerson.tilPersonDto()).dto()
    }

    fun somJson(rettighetskategori: Rettighetskategori) =
        objectMapper.createObjectNode().apply {
            put("rettighetskategoriId", rettighetskategori.rettighetskategoriId.toString())
            put("skjæringstidspunkt", rettighetskategori.skjæringstidspunkt.toString())
            put("opprettet", rettighetskategori.opprettet.toString())
            put("kilde", rettighetskategori.kilde.name)
            put("harSelvstendigInntekt", rettighetskategori.harSelvstendigInntekt)
            putObject("yrkesaktivitetstyper").apply {
                putArray("aktive").apply { rettighetskategori.aktive.forEach { add(it.name) } }
                putArray("forkastede").apply { rettighetskategori.forkastede.forEach { add(it.name) } }
            }
        }

    routing {
        authenticate {
            post("/api/rettighetskategorier") {
                val request = call.receive<PersonRequest>()

                withContext(Dispatchers.IO) {
                    val response =
                        objectMapper
                            .createObjectNode()
                            .apply {
                                val rettighetskategorier = person(request.fødselsnummer)?.let(::rettighetskategorier).orEmpty()
                                putArray("rettighetskategorier").addAll(rettighetskategorier.map(::somJson))
                            }.toString()

                    call.respondText(response, ContentType.Application.Json)
                }
            }
        }
    }
}
