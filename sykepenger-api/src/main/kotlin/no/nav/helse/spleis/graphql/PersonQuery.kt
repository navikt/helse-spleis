package no.nav.helse.spleis.graphql

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.etterlevelse.Subsumsjonslogg.Companion.EmptyLog
import no.nav.helse.person.Person
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.spleis.SpekematClient
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.dto.HendelseDTO
import no.nav.helse.spleis.graphql.dto.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.dto.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.dto.GraphQLGhostPeriode
import no.nav.helse.spleis.graphql.dto.GraphQLNyttInntektsforholdPeriode
import no.nav.helse.spleis.graphql.dto.GraphQLPerson
import no.nav.helse.spleis.speil.dto.PersonDTO
import no.nav.helse.spleis.speil.serializePersonForSpeil

private object ApiMetrikker {
    private val metrics: MeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    fun målDatabase(block: () -> SerialisertPerson?): SerialisertPerson? = mål("hent_person", block)

    fun målDeserialisering(block: () -> Person): Person = mål("deserialiser_person", block)

    fun målByggSnapshot(block: () -> PersonDTO): PersonDTO = mål("bygg_snapshot", block)

    private fun <R> mål(operasjon: String, block: () -> R): R {
        val timer = Timer.start(metrics)
        return block().also {
            timer.stop(
                Timer.builder("person_snapshot_api")
                    .description("Metrikker for henting av speil-snapshot")
                    .tag("operasjon", operasjon)
                    .register(metrics)
            )
        }
    }
}

internal fun personResolver(spekematClient: SpekematClient, personDao: PersonDao, hendelseDao: HendelseDao, fnr: String, callId: String): GraphQLPerson? {
    return ApiMetrikker.målDatabase { personDao.hentPersonFraFnr(fnr.toLong()) }?.let { serialisertPerson ->
        val spekemat = spekematClient.hentSpekemat(fnr, callId)
        ApiMetrikker.målDeserialisering {
            val dto = serialisertPerson.tilPersonDto { hendelseDao.hentAlleHendelser(fnr.toLong()) }
            Person.gjenopprett(EmptyLog, dto)
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
                },
                nyeInntektsforholdPerioder = arbeidsgiver.nyeInntektsforhold.map { periode ->
                    GraphQLNyttInntektsforholdPeriode(
                        id = periode.id,
                        organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                        fom = periode.fom,
                        tom = periode.tom,
                        vilkarsgrunnlagId = periode.vilkårsgrunnlagId,
                        skjaeringstidspunkt = periode.skjæringstidspunkt
                    )
                }
            )
        },
        dodsdato = person.dødsdato,
        versjon = person.versjon,
        vilkarsgrunnlag = person.vilkårsgrunnlag.map { (id, vilkårsgrunnlag) -> mapVilkårsgrunnlag(id, vilkårsgrunnlag) }
    )