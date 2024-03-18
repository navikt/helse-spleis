package no.nav.helse.dto.deserialisering

import java.time.LocalDateTime
import no.nav.helse.dto.AlderDto

data class PersonInnDto(
    val aktørId: String,
    val fødselsnummer: String,
    val alder: AlderDto,
    val opprettet: LocalDateTime,
    val arbeidsgivere: List<ArbeidsgiverInnDto>,
    val infotrygdhistorikk: InfotrygdhistorikkInnDto,
    val vilkårsgrunnlagHistorikk: VilkårsgrunnlaghistorikkInnDto
)