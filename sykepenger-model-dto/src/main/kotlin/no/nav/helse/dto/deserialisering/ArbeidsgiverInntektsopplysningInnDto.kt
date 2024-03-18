package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.RefusjonsopplysningerDto

data class ArbeidsgiverInntektsopplysningInnDto(
    val orgnummer: String,
    val gjelder: PeriodeDto,
    val inntektsopplysning: InntektsopplysningInnDto,
    val refusjonsopplysninger: RefusjonsopplysningerDto
)