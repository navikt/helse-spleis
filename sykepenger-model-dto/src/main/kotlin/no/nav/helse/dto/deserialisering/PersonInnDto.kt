package no.nav.helse.dto.deserialisering

import java.time.LocalDateTime
import no.nav.helse.dto.AlderDto
import no.nav.helse.dto.InfotrygdhistorikkDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlaghistorikkUtDto

data class PersonInnDto(
    val aktørId: String,
    val fødselsnummer: String,
    val alder: AlderDto,
    val opprettet: LocalDateTime,
    val arbeidsgivere: List<ArbeidsgiverInnDto>,
    val infotrygdhistorikk: InfotrygdhistorikkDto,
    val vilkårsgrunnlagHistorikk: VilkårsgrunnlaghistorikkInnDto
)