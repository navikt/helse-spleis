package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Bokstav
import no.nav.helse.person.Ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum
import java.time.DayOfWeek
import java.time.LocalDate

typealias SammenstillingStrategi<T> = (other: T) -> List<JuridiskVurdering>

abstract class JuridiskVurdering {
    abstract val oppfylt: Boolean
    abstract val versjon: LocalDate
    abstract val paragraf: Paragraf
    abstract val ledd: Ledd
    open val punktum: List<Punktum> = emptyList()
    open val bokstav: List<Bokstav> = emptyList()

    //TODO: Ta stilling til om disse skal types sterkt for å ungå problematikk med equals på komplekse datastrukturer
    abstract val input: Map<String, Any>
    abstract val output: Map<String, Any>

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
        fun List<JuridiskVurdering>.erstatt(replacee: JuridiskVurdering, replacement: JuridiskVurdering): List<JuridiskVurdering> {
            return this.toMutableList().apply {
                remove(replacee)
                add(replacement)
            }
        }
    }
}

class EnkelVurdering(
    override val oppfylt: Boolean,
    override val paragraf: Paragraf,
    override val versjon: LocalDate,
    override val ledd: Ledd,
    override val punktum: List<Punktum> = emptyList(),
    override val bokstav: List<Bokstav> = emptyList(),
    override val input: Map<String, Any>,
    override val output: Map<String, Any>
) : JuridiskVurdering() {
    override fun sammenstill(vurderinger: List<JuridiskVurdering>) =
        sammenstill<EnkelVurdering>(vurderinger) { vurderinger.erstatt(it, this) }
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
    override val punktum: List<Punktum> = emptyList(),
    override val bokstav: List<Bokstav> = emptyList(),
    override val input: Map<String, Any>,
    override val output: Map<String, Any>
) : JuridiskVurdering() {
    internal constructor(
        dato: LocalDate,
        input: Map<String, Any>,
        output: Map<String, Any>,
        oppfylt: Boolean,
        versjon: LocalDate,
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: List<Punktum> = emptyList(),
        bokstav: List<Bokstav> = emptyList()
    ) : this(dato, dato, dato, oppfylt, versjon, paragraf, ledd, punktum, bokstav, input, output)

    override fun sammenstill(vurderinger: List<JuridiskVurdering>) =
        sammenstill<GrupperbarVurdering>(vurderinger) { grupperSammenhengende(it, vurderinger) }

    private fun grupperSammenhengende(other: GrupperbarVurdering, tidligereVurderinger: List<JuridiskVurdering>): List<JuridiskVurdering> {
        return when {
            overlapper(other) -> tidligereVurderinger
            liggerInntilFør(other) -> tidligereVurderinger.kopierMed(other, other.originalDato, fom, other.tom)
            liggerInntilEtter(other) -> tidligereVurderinger.kopierMed(other, other.originalDato, other.fom, tom)
            else -> tidligereVurderinger + this
        }
    }

    private fun List<JuridiskVurdering>.kopierMed(
        other: GrupperbarVurdering,
        originalDato: LocalDate,
        fom: LocalDate,
        tom: LocalDate
    ): List<JuridiskVurdering> {
        return erstatt(other, GrupperbarVurdering(originalDato, fom, tom, oppfylt, versjon, paragraf, ledd, punktum, bokstav, input, output))
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

class BetingetVurdering(
    private val funnetRelevant: Boolean,
    override val oppfylt: Boolean,
    override val versjon: LocalDate,
    override val paragraf: Paragraf,
    override val ledd: Ledd,
    override val punktum: List<Punktum> = emptyList(),
    override val bokstav: List<Bokstav> = emptyList(),
    override val input: Map<String, Any>,
    override val output: Map<String, Any>
) : JuridiskVurdering() {
    override fun sammenstill(vurderinger: List<JuridiskVurdering>): List<JuridiskVurdering> {
        if (!funnetRelevant) return vurderinger
        return sammenstill<BetingetVurdering>(vurderinger) { vurderinger.erstatt(it, this) }
    }
}
