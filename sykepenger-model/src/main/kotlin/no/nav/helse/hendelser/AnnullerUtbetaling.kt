package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDateTime
import java.util.*

class AnnullerUtbetaling(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val fagsystemId: String,
    private val saksbehandlerIdent: String,
    private val saksbehandlerEpost: String,
    private val opprettet: LocalDateTime,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : ArbeidstakerHendelse(meldingsreferanseId, aktivitetslogg) {
    private val annullerte = mutableListOf<String>()

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer

    internal fun erRelevant(fagsystemId: String) = this.fagsystemId == fagsystemId

    internal fun vurdering() = Utbetaling.Vurdering(
        saksbehandlerIdent,
        saksbehandlerEpost,
        opprettet,
        false
    )

    internal fun erAnnullert(fagsystemId: String) {
        annullerte.add(fagsystemId)
    }

    internal fun erAnnullertFør(fagsystemId: String) = fagsystemId in annullerte
}
