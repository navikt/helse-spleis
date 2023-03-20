package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.omsluttendePeriode
import no.nav.helse.hendelser.Periode.Companion.periodeRettFør
import no.nav.helse.hendelser.tilOrNull
import no.nav.helse.nesteDag
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode

internal class DagerFraInntektsmelding(
    private val inntektsmelding: Inntektsmelding,
    private val sammenhengendePerioder: List<Periode> = emptyList()
): IAktivitetslogg by inntektsmelding {
    private val opprinneligPeriode = inntektsmelding.sykdomstidslinje().periode()
    private val arbeidsdager = mutableSetOf<LocalDate>()
    private val gjenståendeDager = opprinneligPeriode?.toMutableSet() ?: mutableSetOf()
    private val alleDager get() = (opprinneligPeriode?: emptySet()) + arbeidsdager
    private val håndterteDager get() = alleDager - gjenståendeDager

    internal fun accept(visitor: DagerFraInntektsmeldingVisitor) {
        visitor.visitGjenståendeDager(gjenståendeDager)
    }

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
        val arbeidsdagerFør = oppdatertPeriode - opprinneligPeriode
        arbeidsdager.addAll(arbeidsdagerFør)
        gjenståendeDager.addAll(arbeidsdagerFør)
    }

    internal fun håndterPeriodeRettFør(periode: Periode, oppdaterSykdom: (sykdomstidslinje: SykdomstidslinjeHendelse) -> Unit) {
        val periodeRettFør = periodeRettFør(periode) ?: return
        oppdaterSykdom(BitAvInntektsmelding(inntektsmelding, periodeRettFør))
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
        val arbeidsgiverSykedomstidslinje = oppdaterSykdom(BitAvInntektsmelding(inntektsmelding, overlappendeDager.omsluttendePeriode!!))
        gjenståendeDager.removeAll(overlappendeDager)
        forrigePeriodeSomHåndterte = periode
        return arbeidsgiverSykedomstidslinje.subset(periode)
    }

    internal fun håndterGjenstående(oppdaterSykdom: (sykdomstidslinje: SykdomstidslinjeHendelse) -> Unit) {
        val gjenståendePeriode = gjenståendeDager.omsluttendePeriode ?: return
        oppdaterSykdom(BitAvInntektsmelding(inntektsmelding, gjenståendePeriode))
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
            valider(arbeidsgiver.arbeidsgiverperiode(it))
        }
    }

    internal fun håndterHaleEtter(periode: Periode, arbeidsgiver: Arbeidsgiver) {
        håndterHaleEtter(periode) {
            arbeidsgiver.oppdaterSykdom(it)
        }
    }

    internal fun håndterHaleEtter(periode: Periode, oppdaterSykdom: (sykdomstidslinje: SykdomstidslinjeHendelse) -> Unit) {
        val fom = periode.endInclusive.nesteDag
        val tom = gjenståendeDager.maxOrNull() ?: return
        val hale = (fom tilOrNull tom) ?: return
        oppdaterSykdom(BitAvInntektsmelding(inntektsmelding, hale.omsluttendePeriode!!))
        gjenståendeDager.removeAll(hale)
    }

    internal fun noenDagerHåndtert() = håndterteDager.isNotEmpty()

    internal fun påvirker(sykdomstidslinje: Sykdomstidslinje): Boolean {
        val periode = sykdomstidslinje.periode() ?: return false
        return sykdomstidslinje.påvirkesAv(BitAvInntektsmelding(inntektsmelding, periode).sykdomstidslinje())
    }

    private class BitAvInntektsmelding(
        private val inntektsmelding: Inntektsmelding,
        private val periode: Periode
    ): SykdomstidslinjeHendelse(inntektsmelding.meldingsreferanseId(), inntektsmelding) {
        override fun sykdomstidslinje() = inntektsmelding.sykdomstidslinje().subset(periode)
        override fun valider(periode: Periode, subsumsjonObserver: SubsumsjonObserver) = throw IllegalStateException("Ikke i bruk")
        override fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) = throw IllegalStateException("Ikke i bruk")
    }
}

internal interface DagerFraInntektsmeldingVisitor {
    fun visitGjenståendeDager(dager: Set<LocalDate>)
}