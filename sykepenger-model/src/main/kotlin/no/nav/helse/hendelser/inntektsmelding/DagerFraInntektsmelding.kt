package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse

internal class DagerFraInntektsmelding(
    private val inntektsmelding: Inntektsmelding,
    inntektsmeldingPeriode: Periode
) {
    private val opprinneligeDager = inntektsmeldingPeriode.toSet()
    private val gjenståendeDager = opprinneligeDager.toMutableSet()


    internal fun håndterFør(periode: Periode, arbeidsgiver: Arbeidsgiver) {
        val dagerFør = gjenståendeDager.filter { it < periode.start }.takeUnless { it.isEmpty() } ?: return
        arbeidsgiver.oppdaterSykdom(PeriodeFraInntektsmelding(inntektsmelding, dagerFør.overordnetPeriode))
        gjenståendeDager.removeAll(dagerFør.toSet())
    }

    internal fun håndter(periode: Periode, arbeidsgiver: Arbeidsgiver) {
        val overlappendeDager = periode.intersect(gjenståendeDager).takeUnless { it.isEmpty() } ?: return
        arbeidsgiver.oppdaterSykdom(PeriodeFraInntektsmelding(inntektsmelding, overlappendeDager.overordnetPeriode))
        gjenståendeDager.removeAll(overlappendeDager)
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