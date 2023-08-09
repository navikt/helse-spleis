package no.nav.helse.etterlevelse

import java.time.LocalDate
import no.nav.helse.etterlevelse.RangeIterator.Companion.iterator
import no.nav.helse.etterlevelse.RangeIterator.Companion.merge
import no.nav.helse.etterlevelse.RangeIterator.Companion.utvide

abstract class Subsumsjon {

    enum class Utfall {
        VILKAR_OPPFYLT, VILKAR_IKKE_OPPFYLT, VILKAR_UAVKLART, VILKAR_BEREGNET
    }

    abstract val lovverk: String
    abstract val utfall: Utfall
    abstract val versjon: LocalDate
    abstract val paragraf: Paragraf
    abstract val ledd: Ledd?
    open val punktum: Punktum? = null
    open val bokstav: Bokstav? = null

    abstract val input: Map<String, Any>
    abstract val output: Map<String, Any>
    protected abstract val kontekster: Map<String, KontekstType>

    fun accept(visitor: SubsumsjonVisitor) {
        visitor.preVisitSubsumsjon(utfall, lovverk, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster)
        acceptSpesifikk(visitor)
        visitor.postVisitSubsumsjon(utfall, lovverk, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster)
    }

    protected abstract fun acceptSpesifikk(visitor: SubsumsjonVisitor)

    fun sammenstill(subsumsjoner: List<Subsumsjon>): List<Subsumsjon> {
        if (!skalSammenstille()) return subsumsjoner
        val (medSammeDatagrunnlag, utenSammeDatagrunnlag) = subsumsjoner.partition { subsumsjon -> subsumsjon == this }
        if (medSammeDatagrunnlag.isEmpty()) return subsumsjoner + this
        return utenSammeDatagrunnlag + sammenstillMed(medSammeDatagrunnlag)
    }

    protected open fun sammenstillMed(medSammeDatagrunnlag: List<Subsumsjon>) = this
    protected open fun skalSammenstille() = true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Subsumsjon) return false
        if (other::class != this::class) return false
        return ekstraEquals(other) && utfall == other.utfall &&
            paragraf == other.paragraf &&
            ledd == other.ledd &&
            punktum == other.punktum &&
            bokstav == other.bokstav &&
            input == other.input &&
            kontekster == other.kontekster
    }

    protected open fun ekstraEquals(other: Subsumsjon): Boolean {
        return other.versjon == this.versjon && other.output == this.output
    }

    override fun hashCode(): Int {
        var result = utfall.hashCode()
        result = 31 * result + versjon.hashCode()
        result = 31 * result + paragraf.hashCode()
        result = 31 * result + ledd.hashCode()
        result = 31 * result + punktum.hashCode()
        result = 31 * result + bokstav.hashCode()
        result = 31 * result + input.hashCode()
        result = 31 * result + output.hashCode()
        result = 31 * result + kontekster.hashCode()
        return result
    }

    override fun toString(): String {
        return "$paragraf $ledd ${if (punktum == null) "" else punktum} ${if (bokstav == null) "" else bokstav} [$utfall]"
    }
}

class EnkelSubsumsjon(
    override val utfall: Utfall,
    override val lovverk: String,
    override val versjon: LocalDate,
    override val paragraf: Paragraf,
    override val ledd: Ledd?,
    override val punktum: Punktum? = null,
    override val bokstav: Bokstav? = null,
    override val input: Map<String, Any>,
    override val output: Map<String, Any>,
    override val kontekster: Map<String, KontekstType>
) : Subsumsjon() {
    override fun acceptSpesifikk(visitor: SubsumsjonVisitor) {}
}

class GrupperbarSubsumsjon private constructor(
    private val perioder: List<ClosedRange<LocalDate>>,
    override val lovverk: String,
    override val utfall: Utfall,
    override val versjon: LocalDate,
    override val paragraf: Paragraf,
    override val ledd: Ledd?,
    override val punktum: Punktum? = null,
    override val bokstav: Bokstav? = null,
    private val originalOutput: Map<String, Any>,
    override val input: Map<String, Any>,
    override val kontekster: Map<String, KontekstType>
) : Subsumsjon() {
    constructor(
        dato: LocalDate,
        lovverk: String,
        input: Map<String, Any>,
        output: Map<String, Any>,
        utfall: Utfall,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd?,
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        kontekster: Map<String, KontekstType>
    ) : this(listOf(dato.rangeTo(dato)), lovverk, utfall, versjon, paragraf, ledd, punktum, bokstav, output, input, kontekster)

    override val output: Map<String, Any> = originalOutput.toMutableMap().apply {
        this["perioder"] = perioder.map {
            mapOf(
                "fom" to it.start,
                "tom" to it.endInclusive
            )
        }
    }

    override fun acceptSpesifikk(visitor: SubsumsjonVisitor) {
        visitor.visitGrupperbarSubsumsjon(perioder)
    }

    override fun ekstraEquals(other: Subsumsjon): Boolean {
        if (other !is GrupperbarSubsumsjon) return false
        return originalOutput == other.originalOutput
    }

    override fun sammenstillMed(medSammeDatagrunnlag: List<Subsumsjon>): Subsumsjon {
        // en ny grupperbar subsumsjon er alltid én enkelt dato
        val ny = this.perioder.single().iterator().asSequence().toList().single()
        var dato: LocalDate? = ny
        // utvider eventuelt en av de andre periodene om <ny> forlenger (over evt. helg)
        val sammenstiltePerioder = medSammeDatagrunnlag
            .filterIsInstance<GrupperbarSubsumsjon>()
            .flatMap { it.perioder }
            .map { other ->
                dato?.let {
                    other.utvide(it)?.also { dato = null }
                } ?: other
            } + listOfNotNull(dato?.let { it.rangeTo(it) })
        return GrupperbarSubsumsjon(
            perioder = sammenstiltePerioder.merge(),
            lovverk = lovverk,
            utfall = utfall,
            versjon = versjon,
            paragraf = paragraf,
            ledd = ledd,
            punktum = punktum,
            bokstav = bokstav,
            originalOutput = originalOutput,
            input = input,
            kontekster = kontekster
        )
    }
}

class BetingetSubsumsjon(
    private val funnetRelevant: Boolean,
    override val lovverk: String,
    override val utfall: Utfall,
    override val versjon: LocalDate,
    override val paragraf: Paragraf,
    override val ledd: Ledd?,
    override val punktum: Punktum? = null,
    override val bokstav: Bokstav? = null,
    override val input: Map<String, Any>,
    override val output: Map<String, Any>,
    override val kontekster: Map<String, KontekstType>
) : Subsumsjon() {
    override fun skalSammenstille() = funnetRelevant

    override fun acceptSpesifikk(visitor: SubsumsjonVisitor) {
        visitor.visitBetingetSubsumsjon(funnetRelevant)
    }
}
