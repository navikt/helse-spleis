package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import java.time.LocalDateTime
import java.util.*

class Påminnelse(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val antallGangerPåminnet: Int,
    private val tilstand: TilstandType,
    private val tilstandsendringstidspunkt: LocalDateTime,
    private val påminnelsestidspunkt: LocalDateTime,
    private val nestePåminnelsestidspunkt: LocalDateTime
) : ArbeidstakerHendelse(meldingsreferanseId, Aktivitetslogg()) {

    fun antallGangerPåminnet() = antallGangerPåminnet
    fun tilstand() = tilstand
    fun tilstandsendringstidspunkt() = tilstandsendringstidspunkt
    fun påminnelsestidspunkt() = påminnelsestidspunkt
    fun nestePåminnelsestidspunkt() = nestePåminnelsestidspunkt

    internal fun erRelevant(vedtaksperiodeId: UUID) = vedtaksperiodeId.toString() == this.vedtaksperiodeId

    internal fun gjelderTilstand(tilstandType: TilstandType) = (tilstandType == tilstand).also {
        if (!it) {
            info("Påminnelse var ikke aktuell i tilstand: ${tilstandType.name} da den gjaldt: ${tilstand.name}")
        } else {
            info("Vedtaksperiode blir påminnet")
        }
    }

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer

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
