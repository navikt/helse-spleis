package no.nav.helse.person.etterlevelse

import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioderMedHensynTilHelg
import no.nav.helse.hendelser.til
import no.nav.helse.person.Bokstav
import no.nav.helse.person.Ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum
import java.time.LocalDate

typealias SammenstillingStrategi<T> = (other: T) -> List<JuridiskVurdering>

abstract class JuridiskVurdering {

    enum class Utfall {
        VILKAR_OPPFYLT, VILKAR_IKKE_OPPFYLT, VILKAR_UAVKLART, VILKAR_BEREGNET
    }

    abstract val utfall: Utfall
    abstract val versjon: LocalDate
    abstract val paragraf: Paragraf
    abstract val ledd: Ledd
    open val punktum: List<Punktum> = emptyList()
    open val bokstaver: List<Bokstav> = emptyList()

    //TODO: Ta stilling til om disse skal types sterkt for å ungå problematikk med equals på komplekse datastrukturer
    abstract val input: Map<String, Any>
    abstract val output: Map<String, Any>
    protected abstract val kontekster: Map<String, String>

    internal fun accept(visitor: JuridiskVurderingVisitor) {
        visitor.preVisitVurdering(utfall, versjon, paragraf, ledd, punktum, bokstaver, input, output, kontekster)
        acceptSpesifikk(visitor)
        visitor.postVisitVurdering(utfall, versjon, paragraf, ledd, punktum, bokstaver, input, output, kontekster)
    }

    abstract fun acceptSpesifikk(visitor: JuridiskVurderingVisitor)

    abstract fun sammenstill(vurderinger: List<JuridiskVurdering>): List<JuridiskVurdering>

    protected inline fun <reified T : JuridiskVurdering> sammenstill(
        vurderinger: List<JuridiskVurdering>,
        strategi: SammenstillingStrategi<T>
    ): List<JuridiskVurdering> {
        val tidligereVurdering = vurderinger.filterIsInstance<T>().firstOrNull { it == this }
        if (tidligereVurdering != null) return strategi(tidligereVurdering)
        return vurderinger + this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return other is JuridiskVurdering &&
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

    internal companion object {
        fun List<JuridiskVurdering>.erstatt(replacee: JuridiskVurdering, replacement: JuridiskVurdering): List<JuridiskVurdering> {
            return this.toMutableList().apply {
                remove(replacee)
                add(replacement)
            }
        }
    }
}

class EnkelVurdering(
    override val utfall: Utfall,
    override val versjon: LocalDate,
    override val paragraf: Paragraf,
    override val ledd: Ledd,
    override val punktum: List<Punktum> = emptyList(),
    override val bokstaver: List<Bokstav> = emptyList(),
    override val input: Map<String, Any>,
    override val output: Map<String, Any>,
    override val kontekster: Map<String, String>
) : JuridiskVurdering() {
    override fun sammenstill(vurderinger: List<JuridiskVurdering>) =
        sammenstill<EnkelVurdering>(vurderinger) { vurderinger.erstatt(it, this) }

    override fun acceptSpesifikk(visitor: JuridiskVurderingVisitor) {}
}

class GrupperbarVurdering private constructor(
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
) : JuridiskVurdering() {
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

    override fun acceptSpesifikk(visitor: JuridiskVurderingVisitor) {
        visitor.visitGrupperbarVurdering(fom, tom)
    }

    override fun sammenstill(vurderinger: List<JuridiskVurdering>): List<JuridiskVurdering> {
        val sammenstilt = (vurderinger + this)
            .filterIsInstance<GrupperbarVurdering>()
            .filter { it == this }
            .map { it.fom til it.tom }
            .grupperSammenhengendePerioderMedHensynTilHelg()
            .map { GrupperbarVurdering(it.start, it.endInclusive, utfall, versjon, paragraf, ledd, punktum, bokstaver, input, output, kontekster) }

        return vurderinger.filter { it != this } + sammenstilt
    }
}

class BetingetVurdering(
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
) : JuridiskVurdering() {
    override fun sammenstill(vurderinger: List<JuridiskVurdering>): List<JuridiskVurdering> {
        if (!funnetRelevant) return vurderinger
        return sammenstill<BetingetVurdering>(vurderinger) { vurderinger.erstatt(it, this) }
    }

    override fun acceptSpesifikk(visitor: JuridiskVurderingVisitor) {
        visitor.visitBetingetVurdering(funnetRelevant)
    }
}
