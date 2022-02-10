package no.nav.helse.person.etterlevelse

import no.nav.helse.hendelser.Periode
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
    open val punktum: Punktum? = null
    open val bokstav: Bokstav? = null

    abstract val input: Map<String, Any>
    abstract val output: Map<String, Any>
    protected abstract val kontekster: Map<String, String>

    internal fun accept(visitor: SubsumsjonVisitor) {
        visitor.preVisitSubsumsjon(utfall, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster)
        acceptSpesifikk(visitor)
        visitor.postVisitSubsumsjon(utfall, versjon, paragraf, ledd, punktum, bokstav, input, output, kontekster)
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
            bokstav == other.bokstav &&
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
        result = 31 * result + bokstav.hashCode()
        result = 31 * result + input.hashCode()
        result = 31 * result + output.hashCode()
        result = 31 * result + kontekster.hashCode()
        return result
    }

    override fun toString(): String {
        return "$paragraf $ledd ${if (punktum == null) "" else punktum} ${if (bokstav == null) "" else bokstav} [$utfall]"
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
    override val punktum: Punktum? = null,
    override val bokstav: Bokstav? = null,
    override val input: Map<String, Any>,
    override val output: Map<String, Any>,
    override val kontekster: Map<String, String>
) : Subsumsjon() {
    override fun sammenstill(subsumsjoner: List<Subsumsjon>) =
        sammenstill<EnkelSubsumsjon>(subsumsjoner) { subsumsjoner.erstatt(it, this) }

    override fun acceptSpesifikk(visitor: SubsumsjonVisitor) {}
}

class GrupperbarSubsumsjon private constructor(
    private val perioder: List<Periode>,
    override val utfall: Utfall,
    override val versjon: LocalDate,
    override val paragraf: Paragraf,
    override val ledd: Ledd,
    override val punktum: Punktum? = null,
    override val bokstav: Bokstav? = null,
    private val originalOutput: Map<String, Any>,
    override val input: Map<String, Any>,
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
        punktum: Punktum? = null,
        bokstav: Bokstav? = null,
        kontekster: Map<String, String>
    ) : this(listOf(dato til dato), utfall, versjon, paragraf, ledd, punktum, bokstav, output, input, kontekster)

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

    private fun harSammeDatagrunnlag(other: GrupperbarSubsumsjon) =
        paragraf == other.paragraf &&
            ledd == other.ledd &&
            punktum == other.punktum &&
            bokstav == other.bokstav &&
            kontekster == other.kontekster &&
            input == other.input &&
            originalOutput == other.originalOutput &&
            utfall == other.utfall

    override fun sammenstill(subsumsjoner: List<Subsumsjon>): List<Subsumsjon> {
        val sammenstiltePerioder = (subsumsjoner + this)
            .filterIsInstance<GrupperbarSubsumsjon>()
            .filter { it.harSammeDatagrunnlag(this) }
            .flatMap { it.perioder }
            .grupperSammenhengendePerioderMedHensynTilHelg()

        val gruppertSubsumsjon = GrupperbarSubsumsjon(sammenstiltePerioder, utfall, versjon, paragraf, ledd, punktum, bokstav, originalOutput, input, kontekster)

        return subsumsjoner.toMutableList().let { it ->
            it.removeAll { subsumsjon -> subsumsjon is GrupperbarSubsumsjon && subsumsjon.harSammeDatagrunnlag(this) }
            it + gruppertSubsumsjon
        }
    }
}

class BetingetSubsumsjon(
    private val funnetRelevant: Boolean,
    override val utfall: Utfall,
    override val versjon: LocalDate,
    override val paragraf: Paragraf,
    override val ledd: Ledd,
    override val punktum: Punktum? = null,
    override val bokstav: Bokstav? = null,
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
