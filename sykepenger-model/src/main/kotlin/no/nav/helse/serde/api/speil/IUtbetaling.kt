package no.nav.helse.serde.api.speil

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.serde.api.dto.SammenslåttDag
import no.nav.helse.serde.api.dto.SpeilOppdrag
import no.nav.helse.serde.api.dto.Utbetaling
import no.nav.helse.serde.api.dto.Utbetalingstatus
import no.nav.helse.serde.api.dto.Utbetalingstidslinjedag
import no.nav.helse.serde.api.dto.Utbetalingtype

internal class IUtbetaling(
    val id: UUID,
    val korrelasjonsId: UUID,
    val beregning: Tidslinjeberegninger.ITidslinjeberegning,
    val opprettet: LocalDateTime,
    val utbetalingstidslinje: List<Utbetalingstidslinjedag>,
    val maksdato: LocalDate,
    val gjenståendeSykedager: Int?,
    val forbrukteSykedager: Int?,
    type: String,
    tilstand: String,
    private val arbeidsgiverNettoBeløp: Int,
    private val personNettoBeløp: Int,
    private val arbeidsgiverFagsystemId: String,
    private val personFagsystemId: String,
    private val vurdering: Utbetaling.Vurdering?,
    private val oppdrag: Map<String, SpeilOppdrag>,
    private val erTilGodkjenning: Boolean
) {
    private val type: Utbetalingtype = utledType(type)
    private val status: Utbetalingstatus = utledStatus(this.type, tilstand)


    fun annulleringFor(other: IUtbetaling) = this.type == Utbetalingtype.ANNULLERING && this.korrelasjonsId == other.korrelasjonsId

    fun toDTO(): Utbetaling {
        return Utbetaling(
            type = type,
            status = status,
            arbeidsgiverNettoBeløp = arbeidsgiverNettoBeløp,
            personNettoBeløp = personNettoBeløp,
            arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
            personFagsystemId = personFagsystemId,
            vurdering = vurdering,
            id = id,
            oppdrag = oppdrag,
            tilGodkjenning = erTilGodkjenning
        )
    }

    internal fun sammenslåttTidslinje(fom: LocalDate, tom: LocalDate): List<SammenslåttDag> {
        return beregning.sammenslåttTidslinje(utbetalingstidslinje, fom, tom)
    }

    internal companion object {
        internal fun Map<UUID, IUtbetaling>.leggTil(utbetaling: IUtbetaling): Map<UUID, IUtbetaling> {
            return this.toMutableMap().also {
                it.putIfAbsent(utbetaling.korrelasjonsId, utbetaling)
            }
        }

        private fun utledType(type: String) = when (type) {
            "UTBETALING" -> Utbetalingtype.UTBETALING
            "ETTERUTBETALING" -> Utbetalingtype.ETTERUTBETALING
            "ANNULLERING" -> Utbetalingtype.ANNULLERING
            "REVURDERING" -> Utbetalingtype.REVURDERING
            else -> error("har ingen mappingregel for $type")
        }

        private fun utledStatus(type: Utbetalingtype, tilstand: String): Utbetalingstatus {
            return when (tilstand) {
                "Annullert" -> Utbetalingstatus.Annullert
                "Godkjent" -> Utbetalingstatus.Godkjent
                "GodkjentUtenUtbetaling" -> Utbetalingstatus.GodkjentUtenUtbetaling
                "IkkeGodkjent" -> when (type) {
                    Utbetalingtype.REVURDERING -> Utbetalingstatus.IkkeGodkjent
                    else -> error("forsøker å mappe en IKKE_GODKJENT-utbetaling til Speil, som ikke er revurdering")
                }
                "Overført" -> Utbetalingstatus.Overført
                "Ubetalt" -> Utbetalingstatus.Ubetalt
                "Utbetalt" -> Utbetalingstatus.Utbetalt
                else -> error("har ingen mappingregel for $tilstand")
            }
        }
    }
}