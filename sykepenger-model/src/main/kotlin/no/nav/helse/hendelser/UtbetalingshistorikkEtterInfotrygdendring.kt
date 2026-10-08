package no.nav.helse.hendelser

import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import java.time.LocalDateTime

class UtbetalingshistorikkEtterInfotrygdendring(
    meldingsreferanseId: MeldingsreferanseId,
    val element: InfotrygdhistorikkElement,
    besvart: LocalDateTime,
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.IngenYrkesaktivitet
    override val metadata =
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = besvart,
            registrert = LocalDateTime.now(),
            automatiskBehandling = true,
        )
}
