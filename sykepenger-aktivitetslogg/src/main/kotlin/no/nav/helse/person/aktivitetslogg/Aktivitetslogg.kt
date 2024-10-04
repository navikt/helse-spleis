package no.nav.helse.person.aktivitetslogg

import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov

// Understands issues that arose when analyzing a JSON message
// Implements Collecting Parameter in Refactoring by Martin Fowler
// Implements Visitor pattern to traverse the messages
class Aktivitetslogg(
    private var forelder: Aktivitetslogg? = null
) : IAktivitetslogg {
    private val _aktiviteter = mutableListOf<Aktivitet>()
    val aktiviteter: List<Aktivitet> get() = _aktiviteter.toList()
    private val kontekster = mutableListOf<Aktivitetskontekst>()  // Doesn't need serialization
    private val observers = mutableListOf<AktivitetsloggObserver>()

    val behov get() = aktiviteter.filterIsInstance<Aktivitet.Behov>()
    val info get() = aktiviteter.filterIsInstance<Aktivitet.Info>()
    val varsel get() = aktiviteter.filterIsInstance<Aktivitet.Varsel>()
    val funksjonellFeil get() = aktiviteter.filterIsInstance<Aktivitet.FunksjonellFeil>()
    val logiskFeil get() = aktiviteter.filterIsInstance<Aktivitet.LogiskFeil>()

    override fun behov() = behov

    override fun register(observer: AktivitetsloggObserver) {
        observers.add(observer)
    }

    override fun info(melding: String, vararg params: Any?) {
        val formatertMelding = if (params.isEmpty()) melding else String.format(melding, *params)
        add(Aktivitet.Info.opprett(kontekster.toSpesifikk(), formatertMelding))
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

    override fun logiskFeil(melding: String, vararg params: Any?): Nothing {
        add(Aktivitet.LogiskFeil.opprett(kontekster.toSpesifikk(), String.format(melding, *params)))

        throw AktivitetException(this)
    }

    private fun add(aktivitet: Aktivitet) {
        observers.forEach { aktivitet.notify(it) }
        this._aktiviteter.add(aktivitet)
        forelder?.add(aktivitet)
    }

    private fun MutableList<Aktivitetskontekst>.toSpesifikk() = this.map { it.toSpesifikkKontekst() }

    override fun harAktiviteter() = info.isNotEmpty() || harVarslerEllerVerre() || behov.isNotEmpty()

    override fun harVarslerEllerVerre() = varsel.isNotEmpty() || harFunksjonelleFeilEllerVerre()

    override fun harFunksjonelleFeilEllerVerre() = funksjonellFeil.isNotEmpty() || logiskFeil.isNotEmpty()

    override fun barn() = Aktivitetslogg(this).also { it.kontekster.addAll(this.kontekster) }

    override fun toString() = this._aktiviteter.map { it.inOrder() }.joinToString(separator = "\n") { it }

    override fun aktivitetsteller() = _aktiviteter.size

    override fun kontekst(kontekst: Aktivitetskontekst) {
        val spesifikkKontekst = kontekst.toSpesifikkKontekst()
        val index = kontekster.indexOfFirst { spesifikkKontekst.sammeType(it) }
        if (index >= 0) fjernKonteksterFraOgMed(index)
        kontekster.add(kontekst)
    }

    private fun fjernKonteksterFraOgMed(indeks: Int) {
        val antall = kontekster.size - indeks
        repeat(antall) { kontekster.removeLast() }
    }

    override fun kontekst(parent: Aktivitetslogg, kontekst: Aktivitetskontekst) {
        forelder = parent
        kontekst(kontekst)
    }

    fun logg(vararg kontekst: Aktivitetskontekst): Aktivitetslogg {
        return Aktivitetslogg(this).also {
            it._aktiviteter.addAll(this._aktiviteter.filter { aktivitet ->
                kontekst.any { it in aktivitet }
            })
        }
    }

    internal fun logg(vararg kontekst: String): Aktivitetslogg {
        return Aktivitetslogg(this).also { aktivitetslogg ->
            aktivitetslogg._aktiviteter.addAll(this._aktiviteter.filter { aktivitet ->
                kontekst.any { kontekst -> kontekst in aktivitet.kontekster.map { it.kontekstType } }
            })
        }
    }

    override fun kontekster() =
        _aktiviteter
            .groupBy { it.kontekst(null) }
            .map { Aktivitetslogg(this).apply { _aktiviteter.addAll(it.value) } }

    class AktivitetException internal constructor(private val aktivitetslogg: Aktivitetslogg) :
        RuntimeException(aktivitetslogg.toString()) {

        fun kontekst() = aktivitetslogg.kontekster.fold(mutableMapOf<String, String>()) { result, kontekst ->
            result.apply { putAll(kontekst.toSpesifikkKontekst().kontekstMap) }
        }

        fun aktivitetslogg() = aktivitetslogg
    }

}