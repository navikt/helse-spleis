package no.nav.helse.person

enum class TilstandType(val sluttilstand: Boolean, val hindererAndrePerioder: Boolean) {
    START(sluttilstand = false, hindererAndrePerioder = true),
    MOTTATT_SYKMELDING(sluttilstand = false, hindererAndrePerioder = true),
    AVVENTER_SØKNAD(sluttilstand = false, hindererAndrePerioder = true),
    AVVENTER_TIDLIGERE_PERIODE_ELLER_INNTEKTSMELDING(sluttilstand = false, hindererAndrePerioder = true),
    AVVENTER_TIDLIGERE_PERIODE(sluttilstand = false, hindererAndrePerioder = true),
    UNDERSØKER_HISTORIKK(sluttilstand = false, hindererAndrePerioder = true),
    AVVENTER_INNTEKTSMELDING(sluttilstand = false, hindererAndrePerioder = true),
    AVVENTER_VILKÅRSPRØVING(sluttilstand = false, hindererAndrePerioder = true),
    AVVENTER_HISTORIKK(sluttilstand = false, hindererAndrePerioder = true),
    AVVENTER_GODKJENNING(sluttilstand = false, hindererAndrePerioder = true),
    TIL_UTBETALING(sluttilstand = false, hindererAndrePerioder = true),
    TIL_INFOTRYGD(sluttilstand = true, hindererAndrePerioder = true),
    UTBETALT(sluttilstand = true, hindererAndrePerioder = false),
    UTBETALING_FEILET(sluttilstand = true, hindererAndrePerioder = false)
}
