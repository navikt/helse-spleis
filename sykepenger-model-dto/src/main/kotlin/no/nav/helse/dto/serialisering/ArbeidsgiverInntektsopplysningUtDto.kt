package no.nav.helse.dto.serialisering

data class ArbeidsgiverInntektsopplysningUtDto(
    val orgnummer: String,
    val faktaavklartInntekt: ArbeidstakerFaktaavklartInntektUtDto,
    val korrigertInntekt: SaksbehandlerUtDto?,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsattUtDto?,
)
