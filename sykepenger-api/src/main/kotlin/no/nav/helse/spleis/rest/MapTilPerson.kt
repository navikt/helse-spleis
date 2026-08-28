package no.nav.helse.spleis.rest

import no.nav.helse.spleis.dto.HendelseDTO
import no.nav.helse.spleis.rest.dto.ApiArbeidsgiver
import no.nav.helse.spleis.rest.dto.ApiGenerasjon
import no.nav.helse.spleis.rest.dto.ApiGhostPeriode
import no.nav.helse.spleis.rest.dto.ApiPerson
import no.nav.helse.spleis.speil.dto.PersonDTO

internal fun mapTilPerson(person: PersonDTO, fnr: String, aktørId: String, hendelser: List<HendelseDTO>) =
    ApiPerson(
        aktorId = aktørId,
        fodselsnummer = fnr,
        arbeidsgivere = person.arbeidsgivere.map { arbeidsgiver ->
            ApiArbeidsgiver(
                organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                generasjoner = arbeidsgiver.generasjoner.map { generasjon ->
                    ApiGenerasjon(
                        id = generasjon.id,
                        perioder = generasjon.perioder.map { periode -> mapTidslinjeperiode(periode, hendelser) },
                        kildeTilGenerasjon = generasjon.kildeTilGenerasjon
                    )
                },
                ghostPerioder = arbeidsgiver.ghostPerioder.map { periode ->
                    ApiGhostPeriode(
                        id = periode.id,
                        fom = periode.fom,
                        tom = periode.tom,
                        skjaeringstidspunkt = periode.skjæringstidspunkt,
                        vilkarsgrunnlagId = periode.vilkårsgrunnlagId,
                        deaktivert = periode.deaktivert
                    )
                }
            )
        },
        dodsdato = person.dødsdato,
        versjon = person.versjon,
        vilkarsgrunnlag = person.vilkårsgrunnlag.map { (id, vilkårsgrunnlag) -> mapVilkårsgrunnlag(id, vilkårsgrunnlag) }
    )
