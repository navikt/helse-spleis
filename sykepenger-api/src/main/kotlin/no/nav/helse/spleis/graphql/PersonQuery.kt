package no.nav.helse.spleis.graphql

import io.prometheus.client.Histogram
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.person.Person
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.spleis.SpekematClient
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.dto.HendelseDTO
import no.nav.helse.spleis.graphql.dto.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.dto.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.dto.GraphQLGhostPeriode
import no.nav.helse.spleis.graphql.dto.GraphQLPerson
import no.nav.helse.spleis.speil.dto.PersonDTO
import no.nav.helse.spleis.speil.serializePersonForSpeil
import org.slf4j.LoggerFactory

private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
private object ApiMetrikker {
    private val responstid = Histogram
        .build("person_snapshot_api", "Metrikker for henting av speil-snapshot")
        .labelNames("operasjon")
        .register()

    fun målDatabase(block: () -> SerialisertPerson?): SerialisertPerson? = responstid.labels("hent_person").time(block)

    fun målDeserialisering(block: () -> Person): Person = responstid.labels("deserialiser_person").time(block)

    fun målByggSnapshot(block: () -> PersonDTO): PersonDTO = responstid.labels("bygg_snapshot").time(block)
}

internal fun personResolver(spekematClient: SpekematClient, personDao: PersonDao, hendelseDao: HendelseDao, fnr: String, callId: String): GraphQLPerson? {
    return ApiMetrikker.målDatabase { personDao.hentPersonFraFnr(fnr.toLong()) }?.let { serialisertPerson ->
        val spekemat = spekematClient.hentSpekemat(fnr, callId)
        ApiMetrikker.målDeserialisering {
            val dto = serialisertPerson.tilPersonDto { hendelseDao.hentAlleHendelser(fnr.toLong()) }
            Person.gjenopprett(MaskinellJurist(), dto)
        }
            .let { ApiMetrikker.målByggSnapshot { serializePersonForSpeil(it, spekemat) } }
            .let { person -> mapTilDto(person, hendelseDao.hentHendelser(fnr.toLong())) }
    }
}

private fun mapTilDto(person: PersonDTO, hendelser: List<HendelseDTO>) =
    GraphQLPerson(
        aktorId = person.aktørId,
        fodselsnummer = person.fødselsnummer,
        arbeidsgivere = person.arbeidsgivere.map { arbeidsgiver ->
            GraphQLArbeidsgiver(
                organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                id = arbeidsgiver.id,
                generasjoner = arbeidsgiver.generasjoner.map { generasjon ->
                    GraphQLGenerasjon(
                        id = generasjon.id,
                        perioder = generasjon.perioder.map { periode -> mapTidslinjeperiode(periode, hendelser) },
                        kildeTilGenerasjon = generasjon.kildeTilGenerasjon
                    )
                },
                ghostPerioder = arbeidsgiver.ghostPerioder.map { periode ->
                    GraphQLGhostPeriode(
                        id = periode.id,
                        fom = periode.fom,
                        tom = periode.tom,
                        skjaeringstidspunkt = periode.skjæringstidspunkt,
                        vilkarsgrunnlagId = periode.vilkårsgrunnlagId,
                        deaktivert = periode.deaktivert,
                        organisasjonsnummer = arbeidsgiver.organisasjonsnummer
                    )
                }
            )
        },
        dodsdato = person.dødsdato,
        versjon = person.versjon,
        vilkarsgrunnlag = person.vilkårsgrunnlag.map { (id, vilkårsgrunnlag) -> mapVilkårsgrunnlag(id, vilkårsgrunnlag) }
    )