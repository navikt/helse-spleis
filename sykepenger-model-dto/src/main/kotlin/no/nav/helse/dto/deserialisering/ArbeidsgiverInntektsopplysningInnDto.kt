package no.nav.helse.dto.deserialisering

data class ArbeidsgiverInntektsopplysningInnDto(
    val orgnummer: String,
    val faktaavklartInntekt: FaktaavklartInntektInnDto,
    val korrigertInntekt: SaksbehandlerInnDto?,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsattInnDto?
)
