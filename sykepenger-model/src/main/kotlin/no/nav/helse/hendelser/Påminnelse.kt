package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeVenter
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg

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
    private val nestePåminnelsestidspunkt: LocalDateTime,
    private val ønskerReberegning: Boolean = false,
    private val nå: LocalDateTime = LocalDateTime.now(),
    private val opprettet: LocalDateTime
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, Aktivitetslogg()) {

    override fun innsendt() = opprettet

    override fun avsender() = Avsender.SAKSBEHANDLER

    fun antallGangerPåminnet() = antallGangerPåminnet
    fun tilstand() = tilstand
    fun tilstandsendringstidspunkt() = tilstandsendringstidspunkt
    fun påminnelsestidspunkt() = påminnelsestidspunkt
    fun nestePåminnelsestidspunkt() = nestePåminnelsestidspunkt

    internal fun nåddMakstid(vedtaksperiode: Vedtaksperiode, person: Person): Boolean {
        val beregnetMakstid = person.makstid(vedtaksperiode, tilstandsendringstidspunkt)
        return nå >= beregnetMakstid
    }

    internal fun erRelevant(vedtaksperiodeId: UUID) = vedtaksperiodeId.toString() == this.vedtaksperiodeId

    internal fun skalReberegnes() = ønskerReberegning

    internal fun gjelderTilstand(tilstandType: TilstandType) = (tilstandType == tilstand).also {
        if (!it) {
            info("Påminnelse var ikke aktuell i tilstand: ${tilstandType.name} da den gjaldt: ${tilstand.name}")
        }
    }

    internal fun venter(builder: VedtaksperiodeVenter.Builder, makstid: (tilstandsendringstidspunkt: LocalDateTime) -> LocalDateTime) {
        builder.venter(
            UUID.fromString(vedtaksperiodeId),
            organisasjonsnummer,
            tilstandsendringstidspunkt,
            makstid(tilstandsendringstidspunkt)
        )
    }

    internal fun vedtaksperiodeIkkeFunnet(observer: PersonObserver) {
        observer.vedtaksperiodeIkkeFunnet(
            PersonObserver.VedtaksperiodeIkkeFunnetEvent(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = UUID.fromString(vedtaksperiodeId)
            )
        )
    }

    fun toOutgoingMessage() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "tilstand" to tilstand,
        "antallGangerPåminnet" to antallGangerPåminnet,
        "tilstandsendringstidspunkt" to tilstandsendringstidspunkt,
        "påminnelsestidspunkt" to påminnelsestidspunkt,
        "nestePåminnelsestidspunkt" to nestePåminnelsestidspunkt
    )
}
