package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.hendelser.Periode

/**
 * Perioder saksbehandler har vurdert dithet at bruker har tapt nok arbeidstid til å ha rett på sykepenger,
 * tross < 20% tapt inntekt
 */
internal class MinimumSykdomsgradsvurdering(private val perioderMedTilstrekkeligTaptArbeidstid: MutableSet<LocalDate> = mutableSetOf()) {
    fun leggTil(nyePerioder: Set<Periode>) {
        perioderMedTilstrekkeligTaptArbeidstid.addAll(nyePerioder.flatten())
    }

    fun trekkFra(perioderSomIkkeHaddeNokLikevel: Set<Periode>) {
        perioderMedTilstrekkeligTaptArbeidstid.removeAll(perioderSomIkkeHaddeNokLikevel.flatten().toSet())
    }

    fun harTilstrekkeligFravær(dato: LocalDate) = perioderMedTilstrekkeligTaptArbeidstid.contains(dato)
}