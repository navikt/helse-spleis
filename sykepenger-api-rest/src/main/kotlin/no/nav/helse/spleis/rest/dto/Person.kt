package no.nav.helse.spleis.rest.dto

import java.time.LocalDate
import java.util.UUID

data class Person(
    val aktorId: String,
    val fodselsnummer: String,
    val arbeidsgivere: List<Arbeidsgiver>,
    val dodsdato: LocalDate?,
    val versjon: Int,
    val vilkarsgrunnlag: List<Vilkarsgrunnlag>
)

data class Arbeidsgiver(
    val organisasjonsnummer: String,
    val generasjoner: List<Generasjon>,
    val ghostPerioder: List<GhostPeriode>
)

data class Generasjon(
    val id: UUID,
    val perioder: List<Tidslinjeperiode>,
    val kildeTilGenerasjon: UUID
)

data class GhostPeriode(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjaeringstidspunkt: LocalDate,
    val vilkarsgrunnlagId: UUID,
    val deaktivert: Boolean
)
