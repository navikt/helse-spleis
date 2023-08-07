package no.nav.helse.serde.api.dto

import no.nav.helse.utbetalingstidslinje.Begrunnelse

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
        fun fraBegrunnelse(begrunnelse: Begrunnelse) = when (begrunnelse) {
            is Begrunnelse.SykepengedagerOppbrukt -> SykepengedagerOppbrukt
            is Begrunnelse.SykepengedagerOppbruktOver67 -> SykepengedagerOppbruktOver67
            is Begrunnelse.MinimumSykdomsgrad -> MinimumSykdomsgrad
            is Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode -> EgenmeldingUtenforArbeidsgiverperiode
            is Begrunnelse.MinimumInntekt -> MinimumInntekt
            is Begrunnelse.MinimumInntektOver67 -> MinimumInntektOver67
            is Begrunnelse.EtterDødsdato -> EtterDødsdato
            is Begrunnelse.ManglerMedlemskap -> ManglerMedlemskap
            is Begrunnelse.ManglerOpptjening -> ManglerOpptjening
            is Begrunnelse.Over70 -> Over70
            is Begrunnelse.AndreYtelserAap -> AndreYtelserAap
            is Begrunnelse.AndreYtelserDagpenger -> AndreYtelserDagpenger
            is Begrunnelse.AndreYtelserForeldrepenger -> AndreYtelserForeldrepenger
            is Begrunnelse.AndreYtelserOmsorgspenger -> AndreYtelserOmsorgspenger
            is Begrunnelse.AndreYtelserOpplaringspenger -> AndreYtelserOpplaringspenger
            is Begrunnelse.AndreYtelserPleiepenger -> AndreYtelserPleiepenger
            is Begrunnelse.AndreYtelserSvangerskapspenger -> AndreYtelserSvangerskapspenger
            is Begrunnelse.NyVilkårsprøvingNødvendig -> SykepengedagerOppbrukt // TODO: Map til NyVilkårsprøvingNødvendig
        }
    }
}