package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Bokstav
import no.nav.helse.person.Ledd
import no.nav.helse.person.Ledd.Companion.ledd
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
    abstract fun aggreger(vurderinger: Set<ParagrafIKode>): ParagrafIKode

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return other is ParagrafIKode &&
            oppfylt == other.oppfylt &&
            versjon == other.versjon &&
            paragraf == other.paragraf &&
            ledd == other.ledd &&
            punktum == other.punktum &&
            bokstav == other.bokstav
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

class Paragraf816Ledd1 private constructor(
    private val fom: LocalDate,
    private val tom: LocalDate,
    private val dekningsgrad: Double,
    private val inntekt: Double,
    private val dekningsgrunnlag: Double
) : ParagrafIKode() {
    internal constructor(
        dato: LocalDate,
        dekningsgrad: Double,
        inntekt: Double,
        dekningsgrunnlag: Double
    ) : this(dato, dato, dekningsgrad, inntekt, dekningsgrunnlag)

    override val oppfylt: Boolean = true
    override val versjon: LocalDate = LocalDate.of(2020, 6, 12)
    override val paragraf = Paragraf.PARAGRAF_8_16
    override val ledd = 1.ledd

    override fun aggreger(vurderinger: Set<ParagrafIKode>): ParagrafIKode {
        val tidligereVurdering = vurderinger.find { it is Paragraf816Ledd1 } as? Paragraf816Ledd1
        if (tidligereVurdering == null || !harSammeParametre(tidligereVurdering)) return this

        return Paragraf816Ledd1(tidligereVurdering.fom, this.tom, dekningsgrad, inntekt, dekningsgrunnlag)
    }

    private fun harSammeParametre(other: Paragraf816Ledd1): Boolean =
        dekningsgrad == other.dekningsgrad && inntekt == other.inntekt && dekningsgrunnlag == other.dekningsgrunnlag
}

class EnkelVurdering(
    override val oppfylt: Boolean,
    private val skjæringstidspunkt: LocalDate,
    private val tilstrekkeligAntallOpptjeningsdager: Int,
    private val arbeidsforhold: List<Map<String, Any?>>,
    private val antallOpptjeningsdager: Int,
    override val paragraf: Paragraf,
    override val versjon: LocalDate,
    override val ledd: Ledd,
    override val bokstav: List<Bokstav> = emptyList(),
    override val punktum: List<Punktum> = emptyList()

) : ParagrafIKode() {

    override fun aggreger(vurderinger: Set<ParagrafIKode>) = this
}
