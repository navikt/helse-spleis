package no.nav.helse.dto.serialisering

import no.nav.helse.dto.PeriodeDto

data class InfotrygdArbeidsgiverutbetalingsperiodeUtDto(
    val orgnr: String,
    val periode: PeriodeDto
)
