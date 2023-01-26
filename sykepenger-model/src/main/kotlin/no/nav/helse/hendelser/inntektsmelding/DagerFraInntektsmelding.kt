package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.omsluttendePeriode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode

internal class DagerFraInntektsmelding(
    private val inntektsmelding: Inntektsmelding
): IAktivitetslogg by inntektsmelding {
    private val opprinneligPeriode = inntektsmelding.sykdomstidslinje().periode()
    private val gjenståendeDager = opprinneligPeriode?.toMutableSet() ?: mutableSetOf()
    private val håndterteDager = mutableSetOf<LocalDate>()

    internal fun meldingsreferanseId() = inntektsmelding.meldingsreferanseId()
    internal fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) = inntektsmelding.leggTil(hendelseIder)

    private fun valider(arbeidsgiverperiode: Arbeidsgiverperiode?) = inntektsmelding.validerArbeidsgiverperiode(arbeidsgiverperiode)
    internal fun vurdertTilOgMed(dato: LocalDate) = inntektsmelding.trimLeft(dato)
    internal fun oppdatertFom(periode: Periode) = inntektsmelding.oppdaterFom(periode)
    internal fun leggTilArbeidsdagerFør(dato: LocalDate) {
        checkNotNull(opprinneligPeriode) { "Forventer ikke å utvide en tom sykdomstidslinje" }
        inntektsmelding.padLeft(dato)
        val oppdatertPeriode = inntektsmelding.sykdomstidslinje().periode() ?: return
        if (opprinneligPeriode == oppdatertPeriode) return
        val nyeDager = oppdatertPeriode - opprinneligPeriode
        gjenståendeDager.addAll(nyeDager)
    }
    private fun dagerFør(periode: Periode) = gjenståendeDager.filter { it < periode.start }.toSet()

    internal fun håndterGjenståendeFør(periode: Periode, oppdaterSykdom: (sykdomstidslinje: SykdomstidslinjeHendelse) -> Unit) {
        val dagerFør = dagerFør(periode).takeUnless { it.isEmpty() } ?: return
        oppdaterSykdom(PeriodeFraInntektsmelding(inntektsmelding, dagerFør.omsluttendePeriode!!))
        gjenståendeDager.removeAll(dagerFør)
        håndterteDager.addAll(dagerFør)
    }

    internal fun håndterGjenståendeFør(periode: Periode, arbeidsgiver: Arbeidsgiver) = håndterGjenståendeFør(periode) {
        arbeidsgiver.oppdaterSykdom(it)
    }

    private fun overlappendeDager(periode: Periode) = periode.intersect(gjenståendeDager)
    internal fun skalHåndteresAv(periode: Periode) = overlappendeDager(periode).isNotEmpty()

    internal fun harBlittHåndtertAv(periode: Periode): Boolean {
        return håndterteDager.any { it in periode } || skalHåndteresAv(periode)
    }

    internal fun håndter(periode: Periode, arbeidsgiver: Arbeidsgiver) = håndter(periode) {
        arbeidsgiver.oppdaterSykdom(it)
    }

    private var forrigePeriodeSomHåndterte: Periode? = null

    internal fun håndter(periode: Periode, oppdaterSykdom: (sykdomstidslinje: SykdomstidslinjeHendelse) -> Sykdomstidslinje): Sykdomstidslinje? {
        val overlappendeDager = overlappendeDager(periode).takeUnless { it.isEmpty() } ?: return null
        val arbeidsgiverSykedomstidslinje = oppdaterSykdom(PeriodeFraInntektsmelding(inntektsmelding, overlappendeDager.omsluttendePeriode!!))
        gjenståendeDager.removeAll(overlappendeDager)
        håndterteDager.addAll(overlappendeDager)
        forrigePeriodeSomHåndterte = periode
        return arbeidsgiverSykedomstidslinje.subset(periode)
    }

    internal fun håndterGjenståendeArbeidsgiverperiodeHale(oppdaterSykdom: (sykdomstidslinje: SykdomstidslinjeHendelse) -> Unit) {
        val sisteDatoViHarHåndtert = håndterteDager.maxOrNull() ?: return
        gjenståendeDager.removeAll { it <= sisteDatoViHarHåndtert }
        if (gjenståendeDager.isEmpty()) return
        oppdaterSykdom(PeriodeFraInntektsmelding(inntektsmelding, gjenståendeDager.omsluttendePeriode!!))
        gjenståendeDager.clear()
    }

    internal fun håndterGjenståendeArbeidsgiverperiodeHale(arbeidsgiver: Arbeidsgiver) {
        håndterGjenståendeArbeidsgiverperiodeHale {
            arbeidsgiver.oppdaterSykdom(it)
        }
        forrigePeriodeSomHåndterte?.let {
            // Vi ønsker kun å validere arbeidsgiverperioden en gang, og dette til slutt
            // Dette for å unngå uenighet om agp hvis kun deler av historikken er lagt til
            valider(arbeidsgiver.arbeidsgiverperiode(it, SubsumsjonObserver.NullObserver))
        }
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