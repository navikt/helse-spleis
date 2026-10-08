package no.nav.helse.dto.deserialisering

data class SelvstendigInntektsopplysningInnDto(
    val faktaavklartInntekt: SelvstendigFaktaavklartInntektInnDto,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsattInnDto?,
)
