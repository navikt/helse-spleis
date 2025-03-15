package no.nav.helse.hendelser

import java.time.LocalDateTime
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement

class Utbetalingshistorikk(
    meldingsreferanseId: MeldingsreferanseId,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    val element: InfotrygdhistorikkElement,
    besvart: LocalDateTime
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(
        organisasjonsnummer = organisasjonsnummer
    )
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = SYSTEM,
        innsendt = besvart,
        registrert = LocalDateTime.now(),
        automatiskBehandling = true
    )
}
