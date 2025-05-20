package no.nav.helse.dto.deserialisering

data class SelvstendigInntektsopplysningInnDto(
    val faktaavklartInntekt: FaktaavklartInntektInnDto,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsattInnDto?
)
