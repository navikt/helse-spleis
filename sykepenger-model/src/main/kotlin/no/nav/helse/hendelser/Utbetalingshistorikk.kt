package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM
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
    private val besvart: LocalDateTime
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, aktivitetslogg) {

    internal fun oppdaterHistorikk(historikk: Infotrygdhistorikk) {
        info("Oppdaterer Infotrygdhistorikk")
        if (!historikk.oppdaterHistorikk(element)) return info("Oppfrisket Infotrygdhistorikk medførte ingen endringer")
        info("Oppfrisket Infotrygdhistorikk ble lagret")
    }

    override fun innsendt() = besvart
    override fun avsender() = SYSTEM
}
