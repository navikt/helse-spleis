package no.nav.helse.hendelser.inntektsmelding

import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import no.nav.helse.førsteArbeidsdag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode

internal class InntektOgRefusjonFraInntektsmelding(
    private val inntektsmelding: Inntektsmelding,
    private val førsteFraværsdag: LocalDate?
): IAktivitetslogg by inntektsmelding {

    internal fun meldingsreferanseId() = inntektsmelding.meldingsreferanseId()
    internal fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) = inntektsmelding.leggTil(hendelseIder)
    internal fun nyeRefusjonsopplysninger(skjæringstidspunkt: LocalDate, person: Person) =
        person.nyeRefusjonsopplysninger(skjæringstidspunkt, inntektsmelding)
    internal fun valider(periode: Periode, skjæringstidspunkt: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode?, jurist: SubsumsjonObserver) =
        inntektsmelding.valider(periode, skjæringstidspunkt, arbeidsgiverperiode, jurist)
    internal fun addInntektsmelding(skjæringstidspunkt: LocalDate, arbeidsgiver: Arbeidsgiver, jurist: SubsumsjonObserver) =
        arbeidsgiver.addInntektsmelding(skjæringstidspunkt, inntektsmelding, jurist)

    private fun førsteFraværsdagEtterArbeidsgiverperioden(sisteDagIArbeidsgiverperioden: LocalDate?) =
        førsteFraværsdag != null && sisteDagIArbeidsgiverperioden != null && førsteFraværsdag > sisteDagIArbeidsgiverperioden

    private fun ingenArbeidsgiverperiode(sisteDagIArbeidsgiverperioden: LocalDate?) = sisteDagIArbeidsgiverperioden == null


    private var erHåndtert: Boolean = false
    internal fun skalHåndteresAv(vedtaksperiode: Periode, sisteDagIArbeidsgiverperioden: LocalDate?): Boolean {
        if (erHåndtert) return false
        if (førsteFraværsdag == null && sisteDagIArbeidsgiverperioden == null) return false
        val bestemmendeDag = when (ingenArbeidsgiverperiode(sisteDagIArbeidsgiverperioden) || førsteFraværsdagEtterArbeidsgiverperioden(sisteDagIArbeidsgiverperioden)) {
            true -> førsteFraværsdag!!
            false -> sisteDagIArbeidsgiverperioden!!.nesteDag
        }
        if (periodeOrNull(bestemmendeDag, vedtaksperiode.endInclusive)?.kunHelg == true) return false

        erHåndtert = bestemmendeDag in vedtaksperiode || bestemmendeDag.førsteArbeidsdag() in vedtaksperiode
        return erHåndtert
    }

    private companion object {
        private fun periodeOrNull(fom: LocalDate, tom: LocalDate) = if (tom >= fom) fom til tom else null
        private val helg = setOf(SATURDAY, SUNDAY)
        private val Periode.kunHelg get() = all { it.dayOfWeek in helg }
    }
}