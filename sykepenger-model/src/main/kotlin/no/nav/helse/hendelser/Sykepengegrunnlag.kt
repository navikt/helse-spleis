package no.nav.helse.hendelser

import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// Sykepengegrunnlag-ID-en fra løsningen lagres ikke mens sp-sykepengegrunnlag er i skyggedrift (ADR 0020)
class Sykepengegrunnlag(
    meldingsreferanseId: MeldingsreferanseId,
    private val vedtaksperiodeId: UUID,
    private val skjæringstidspunkt: LocalDate,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
) : Hendelse {
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

    internal fun erRelevant(
        aktivitetslogg: IAktivitetslogg,
        other: UUID,
        skjæringstidspunktVedtaksperiode: LocalDate,
    ): Boolean {
        if (other != vedtaksperiodeId) return false
        if (skjæringstidspunktVedtaksperiode == skjæringstidspunkt) return true
        aktivitetslogg.info(
            "Sykepengegrunnlag var relevant for Vedtaksperiode, men skjæringstidspunktene var ulikte: [$skjæringstidspunkt, $skjæringstidspunktVedtaksperiode]",
        )
        return false
    }
}
