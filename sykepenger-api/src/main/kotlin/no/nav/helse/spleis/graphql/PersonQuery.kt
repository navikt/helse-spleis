package no.nav.helse.spleis.graphql

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.spleis.SpekematClient
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.dto.HendelseDTO
import no.nav.helse.spleis.graphql.dto.GraphQLArbeidsgiver
import no.nav.helse.spleis.graphql.dto.GraphQLGenerasjon
import no.nav.helse.spleis.graphql.dto.GraphQLGhostPeriode
import no.nav.helse.spleis.graphql.dto.GraphQLPerson
import no.nav.helse.spleis.hentPersonSnapshot
import no.nav.helse.spleis.speil.dto.PersonDTO

internal fun personResolver(spekematClient: SpekematClient, personDao: PersonDao, hendelseDao: HendelseDao, fnr: String, aktørId: String, callId: String, meterRegistry: MeterRegistry): GraphQLPerson? {
    val snapshot = hentPersonSnapshot(spekematClient, personDao, hendelseDao, fnr, callId, meterRegistry) ?: return null
    return mapTilDto(snapshot.person, fnr, aktørId, snapshot.hendelser)
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
