package no.nav.helse.person

import java.util.*

abstract class ArbeidstakerHendelse protected constructor(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    protected val organisasjonsnummer: String,
    private val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : PersonHendelse(meldingsreferanseId, fødselsnummer, aktørId, aktivitetslogg) {

    protected constructor(other: ArbeidstakerHendelse) : this(other.meldingsreferanseId(), other.fødselsnummer(), other.aktørId(), other.organisasjonsnummer, other.aktivitetslogg)

    fun organisasjonsnummer() = organisasjonsnummer

    override fun kontekst() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer()
    )

    fun errorsAndWorse(): List<String> {
        val meldingsoppsamler = ErrorsAndWorse()
        aktivitetslogg.accept(meldingsoppsamler)
        return meldingsoppsamler.meldinger()
    }

    internal class ErrorsAndWorse: AktivitetsloggVisitor {
        private val meldinger = mutableListOf<String>()
        fun meldinger() = meldinger.toList()
        override fun visitError(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) {
            meldinger.add(melding)
        }

        override fun visitSevere(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Severe, melding: String, tidsstempel: String) {
            meldinger.add(melding)
        }
    }
}
