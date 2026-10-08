package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.InfotrygdFerieperiodeDto
import no.nav.helse.dto.MeldingsreferanseDto
import java.time.LocalDateTime
import java.util.*

data class InfotrygdhistorikkelementInnDto(
    val id: UUID,
    val tidsstempel: LocalDateTime,
    val hendelseId: MeldingsreferanseDto,
    val ferieperioder: List<InfotrygdFerieperiodeDto>,
    val arbeidsgiverutbetalingsperioder: List<InfotrygdArbeidsgiverutbetalingsperiodeInnDto>,
    val personutbetalingsperioder: List<InfotrygdPersonutbetalingsperiodeInnDto>,
    val oppdatert: LocalDateTime,
)
