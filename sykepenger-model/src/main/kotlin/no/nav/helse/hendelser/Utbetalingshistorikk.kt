package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement

class Utbetalingshistorikk(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val element: InfotrygdhistorikkElement,
    private val besvart: LocalDateTime
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer) {

    internal fun oppdaterHistorikk(aktivitetslogg: IAktivitetslogg, historikk: Infotrygdhistorikk): Boolean {
        aktivitetslogg.info("Oppdaterer Infotrygdhistorikk")
        if (!historikk.oppdaterHistorikk(element)) return false.also { aktivitetslogg.info("Oppfrisket Infotrygdhistorikk medførte ingen endringer") }
        aktivitetslogg.info("Oppfrisket Infotrygdhistorikk ble lagret")
        return true
    }

    override fun innsendt() = besvart
    override fun avsender() = SYSTEM
}
