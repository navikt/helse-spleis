package no.nav.helse.person.aktivitetslogg

import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov

// Understands issues that arose when analyzing a JSON message
// Implements Collecting Parameter in Refactoring by Martin Fowler
// Implements Visitor pattern to traverse the messages
class Aktivitetslogg(
    private var forelder: Aktivitetslogg? = null
) : IAktivitetslogg {
    val aktiviteter = mutableListOf<Aktivitet>()
    private val kontekster = mutableListOf<Aktivitetskontekst>()  // Doesn't need serialization
    private val observers = mutableListOf<AktivitetsloggObserver>()

    fun accept(visitor: AktivitetsloggVisitor) {
        visitor.preVisitAktivitetslogg(this)
        aktiviteter.forEach { it.accept(visitor) }
        visitor.postVisitAktivitetslogg(this)
    }

    override fun register(observer: AktivitetsloggObserver) {
        observers.add(observer)
    }

    override fun info(melding: String, vararg params: Any?) {
        add(Aktivitet.Info.opprett(kontekster.toSpesifikk(), String.format(melding, *params)))
    }

    override fun varsel(melding: String) {
        add(Aktivitet.Varsel.opprett(kontekster.toSpesifikk(), melding = melding))
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
        this.aktiviteter.add(aktivitet)
        forelder?.add(aktivitet)
    }

    private fun MutableList<Aktivitetskontekst>.toSpesifikk() = this.map { it.toSpesifikkKontekst() }

    override fun harAktiviteter() = info().isNotEmpty() || harVarslerEllerVerre() || behov().isNotEmpty()

    override fun harVarslerEllerVerre() = varsel().isNotEmpty() || harFunksjonelleFeilEllerVerre()

    override fun harFunksjonelleFeilEllerVerre() = funksjonellFeil().isNotEmpty() || logiskFeil().isNotEmpty()

    override fun barn() = Aktivitetslogg(this).also { it.kontekster.addAll(this.kontekster) }

    override fun toString() = this.aktiviteter.map { it.inOrder() }.joinToString(separator = "\n") { it }

    override fun aktivitetsteller() = aktiviteter.size

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

    override fun kontekst(kontekst: Subaktivitetskontekst) {
        forelder = kontekst.aktivitetslogg
        kontekst(kontekst as Aktivitetskontekst)
    }

    override fun toMap(mapper: AktivitetsloggMappingPort): Map<String, List<Map<String, Any>>> = mapper.map(this)

    fun logg(vararg kontekst: Aktivitetskontekst): Aktivitetslogg {
        return Aktivitetslogg(this).also {
            it.aktiviteter.addAll(this.aktiviteter.filter { aktivitet ->
                kontekst.any { it in aktivitet }
            })
        }
    }

    internal fun logg(vararg kontekst: String): Aktivitetslogg {
        return Aktivitetslogg(this).also { aktivitetslogg ->
            aktivitetslogg.aktiviteter.addAll(this.aktiviteter.filter { aktivitet ->
                kontekst.any { kontekst -> kontekst in aktivitet.kontekster.map { it.kontekstType } }
            })
        }
    }

    override fun kontekster() =
        aktiviteter
            .groupBy { it.kontekst(null) }
            .map { Aktivitetslogg(this).apply { aktiviteter.addAll(it.value) } }

    private fun info() = Aktivitet.Info.filter(aktiviteter)
    fun varsel() = Aktivitet.Varsel.filter(aktiviteter)
    override fun behov() = Behov.filter(aktiviteter)
    private fun funksjonellFeil() = Aktivitet.FunksjonellFeil.filter(aktiviteter)
    private fun logiskFeil() = Aktivitet.LogiskFeil.filter(aktiviteter)


    class AktivitetException internal constructor(private val aktivitetslogg: Aktivitetslogg) :
        RuntimeException(aktivitetslogg.toString()) {

        fun kontekst() = aktivitetslogg.kontekster.fold(mutableMapOf<String, String>()) { result, kontekst ->
            result.apply { putAll(kontekst.toSpesifikkKontekst().kontekstMap) }
        }

        fun aktivitetslogg() = aktivitetslogg
    }

}

interface AktivitetsloggMappingPort {
    fun map(log: Aktivitetslogg): Map<String, List<Map<String, Any>>> // sorry
}