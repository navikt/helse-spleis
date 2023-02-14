package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement

class UtbetalingshistorikkEtterInfotrygdendring(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    private val element: InfotrygdhistorikkElement,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, aktivitetslogg) {

    internal fun oppdaterHistorikk(historikk: Infotrygdhistorikk) {
        info("Oppdaterer Infotrygdhistorikk etter infotrygendring")
        if (!historikk.oppdaterHistorikk(element)) return info("Oppfrisket Infotrygdhistorikk medførte ingen endringer etter infotrygdendring")
        info("Oppfrisket Infotrygdhistorikk ble lagret etter infotrygdendring")
    }
}
