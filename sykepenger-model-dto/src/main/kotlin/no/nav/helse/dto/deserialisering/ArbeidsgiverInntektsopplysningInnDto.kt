package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.PeriodeDto

data class ArbeidsgiverInntektsopplysningInnDto(
    val orgnummer: String,
    val gjelder: PeriodeDto,
    val inntektsopplysning: InntektsopplysningInnDto,
    val korrigertInntekt: SaksbehandlerInnDto?,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsattInnDto?
)
