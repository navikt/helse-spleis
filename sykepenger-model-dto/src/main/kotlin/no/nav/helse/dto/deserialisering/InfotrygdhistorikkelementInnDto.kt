package no.nav.helse.dto.deserialisering

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.InfotrygdFerieperiodeDto

data class InfotrygdhistorikkelementInnDto(
    val id: UUID,
    val tidsstempel: LocalDateTime,
    val hendelseId: UUID?,
    val ferieperioder: List<InfotrygdFerieperiodeDto>,
    val arbeidsgiverutbetalingsperioder: List<InfotrygdArbeidsgiverutbetalingsperiodeInnDto>,
    val personutbetalingsperioder: List<InfotrygdPersonutbetalingsperiodeInnDto>,
    val inntekter: List<InfotrygdInntektsopplysningInnDto>,
    val arbeidskategorikoder: Map<String, LocalDate>,
    val oppdatert: LocalDateTime
)