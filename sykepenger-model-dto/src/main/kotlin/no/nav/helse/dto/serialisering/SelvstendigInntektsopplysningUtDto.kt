package no.nav.helse.dto.serialisering

data class SelvstendigInntektsopplysningUtDto(
    val faktaavklartInntekt: SelvstendigFaktaavklartInntektUtDto,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsattUtDto?,
)
