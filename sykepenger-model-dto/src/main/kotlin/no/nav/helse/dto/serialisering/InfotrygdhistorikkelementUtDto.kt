package no.nav.helse.dto.serialisering

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dto.InfotrygdFerieperiodeDto
import no.nav.helse.dto.MeldingsreferanseDto

data class InfotrygdhistorikkelementUtDto(
    val id: UUID,
    val tidsstempel: LocalDateTime,
    val hendelseId: MeldingsreferanseDto,
    val ferieperioder: List<InfotrygdFerieperiodeDto>,
    val arbeidsgiverutbetalingsperioder: List<InfotrygdArbeidsgiverutbetalingsperiodeUtDto>,
    val personutbetalingsperioder: List<InfotrygdPersonutbetalingsperiodeUtDto>,
    val oppdatert: LocalDateTime
)
