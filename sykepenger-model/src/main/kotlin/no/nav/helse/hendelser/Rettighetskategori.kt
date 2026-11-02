package no.nav.helse.hendelser

import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Rettighetskategori(
    meldingsreferanseId: MeldingsreferanseId,
    private val vedtaksperiodeId: UUID,
    private val skjæringstidspunkt: LocalDate,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
    val rettighetskategoriId: UUID,
    // Hovedkategorien lagres ikke i Spleis (ADR 0001)
    @Suppress("unused") private val hovedkategori: Hovedkategori,
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
            "Rettighetskategori var relevant for Vedtaksperiode, men skjæringstidspunktene var ulikte: [$skjæringstidspunkt, $skjæringstidspunktVedtaksperiode]",
        )
        return false
    }

    enum class Hovedkategori {
        ARBEIDSTAKER,
        SELVSTENDIG_NÆRINGSDRIVENDE,
        ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE,
    }
}
