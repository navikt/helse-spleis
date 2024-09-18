package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.Alder
import no.nav.helse.hendelser.Periode

internal data class Maksdatokontekst(
    // dato for vurderingene
    val vurdertTilOgMed: LocalDate,
    val startdatoSykepengerettighet: LocalDate,
    val startdatoTreårsvindu: LocalDate,
    val betalteDager: Set<LocalDate>,
    val oppholdsdager: Set<LocalDate>,
    val avslåtteDager: Set<LocalDate>
) {
    companion object {
        val TomKontekst = Maksdatokontekst(LocalDate.MIN, LocalDate.MIN, LocalDate.MIN, emptySet(), emptySet(), emptySet())
    }
    internal val oppholdsteller = oppholdsdager.size
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

    internal fun harNåddMaks(vedtaksperiode: Periode) =
        avslåtteDager.any { it in vedtaksperiode }

    internal fun datoForTilstrekkeligOppholdOppnådd(tilstrekkeligOpphold: Int) =
        oppholdsdager.sorted().getOrNull(tilstrekkeligOpphold - 1)

    internal fun fremdelesSykEtterTilstrekkeligOpphold(vedtaksperiode: Periode, tilstrekkeligOpphold: Int): Boolean {
        val datoForTilstrekkeligOpphold = datoForTilstrekkeligOppholdOppnådd(tilstrekkeligOpphold) ?: return false
        return avslåtteDager.any { it > datoForTilstrekkeligOpphold && it in vedtaksperiode }
    }
    internal fun begrunnelseForAvslåtteDager(alder: Alder, regler: ArbeidsgiverRegler, tilstrekkeligOpphold: Int): List<Pair<Begrunnelse, LocalDate>> {
        val datoForTilstrekkeligOppholdOppnådd = datoForTilstrekkeligOppholdOppnådd(tilstrekkeligOpphold)
        return avslåtteDager.map { avslåttDag ->
            val begrunnelseForAvslåttDag = when {
                alder.mistetSykepengerett(avslåttDag) -> Begrunnelse.Over70
                datoForTilstrekkeligOppholdOppnådd != null && avslåttDag > datoForTilstrekkeligOppholdOppnådd -> Begrunnelse.NyVilkårsprøvingNødvendig
                erDagerUnder67ÅrForbrukte(alder, regler) -> Begrunnelse.SykepengedagerOppbrukt
                else -> Begrunnelse.SykepengedagerOppbruktOver67
            }
            begrunnelseForAvslåttDag to avslåttDag
        }
    }

    internal fun inkrementer(dato: LocalDate) = copy(
        vurdertTilOgMed = dato,
        betalteDager = betalteDager.plus(dato),
        oppholdsdager = emptySet()
    )
    // tilgir forbrukte dager som følge av at treårsvinduet forskyves
    internal fun dekrementer(dato: LocalDate, nyStartdatoTreårsvindu: LocalDate) = copy(
        vurdertTilOgMed = dato,
        startdatoTreårsvindu = nyStartdatoTreårsvindu,
        betalteDager = betalteDager.filter { it >= nyStartdatoTreårsvindu }.toSet() + dato,
        oppholdsdager = emptySet()
    )
    internal fun medOppholdsdag(dato: LocalDate) = copy(
        vurdertTilOgMed = dato,
        oppholdsdager = oppholdsdager + dato
    )
    internal fun medAvslåttDag(dato: LocalDate) = copy(
        vurdertTilOgMed = dato,
        avslåtteDager = this.avslåtteDager + dato,
        oppholdsdager = oppholdsdager + dato
    )
}