package no.nav.helse.dto

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

sealed class BegrunnelseDto {
    data object SykepengedagerOppbrukt : BegrunnelseDto()
    data object SykepengedagerOppbruktOver67 : BegrunnelseDto()
    data object MinimumInntekt : BegrunnelseDto()
    data object MinimumInntektOver67 : BegrunnelseDto()
    data object EgenmeldingUtenforArbeidsgiverperiode : BegrunnelseDto()
    data object AndreYtelserForeldrepenger : BegrunnelseDto()
    data object AndreYtelserAap : BegrunnelseDto()
    data object AndreYtelserOmsorgspenger : BegrunnelseDto()
    data object AndreYtelserPleiepenger : BegrunnelseDto()
    data object AndreYtelserSvangerskapspenger : BegrunnelseDto()
    data object AndreYtelserOpplaringspenger : BegrunnelseDto()
    data object AndreYtelserDagpenger : BegrunnelseDto()
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
    data object SykepengerArbeidstakerOrdinær : KlassekodeDto("SPATORD")
    data object SelvstendigNæringsdrivendeOppgavepliktig : KlassekodeDto("SPSND-OP")
}

sealed class FeriepengerfagområdeDto {
    data object SPREF : FeriepengerfagområdeDto()
    data object SP : FeriepengerfagområdeDto()
}

sealed class FeriepengerendringskodeDto {
    data object NY : FeriepengerendringskodeDto()
    data object UEND : FeriepengerendringskodeDto()
    data object ENDR : FeriepengerendringskodeDto()
}

sealed class FeriepengerklassekodeDto(val verdi: String) {
    data object RefusjonFeriepengerIkkeOpplysningspliktig : FeriepengerklassekodeDto("SPREFAGFER-IOP")
    data object SykepengerArbeidstakerFeriepenger : FeriepengerklassekodeDto("SPATFER")
}
