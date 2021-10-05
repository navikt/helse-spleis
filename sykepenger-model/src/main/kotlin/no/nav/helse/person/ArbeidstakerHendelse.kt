package no.nav.helse.person

import java.util.*

abstract class ArbeidstakerHendelse protected constructor(
    meldingsreferanseId: UUID,
    private val aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : PersonHendelse(meldingsreferanseId, aktivitetslogg) {

    protected constructor(other: ArbeidstakerHendelse) : this(other.meldingsreferanseId(), other.aktivitetslogg)

    abstract fun organisasjonsnummer(): String

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
