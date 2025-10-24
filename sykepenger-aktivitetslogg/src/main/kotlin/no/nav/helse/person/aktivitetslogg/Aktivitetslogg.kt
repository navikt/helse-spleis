package no.nav.helse.person.aktivitetslogg

import no.nav.helse.person.aktivitetslogg.Aktivitet.*

// Understands issues that arose when analyzing a JSON message
// Implements Collecting Parameter in Refactoring by Martin Fowler
// Implements Visitor pattern to traverse the messages
data class Aktivitetslogg(
    private val forelder: Aktivitetslogg? = null,
    private val kontekster: List<Aktivitetskontekst> = emptyList(),
    val aktiviteter: MutableList<Aktivitet> = mutableListOf()
) : IAktivitetslogg {

    val behov get() = aktiviteter.filterIsInstance<Behov>()
    val info get() = aktiviteter.filterIsInstance<Info>()
    val varsel get() = aktiviteter.filterIsInstance<Varsel>()
    val funksjonellFeil get() = aktiviteter.filterIsInstance<FunksjonellFeil>()

    override fun info(melding: String, vararg params: Any?) {
        val formatertMelding = if (params.isEmpty()) melding else String.format(melding, *params)
        add(Info.opprett(kontekster.toSpesifikk(), formatertMelding))
    }

    override fun varsel(kode: Varselkode) {
        add(kode.varsel(kontekster.toSpesifikk()))
    }

    override fun behov(type: Behov.Behovtype, melding: String, detaljer: Map<String, Any?>) {
        add(Behov.opprett(type, kontekster.toSpesifikk(), melding, detaljer))
    }

    override fun funksjonellFeil(kode: Varselkode) {
        add(kode.funksjonellFeil(kontekster.toSpesifikk()))
    }

    private fun add(aktivitet: Aktivitet) {
        this.aktiviteter.add(aktivitet)
        forelder?.add(aktivitet)
    }

    private fun Collection<Aktivitetskontekst>.toSpesifikk() = this.map { it.toSpesifikkKontekst() }

    override fun harVarslerEllerVerre() = varsel.isNotEmpty() || harFunksjonelleFeil()

    override fun harFunksjonelleFeil() = funksjonellFeil.isNotEmpty()

    override fun toString() = this.aktiviteter.joinToString(separator = "\n") { "$it" }

    override fun kontekst(kontekst: Aktivitetskontekst): Aktivitetslogg {
        val spesifikkKontekst = kontekst.toSpesifikkKontekst()
        val index = kontekster.indexOfFirst { spesifikkKontekst.sammeType(it) }
        val nyeKontekster = (if (index >= 0) kontekster.take(index) else kontekster).plusElement(kontekst)
        return copy(forelder = this, kontekster = nyeKontekster, aktiviteter = aktiviteter.toMutableList())
    }
}
