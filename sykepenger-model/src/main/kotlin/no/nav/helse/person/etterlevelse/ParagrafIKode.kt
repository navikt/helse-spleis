package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Bokstav
import no.nav.helse.person.Ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum
import java.time.LocalDate

abstract class ParagrafIKode {
    abstract val oppfylt: Boolean
    abstract val versjon: LocalDate
    abstract val paragraf: Paragraf
    abstract val ledd: Ledd
    open val punktum: List<Punktum> = emptyList()
    open val bokstav: List<Bokstav> = emptyList()
    abstract val input: Map<String, Any>
    abstract val output: Map<String, Any>

    abstract fun aggreger(vurderinger: Set<ParagrafIKode>): ParagrafIKode

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return other is ParagrafIKode &&
            oppfylt == other.oppfylt &&
            versjon == other.versjon &&
            paragraf == other.paragraf &&
            ledd == other.ledd &&
            punktum == other.punktum &&
            bokstav == other.bokstav &&
            input == other.input &&
            output == other.output
    }

    final override fun hashCode(): Int {
        var result = versjon.hashCode()
        result = 31 * result + paragraf.hashCode()
        result = 31 * result + ledd.hashCode()
        result = 31 * result + punktum.hashCode()
        result = 31 * result + bokstav.hashCode()
        return result
    }
}

class GrupperbarVurdering private constructor(
    private val fom: LocalDate,
    private val tom: LocalDate,
    override val oppfylt: Boolean,
    override val versjon: LocalDate,
    override val paragraf: Paragraf,
    override val ledd: Ledd,
    override val bokstav: List<Bokstav> = emptyList(),
    override val punktum: List<Punktum> = emptyList(),
    override val input: Map<String, Any>,
    override val output: Map<String, Any>
) : ParagrafIKode() {
    internal constructor(
        dato: LocalDate,
        input: Map<String, Any>,
        output: Map<String, Any>,
        oppfylt: Boolean,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd,
        bokstav: List<Bokstav> = emptyList(),
        punktum: List<Punktum> = emptyList()
    ) : this(dato, dato, oppfylt, versjon, paragraf, ledd, bokstav, punktum, input, output)

    override fun aggreger(vurderinger: Set<ParagrafIKode>): ParagrafIKode {
        val tidligereVurdering = vurderinger.filterIsInstance<GrupperbarVurdering>().firstOrNull()
        if (tidligereVurdering == null || tidligereVurdering != this) return this
        return GrupperbarVurdering(tidligereVurdering.fom, this.tom, oppfylt, versjon, paragraf, ledd, bokstav, punktum, input, output)
    }
}

class EnkelVurdering(
    override val oppfylt: Boolean,
    override val paragraf: Paragraf,
    override val versjon: LocalDate,
    override val ledd: Ledd,
    override val bokstav: List<Bokstav> = emptyList(),
    override val punktum: List<Punktum> = emptyList(),
    override val input: Map<String, Any>,
    override val output: Map<String, Any>
) : ParagrafIKode() {
    override fun aggreger(vurderinger: Set<ParagrafIKode>) = this
}
