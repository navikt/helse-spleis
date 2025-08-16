package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
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
    private val nå: LocalDateTime = LocalDateTime.now(),
    opprettet: LocalDateTime
) : Hendelse {
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = SYSTEM,
        innsendt = opprettet,
        registrert = LocalDateTime.now(),
        automatiskBehandling = true
    )

    fun antallGangerPåminnet() = antallGangerPåminnet
    fun tilstand() = tilstand
    fun tilstandsendringstidspunkt() = tilstandsendringstidspunkt
    fun påminnelsestidspunkt() = påminnelsestidspunkt
    fun nestePåminnelsestidspunkt() = nestePåminnelsestidspunkt

    internal fun nåddMakstid(beregnetMakstid: (LocalDateTime) -> LocalDateTime): Boolean {
        return nå >= beregnetMakstid(tilstandsendringstidspunkt)
    }

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

    fun eventyr(skjæringstidspunkt: LocalDate, periode: Periode): Revurderingseventyr? {
        if (!når(Predikat.Flagg("ønskerReberegning"))) return null
        return Revurderingseventyr.reberegning(this, skjæringstidspunkt, periode)
    }

    internal sealed interface Predikat {
        fun evaluer(påminnelse: Påminnelse): Boolean
        data class Flagg(private val flagg: String): Predikat {
            override fun evaluer(påminnelse: Påminnelse) = flagg in påminnelse.flagg
        }
        data class VentetMinst(private val varighet: Period): Predikat {
            override fun evaluer(påminnelse: Påminnelse) = påminnelse.tilstandsendringstidspunkt.plus(varighet) <= påminnelse.nå
        }
    }
}
