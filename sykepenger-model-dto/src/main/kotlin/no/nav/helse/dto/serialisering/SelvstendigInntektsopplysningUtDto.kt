package no.nav.helse.dto.serialisering

data class SelvstendigInntektsopplysningUtDto(
    val faktaavklartInntekt: FaktaavklartInntektUtDto,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsattUtDto?
)
