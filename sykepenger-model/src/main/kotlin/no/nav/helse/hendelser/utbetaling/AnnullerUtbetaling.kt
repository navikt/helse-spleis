package no.nav.helse.hendelser.utbetaling

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDateTime
import java.util.*

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

    internal fun erRelevant(fagsystemId: String) = this.fagsystemId == fagsystemId

    internal fun vurdering() = Utbetaling.Vurdering(
        true,
        saksbehandlerIdent,
        saksbehandlerEpost,
        opprettet,
        false
    )
}
