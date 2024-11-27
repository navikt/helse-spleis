package no.nav.helse.dto.serialisering

import no.nav.helse.dto.AlderDto
import java.time.LocalDateTime

data class PersonUtDto(
    val fødselsnummer: String,
    val alder: AlderDto,
    val opprettet: LocalDateTime,
    val arbeidsgivere: List<ArbeidsgiverUtDto>,
    val infotrygdhistorikk: InfotrygdhistorikkUtDto,
    val vilkårsgrunnlagHistorikk: VilkårsgrunnlaghistorikkUtDto,
    val minimumSykdomsgradVurdering: MinimumSykdomsgradVurderingUtDto,
)
