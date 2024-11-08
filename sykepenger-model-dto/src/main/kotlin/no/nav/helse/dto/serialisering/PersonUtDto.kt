package no.nav.helse.dto.serialisering

import java.time.LocalDateTime
import no.nav.helse.dto.AlderDto

data class PersonUtDto(
    val fødselsnummer: String,
    val alder: AlderDto,
    val opprettet: LocalDateTime,
    val arbeidsgivere: List<ArbeidsgiverUtDto>,
    val infotrygdhistorikk: InfotrygdhistorikkUtDto,
    val vilkårsgrunnlagHistorikk: VilkårsgrunnlaghistorikkUtDto,
    val minimumSykdomsgradVurdering: MinimumSykdomsgradVurderingUtDto
)

