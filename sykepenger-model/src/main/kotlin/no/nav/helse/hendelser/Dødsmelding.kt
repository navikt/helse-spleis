package no.nav.helse.hendelser

import no.nav.helse.Alder
import no.nav.helse.hendelser.Avsender.SYSTEM
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Dødsmelding(
    meldingsreferanseId: UUID,
    private val dødsdato: LocalDate,
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.IngenArbeidsgiver

    override val metadata =
        LocalDateTime.now().let { nå ->
            HendelseMetadata(
                meldingsreferanseId = meldingsreferanseId,
                avsender = SYSTEM,
                innsendt = nå,
                registrert = nå,
                automatiskBehandling = true,
            )
        }

    internal fun dødsdato(alder: Alder) = alder.medDød(this.dødsdato)
}
