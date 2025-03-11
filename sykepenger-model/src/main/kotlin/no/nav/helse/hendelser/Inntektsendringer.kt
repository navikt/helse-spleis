package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.hendelser.Avsender.SYSTEM

class Inntektsendringer(meldingsreferanseId: MeldingsreferanseId, inntektsendringFom: LocalDate) : Hendelse {
    override val behandlingsporing = Behandlingsporing.IngenArbeidsgiver
    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = true
        )
    }
}
