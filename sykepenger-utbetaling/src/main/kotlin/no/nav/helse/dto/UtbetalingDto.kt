package no.nav.helse.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.SimuleringResultat

data class UtbetalingDto(
    val id: UUID,
    val korrelasjonsId: UUID,
    val periode: PeriodeDto,
    val utbetalingstidslinje: UtbetalingstidslinjeDto,
    val arbeidsgiverOppdrag: OppdragDto,
    val personOppdrag: OppdragDto,
    val tidsstempel: LocalDateTime,
    val tilstand: UtbetalingTilstandDto,
    val type: UtbetalingtypeDto,
    val maksdato: LocalDate,
    val forbrukteSykedager: Int?,
    val gjenståendeSykedager: Int?,
    val annulleringer: List<UUID>,
    val vurdering: UtbetalingVurderingDto?,
    val overføringstidspunkt: LocalDateTime?,
    val avstemmingsnøkkel: Long?,
    val avsluttet: LocalDateTime?,
    val oppdatert: LocalDateTime
)

data class UtbetalingVurderingDto(
    val godkjent: Boolean,
    val ident: String,
    val epost: String,
    val tidspunkt: LocalDateTime,
    val automatiskBehandling: Boolean
)

sealed class UtbetalingtypeDto {
    data object UTBETALING : UtbetalingtypeDto()
    data object ETTERUTBETALING : UtbetalingtypeDto()
    data object ANNULLERING : UtbetalingtypeDto()
    data object REVURDERING : UtbetalingtypeDto()
    data object FERIEPENGER : UtbetalingtypeDto()
}

sealed class UtbetalingTilstandDto {
    data object NY : UtbetalingTilstandDto()
    data object IKKE_UTBETALT : UtbetalingTilstandDto()
    data object IKKE_GODKJENT : UtbetalingTilstandDto()
    data object OVERFØRT : UtbetalingTilstandDto()
    data object UTBETALT : UtbetalingTilstandDto()
    data object GODKJENT : UtbetalingTilstandDto()
    data object GODKJENT_UTEN_UTBETALING : UtbetalingTilstandDto()
    data object ANNULLERT : UtbetalingTilstandDto()
    data object FORKASTET : UtbetalingTilstandDto()
}

data class UtbetalingstidslinjeDto(
    val dager: List<UtbetalingsdagDto>
)

sealed class UtbetalingsdagDto {
    abstract val dato: LocalDate
    abstract val økonomi: ØkonomiDto

    data class ArbeidsgiverperiodeDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiDto
    ) : UtbetalingsdagDto()
    data class ArbeidsgiverperiodeDagNavDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiDto
    ) : UtbetalingsdagDto()
    data class NavDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiDto
    ) : UtbetalingsdagDto()
    data class NavHelgDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiDto
    ) : UtbetalingsdagDto()
    data class FridagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiDto
    ) : UtbetalingsdagDto()
    data class ArbeidsdagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiDto
    ) : UtbetalingsdagDto()
    data class AvvistDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiDto,
        val begrunnelser: List<BegrunnelseDto>
    ) : UtbetalingsdagDto()
    data class ForeldetDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiDto
    ) : UtbetalingsdagDto()
    data class UkjentDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiDto
    ) : UtbetalingsdagDto()
}

sealed class BegrunnelseDto {
    data object SykepengedagerOppbrukt : BegrunnelseDto()
    data object SykepengedagerOppbruktOver67 : BegrunnelseDto()
    data object MinimumInntekt : BegrunnelseDto()
    data object MinimumInntektOver67 : BegrunnelseDto()
    data object EgenmeldingUtenforArbeidsgiverperiode : BegrunnelseDto()
    data object AndreYtelserForeldrepenger: BegrunnelseDto()
    data object AndreYtelserAap: BegrunnelseDto()
    data object AndreYtelserOmsorgspenger: BegrunnelseDto()
    data object AndreYtelserPleiepenger: BegrunnelseDto()
    data object AndreYtelserSvangerskapspenger: BegrunnelseDto()
    data object AndreYtelserOpplaringspenger: BegrunnelseDto()
    data object AndreYtelserDagpenger: BegrunnelseDto()
    data object MinimumSykdomsgrad : BegrunnelseDto()
    data object EtterDødsdato : BegrunnelseDto()
    data object Over70 : BegrunnelseDto()
    data object ManglerOpptjening : BegrunnelseDto()
    data object ManglerMedlemskap : BegrunnelseDto()
    data object NyVilkårsprøvingNødvendig : BegrunnelseDto()
}

data class OppdragDto(
    val mottaker: String,
    val fagområde: FagområdeDto,
    val linjer: List<UtbetalingslinjeDto>,
    val fagsystemId: String,
    val endringskode: EndringskodeDto,
    val nettoBeløp: Int,
    val overføringstidspunkt: LocalDateTime?,
    val avstemmingsnøkkel: Long?,
    val status: OppdragstatusDto?,
    val tidsstempel: LocalDateTime,
    val erSimulert: Boolean,
    val simuleringsResultat: SimuleringResultat?
)

sealed class FagområdeDto {
    data object SPREF : FagområdeDto()
    data object SP : FagområdeDto()
}
sealed class EndringskodeDto {
    data object NY : EndringskodeDto()
    data object UEND : EndringskodeDto()
    data object ENDR : EndringskodeDto()
}

sealed class OppdragstatusDto {
    data object OVERFØRT : OppdragstatusDto()
    data object AKSEPTERT : OppdragstatusDto()
    data object AKSEPTERT_MED_FEIL : OppdragstatusDto()
    data object AVVIST : OppdragstatusDto()
    data object FEIL : OppdragstatusDto()
}

data class UtbetalingslinjeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val satstype: SatstypeDto,
    val beløp: Int?,
    val grad: Int?,
    val refFagsystemId: String?,
    val delytelseId: Int,
    val refDelytelseId: Int?,
    val endringskode: EndringskodeDto,
    val klassekode: KlassekodeDto,
    val datoStatusFom: LocalDate?
)

sealed class KlassekodeDto(val verdi: String) {
    data object RefusjonIkkeOpplysningspliktig : KlassekodeDto("SPREFAG-IOP")
    data object RefusjonFeriepengerIkkeOpplysningspliktig : KlassekodeDto("SPREFAGFER-IOP")
    data object SykepengerArbeidstakerOrdinær : KlassekodeDto("SPATORD")
    data object SykepengerArbeidstakerFeriepenger : KlassekodeDto("SPATFER")
}

sealed class SatstypeDto {
    data object Daglig : SatstypeDto()
    data object Engang : SatstypeDto()
}