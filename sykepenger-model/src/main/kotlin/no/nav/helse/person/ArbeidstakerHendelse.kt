package no.nav.helse.person

import java.util.UUID

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

    fun funksjonelleFeilOgVerre(): List<String> {
        val meldingsoppsamler = FunksjonelleFeilOgVerre()
        aktivitetslogg.accept(meldingsoppsamler)
        return meldingsoppsamler.meldinger()
    }

    internal class FunksjonelleFeilOgVerre: AktivitetsloggVisitor {
        private val meldinger = mutableListOf<String>()
        fun meldinger() = meldinger.toList()
        override fun visitFunksjonellFeil(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.FunksjonellFeil, melding: String, tidsstempel: String) {
            meldinger.add(melding)
        }

        override fun visitLogiskFeil(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.LogiskFeil, melding: String, tidsstempel: String) {
            meldinger.add(melding)
        }
    }
}
