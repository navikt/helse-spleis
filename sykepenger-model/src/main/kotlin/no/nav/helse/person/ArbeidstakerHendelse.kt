package no.nav.helse.person

abstract class ArbeidstakerHendelse protected constructor(
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg()
) : PersonHendelse(aktivitetslogg) {

    init {
        aktivitetslogg.kontekst(this)
    }

    abstract fun organisasjonsnummer(): String

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return this.javaClass.canonicalName.split('.').last().let {
            SpesifikkKontekst(it, mapOf(
                "aktørId" to aktørId(),
                "fødselsnummer" to fødselsnummer(),
                "organisasjonsnummer" to organisasjonsnummer()
            ) + kontekst())
        }
    }
}
