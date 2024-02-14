package no.nav.helse.spleis.graphql

import io.prometheus.client.Histogram
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.person.Person
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.serde.api.dto.PersonDTO
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.spleis.SpekematClient
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.dto.HendelseDTO
import no.nav.helse.spleis.graphql.dto.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.dto.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.dto.GraphQLGhostPeriode
import no.nav.helse.spleis.graphql.dto.GraphQLPerson
import no.nav.helse.spleis.logg
import org.slf4j.LoggerFactory
import kotlin.math.log

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
        val spekemat = try {
            spekematClient.hentSpekemat(fnr, callId).takeUnless { it.pakker.isEmpty() }
        } catch (err: Exception) {
            sikkerlogg.info("klarte ikke hente data fra spekemat: ${err.message}", err)
            null
        }
        "Bruker ${if (spekemat == null) "spleis" else "spekemat"} for å lage pølsevisning".also {
            logg.info(it)
            sikkerlogg.info(it, kv("fødselsnummer", fnr))
        }
        ApiMetrikker.målDeserialisering { serialisertPerson.deserialize(MaskinellJurist()) { hendelseDao.hentAlleHendelser(fnr.toLong()) } }
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