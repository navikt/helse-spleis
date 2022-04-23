package no.nav.helse.serde.api.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.api.DagtypeDTO
import no.nav.helse.serde.api.SykdomstidslinjedagDTO
import no.nav.helse.serde.api.TilstandstypeDTO
import no.nav.helse.serde.api.UtbetalingstidslinjedagDTO
import no.nav.helse.serde.api.builders.OppdragDTO
import no.nav.helse.serde.reflection.Utbetalingstatus
import no.nav.helse.serde.reflection.Utbetalingstatus.ANNULLERT
import no.nav.helse.serde.reflection.Utbetalingstatus.FORKASTET
import no.nav.helse.serde.reflection.Utbetalingstatus.GODKJENT_UTEN_UTBETALING
import no.nav.helse.serde.reflection.Utbetalingstatus.UTBETALING_FEILET
import no.nav.helse.serde.reflection.Utbetalingstatus.UTBETALT
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype.ANNULLERING
import no.nav.helse.utbetalingslinjer.Utbetaling.Utbetalingtype.REVURDERING

data class UtbetalingshistorikkElementDTO(
    val hendelsetidslinje: List<SykdomstidslinjedagDTO>,
    val beregnettidslinje: List<SykdomstidslinjedagDTO>,
    val vilkårsgrunnlagHistorikkId: UUID,
    val tidsstempel: LocalDateTime,
    val utbetaling: UtbetalingDTO
) {
    val beregningId = utbetaling.beregningId

    data class UtbetalingDTO internal constructor(
        val utbetalingId: UUID,
        val korrelasjonsId: UUID,
        val utbetalingstidslinje: List<UtbetalingstidslinjedagDTO>,
        val beregningId: UUID,
        val type: Utbetalingtype,
        val maksdato: LocalDate,
        val status: Utbetalingstatus,
        val gjenståendeSykedager: Int?,
        val forbrukteSykedager: Int?,
        val arbeidsgiverNettoBeløp: Int,
        val personNettoBeløp: Int,
        val arbeidsgiverOppdrag: OppdragDTO,
        val personOppdrag: OppdragDTO,
        val tidsstempel: LocalDateTime,
        val vurdering: VurderingDTO?
    ) {
        @Deprecated("hent fra oppdraget")
        val arbeidsgiverFagsystemId = arbeidsgiverOppdrag.fagsystemId
        @Deprecated("hent fra oppdraget")
        val personFagsystemId = personOppdrag.fagsystemId

        fun tilstandFor(periode: Periode) = when (type) {
            ANNULLERING -> when (status) {
                ANNULLERT -> TilstandstypeDTO.Annullert
                UTBETALING_FEILET -> TilstandstypeDTO.AnnulleringFeilet
                else -> TilstandstypeDTO.TilAnnullering
            }
            REVURDERING -> TilstandstypeDTO.Utbetalt
            else -> when (status) {
                UTBETALT,GODKJENT_UTEN_UTBETALING -> {
                    val kunFerie = kunFerie(periode)
                    val harUtbetaling = harUtbetaling(periode)
                    when {
                        kunFerie -> TilstandstypeDTO.KunFerie
                        harUtbetaling -> TilstandstypeDTO.Utbetalt
                        else -> TilstandstypeDTO.IngenUtbetaling
                    }
                }
                UTBETALING_FEILET -> TilstandstypeDTO.Feilet
                else -> TilstandstypeDTO.TilUtbetaling
            }
        }

        private fun harUtbetaling(periode: Periode) =
            utbetalingstidslinje.filter { it.dato in periode }
                .any { it.type in setOf(DagtypeDTO.NavDag) }

        private fun kunFerie(periode: Periode) =
            utbetalingstidslinje.filter { it.dato in periode }
                .all { it.type in setOf(DagtypeDTO.NavHelgDag, DagtypeDTO.Feriedag, DagtypeDTO.Helgedag) }

        private fun annulleringFor(utbetalinger: List<UtbetalingshistorikkElementDTO>) =
            utbetalinger
                .map { it.utbetaling }
                .filter { it.erAnnullering() && it.status != FORKASTET }
                .firstOrNull { it.korrelasjonsId == this.korrelasjonsId }

        private fun erUtbetalt() = status == UTBETALT || status == ANNULLERT

        fun erAnnullering() = type == ANNULLERING

        data class VurderingDTO(
            val godkjent: Boolean,
            val tidsstempel: LocalDateTime,
            val automatisk: Boolean,
            val ident: String
        )

        internal companion object {
            internal fun utbetalingFor(liste: List<UtbetalingshistorikkElementDTO>, utbetalingId: UUID) =
                liste.firstOrNull { it.utbetaling.utbetalingId == utbetalingId }?.utbetaling

            internal fun sisteUtbetalingFor(liste: List<UtbetalingshistorikkElementDTO>, utbetaling: UtbetalingDTO) =
                liste
                    .filter { it.utbetaling.erUtbetalt() }
                    .sortedBy { it.tidsstempel }
                    .lastOrNull { it.utbetaling.korrelasjonsId == utbetaling.korrelasjonsId }
                    ?.utbetaling

            internal fun tilstandFor(periode: Periode, tilstandstype: TilstandType, utbetaling: UtbetalingDTO?, utbetalinger: List<UtbetalingshistorikkElementDTO>): TilstandstypeDTO {
                val annullering = utbetaling?.annulleringFor(utbetalinger)

                return when (tilstandstype) {
                    TilstandType.START,
                    TilstandType.MOTTATT_SYKMELDING_FERDIG_FORLENGELSE,
                    TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE,
                    TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP,
                    TilstandType.MOTTATT_SYKMELDING_UFERDIG_GAP,
                    TilstandType.AVVENTER_SØKNAD_FERDIG_GAP,
                    TilstandType.AVVENTER_SØKNAD_UFERDIG_GAP,
                    TilstandType.AVVENTER_VILKÅRSPRØVING,
                    TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK,
                    TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP,
                    TilstandType.AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE,
                    TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_GAP,
                    TilstandType.AVVENTER_SØKNAD_UFERDIG_FORLENGELSE,
                    TilstandType.AVVENTER_SØKNAD_FERDIG_FORLENGELSE,
                    TilstandType.AVVENTER_SIMULERING,
                    TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING,
                    TilstandType.AVVENTER_SIMULERING_REVURDERING,
                    TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING,
                    TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING,
                    TilstandType.AVVENTER_HISTORIKK_REVURDERING,
                    TilstandType.AVVENTER_REVURDERING,
                    TilstandType.AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE,
                    TilstandType.AVVENTER_HISTORIKK -> TilstandstypeDTO.Venter
                    TilstandType.AVVENTER_UFERDIG,
                    TilstandType.AVVENTER_BLOKKERENDE_PERIODE,
                    TilstandType.AVVENTER_ARBEIDSGIVERE -> TilstandstypeDTO.VenterPåKiling
                    TilstandType.TIL_INFOTRYGD -> TilstandstypeDTO.TilInfotrygd
                    TilstandType.UTBETALING_FEILET -> TilstandstypeDTO.Feilet
                    TilstandType.REVURDERING_FEILET -> TilstandstypeDTO.RevurderingFeilet
                    TilstandType.TIL_UTBETALING -> TilstandstypeDTO.TilUtbetaling
                    TilstandType.AVVENTER_GODKJENNING_REVURDERING,
                    TilstandType.AVVENTER_GODKJENNING -> TilstandstypeDTO.Oppgaver
                    TilstandType.AVSLUTTET,
                    TilstandType.AVSLUTTET_UTEN_UTBETALING -> when {
                        annullering != null -> annullering.tilstandFor(periode)
                        else -> utbetaling?.tilstandFor(periode) ?: TilstandstypeDTO.IngenUtbetaling
                    }
                }
            }
        }
    }
}
