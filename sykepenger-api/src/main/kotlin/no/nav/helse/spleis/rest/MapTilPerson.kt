package no.nav.helse.spleis.rest

import no.nav.helse.spleis.dto.HendelseDTO
import no.nav.helse.spleis.rest.dto.Arbeidsgiver
import no.nav.helse.spleis.rest.dto.Generasjon
import no.nav.helse.spleis.rest.dto.GhostPeriode
import no.nav.helse.spleis.rest.dto.Person
import no.nav.helse.spleis.speil.dto.PersonDTO

internal fun mapTilPerson(person: PersonDTO, fnr: String, aktørId: String, hendelser: List<HendelseDTO>) =
    Person(
        aktorId = aktørId,
        fodselsnummer = fnr,
        arbeidsgivere = person.arbeidsgivere.map { arbeidsgiver ->
            Arbeidsgiver(
                organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                generasjoner = arbeidsgiver.generasjoner.map { generasjon ->
                    Generasjon(
                        id = generasjon.id,
                        perioder = generasjon.perioder.map { periode -> mapTidslinjeperiode(periode, hendelser) },
                        kildeTilGenerasjon = generasjon.kildeTilGenerasjon
                    )
                },
                ghostPerioder = arbeidsgiver.ghostPerioder.map { periode ->
                    GhostPeriode(
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
