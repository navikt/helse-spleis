package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.AlderDto
import java.time.LocalDateTime

data class PersonInnDto(
    val fødselsnummer: String,
    val alder: AlderDto,
    val opprettet: LocalDateTime,
    val arbeidsgivere: List<ArbeidsgiverInnDto>,
    val infotrygdhistorikk: InfotrygdhistorikkInnDto,
    val vilkårsgrunnlagHistorikk: VilkårsgrunnlaghistorikkInnDto,
    val minimumSykdomsgradVurdering: MinimumSykdomsgradVurderingInnDto,
)
