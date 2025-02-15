package no.nav.helse.spleis.graphql

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
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

private object ApiMetrikker {
    fun målDatabase(meterRegistry: MeterRegistry, block: () -> SerialisertPerson?): SerialisertPerson? = mål(meterRegistry, "hent_person", block)

    fun målDeserialisering(meterRegistry: MeterRegistry, block: () -> Person): Person = mål(meterRegistry, "deserialiser_person", block)

    fun målByggSnapshot(meterRegistry: MeterRegistry, block: () -> PersonDTO): PersonDTO = mål(meterRegistry, "bygg_snapshot", block)

    private fun <R> mål(meterRegistry: MeterRegistry, operasjon: String, block: () -> R): R {
        val timer = Timer.start(meterRegistry)
        return block().also {
            timer.stop(
                Timer.builder("person_snapshot_api")
                    .description("Metrikker for henting av speil-snapshot")
                    .tag("operasjon", operasjon)
                    .register(meterRegistry)
            )
        }
    }
}

internal fun personResolver(spekematClient: SpekematClient, personDao: PersonDao, hendelseDao: HendelseDao, fnr: String, aktørId: String, callId: String, meterRegistry: MeterRegistry): GraphQLPerson? {
    return ApiMetrikker.målDatabase(meterRegistry) { personDao.hentPersonFraFnr(fnr.toLong()) }?.let { serialisertPerson ->
        val spekemat = spekematClient.hentSpekemat(fnr, callId)
        ApiMetrikker.målDeserialisering(meterRegistry) {
            val dto = serialisertPerson.tilPersonDto()
            Person.gjenopprett(EmptyLog, dto)
        }
            .let { ApiMetrikker.målByggSnapshot(meterRegistry) { serializePersonForSpeil(it, spekemat) } }
            .let { person -> mapTilDto(person, fnr, aktørId, hendelseDao.hentHendelser(fnr.toLong())) }
    }
}

private fun mapTilDto(person: PersonDTO, fnr: String, aktørId: String, hendelser: List<HendelseDTO>) =
    GraphQLPerson(
        aktorId = aktørId,
        fodselsnummer = fnr,
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
