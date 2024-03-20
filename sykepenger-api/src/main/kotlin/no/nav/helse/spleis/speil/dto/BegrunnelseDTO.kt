package no.nav.helse.spleis.speil.dto

import no.nav.helse.dto.BegrunnelseDto

enum class BegrunnelseDTO {
    SykepengedagerOppbrukt,
    SykepengedagerOppbruktOver67,
    MinimumInntekt,
    MinimumInntektOver67,
    EgenmeldingUtenforArbeidsgiverperiode,
    AndreYtelserAap,
    AndreYtelserDagpenger,
    AndreYtelserForeldrepenger,
    AndreYtelserOmsorgspenger,
    AndreYtelserOpplaringspenger,
    AndreYtelserPleiepenger,
    AndreYtelserSvangerskapspenger,
    MinimumSykdomsgrad,
    EtterDødsdato,
    ManglerMedlemskap,
    ManglerOpptjening,
    Over70;

    internal companion object {
        fun fraBegrunnelse(begrunnelse: BegrunnelseDto) = when (begrunnelse) {
            is BegrunnelseDto.SykepengedagerOppbrukt -> SykepengedagerOppbrukt
            is BegrunnelseDto.SykepengedagerOppbruktOver67 -> SykepengedagerOppbruktOver67
            is BegrunnelseDto.MinimumSykdomsgrad -> MinimumSykdomsgrad
            is BegrunnelseDto.EgenmeldingUtenforArbeidsgiverperiode -> EgenmeldingUtenforArbeidsgiverperiode
            is BegrunnelseDto.MinimumInntekt -> MinimumInntekt
            is BegrunnelseDto.MinimumInntektOver67 -> MinimumInntektOver67
            is BegrunnelseDto.EtterDødsdato -> EtterDødsdato
            is BegrunnelseDto.ManglerMedlemskap -> ManglerMedlemskap
            is BegrunnelseDto.ManglerOpptjening -> ManglerOpptjening
            is BegrunnelseDto.Over70 -> Over70
            is BegrunnelseDto.AndreYtelserAap -> AndreYtelserAap
            is BegrunnelseDto.AndreYtelserDagpenger -> AndreYtelserDagpenger
            is BegrunnelseDto.AndreYtelserForeldrepenger -> AndreYtelserForeldrepenger
            is BegrunnelseDto.AndreYtelserOmsorgspenger -> AndreYtelserOmsorgspenger
            is BegrunnelseDto.AndreYtelserOpplaringspenger -> AndreYtelserOpplaringspenger
            is BegrunnelseDto.AndreYtelserPleiepenger -> AndreYtelserPleiepenger
            is BegrunnelseDto.AndreYtelserSvangerskapspenger -> AndreYtelserSvangerskapspenger
            is BegrunnelseDto.NyVilkårsprøvingNødvendig -> SykepengedagerOppbrukt // TODO: Map til NyVilkårsprøvingNødvendig
        }
    }
}