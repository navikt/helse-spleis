package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.PeriodeDto

data class InfotrygdArbeidsgiverutbetalingsperiodeInnDto(
    val orgnr: String,
    val periode: PeriodeDto
)
