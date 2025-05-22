package no.nav.helse.dto.deserialisering

data class ArbeidsgiverInntektsopplysningInnDto(
    val orgnummer: String,
    val faktaavklartInntekt: ArbeidstakerFaktaavklartInntektInnDto,
    val korrigertInntekt: SaksbehandlerInnDto?,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsattInnDto?
)
