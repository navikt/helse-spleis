package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement

class Utbetalingshistorikk(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val element: InfotrygdhistorikkElement,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, aktivitetslogg) {

    internal fun oppdaterHistorikk(historikk: Infotrygdhistorikk) {
        info("Oppdaterer Infotrygdhistorikk")
        if (!historikk.oppdaterHistorikk(element)) return info("Oppfrisket Infotrygdhistorikk medførte ingen endringer")
        info("Oppfrisket Infotrygdhistorikk ble lagret")
    }

    internal fun erRelevant(vedtaksperiodeId: UUID) =
        vedtaksperiodeId.toString() == this.vedtaksperiodeId
}
