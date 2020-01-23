package no.nav.helse.hendelser

import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.TilstandType
import no.nav.helse.person.VedtaksperiodeHendelse
import java.time.LocalDateTime
import java.util.*

class ModelPåminnelse(
    hendelseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val antallGangerPåminnet: Int,
    private val tilstand: TilstandType,
    private val tilstandsendringstidspunkt: LocalDateTime,
    private val påminnelsestidspunkt: LocalDateTime,
    private val nestePåminnelsestidspunkt: LocalDateTime
    ) : ArbeidstakerHendelse(hendelseId, Hendelsestype.Påminnelse), VedtaksperiodeHendelse {

    override fun toJson() = "{}"

    fun antallGangerPåminnet() = antallGangerPåminnet
    fun tilstand() = tilstand
    fun tilstandsendringstidspunkt() = tilstandsendringstidspunkt
    fun påminnelsestidspunkt() = påminnelsestidspunkt
    fun nestePåminnelsestidspunkt() = nestePåminnelsestidspunkt

    fun gjelderTilstand(tilstandType: TilstandType) = tilstandType == tilstand

    override fun rapportertdato() = påminnelsestidspunkt
    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
}
