package no.nav.helse.hendelser

import no.nav.helse.person.Sykmeldingsperioder
import java.time.LocalDateTime

class ForkastSykmeldingsperioder(
    meldingsreferanseId: MeldingsreferanseId,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
    private val periode: Periode,
) : Hendelse {
    override val metadata =
        LocalDateTime.now().let { nå ->
            HendelseMetadata(
                meldingsreferanseId = meldingsreferanseId,
                avsender = Avsender.SAKSBEHANDLER,
                innsendt = nå,
                registrert = nå,
                automatiskBehandling = false,
            )
        }

    internal fun forkast(sykdomsperioder: Sykmeldingsperioder) {
        sykdomsperioder.fjern(periode)
    }
}
