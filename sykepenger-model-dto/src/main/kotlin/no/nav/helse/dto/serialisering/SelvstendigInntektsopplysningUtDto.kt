package no.nav.helse.dto.serialisering

import no.nav.helse.dto.deserialisering.YrkesaktivitetstypeDto

data class SelvstendigInntektsopplysningUtDto(
    val faktaavklartInntekt: SelvstendigFaktaavklartInntektUtDto,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsattUtDto?,
    val yrkesaktivitetstype: YrkesaktivitetstypeDto
)
