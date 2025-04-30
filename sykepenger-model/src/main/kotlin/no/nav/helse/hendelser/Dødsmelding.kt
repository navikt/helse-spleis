package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.Alder
import no.nav.helse.hendelser.Avsender.SYSTEM

class Dødsmelding(
    meldingsreferanseId: MeldingsreferanseId,
    private val dødsdato: LocalDate
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.IngenYrkesaktivitet

    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = true
        )
    }

    internal fun dødsdato(alder: Alder) = alder.medDød(this.dødsdato)
}
