package no.nav.helse.dto

sealed class BegrunnelseDto {
    data object SykepengedagerOppbrukt : BegrunnelseDto()
    data object SykepengedagerOppbruktOver67 : BegrunnelseDto()
    data object MinimumInntekt : BegrunnelseDto()
    data object MinimumInntektOver67 : BegrunnelseDto()
    data object EgenmeldingUtenforArbeidsgiverperiode : BegrunnelseDto()
    data object MeldingTilNavDagUtenforVentetid : BegrunnelseDto()
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
