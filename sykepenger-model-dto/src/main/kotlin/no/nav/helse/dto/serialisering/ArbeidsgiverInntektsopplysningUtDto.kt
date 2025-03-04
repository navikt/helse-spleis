package no.nav.helse.dto.serialisering

import no.nav.helse.dto.PeriodeDto

data class ArbeidsgiverInntektsopplysningUtDto(
    val orgnummer: String,
    val gjelder: PeriodeDto,
    val faktaavklartInntekt: FaktaavklartInntektUtDto,
    val korrigertInntekt: SaksbehandlerUtDto?,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsattUtDto?
)
