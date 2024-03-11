package no.nav.helse.memento

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.Utbetalingslinje

data class UtbetalingMemento(
    val id: UUID,
    val korrelasjonsId: UUID,
    val periode: PeriodeMemento,
    val utbetalingstidslinje: UtbetalingstidslinjeMemento,
    val arbeidsgiverOppdrag: OppdragMemento,
    val personOppdrag: OppdragMemento,
    val tidsstempel: LocalDateTime,
    val tilstand: UtbetalingTilstandMemento,
    val type: UtbetalingtypeMemento,
    val maksdato: LocalDate,
    val forbrukteSykedager: Int?,
    val gjenståendeSykedager: Int?,
    val annulleringer: List<UUID>,
    val vurdering: UtbetalingVurderingMemento?,
    val overføringstidspunkt: LocalDateTime?,
    val avstemmingsnøkkel: Long?,
    val avsluttet: LocalDateTime?,
    val oppdatert: LocalDateTime
)

data class UtbetalingVurderingMemento(
    val godkjent: Boolean,
    val ident: String,
    val epost: String,
    val tidspunkt: LocalDateTime,
    val automatiskBehandling: Boolean
)

sealed class UtbetalingtypeMemento {
    data object UTBETALING : UtbetalingtypeMemento()
    data object ETTERUTBETALING : UtbetalingtypeMemento()
    data object ANNULLERING : UtbetalingtypeMemento()
    data object REVURDERING : UtbetalingtypeMemento()
    data object FERIEPENGER : UtbetalingtypeMemento()
}

sealed class UtbetalingTilstandMemento {
    data object NY : UtbetalingTilstandMemento()
    data object IKKE_UTBETALT : UtbetalingTilstandMemento()
    data object IKKE_GODKJENT : UtbetalingTilstandMemento()
    data object OVERFØRT : UtbetalingTilstandMemento()
    data object UTBETALT : UtbetalingTilstandMemento()
    data object GODKJENT : UtbetalingTilstandMemento()
    data object GODKJENT_UTEN_UTBETALING : UtbetalingTilstandMemento()
    data object ANNULLERT : UtbetalingTilstandMemento()
    data object FORKASTET : UtbetalingTilstandMemento()
}

data class UtbetalingstidslinjeMemento(
    val dager: List<UtbetalingsdagMemento>
)

sealed class UtbetalingsdagMemento {
    abstract val dato: LocalDate
    abstract val økonomi: ØkonomiMemento

    data class ArbeidsgiverperiodeDagMemento(
        override val dato: LocalDate,
        override val økonomi: ØkonomiMemento
    ) : UtbetalingsdagMemento()
    data class ArbeidsgiverperiodeDagNavMemento(
        override val dato: LocalDate,
        override val økonomi: ØkonomiMemento
    ) : UtbetalingsdagMemento()
    data class NavDagMemento(
        override val dato: LocalDate,
        override val økonomi: ØkonomiMemento
    ) : UtbetalingsdagMemento()
    data class NavHelgDagMemento(
        override val dato: LocalDate,
        override val økonomi: ØkonomiMemento
    ) : UtbetalingsdagMemento()
    data class FridagMemento(
        override val dato: LocalDate,
        override val økonomi: ØkonomiMemento
    ) : UtbetalingsdagMemento()
    data class ArbeidsdagMemento(
        override val dato: LocalDate,
        override val økonomi: ØkonomiMemento
    ) : UtbetalingsdagMemento()
    data class AvvistDagMemento(
        override val dato: LocalDate,
        override val økonomi: ØkonomiMemento,
        val begrunnelser: List<BegrunnelseMemento>
    ) : UtbetalingsdagMemento()
    data class ForeldetDagMemento(
        override val dato: LocalDate,
        override val økonomi: ØkonomiMemento
    ) : UtbetalingsdagMemento()
    data class UkjentDagMemento(
        override val dato: LocalDate,
        override val økonomi: ØkonomiMemento
    ) : UtbetalingsdagMemento()
}

sealed class BegrunnelseMemento {
    data object SykepengedagerOppbrukt : BegrunnelseMemento()
    data object SykepengedagerOppbruktOver67 : BegrunnelseMemento()
    data object MinimumInntekt : BegrunnelseMemento()
    data object MinimumInntektOver67 : BegrunnelseMemento()
    data object EgenmeldingUtenforArbeidsgiverperiode : BegrunnelseMemento()
    data object AndreYtelserForeldrepenger: BegrunnelseMemento()
    data object AndreYtelserAap: BegrunnelseMemento()
    data object AndreYtelserOmsorgspenger: BegrunnelseMemento()
    data object AndreYtelserPleiepenger: BegrunnelseMemento()
    data object AndreYtelserSvangerskapspenger: BegrunnelseMemento()
    data object AndreYtelserOpplaringspenger: BegrunnelseMemento()
    data object AndreYtelserDagpenger: BegrunnelseMemento()
    data object MinimumSykdomsgrad : BegrunnelseMemento()
    data object EtterDødsdato : BegrunnelseMemento()
    data object Over70 : BegrunnelseMemento()
    data object ManglerOpptjening : BegrunnelseMemento()
    data object ManglerMedlemskap : BegrunnelseMemento()
    data object NyVilkårsprøvingNødvendig : BegrunnelseMemento()
}

data class OppdragMemento(
    val mottaker: String,
    val fagområde: FagområdeMemento,
    val linjer: List<UtbetalingslinjeMemento>,
    val fagsystemId: String,
    val endringskode: EndringskodeMemento,
    val nettoBeløp: Int,
    val overføringstidspunkt: LocalDateTime?,
    val avstemmingsnøkkel: Long?,
    val status: OppdragstatusMemento?,
    val tidsstempel: LocalDateTime,
    val erSimulert: Boolean,
    val simuleringsResultat: SimuleringResultat?
)

sealed class FagområdeMemento {
    data object SPREF : FagområdeMemento()
    data object SP : FagområdeMemento()
}
sealed class EndringskodeMemento {
    data object NY : EndringskodeMemento()
    data object UEND : EndringskodeMemento()
    data object ENDR : EndringskodeMemento()
}

sealed class OppdragstatusMemento {
    data object OVERFØRT : OppdragstatusMemento()
    data object AKSEPTERT : OppdragstatusMemento()
    data object AKSEPTERT_MED_FEIL : OppdragstatusMemento()
    data object AVVIST : OppdragstatusMemento()
    data object FEIL : OppdragstatusMemento()
}

data class UtbetalingslinjeMemento(
    val fom: LocalDate,
    val tom: LocalDate,
    val satstype: SatstypeMemento,
    val beløp: Int?,
    val grad: Int?,
    val refFagsystemId: String?,
    val delytelseId: Int,
    val refDelytelseId: Int?,
    val endringskode: EndringskodeMemento,
    val klassekode: KlassekodeMemento,
    val datoStatusFom: LocalDate?
)

sealed class KlassekodeMemento(val verdi: String) {
    data object RefusjonIkkeOpplysningspliktig : KlassekodeMemento("SPREFAG-IOP")
    data object RefusjonFeriepengerIkkeOpplysningspliktig : KlassekodeMemento("SPREFAGFER-IOP")
    data object SykepengerArbeidstakerOrdinær : KlassekodeMemento("SPATORD")
    data object SykepengerArbeidstakerFeriepenger : KlassekodeMemento("SPATFER")
}

sealed class SatstypeMemento {
    data object Daglig : SatstypeMemento()
    data object Engang : SatstypeMemento()
}