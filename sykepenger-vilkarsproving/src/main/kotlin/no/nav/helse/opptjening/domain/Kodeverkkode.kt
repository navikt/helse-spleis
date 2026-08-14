package no.nav.helse.opptjening.domain

internal enum class Utfall {
    Oppfylt, IkkeOppfylt
}

internal enum class Kodeverkkode(val utfall: Utfall) {
    OPPTJENING_MINST_4_UKER(Utfall.Oppfylt),
    OPPTJENING_ANNEN_YTELSE(Utfall.Oppfylt),
    OPPTJENING_YRKESAKTIV_FOER_FORELDREPENGER(Utfall.Oppfylt),

    IKKE_OPPTJENING_AAP_FOER_FORELDREPENGER(Utfall.IkkeOppfylt),
    IKKE_OPPTJENING_ARBEID_ELLER_YTELSE(Utfall.IkkeOppfylt),
}
