package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

class Påminnelse(
    meldingsreferanseId: MeldingsreferanseId,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
    val vedtaksperiodeId: String,
    val antallGangerPåminnet: Int,
    val tilstand: TilstandType,
    val tilstandsendringstidspunkt: LocalDateTime,
    val påminnelsestidspunkt: LocalDateTime,
    val nestePåminnelsestidspunkt: LocalDateTime,
    private val flagg: Set<String>,
    opprettet: LocalDateTime
) : Hendelse {
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = SYSTEM,
        innsendt = opprettet,
        registrert = LocalDateTime.now(),
        automatiskBehandling = true
    )

    internal fun erRelevant(vedtaksperiodeId: UUID) = vedtaksperiodeId.toString() == this.vedtaksperiodeId

    internal fun når(vararg predikat: Predikat): Boolean {
        check(predikat.isNotEmpty()) { "Nå må sende med minst et predikat da.." }
        return predikat.all { it.evaluer(this) }
    }

    internal fun gjelderTilstand(aktivitetslogg: IAktivitetslogg, tilstandType: TilstandType) = (tilstandType == tilstand).also {
        if (!it) {
            aktivitetslogg.info("Påminnelse var ikke aktuell i tilstand: ${tilstandType.name} da den gjaldt: ${tilstand.name}")
        }
    }

    internal sealed interface Predikat {
        fun evaluer(påminnelse: Påminnelse): Boolean
        data class Flagg(private val flagg: String): Predikat {
            override fun evaluer(påminnelse: Påminnelse) = flagg in påminnelse.flagg
            override fun toString() = flagg
        }
    }
}
