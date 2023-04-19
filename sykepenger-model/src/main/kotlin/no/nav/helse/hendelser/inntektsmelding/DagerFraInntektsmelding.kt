package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import no.nav.helse.erRettFør
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.omsluttendePeriode
import no.nav.helse.hendelser.Periode.Companion.periodeRettFør
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode

internal class DagerFraInntektsmelding(
    private val inntektsmelding: Inntektsmelding
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
    internal fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) =
        hendelseIder.add(Dokumentsporing.inntektsmeldingDager(meldingsreferanseId()))

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

    private fun overlappendeDager(periode: Periode) =  periode.intersect(gjenståendeDager)

    private fun periodeRettFør(periode: Periode) = gjenståendeDager.periodeRettFør(periode.start)

    internal fun skalHåndteresAv(periode: Periode): Boolean {
        val overlapperMedVedtaksperiode = overlappendeDager(periode).isNotEmpty()
        val periodeRettFør = periodeRettFør(periode) != null
        val vedtaksperiodeRettFør = gjenståendeDager.isNotEmpty() && periode.endInclusive.erRettFør(gjenståendeDager.first())
        return overlapperMedVedtaksperiode || periodeRettFør || vedtaksperiodeRettFør
    }

    internal fun harBlittHåndtertAv(periode: Periode) = håndterteDager.any { it in periode }

    internal fun håndter(periode: Periode, arbeidsgiverperiode: () -> Arbeidsgiverperiode?, oppdaterSykdom: (sykdomstidslinje: SykdomstidslinjeHendelse) -> Sykdomstidslinje): Sykdomstidslinje? {
        val overlappendeDager = overlappendeDager(periode).takeUnless { it.isEmpty() } ?: return null
        val arbeidsgiverSykedomstidslinje = oppdaterSykdom(BitAvInntektsmelding(inntektsmelding, overlappendeDager.omsluttendePeriode!!))
        gjenståendeDager.removeAll(overlappendeDager)
        if (gjenståendeDager.isEmpty()) inntektsmelding.validerArbeidsgiverperiode(arbeidsgiverperiode())
        return arbeidsgiverSykedomstidslinje.subset(periode)
    }

    internal fun valider(arbeidsgiverperiode: Arbeidsgiverperiode?) {
        if (gjenståendeDager.isEmpty()) return
        inntektsmelding.validerArbeidsgiverperiode(arbeidsgiverperiode)
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