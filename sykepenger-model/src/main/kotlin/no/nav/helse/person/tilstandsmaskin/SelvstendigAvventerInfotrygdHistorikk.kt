package no.nav.helse.person.tilstandsmaskin

internal data object SelvstendigAvventerInfotrygdHistorikk : Vedtaksperiodetilstand by(AvventerInfotrygdHistorikk) {
    override val type = TilstandType.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK
}
