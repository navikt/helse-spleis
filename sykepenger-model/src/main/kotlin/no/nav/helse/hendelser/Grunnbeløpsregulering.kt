package no.nav.helse.hendelser

import no.nav.helse.hendelser.Avsender.SYSTEM
import java.time.LocalDate
import java.time.LocalDateTime

class Grunnbeløpsregulering(
    meldingsreferanseId: MeldingsreferanseId,
    val skjæringstidspunkt: LocalDate,
    opprettet: LocalDateTime,
) : Hendelse,
    OverstyrInntektsgrunnlag {
    override val behandlingsporing = Behandlingsporing.IngenYrkesaktivitet

    override val metadata =
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = opprettet,
            registrert = LocalDateTime.now(),
            automatiskBehandling = true,
        )

    override fun erRelevant(skjæringstidspunkt: LocalDate) = this.skjæringstidspunkt == skjæringstidspunkt
}
