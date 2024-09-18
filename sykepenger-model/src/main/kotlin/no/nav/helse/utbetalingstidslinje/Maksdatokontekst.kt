package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.Alder

internal data class Maksdatokontekst(
    // dato for vurderingene
    val vurdertTilOgMed: LocalDate,
    val startdatoSykepengerettighet: LocalDate,
    val startdatoTreårsvindu: LocalDate,
    val betalteDager: Set<LocalDate>,
    val oppholdsteller: Int
) {
    companion object {
        val TomKontekst = Maksdatokontekst(LocalDate.MIN, LocalDate.MIN, LocalDate.MIN, emptySet(), 0)
    }
    internal val forbrukteDager = betalteDager.size

    internal fun erDagerUnder67ÅrForbrukte(alder: Alder, regler: ArbeidsgiverRegler) =
        gjenståendeDagerUnder67År(alder, regler) == 0

    internal fun erDagerOver67ÅrForbrukte(alder: Alder, regler: ArbeidsgiverRegler) =
        gjenståendeDagerOver67År(alder, regler) == 0

    internal fun gjenståendeDagerUnder67År(alder: Alder, regler: ArbeidsgiverRegler) = regler.maksSykepengedager() - forbrukteDager
    internal fun gjenståendeDagerOver67År(alder: Alder, regler: ArbeidsgiverRegler): Int {
        val redusertYtelseAlder = alder.redusertYtelseAlder
        val forbrukteDagerOver67 = betalteDager.count { it > redusertYtelseAlder }
        return regler.maksSykepengedagerOver67() - forbrukteDagerOver67
    }

    internal fun inkrementer(dato: LocalDate) = copy(
        vurdertTilOgMed = dato,
        betalteDager = betalteDager.plus(dato),
        oppholdsteller = 0
    )
    // tilgir forbrukte dager som følge av at treårsvinduet forskyves
    internal fun dekrementer(dato: LocalDate, nyStartdatoTreårsvindu: LocalDate) = copy(
        vurdertTilOgMed = dato,
        startdatoTreårsvindu = nyStartdatoTreårsvindu,
        betalteDager = betalteDager.filter { it >= nyStartdatoTreårsvindu }.toSet() + dato,
        oppholdsteller = 0
    )
    internal fun økOppholdstelling(dato: LocalDate) = copy(
        vurdertTilOgMed = dato,
        oppholdsteller = this.oppholdsteller + 1
    )
}