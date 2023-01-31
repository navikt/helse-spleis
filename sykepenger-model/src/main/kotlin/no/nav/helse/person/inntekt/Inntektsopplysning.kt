package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.InntektsopplysningVisitor
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.Inntekt

abstract class Inntektsopplysning protected constructor(
    protected val dato: LocalDate,
    private val prioritet: Int
) {
    internal abstract fun accept(visitor: InntektsopplysningVisitor)
    protected open fun avklarSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?): Inntektsopplysning? = null
    internal abstract fun omregnetÅrsinntekt(): Inntekt

    internal open fun overstyres(ny: Inntektsopplysning): Inntektsopplysning {
        if (ny.omregnetÅrsinntekt() == this.omregnetÅrsinntekt()) return this
        return ny
    }

    final override fun equals(other: Any?) = other is Inntektsopplysning && erSamme(other)

    final override fun hashCode(): Int {
        var result = dato.hashCode()
        result = 31 * result + prioritet
        return result
    }

    protected abstract fun erSamme(other: Inntektsopplysning): Boolean

    internal open fun subsumerSykepengegrunnlag(subsumsjonObserver: SubsumsjonObserver, organisasjonsnummer: String, startdatoArbeidsforhold: LocalDate?) { }

    internal open fun subsumerArbeidsforhold(
        subsumsjonObserver: SubsumsjonObserver,
        organisasjonsnummer: String,
        forklaring: String,
        oppfylt: Boolean
    ) {}

    private fun beste(other: Inntektsopplysning): Inntektsopplysning {
        if (this.dato > other.dato) return this
        if (this.prioritet >= other.prioritet) return this
        return other
    }

    internal companion object {
        internal fun List<Inntektsmelding>?.avklarSykepengegrunnlag(
            skjæringstidspunkt: LocalDate,
            førsteFraværsdag: LocalDate?,
            skattSykepengegrunnlag: SkattSykepengegrunnlag?,
            arbeidsforholdhistorikk: Arbeidsforholdhistorikk
        ): Inntektsopplysning? {
            val reserve = Skatteopplysning.nyoppstartetArbeidsforhold(skjæringstidspunkt, arbeidsforholdhistorikk)
            val tilgjengelige = listOfNotNull(skattSykepengegrunnlag) + (this ?: emptyList())
            val kandidater = tilgjengelige.mapNotNull { it.avklarSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag) }
            if (kandidater.isEmpty()) return reserve
            return kandidater.reduce { champion, challenger -> champion.beste(challenger) }
        }

        internal fun List<Inntektsopplysning>.valider(aktivitetslogg: IAktivitetslogg) {
            if (all { it is SkattSykepengegrunnlag }) {
                aktivitetslogg.funksjonellFeil(Varselkode.RV_VV_5)
            }
        }
    }
}