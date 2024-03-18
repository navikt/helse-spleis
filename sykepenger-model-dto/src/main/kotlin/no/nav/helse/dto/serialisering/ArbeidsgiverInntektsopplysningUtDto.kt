package no.nav.helse.dto.serialisering

import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.RefusjonsopplysningerDto

data class ArbeidsgiverInntektsopplysningUtDto(
    val orgnummer: String,
    val gjelder: PeriodeDto,
    val inntektsopplysning: InntektsopplysningUtDto,
    val refusjonsopplysninger: RefusjonsopplysningerDto
)