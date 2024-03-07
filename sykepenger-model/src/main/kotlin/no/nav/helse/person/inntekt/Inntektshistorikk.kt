package no.nav.helse.person.inntekt


import java.time.LocalDate
import no.nav.helse.dto.InntektshistorikkDto
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.inntekt.AvklarbarSykepengegrunnlag.Companion.avklarSykepengegrunnlag

internal class Inntektshistorikk private constructor(private val historikk: MutableList<Inntektsmelding>) {

    internal constructor() : this(mutableListOf())

    internal companion object {
        internal fun gjenopprett(list: List<Inntektsmelding>) = Inntektshistorikk(list.toMutableList())
    }

    internal fun accept(visitor: InntekthistorikkVisitor) {
        visitor.preVisitInntekthistorikk(this)
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitInntekthistorikk(this)
    }

    internal fun leggTil(inntekt: Inntektsmelding): Boolean {
        if (historikk.any { !it.kanLagres(inntekt) }) return false
        historikk.add(0, inntekt)
        return true
    }

    internal fun avklarSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?, skattSykepengegrunnlag: SkattSykepengegrunnlag?): Inntektsopplysning? =
        historikk.avklarSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag, skattSykepengegrunnlag)

    internal fun dto() = InntektshistorikkDto(
        historikk = historikk.map { it.dto() }
    )
}
