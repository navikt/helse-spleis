package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType

class Påminnelse(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val antallGangerPåminnet: Int,
    private val tilstand: TilstandType,
    private val tilstandsendringstidspunkt: LocalDateTime,
    private val påminnelsestidspunkt: LocalDateTime,
    private val nestePåminnelsestidspunkt: LocalDateTime
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, Aktivitetslogg()) {
    private companion object {
        private const val MAGISK_VERDI_FOR_MAIGATE = 9000
    }

    fun antallGangerPåminnet() = antallGangerPåminnet
    fun tilstand() = tilstand
    fun tilstandsendringstidspunkt() = tilstandsendringstidspunkt
    fun påminnelsestidspunkt() = påminnelsestidspunkt
    fun nestePåminnelsestidspunkt() = nestePåminnelsestidspunkt

    internal fun maigate(): Boolean {
        return antallGangerPåminnet == MAGISK_VERDI_FOR_MAIGATE
    }

    internal fun erRelevant(vedtaksperiodeId: UUID) = vedtaksperiodeId.toString() == this.vedtaksperiodeId

    internal fun gjelderTilstand(tilstandType: TilstandType) = (tilstandType == tilstand).also {
        if (!it) {
            info("Påminnelse var ikke aktuell i tilstand: ${tilstandType.name} da den gjaldt: ${tilstand.name}")
        }
    }

    internal fun vedtaksperiodeIkkeFunnet(observer: PersonObserver) {
        observer.vedtaksperiodeIkkeFunnet(
            hendelseskontekst(),
            PersonObserver.VedtaksperiodeIkkeFunnetEvent(
                vedtaksperiodeId = UUID.fromString(vedtaksperiodeId)
            )
        )
    }

    fun toOutgoingMessage() = mapOf(
        "vedtaksperiodeId" to vedtaksperiodeId,
        "tilstand" to tilstand,
        "antallGangerPåminnet" to antallGangerPåminnet,
        "tilstandsendringstidspunkt" to tilstandsendringstidspunkt,
        "påminnelsestidspunkt" to påminnelsestidspunkt,
        "nestePåminnelsestidspunkt" to nestePåminnelsestidspunkt
    )
}
