package no.nav.helse.person.etterlevelse

import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioderMedHensynTilHelg
import no.nav.helse.hendelser.til
import no.nav.helse.person.Bokstav
import no.nav.helse.person.Ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum
import java.time.LocalDate

typealias SammenstillingStrategi<T> = (other: T) -> List<Subsumsjon>

abstract class Subsumsjon {

    enum class Utfall {
        VILKAR_OPPFYLT, VILKAR_IKKE_OPPFYLT, VILKAR_UAVKLART, VILKAR_BEREGNET
    }

    abstract val utfall: Utfall
    abstract val versjon: LocalDate
    abstract val paragraf: Paragraf
    abstract val ledd: Ledd
    open val punktum: List<Punktum> = emptyList()
    open val bokstaver: List<Bokstav> = emptyList()

    abstract val input: Map<String, Any>
    abstract val output: Map<String, Any>
    protected abstract val kontekster: Map<String, String>

    internal fun accept(visitor: SubsumsjonVisitor) {
        visitor.preVisitSubsumsjon(utfall, versjon, paragraf, ledd, punktum, bokstaver, input, output, kontekster)
        acceptSpesifikk(visitor)
        visitor.postVisitSubsumsjon(utfall, versjon, paragraf, ledd, punktum, bokstaver, input, output, kontekster)
    }

    abstract fun acceptSpesifikk(visitor: SubsumsjonVisitor)

    abstract fun sammenstill(subsumsjoner: List<Subsumsjon>): List<Subsumsjon>

    protected inline fun <reified T : Subsumsjon> sammenstill(
        subsumsjoner: List<Subsumsjon>,
        strategi: SammenstillingStrategi<T>
    ): List<Subsumsjon> {
        val tidligereSubsumsjon = subsumsjoner.filterIsInstance<T>().firstOrNull { it == this }
        if (tidligereSubsumsjon != null) return strategi(tidligereSubsumsjon)
        return subsumsjoner + this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return other is Subsumsjon &&
            utfall == other.utfall &&
            versjon == other.versjon &&
            paragraf == other.paragraf &&
            ledd == other.ledd &&
            punktum == other.punktum &&
            bokstaver == other.bokstaver &&
            input == other.input &&
            output == other.output &&
            kontekster == other.kontekster
    }

    override fun hashCode(): Int {
        var result = utfall.hashCode()
        result = 31 * result + versjon.hashCode()
        result = 31 * result + paragraf.hashCode()
        result = 31 * result + ledd.hashCode()
        result = 31 * result + punktum.hashCode()
        result = 31 * result + bokstaver.hashCode()
        result = 31 * result + input.hashCode()
        result = 31 * result + output.hashCode()
        result = 31 * result + kontekster.hashCode()
        return result
    }

    override fun toString(): String {
        return "$paragraf $ledd ${if (punktum.isEmpty()) "" else punktum[0]} ${if (bokstaver.isEmpty()) "" else bokstaver[0]} [$utfall]"
    }

    internal companion object {
        fun List<Subsumsjon>.erstatt(replacee: Subsumsjon, replacement: Subsumsjon): List<Subsumsjon> {
            return this.toMutableList().apply {
                remove(replacee)
                add(replacement)
            }
        }
    }
}

class EnkelSubsumsjon(
    override val utfall: Utfall,
    override val versjon: LocalDate,
    override val paragraf: Paragraf,
    override val ledd: Ledd,
    override val punktum: List<Punktum> = emptyList(),
    override val bokstaver: List<Bokstav> = emptyList(),
    override val input: Map<String, Any>,
    override val output: Map<String, Any>,
    override val kontekster: Map<String, String>
) : Subsumsjon() {
    override fun sammenstill(subsumsjoner: List<Subsumsjon>) =
        sammenstill<EnkelSubsumsjon>(subsumsjoner) { subsumsjoner.erstatt(it, this) }

    override fun acceptSpesifikk(visitor: SubsumsjonVisitor) {}
}

class GrupperbarSubsumsjon private constructor(
    private val fom: LocalDate,
    private val tom: LocalDate,
    override val utfall: Utfall,
    override val versjon: LocalDate,
    override val paragraf: Paragraf,
    override val ledd: Ledd,
    override val punktum: List<Punktum> = emptyList(),
    override val bokstaver: List<Bokstav> = emptyList(),
    override val input: Map<String, Any>,
    override val output: Map<String, Any>,
    override val kontekster: Map<String, String>
) : Subsumsjon() {
    internal constructor(
        dato: LocalDate,
        input: Map<String, Any>,
        output: Map<String, Any>,
        utfall: Utfall,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: List<Punktum> = emptyList(),
        bokstav: List<Bokstav> = emptyList(),
        kontekster: Map<String, String>
    ) : this(dato, dato, utfall, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster)

    override fun acceptSpesifikk(visitor: SubsumsjonVisitor) {
        visitor.visitGrupperbarSubsumsjon(fom, tom)
    }

    override fun sammenstill(subsumsjoner: List<Subsumsjon>): List<Subsumsjon> {
        val sammenstilt = (subsumsjoner + this)
            .filterIsInstance<GrupperbarSubsumsjon>()
            .filter { it == this }
            .map { it.fom til it.tom }
            .grupperSammenhengendePerioderMedHensynTilHelg()
            .map { GrupperbarSubsumsjon(it.start, it.endInclusive, utfall, versjon, paragraf, ledd, punktum, bokstaver, input, output, kontekster) }

        return subsumsjoner.filter { it != this } + sammenstilt
    }
}

class BetingetSubsumsjon(
    private val funnetRelevant: Boolean,
    override val utfall: Utfall,
    override val versjon: LocalDate,
    override val paragraf: Paragraf,
    override val ledd: Ledd,
    override val punktum: List<Punktum> = emptyList(),
    override val bokstaver: List<Bokstav> = emptyList(),
    override val input: Map<String, Any>,
    override val output: Map<String, Any>,
    override val kontekster: Map<String, String>
) : Subsumsjon() {
    override fun sammenstill(subsumsjoner: List<Subsumsjon>): List<Subsumsjon> {
        if (!funnetRelevant) return subsumsjoner
        return sammenstill<BetingetSubsumsjon>(subsumsjoner) { subsumsjoner.erstatt(it, this) }
    }

    override fun acceptSpesifikk(visitor: SubsumsjonVisitor) {
        visitor.visitBetingetSubsumsjon(funnetRelevant)
    }
}
