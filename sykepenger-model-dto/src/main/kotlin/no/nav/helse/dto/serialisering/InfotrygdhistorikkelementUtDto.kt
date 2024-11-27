package no.nav.helse.dto.serialisering

import no.nav.helse.dto.InfotrygdFerieperiodeDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class InfotrygdhistorikkelementUtDto(
    val id: UUID,
    val tidsstempel: LocalDateTime,
    val hendelseId: UUID,
    val ferieperioder: List<InfotrygdFerieperiodeDto>,
    val arbeidsgiverutbetalingsperioder: List<InfotrygdArbeidsgiverutbetalingsperiodeUtDto>,
    val personutbetalingsperioder: List<InfotrygdPersonutbetalingsperiodeUtDto>,
    val inntekter: List<InfotrygdInntektsopplysningUtDto>,
    val arbeidskategorikoder: Map<String, LocalDate>,
    val oppdatert: LocalDateTime,
)
