package no.nav.helse.inspectors.view

import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

data class UtbetalingView(
    val id: UUID,
    val korrelasjonsId: UUID,
    val periode: Periode,
    val utbetalingstidslinje: Utbetalingstidslinje,
    val arbeidsgiverOppdrag: Oppdrag,
    val personOppdrag: Oppdrag,
    val status: Utbetalingstatus,
    val type: Utbetalingtype,
    val annulleringer: List<UUID>,
    val erAvsluttet: Boolean
)

val Utbetaling.view: UtbetalingView
    get() = UtbetalingView(
        id = id,
        korrelasjonsId = korrelasjonsId,
        periode = periode,
        utbetalingstidslinje = utbetalingstidslinje,
        arbeidsgiverOppdrag = arbeidsgiverOppdrag,
        personOppdrag = personOppdrag,
        status = tilstand.status,
        type = type,
        annulleringer = annulleringer.map { it.id },
        erAvsluttet = erAvsluttet()
    )
