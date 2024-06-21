package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.Periode

/**
 * Perioder saksbehandler har vurdert dithet at bruker har tapt nok arbeidstid til å ha rett på sykepenger,
 * tross < 20% tapt inntekt
 */
internal class MinimumSykdomsgradsvurdering(private val perioderMedTilstrekkeligTaptArbeidstid: MutableSet<Periode> = mutableSetOf()) {
    fun leggTil(nyePerioder: Set<Periode>) {
        val ny = perioderMedTilstrekkeligTaptArbeidstid.fjernPerioder(nyePerioder) + nyePerioder
        perioderMedTilstrekkeligTaptArbeidstid.clear()
        perioderMedTilstrekkeligTaptArbeidstid.addAll(ny)
    }

    fun trekkFra(perioderSomIkkeHaddeNokLikevel: Set<Periode>) {
        val ny = perioderMedTilstrekkeligTaptArbeidstid.fjernPerioder(perioderSomIkkeHaddeNokLikevel)
        perioderMedTilstrekkeligTaptArbeidstid.clear()
        perioderMedTilstrekkeligTaptArbeidstid.addAll(ny)
    }

    private fun Collection<Periode>.fjernPerioder(nyePerioder: Set<Periode>) =
        this.flatMap { gammelPeriode ->
            nyePerioder.fold(listOf(gammelPeriode)) { result, nyPeriode ->
                result.flatMap { it.trim(nyPeriode) }
            }
        }

    fun fjernDagerSomSkalUtbetalesLikevel(tentativtAvslåtteDager: List<Periode>) =
        tentativtAvslåtteDager.fjernPerioder(perioderMedTilstrekkeligTaptArbeidstid)
}