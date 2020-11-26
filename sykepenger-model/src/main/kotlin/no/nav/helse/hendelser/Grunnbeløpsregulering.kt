package no.nav.helse.hendelser

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.SpesifikkKontekst
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

    private var håndtert = false

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    override fun organisasjonsnummer() = organisasjonsnummer

    internal fun grunnbeløp(skjæringstidspunkt: LocalDate) = Grunnbeløp.`1G`.beløp(skjæringstidspunkt)

    internal val harHistorikk = utbetalingshistorikk != null

    internal fun utbetalingshistorikk() = requireNotNull(utbetalingshistorikk)

    internal fun håndtert() = håndtert.also {
        if (!it) håndtert = true
    }

    internal fun erRelevant(fagsystemId: String) = this.fagsystemId == fagsystemId

    internal fun erRelevant(arbeidsgiverFagsystemId: String?, personFagsystemId: String?, skjæringstidspunkt: LocalDate) =
        relevantFagsystemId(arbeidsgiverFagsystemId, personFagsystemId) && skjæringstidspunkt >= gyldighetsdato

    private fun relevantFagsystemId(arbeidsgiverFagsystemId: String?, personFagsystemId: String?) =
        arbeidsgiverFagsystemId == fagsystemId || personFagsystemId == fagsystemId

    override fun toSpesifikkKontekst(): SpesifikkKontekst =
        super.toSpesifikkKontekst().let {
            SpesifikkKontekst(
                it.kontekstType,
                it.kontekstMap + mapOf(
                    "fagsystemId" to fagsystemId,
                    "gyldighetsdato" to gyldighetsdato.toString()
                )
            )
        }

}
