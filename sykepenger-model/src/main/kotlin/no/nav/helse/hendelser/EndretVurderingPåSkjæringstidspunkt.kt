package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM

sealed interface Vurdering {
    val id: UUID
    data class Forsikringsvurdering(override val id: UUID): Vurdering
    data class Opptjeningsvurdering(override val id: UUID): Vurdering
}
class EndretVurderingPåSkjæringstidspunkt(
    private val meldingsreferanseId: MeldingsreferanseId,
    internal val skjæringstidspunkt: LocalDate,
    internal val endretVurdering: Vurdering,
    private val avsender: Avsender = SYSTEM
): Hendelse {
    override val behandlingsporing = Behandlingsporing.IngenYrkesaktivitet

    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = avsender,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = avsender == SYSTEM
        )
    }

    internal fun revurderingseventyr() = when (endretVurdering) {
        is Vurdering.Forsikringsvurdering -> Revurderingseventyr.endretForsikringsvurdering(this, skjæringstidspunkt)
        is Vurdering.Opptjeningsvurdering -> Revurderingseventyr.endretOpptjeningsvurdering(this, skjæringstidspunkt)
    }
}
