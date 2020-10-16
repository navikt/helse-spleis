package no.nav.helse.hendelser

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.util.*

class GRegulering(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val virkningFra: LocalDate,
    private val fagsystemId: String
) : ArbeidstakerHendelse(meldingsreferanseId, Aktivitetslogg()) {

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    override fun organisasjonsnummer() = organisasjonsnummer

    internal fun grunnbeløp(beregningsdato: LocalDate) = Grunnbeløp.`1G`.beløp(beregningsdato, virkningFra)

    internal fun erRelevant(arbeidsgiverFagsystemId: String?, personFagsystemId: String?, beregningsdato: LocalDate, grunnbeløp: Inntekt) =
        relevantFagsystemId(arbeidsgiverFagsystemId, personFagsystemId) && grunnbeløp(beregningsdato) > grunnbeløp

    private fun relevantFagsystemId(arbeidsgiverFagsystemId: String?, personFagsystemId: String?): Boolean {
        return arbeidsgiverFagsystemId == fagsystemId || personFagsystemId == fagsystemId
    }
}
