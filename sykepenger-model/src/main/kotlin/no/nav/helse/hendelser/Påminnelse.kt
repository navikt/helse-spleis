package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

class Påminnelse(
    meldingsreferanseId: UUID,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val antallGangerPåminnet: Int,
    private val tilstand: TilstandType,
    private val tilstandsendringstidspunkt: LocalDateTime,
    private val påminnelsestidspunkt: LocalDateTime,
    private val nestePåminnelsestidspunkt: LocalDateTime,
    private val ønskerReberegning: Boolean = false,
    private val nå: LocalDateTime = LocalDateTime.now(),
    opprettet: LocalDateTime
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(
        organisasjonsnummer = organisasjonsnummer
    )
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

    internal fun skalReberegnes() = ønskerReberegning

    internal fun gjelderTilstand(aktivitetslogg: IAktivitetslogg, tilstandType: TilstandType) = (tilstandType == tilstand).also {
        if (!it) {
            aktivitetslogg.info("Påminnelse var ikke aktuell i tilstand: ${tilstandType.name} da den gjaldt: ${tilstand.name}")
        }
    }

    internal fun harVentet3MånederEllerMer() = nå.minusMonths(3) >= tilstandsendringstidspunkt

    internal fun vedtaksperiodeIkkeFunnet(observer: PersonObserver) {
        observer.vedtaksperiodeIkkeFunnet(
            PersonObserver.VedtaksperiodeIkkeFunnetEvent(
                organisasjonsnummer = behandlingsporing.organisasjonsnummer,
                vedtaksperiodeId = UUID.fromString(vedtaksperiodeId)
            )
        )
    }

    fun toOutgoingMessage() = mapOf(
        "organisasjonsnummer" to behandlingsporing.organisasjonsnummer,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "tilstand" to tilstand,
        "antallGangerPåminnet" to antallGangerPåminnet,
        "tilstandsendringstidspunkt" to tilstandsendringstidspunkt,
        "påminnelsestidspunkt" to påminnelsestidspunkt,
        "nestePåminnelsestidspunkt" to nestePåminnelsestidspunkt
    )

    fun eventyr(skjæringstidspunkt: LocalDate, periode: Periode): Revurderingseventyr? {
        if (!skalReberegnes()) return null
        return Revurderingseventyr.reberegning(this, skjæringstidspunkt, periode)
    }
}
