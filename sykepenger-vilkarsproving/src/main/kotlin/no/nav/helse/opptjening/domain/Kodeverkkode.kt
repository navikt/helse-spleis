package no.nav.helse.opptjening.domain

internal enum class Utfall {
    Oppfylt, IkkeOppfylt
}

internal enum class Kodeverkkode(val vilkår: Vilkår, val utfall: Utfall) {
    OPPTJENING_MINST_4_UKER(Vilkår.Opptjening, Utfall.Oppfylt),
    OPPTJENING_ANNEN_YTELSE(Vilkår.Opptjening, Utfall.Oppfylt),
    OPPTJENING_YRKESAKTIV_FOER_FORELDREPENGER(Vilkår.Opptjening, Utfall.Oppfylt),

    IKKE_OPPTJENING_AAP_FOER_FORELDREPENGER(Vilkår.Opptjening, Utfall.IkkeOppfylt),
    IKKE_OPPTJENING_ARBEID_ELLER_YTELSE(Vilkår.Opptjening, Utfall.IkkeOppfylt),
}
