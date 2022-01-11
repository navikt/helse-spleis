package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Bokstav
import no.nav.helse.person.Ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum
import java.time.DayOfWeek
import java.time.LocalDate

abstract class ParagrafIKode {
    abstract val oppfylt: Boolean
    abstract val versjon: LocalDate
    abstract val paragraf: Paragraf
    abstract val ledd: Ledd
    open val punktum: List<Punktum> = emptyList()
    open val bokstav: List<Bokstav> = emptyList()

    //TODO: Ta stilling til om disse skal types sterkt for å ungå problematikk med equals på komplekse datastrukturer
    abstract val input: Map<String, Any>
    abstract val output: Map<String, Any>

    abstract fun aggreger(vurderinger: List<ParagrafIKode>): List<ParagrafIKode>

    protected inline fun <reified T : ParagrafIKode> List<ParagrafIKode>.finnOgErstatt(
        onErstatt: (vurderinger: List<ParagrafIKode>, other: T) -> List<ParagrafIKode>
    ): List<ParagrafIKode> {
        val tidligereVurdering = filterIsInstance<T>().firstOrNull { it == this@ParagrafIKode }
        if (tidligereVurdering != null) return onErstatt(this, tidligereVurdering)
        return this + this@ParagrafIKode
    }

    override fun equals(other: Any?): Boolean {
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

    override fun hashCode(): Int {
        var result = oppfylt.hashCode()
        result = 31 * result + versjon.hashCode()
        result = 31 * result + paragraf.hashCode()
        result = 31 * result + ledd.hashCode()
        result = 31 * result + punktum.hashCode()
        result = 31 * result + bokstav.hashCode()
        result = 31 * result + input.hashCode()
        result = 31 * result + output.hashCode()
        return result
    }

    internal companion object {
        fun List<ParagrafIKode>.erstatt(replacee: ParagrafIKode, replacement: ParagrafIKode): List<ParagrafIKode> {
            return this.toMutableList().apply {
                remove(replacee)
                add(replacement)
            }
        }
    }
}

class GrupperbarVurdering private constructor(
    // For enkelthets skyld, datoen objektet opprinnelig gjelder for
    private val originalDato: LocalDate,
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
    ) : this(dato, dato, dato, oppfylt, versjon, paragraf, ledd, bokstav, punktum, input, output)

    override fun aggreger(vurderinger: List<ParagrafIKode>): List<ParagrafIKode> {
        return vurderinger.finnOgErstatt<GrupperbarVurdering>() { vurderinger, other ->
            return grupperSammenhengende(other, vurderinger)
        }
    }

    private fun grupperSammenhengende(other: GrupperbarVurdering, tidligereVurderinger: List<ParagrafIKode>): List<ParagrafIKode> {
        return when {
            overlapper(other) -> tidligereVurderinger
            liggerInntilFør(other) -> tidligereVurderinger.kopierMed(other, other.originalDato, fom, other.tom)
            liggerInntilEtter(other) -> tidligereVurderinger.kopierMed(other, other.originalDato, other.fom, tom)
            else -> tidligereVurderinger + this
        }
    }

    private fun List<ParagrafIKode>.kopierMed(other: GrupperbarVurdering, originalDato: LocalDate, fom: LocalDate, tom: LocalDate): List<ParagrafIKode> {
        return erstatt(other, GrupperbarVurdering(originalDato, fom, tom, oppfylt, versjon, paragraf, ledd, bokstav, punktum, input, output))
    }

    private fun overlapper(other: GrupperbarVurdering) = originalDato >= other.fom && originalDato <= other.tom

    private fun liggerInntilFør(other: GrupperbarVurdering) = originalDato == other.fom.minusDaysInklHelg(originalDato)

    private fun liggerInntilEtter(other: GrupperbarVurdering) = originalDato.minusDaysInklHelg(other.tom) == other.tom

    private fun LocalDate.minusDaysInklHelg(other: LocalDate): LocalDate {
        return this.minusDays(
            when (this.dayOfWeek) {
                DayOfWeek.SUNDAY -> if (other.dayOfWeek == DayOfWeek.FRIDAY) 2 else 1
                DayOfWeek.MONDAY -> when (other.dayOfWeek) {
                    DayOfWeek.FRIDAY -> 3
                    DayOfWeek.SATURDAY -> 2
                    else -> 1
                }
                else -> 1
            }
        )
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
    override fun aggreger(vurderinger: List<ParagrafIKode>): List<ParagrafIKode> {
        return vurderinger.finnOgErstatt<EnkelVurdering> { vurderinger, other ->
            vurderinger.erstatt(other, this)
        }
    }
}
