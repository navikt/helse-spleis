package no.nav.helse.person.inntekt

import no.nav.helse.dto.serialisering.InntektsopplysningUtDto

internal data object Infotrygd : Inntektsopplysning {

    override fun dto() = InntektsopplysningUtDto.InfotrygdDto

}
