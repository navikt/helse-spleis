package no.nav.helse.serde.mapping

import no.nav.helse.dto.MedlemskapsvurderingDto

internal enum class JsonMedlemskapstatus {
    JA, VET_IKKE, NEI, UAVKLART_MED_BRUKERSPØRSMÅL;

    fun tilDto() = when (this) {
        JA -> MedlemskapsvurderingDto.Ja
        VET_IKKE -> MedlemskapsvurderingDto.VetIkke
        NEI -> MedlemskapsvurderingDto.Nei
        UAVKLART_MED_BRUKERSPØRSMÅL -> MedlemskapsvurderingDto.UavklartMedBrukerspørsmål
    }
}
