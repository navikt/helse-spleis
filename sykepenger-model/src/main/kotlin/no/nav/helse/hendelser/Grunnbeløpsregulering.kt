package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import java.time.LocalDate
import java.util.*

class Grunnbeløpsregulering(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val gyldighetsdato: LocalDate,
    private val fagsystemId: String,
    private val utbetalingshistorikk: Utbetalingshistorikk? = null,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : ArbeidstakerHendelse(meldingsreferanseId, aktivitetslogg) {

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer

    internal val harHistorikk = utbetalingshistorikk != null

    internal fun utbetalingshistorikk() = requireNotNull(utbetalingshistorikk)

    internal fun erRelevant(fagsystemId: String) = this.fagsystemId == fagsystemId

    override fun kontekst() =
        super.kontekst() + mapOf(
            "fagsystemId" to fagsystemId,
            "gyldighetsdato" to gyldighetsdato.toString()
        )
}
