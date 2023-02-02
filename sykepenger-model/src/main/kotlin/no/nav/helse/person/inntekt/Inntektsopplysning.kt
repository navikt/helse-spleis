package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import no.nav.helse.person.InntektsopplysningVisitor
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.inntekt.AvklarbarSykepengegrunnlag.Inntektturnering
import no.nav.helse.økonomi.Inntekt
import kotlin.reflect.KClass

abstract class AvklarbarSykepengegrunnlag(
    dato: LocalDate,
    tidsstempel: LocalDateTime
) : Inntektsopplysning(dato, tidsstempel) {
    protected abstract fun avklarSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?): AvklarbarSykepengegrunnlag?

    internal fun beste(other: AvklarbarSykepengegrunnlag): AvklarbarSykepengegrunnlag {
        return turnering.avgjør(this, other)
    }

    private fun interface Inntektturnering {
        fun beste(venstre: AvklarbarSykepengegrunnlag, høyre: AvklarbarSykepengegrunnlag): AvklarbarSykepengegrunnlag
    }


    internal companion object {

        private val SisteAnkomne = Inntektturnering { venstre, høyre ->
            when {
                høyre.tidsstempel < venstre.tidsstempel -> venstre
                else -> høyre
            }
        }
        /*
            gir venstre om måneden er tidligere enn høyre, høyre ellers;
            eksempelvis dersom inntektsmelding (første fraværsdag) er i en annen måned enn skjæringstidspunktet, da
            skal skatteopplysningene brukes
        */
        private val TidligsteMåned = Inntektturnering { venstre, høyre ->
            when {
                YearMonth.from(venstre.dato) <= YearMonth.from(høyre.dato) -> venstre
                else -> høyre
            }
        }
        private val KunHøyre = Inntektturnering { _, høyre -> høyre }

        private val turnering = mapOf(
            Inntektsmelding::class to mapOf(
                Inntektsmelding::class to SisteAnkomne,
                SkattSykepengegrunnlag::class to TidligsteMåned,
                IkkeRapportert::class to TidligsteMåned
            )
        )

        private fun Map<KClass<Inntektsmelding>, Map<out KClass<out AvklarbarSykepengegrunnlag>, Inntektturnering>>.avgjør(venstre: AvklarbarSykepengegrunnlag, høyre: AvklarbarSykepengegrunnlag): AvklarbarSykepengegrunnlag {
            return this[venstre::class]?.get(høyre::class)?.beste(venstre, høyre)
                ?: this[høyre::class]?.get(venstre::class)?.beste(høyre, venstre) // kommutativ variant; a+b = b+a
                ?: error("mangelfull inntektturnering for [${venstre::class.simpleName}, ${høyre::class.simpleName}]")
        }
        internal fun List<Inntektsmelding>?.avklarSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?, skattSykepengegrunnlag: SkattSykepengegrunnlag?): Inntektsopplysning? {
            val tilgjengelige = listOfNotNull(skattSykepengegrunnlag) + (this ?: emptyList())
            val kandidater = tilgjengelige.mapNotNull { it.avklarSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag) }
            if (kandidater.isEmpty()) return null
            return kandidater.reduce { champion, challenger -> champion.beste(challenger) }
        }
    }
}

abstract class Inntektsopplysning protected constructor(
    protected val dato: LocalDate,
    protected val tidsstempel: LocalDateTime
) {
    internal abstract fun accept(visitor: InntektsopplysningVisitor)
    internal abstract fun omregnetÅrsinntekt(): Inntekt

    internal open fun overstyres(ny: Inntektsopplysning): Inntektsopplysning {
        if (ny.omregnetÅrsinntekt() == this.omregnetÅrsinntekt()) return this
        return ny
    }

    final override fun equals(other: Any?) = other is Inntektsopplysning && erSamme(other)

    final override fun hashCode(): Int {
        var result = dato.hashCode()
        result = 31 * result + tidsstempel.hashCode() * 31
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

    internal companion object {

        internal fun List<Inntektsopplysning>.valider(aktivitetslogg: IAktivitetslogg) {
            if (all { it is SkattSykepengegrunnlag }) {
                aktivitetslogg.funksjonellFeil(Varselkode.RV_VV_5)
            }
        }

        internal fun List<Inntektsopplysning>.validerStartdato(aktivitetslogg: IAktivitetslogg) {
            if (distinctBy { it.dato }.size <= 1 && none { it is SkattSykepengegrunnlag || it is IkkeRapportert }) return
            aktivitetslogg.varsel(Varselkode.RV_VV_2)
        }
    }
}