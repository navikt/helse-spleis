package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse

internal class DagerFraInntektsmelding(
    private val inntektsmelding: Inntektsmelding,
    førsteDag: LocalDate,
    sisteDag: LocalDate
): IAktivitetslogg by inntektsmelding {
    private val opprinneligeDager = (førsteDag til sisteDag).toSet()
    private val gjenståendeDager = opprinneligeDager.toMutableSet()

    internal fun trimLeft(dato: LocalDate) = inntektsmelding.trimLeft(dato)
    internal fun oppdatertFom(periode: Periode) = inntektsmelding.oppdaterFom(periode)
    private fun dagerFør(periode: Periode) = gjenståendeDager.filter { it < periode.start }

    internal fun håndter(periode: Periode, arbeidsgiver: Arbeidsgiver): Boolean {
        val overlappendeDager = periode.intersect(gjenståendeDager).takeUnless { it.isEmpty() } ?: return false
        val overlappendeDagerOgDagerFør = overlappendeDager.plus(dagerFør(periode))
        arbeidsgiver.oppdaterSykdom(PeriodeFraInntektsmelding(inntektsmelding, overlappendeDagerOgDagerFør.overordnetPeriode))
        gjenståendeDager.removeAll(overlappendeDagerOgDagerFør)
        return true
    }

    internal fun håndterGjenstående(arbeidsgiver: Arbeidsgiver) {
        if (gjenståendeDager.isEmpty()) return
        arbeidsgiver.oppdaterSykdom(PeriodeFraInntektsmelding(inntektsmelding, gjenståendeDager.overordnetPeriode))
        gjenståendeDager.clear()
    }

    private companion object {
        private val Iterable<LocalDate>.overordnetPeriode get() = min() til max()
    }

    private class PeriodeFraInntektsmelding(
        private val inntektsmelding: Inntektsmelding,
        private val periode: Periode
    ): SykdomstidslinjeHendelse(UUID.randomUUID(), inntektsmelding) {
        override fun sykdomstidslinje() = inntektsmelding.sykdomstidslinje().subset(periode)
        override fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver) = throw IllegalStateException("Ikke i bruk")
        override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = throw IllegalStateException("Ikke i bruk")
        override fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) = throw IllegalStateException("Ikke i bruk")
    }
}