package no.nav.helse.hendelser

import no.nav.helse.Grunnbeløp
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
    private val fagsystemId: String
) : ArbeidstakerHendelse(meldingsreferanseId, Aktivitetslogg()) {

    private var håndtert = false

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    override fun organisasjonsnummer() = organisasjonsnummer

    internal fun grunnbeløp(skjæringstidspunkt: LocalDate) = Grunnbeløp.`1G`.beløp(skjæringstidspunkt)

    internal fun håndtert() = håndtert.also {
        if (!it) håndtert = true
    }

    internal fun erRelevant(arbeidsgiverFagsystemId: String?, personFagsystemId: String?, skjæringstidspunkt: LocalDate) =
        relevantFagsystemId(arbeidsgiverFagsystemId, personFagsystemId) && skjæringstidspunkt >= gyldighetsdato

    private fun relevantFagsystemId(arbeidsgiverFagsystemId: String?, personFagsystemId: String?) =
        arbeidsgiverFagsystemId == fagsystemId || personFagsystemId == fagsystemId

}
