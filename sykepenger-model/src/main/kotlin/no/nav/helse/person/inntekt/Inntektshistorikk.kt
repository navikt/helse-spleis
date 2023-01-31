package no.nav.helse.person.inntekt


import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.inntekt.AvklarbarSykepengegrunnlag.Companion.avklarSykepengegrunnlag

internal class Inntektshistorikk private constructor(private val historikk: MutableList<Inntektsmelding>) {

    internal constructor() : this(mutableListOf())

    internal companion object {
        internal val NULLUUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

        internal fun gjenopprett(list: List<Inntektsmelding>) = Inntektshistorikk(list.toMutableList())
    }

    internal fun accept(visitor: InntekthistorikkVisitor) {
        visitor.preVisitInntekthistorikk(this)
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitInntekthistorikk(this)
    }

    internal fun leggTil(inntekt: Inntektsmelding) {
        if (historikk.any { !it.kanLagres(inntekt) }) return
        historikk.add(0, inntekt)
    }

    internal fun isNotEmpty() = historikk.isNotEmpty()

    internal fun avklarSykepengegrunnlag(
        skjæringstidspunkt: LocalDate,
        førsteFraværsdag: LocalDate?,
        skattSykepengegrunnlag: SkattSykepengegrunnlag?,
        arbeidsforholdhistorikk: Arbeidsforholdhistorikk
    ): Inntektsopplysning? =
        historikk.avklarSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag, skattSykepengegrunnlag, arbeidsforholdhistorikk)

    internal fun isEmpty(): Boolean {
        return historikk.isEmpty()
    }

    internal class Innslag private constructor(private val id: UUID, private val inntekter: List<Inntektsmelding>) {
        constructor(inntekter: List<Inntektsmelding> = emptyList()) : this(UUID.randomUUID(), inntekter)

        override fun equals(other: Any?): Boolean {
            return other is Innslag && this.inntekter == other.inntekter
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + inntekter.hashCode()
            return result
        }

        internal companion object {
            internal fun Innslag?.avklarSykepengegrunnlag(
                skjæringstidspunkt: LocalDate,
                førsteFraværsdag: LocalDate?,
                skattSykepengegrunnlag: SkattSykepengegrunnlag?,
                arbeidsforholdhistorikk: Arbeidsforholdhistorikk
            ) =
                this?.inntekter.avklarSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag, skattSykepengegrunnlag, arbeidsforholdhistorikk)
            internal fun gjenopprett(id: UUID, inntektsopplysninger: List<Inntektsmelding>) =
                Innslag(id, inntektsopplysninger)

        }
    }
}
