package no.nav.helse.spleis.rest.dto

import java.time.LocalDate
import java.util.UUID

data class ApiPerson(
    val aktorId: String,
    val fodselsnummer: String,
    val arbeidsgivere: List<ApiArbeidsgiver>,
    val dodsdato: LocalDate?,
    val versjon: Int,
    val vilkarsgrunnlag: List<ApiVilkarsgrunnlag>
)

data class ApiArbeidsgiver(
    val organisasjonsnummer: String,
    val generasjoner: List<ApiGenerasjon>,
    val ghostPerioder: List<ApiGhostPeriode>
)

data class ApiGenerasjon(
    val id: UUID,
    val perioder: List<ApiTidslinjeperiode>,
    val kildeTilGenerasjon: UUID
)

data class ApiGhostPeriode(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjaeringstidspunkt: LocalDate,
    val vilkarsgrunnlagId: UUID,
    val deaktivert: Boolean
)
