package no.nav.helse.person

interface Aktivitetting {
    fun opprettFor(hendelse: ArbeidstakerHendelse, vararg params: Any?)
    fun opprettFor(aktivitetslogg: Aktivitetslogg, vararg params: Any?)
    fun finnes(aktivitetslogg: Aktivitetslogg): Boolean
}

class Warning private constructor(private val tekst: String) {
    internal companion object {
        val TidslinjeMedDagUtenSøknad = Warning("Tidslinjen har minst én dag det ikke er søkt for")
    }

    internal fun opprettFor(hendelse: ArbeidstakerHendelse, vararg params: Any?) {
        // hendelse.warn(this, *params)
    }

    internal fun finnes(aktivitetslogg: Aktivitetslogg) = aktivitetslogg.warn().any { tekst in "$it" }

    internal fun opprett(kontekster: List<SpesifikkKontekst>, vararg params: Any?) =
        Aktivitetslogg.Aktivitet.Warn(kontekster, String.format(tekst, *params))
}

class Behovtype private constructor(private val tekst: String) {
    internal companion object {
        val Godkjenning = Behovtype("Godkjenning")
    }

    internal fun opprettFor(hendelse: ArbeidstakerHendelse, vararg params: Any?) {
        // hendelse.behov(this, *params)
    }

    internal fun finnes(aktivitetslogg: Aktivitetslogg) = aktivitetslogg.warn().any { tekst in "$it" }

    internal fun opprett(kontekster: List<SpesifikkKontekst>, vararg params: Any?) =
        Aktivitetslogg.Aktivitet.Warn(kontekster, String.format(tekst, *params))
}
