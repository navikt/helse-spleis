package no.nav.helse.hendelser

import java.time.LocalDateTime
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
    private val besvart: LocalDateTime
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, aktivitetslogg) {

    internal fun oppdaterHistorikk(historikk: Infotrygdhistorikk): Boolean {
        info("Oppdaterer Infotrygdhistorikk etter infotrygendring")
        if (!historikk.oppdaterHistorikk(element)) return false.also { info("Oppfrisket Infotrygdhistorikk medførte ingen endringer etter infotrygdendring") }
        info("Oppfrisket Infotrygdhistorikk ble lagret etter infotrygdendring")
        return true
    }

    override fun innsendt() = besvart
    override fun avsender() = Avsender.SYSTEM
}
