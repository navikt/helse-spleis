package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement

class Utbetalingshistorikk(
    meldingsreferanseId: MeldingsreferanseId,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
    val vedtaksperiodeId: UUID,
    val element: InfotrygdhistorikkElement,
    besvart: LocalDateTime
) : Hendelse {
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = SYSTEM,
        innsendt = besvart,
        registrert = LocalDateTime.now(),
        automatiskBehandling = true
    )
}
