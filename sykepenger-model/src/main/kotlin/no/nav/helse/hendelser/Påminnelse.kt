package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.TilstandType
import java.time.LocalDateTime
import java.util.*

class Påminnelse(
    hendelseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    val vedtaksperiodeId: String,
    private val antallGangerPåminnet: Int,
    private val tilstand: TilstandType,
    private val tilstandsendringstidspunkt: LocalDateTime,
    private val påminnelsestidspunkt: LocalDateTime,
    private val nestePåminnelsestidspunkt: LocalDateTime,
    aktivitetslogger: Aktivitetslogger,
    aktivitetslogg: Aktivitetslogg
) : ArbeidstakerHendelse(hendelseId, Hendelsestype.Påminnelse, aktivitetslogger, aktivitetslogg) {

    fun antallGangerPåminnet() = antallGangerPåminnet
    fun tilstand() = tilstand
    fun tilstandsendringstidspunkt() = tilstandsendringstidspunkt
    fun påminnelsestidspunkt() = påminnelsestidspunkt
    fun nestePåminnelsestidspunkt() = nestePåminnelsestidspunkt

    fun gjelderTilstand(tilstandType: TilstandType) = (tilstandType == tilstand).also {
        if (!it) {
            infoOld("Påminnelse var ikke aktuell i tilstand: ${tilstandType.name} da den gjaldt: ${tilstand.name}")
        } else {
            warnOld("Vedtaksperiode blir påminnet")
        }
    }

    override fun rapportertdato() = påminnelsestidspunkt
    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer

    internal fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        aktivitetslogger.addAll(this.aktivitetslogger, "Påminnelse")
    }
}
