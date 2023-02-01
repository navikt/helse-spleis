package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.omsluttendePeriode
import no.nav.helse.hendelser.Periode.Companion.periodeRettFør
import no.nav.helse.hendelser.tilOrNull
import no.nav.helse.nesteDag
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.NullObserver
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode

internal class DagerFraInntektsmelding(
    private val inntektsmelding: Inntektsmelding,
    private val sammenhengendePerioder: List<Periode> = emptyList()
): IAktivitetslogg by inntektsmelding {
    private val opprinneligPeriode = inntektsmelding.sykdomstidslinje().periode()
    private val gjenståendeDager = opprinneligPeriode?.toMutableSet() ?: mutableSetOf()
    private val håndterteDager get() = (opprinneligPeriode?: emptySet()).minus(gjenståendeDager)

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

    internal fun håndterPeriodeRettFør(periode: Periode, oppdaterSykdom: (sykdomstidslinje: SykdomstidslinjeHendelse) -> Unit) {
        val periodeRettFør = periodeRettFør(periode) ?: return
        oppdaterSykdom(PeriodeFraInntektsmelding(inntektsmelding, periodeRettFør))
        gjenståendeDager.removeAll(periodeRettFør)
    }

    internal fun håndterPeriodeRettFør(periode: Periode, arbeidsgiver: Arbeidsgiver) = håndterPeriodeRettFør(periode) {
        arbeidsgiver.oppdaterSykdom(it)
    }

    private fun overlappendeDager(periode: Periode) =  periode.intersect(gjenståendeDager)

    private fun periodeRettFør(periode: Periode) = gjenståendeDager.periodeRettFør(periode.start)

    internal fun skalHåndteresAv(periode: Periode): Boolean {
        val sammenhengendePeriode = sammenhengendePerioder.single { periode.start in it }
        val overlapperMedSammenhengende = overlappendeDager(sammenhengendePeriode).isNotEmpty()
        val periodeRettFør = periodeRettFør(periode) != null
        return overlapperMedSammenhengende || periodeRettFør
    }

    internal fun harBlittHåndtertAv(periode: Periode) = håndterteDager.any { it in periode }

    internal fun håndter(periode: Periode, arbeidsgiver: Arbeidsgiver) = håndter(periode) {
        arbeidsgiver.oppdaterSykdom(it)
    }

    private var forrigePeriodeSomHåndterte: Periode? = null

    internal fun håndter(periode: Periode, oppdaterSykdom: (sykdomstidslinje: SykdomstidslinjeHendelse) -> Sykdomstidslinje): Sykdomstidslinje? {
        val overlappendeDager = overlappendeDager(periode).takeUnless { it.isEmpty() } ?: return null
        val arbeidsgiverSykedomstidslinje = oppdaterSykdom(PeriodeFraInntektsmelding(inntektsmelding, overlappendeDager.omsluttendePeriode!!))
        gjenståendeDager.removeAll(overlappendeDager)
        forrigePeriodeSomHåndterte = periode
        return arbeidsgiverSykedomstidslinje.subset(periode)
    }

    internal fun håndterGjenstående(oppdaterSykdom: (sykdomstidslinje: SykdomstidslinjeHendelse) -> Unit) {
        if (gjenståendeDager.isEmpty()) return
        // Om vi har håndtert noen dager skal vi kun ta med "halen" etter den siste perioden.
        // Om ingen dager er håndtert skal vi håndtere alle dager
        val håndterGjenståendeFraOgMed = håndterteDager.maxOrNull()?.nesteDag ?: gjenståendeDager.min()
        val gjenståendePeriode = (håndterGjenståendeFraOgMed tilOrNull gjenståendeDager.max()) ?: return
        oppdaterSykdom(PeriodeFraInntektsmelding(inntektsmelding, gjenståendePeriode.omsluttendePeriode!!))
        gjenståendeDager.removeAll(gjenståendePeriode)
    }

    internal fun håndterGjenstående(arbeidsgiver: Arbeidsgiver) {
        håndterGjenstående {
            arbeidsgiver.oppdaterSykdom(it)
        }
    }

    internal fun valider(arbeidsgiver: Arbeidsgiver) {
        forrigePeriodeSomHåndterte?.let {
            // Vi ønsker kun å validere arbeidsgiverperioden en gang, og dette til slutt
            // Dette for å unngå uenighet om agp hvis kun deler av historikken er lagt til
            valider(arbeidsgiver.arbeidsgiverperiode(it, NullObserver))
        }
    }

    internal fun håndterHaleEtter(periode: Periode, arbeidsgiver: Arbeidsgiver) {
        val fom = periode.endInclusive.nesteDag
        val tom = gjenståendeDager.maxOrNull() ?: return
        val hale = (fom tilOrNull tom) ?: return
        arbeidsgiver.oppdaterSykdom(PeriodeFraInntektsmelding(inntektsmelding, hale.omsluttendePeriode!!))
        gjenståendeDager.removeAll(hale)
    }

    internal fun ferdigstilt() = gjenståendeDager.isEmpty()


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