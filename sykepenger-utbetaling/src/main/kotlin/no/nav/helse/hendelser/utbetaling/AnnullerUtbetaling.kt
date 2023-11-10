package no.nav.helse.hendelser.utbetaling

import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.hendelser.ArbeidstakerHendelse
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER

class AnnullerUtbetaling(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val fagsystemId: String,
    private val saksbehandlerIdent: String,
    private val saksbehandlerEpost: String,
    internal val opprettet: LocalDateTime,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer, aktivitetslogg) {

    fun erRelevant(fagsystemId: String) = this.fagsystemId == fagsystemId

    fun vurdering() = Utbetaling.Vurdering(
        true,
        saksbehandlerIdent,
        saksbehandlerEpost,
        opprettet,
        false
    )

    override fun avsender() = SAKSBEHANDLER
}
