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
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse

internal class DagerFraInntektsmelding(
    private val inntektsmelding: Inntektsmelding
): IAktivitetslogg by inntektsmelding {
    private val opprinneligeDager = inntektsmelding.sykdomstidslinje().periode()?.toSet() ?: emptySet()
    private val gjenståendeDager = opprinneligeDager.toMutableSet()

    internal fun trimLeft(dato: LocalDate) = inntektsmelding.trimLeft(dato)
    internal fun oppdatertFom(periode: Periode) = inntektsmelding.oppdaterFom(periode)
    private fun dagerFør(periode: Periode) = gjenståendeDager.filter { it < periode.start }.toSet()

    internal fun håndterGjenståendeFør(periode: Periode, oppdaterSykdom: (sykdomstidslinje: SykdomstidslinjeHendelse) -> Unit) {
        val dagerFør = dagerFør(periode).takeUnless { it.isEmpty() } ?: return
        oppdaterSykdom(PeriodeFraInntektsmelding(inntektsmelding, dagerFør.overordnetPeriode))
        gjenståendeDager.removeAll(dagerFør)
    }

    internal fun håndterGjenståendeFør(periode: Periode, arbeidsgiver: Arbeidsgiver) = håndterGjenståendeFør(periode) {
        arbeidsgiver.oppdaterSykdom(it)
    }

    private fun overlappendeDager(periode: Periode) = periode.intersect(gjenståendeDager)
    internal fun skalHåndteresAv(periode: Periode) = overlappendeDager(periode).isNotEmpty()

    internal fun håndter(periode: Periode, arbeidsgiver: Arbeidsgiver) = håndter(periode) {
        arbeidsgiver.oppdaterSykdom(it)
    }

    internal fun håndter(periode: Periode, oppdaterSykdom: (sykdomstidslinje: SykdomstidslinjeHendelse) -> Sykdomstidslinje): Sykdomstidslinje? {
        val overlappendeDager = overlappendeDager(periode).takeUnless { it.isEmpty() } ?: return null
        val arbeidsgiverSykedomstidslinje = oppdaterSykdom(PeriodeFraInntektsmelding(inntektsmelding, overlappendeDager.overordnetPeriode))
        gjenståendeDager.removeAll(overlappendeDager)
        return arbeidsgiverSykedomstidslinje.subset(periode)
    }

    internal fun håndterGjenstående(oppdaterSykdom: (sykdomstidslinje: SykdomstidslinjeHendelse) -> Unit) {
        if (gjenståendeDager.isEmpty()) return
        oppdaterSykdom(PeriodeFraInntektsmelding(inntektsmelding, gjenståendeDager.overordnetPeriode))
        gjenståendeDager.clear()
    }

    internal fun håndterGjenstående(arbeidsgiver: Arbeidsgiver) = håndterGjenstående {
        arbeidsgiver.oppdaterSykdom(it)
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