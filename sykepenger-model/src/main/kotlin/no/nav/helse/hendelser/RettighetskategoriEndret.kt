package no.nav.helse.hendelser

import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.hendelser.Rettighetskategori.Hovedkategori
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class RettighetskategoriEndret(
    meldingsreferanseId: MeldingsreferanseId,
    internal val skjæringstidspunkt: LocalDate,
    internal val rettighetskategoriId: UUID,
    // Hovedkategorien lagres ikke i Spleis (ADR 0001)
    private val hovedkategori: Hovedkategori,
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.IngenYrkesaktivitet

    override val metadata =
        LocalDateTime.now().let { nå ->
            HendelseMetadata(
                meldingsreferanseId = meldingsreferanseId,
                avsender = SAKSBEHANDLER,
                innsendt = nå,
                registrert = nå,
                automatiskBehandling = false,
            )
        }

    internal fun ikkeStøttet() = hovedkategori.ikkeStøttet()

    internal fun revurderingseventyr() = Revurderingseventyr.endretRettighetskategori(this, skjæringstidspunkt)
}
