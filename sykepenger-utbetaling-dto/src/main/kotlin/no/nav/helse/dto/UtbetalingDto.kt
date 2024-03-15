package no.nav.helse.dto

import java.time.LocalDate
import java.time.LocalDateTime

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