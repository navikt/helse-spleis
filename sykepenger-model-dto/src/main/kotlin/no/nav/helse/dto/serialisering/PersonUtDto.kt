package no.nav.helse.dto.serialisering

import java.time.LocalDateTime
import no.nav.helse.dto.AlderDto
import no.nav.helse.dto.InfotrygdhistorikkDto

data class PersonUtDto(
    val aktørId: String,
    val fødselsnummer: String,
    val alder: AlderDto,
    val opprettet: LocalDateTime,
    val arbeidsgivere: List<ArbeidsgiverUtDto>,
    val infotrygdhistorikk: InfotrygdhistorikkDto,
    val vilkårsgrunnlagHistorikk: VilkårsgrunnlaghistorikkUtDto
)

