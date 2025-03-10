package no.nav.helse.hendelser

import java.time.LocalDateTime
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement

class UtbetalingshistorikkEtterInfotrygdendring(
    meldingsreferanseId: MeldingsreferanseId,
    private val element: InfotrygdhistorikkElement,
    besvart: LocalDateTime
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.IngenArbeidsgiver
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = SYSTEM,
        innsendt = besvart,
        registrert = LocalDateTime.now(),
        automatiskBehandling = true
    )

    internal fun oppdaterHistorikk(aktivitetslogg: IAktivitetslogg, historikk: Infotrygdhistorikk): Boolean {
        aktivitetslogg.info("Oppdaterer Infotrygdhistorikk etter infotrygendring")
        if (!historikk.oppdaterHistorikk(element)) return false.also { aktivitetslogg.info("Oppfrisket Infotrygdhistorikk medførte ingen endringer etter infotrygdendring") }
        aktivitetslogg.info("Oppfrisket Infotrygdhistorikk ble lagret etter infotrygdendring")
        return true
    }
}
