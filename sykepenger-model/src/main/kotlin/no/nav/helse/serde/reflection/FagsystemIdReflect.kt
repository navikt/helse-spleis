package no.nav.helse.serde.reflection

import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.serde.reflection.ReflectInstance.Companion.maybe
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.FagsystemId
import no.nav.helse.utbetalingslinjer.FagsystemId.*
import java.time.LocalDateTime

enum class FagsystemTilstandType {
    AKTIV, ANNULLERING_OVERFØRT, ANNULLERING_SENDT, ANNULLERT,
    AVVIST, INITIELL, NY, UBETALT, UTBETALING_OVERFØRT, UTBETALING_SENDT;

    internal companion object {
        fun fraTilstand(tilstand: Tilstand) = when (tilstand) {
            is Aktiv -> AKTIV
            is AnnulleringOverført -> ANNULLERING_OVERFØRT
            is AnnulleringSendt -> ANNULLERING_SENDT
            is Annullert -> ANNULLERT
            is Avvist -> AVVIST
            is Initiell -> INITIELL
            is Ny -> NY
            is Ubetalt -> UBETALT
            is UtbetalingOverført -> UTBETALING_OVERFØRT
            is UtbetalingSendt -> UTBETALING_SENDT
            else -> throw IllegalStateException("Ukjent tilstand ${tilstand::class.simpleName}")
        }

        fun tilTilstand(type: FagsystemTilstandType) = when (type) {
            AKTIV -> Aktiv
            ANNULLERING_OVERFØRT -> AnnulleringOverført
            ANNULLERING_SENDT -> AnnulleringSendt
            ANNULLERT -> Annullert
            AVVIST -> Avvist
            NY -> Ny
            UBETALT -> Ubetalt
            UTBETALING_OVERFØRT -> UtbetalingOverført
            UTBETALING_SENDT -> UtbetalingSendt
            else -> throw IllegalArgumentException("støtter ikke å gjenopprette tilstand $type")
        }
    }
}

internal class FagsystemIdReflect(private val fagsystemId: FagsystemId) {

    internal fun toMap() = mutableMapOf<String, Any>(
        "fagsystemId" to fagsystemId["fagsystemId"],
        "fagområde" to fagsystemId.get<Fagområde>("fagområde").verdi,
        "mottaker" to fagsystemId["mottaker"],
        "tilstand" to FagsystemTilstandType.fraTilstand(fagsystemId.get<Tilstand>("tilstand")),
        "utbetalinger" to fagsystemId.get<List<Utbetaling>>("utbetalinger").map { UtbetalingReflect(it).toMap() },
        "forkastet" to fagsystemId.get<List<Utbetaling>>("forkastet").map { UtbetalingReflect(it).toMap() }
    )

    internal class UtbetalingReflect(private val utbetaling: Utbetaling) {
        private val oppdrag get() = OppdragReflect(utbetaling["oppdrag"]).toMap()
        private val utbetalingstidslinje get() = UtbetalingstidslinjeReflect(utbetaling["utbetalingstidslinje"]).toMap()

        internal fun toMap(): MutableMap<String, Any?> = mutableMapOf(
            "oppdrag" to oppdrag,
            "utbetalingstidslinje" to utbetalingstidslinje,
            "type" to utbetaling.get<Utbetaling.Utbetalingtype>("type"),
            "maksdato" to utbetaling["maksdato"],
            "opprettet" to utbetaling["opprettet"],
            "godkjentAv" to utbetaling.maybe<Triple<String, String, LocalDateTime>>("godkjentAv")?.let { (ident, epost, tidsstempel) ->
                mapOf(
                    "ident" to ident,
                    "epost" to epost,
                    "tidsstempel" to tidsstempel
                )
            },
            "automatiskBehandlet" to utbetaling["automatiskBehandlet"],
            "sendt" to utbetaling["sendt"],
            "avstemmingsnøkkel" to utbetaling["avstemmingsnøkkel"],
            "overføringstidspunkt" to utbetaling["overføringstidspunkt"],
            "avsluttet" to utbetaling["avsluttet"]
        )
    }
}
