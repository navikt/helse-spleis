package no.nav.helse.dto.deserialisering

import java.time.LocalDateTime
import no.nav.helse.dto.AlderDto
import no.nav.helse.dto.PeriodeDto

data class PersonInnDto(
    val fødselsnummer: String,
    val alder: AlderDto,
    val opprettet: LocalDateTime,
    val arbeidsgivere: List<ArbeidsgiverInnDto>,
    val infotrygdhistorikk: InfotrygdhistorikkInnDto,
    val vilkårsgrunnlagHistorikk: VilkårsgrunnlaghistorikkInnDto,
    val minimumSykdomsgradVurdering: MinimumSykdomsgradVurderingInnDto,
    val skjæringstidspunkter: List<PeriodeDto>
)
