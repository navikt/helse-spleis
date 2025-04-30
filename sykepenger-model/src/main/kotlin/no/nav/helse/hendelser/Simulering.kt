package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.utbetalingslinjer.Fagområde

class Simulering(
    meldingsreferanseId: MeldingsreferanseId,
    val vedtaksperiodeId: String,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
    override val fagsystemId: String,
    fagområde: String,
    override val simuleringOK: Boolean,
    override val melding: String,
    override val simuleringsResultat: SimuleringResultatDto?,
    override val utbetalingId: UUID
) : Hendelse, SimuleringHendelse {
    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = true
        )
    }

    override val fagområde = when (fagområde) {
        "SPREF" -> Fagområde.SykepengerRefusjon
        "SP" -> Fagområde.Sykepenger
        else -> error("Kjenner ikke til fagområdet $fagområde")
    }
}
