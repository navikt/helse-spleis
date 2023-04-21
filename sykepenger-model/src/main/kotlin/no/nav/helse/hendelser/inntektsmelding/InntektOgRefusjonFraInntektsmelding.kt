package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.førsteArbeidsdag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.nesteDag
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.Person
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode

internal class InntektOgRefusjonFraInntektsmelding(
    internal val inntektsmelding: Inntektsmelding,
    private val førsteFraværsdag: LocalDate?,
    arbeidsgiverperioder: List<Periode>
): IAktivitetslogg by inntektsmelding {

    private val arbeidsgiverperioden = arbeidsgiverperioder
        .flatten()
        .sorted()
        .take(16)
    private val sisteDagIArbeidsgiverperioden = arbeidsgiverperioden.maxOrNull()

    private val førsteFraværsdagEtterArbeidsgiverperioden =
        førsteFraværsdag != null && sisteDagIArbeidsgiverperioden != null && førsteFraværsdag > sisteDagIArbeidsgiverperioden
    private val ingenArbeidsgiverperiode = sisteDagIArbeidsgiverperioden == null

    internal fun meldingsreferanseId() = inntektsmelding.meldingsreferanseId()
    internal fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) =
        hendelseIder.add(Dokumentsporing.inntektsmeldingInntekt(meldingsreferanseId()))
    internal fun nyeArbeidsgiverInntektsopplysninger(skjæringstidspunkt: LocalDate, person: Person, subsumsjonObserver: SubsumsjonObserver) =
        person.nyeArbeidsgiverInntektsopplysninger(skjæringstidspunkt, inntektsmelding, subsumsjonObserver)

    internal fun valider(periode: Periode, skjæringstidspunkt: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode?) {
        inntektsmelding.validerInntektOgRefusjon(periode, skjæringstidspunkt)
        if (arbeidsgiverperiode?.sammenlign(arbeidsgiverperioden.grupperSammenhengendePerioder()) == true) return
        varsel(Varselkode.RV_IM_3)
    }

    internal fun addInntektsmelding(skjæringstidspunkt: LocalDate, arbeidsgiver: Arbeidsgiver, jurist: SubsumsjonObserver) =
        arbeidsgiver.addInntektsmelding(skjæringstidspunkt, inntektsmelding, jurist)

    private var håndtert: Boolean = false
    internal fun skalHåndteresAv(skjæringstidspunkt: LocalDate, periode: Periode, strategy: InntektOgRefusjonMatchingstrategi, forventerInntekt: () -> Boolean): Boolean {
        if (håndtert) return false
        if (!strategy.matcher(skjæringstidspunkt, periode, forventerInntekt)) return false
        håndtert = true
        return true
    }

    private fun LocalDate.erIPeriode(periode: Periode) = this in periode || this.førsteArbeidsdag() in periode

    internal inner class FørsteFraværsdagStrategi : InntektOgRefusjonMatchingstrategi {
        override fun matcher(skjæringstidspunkt: LocalDate, periode: Periode, forventerInntekt: () -> Boolean): Boolean {
            if (ingenArbeidsgiverperiode || førsteFraværsdagEtterArbeidsgiverperioden) {
                return førsteFraværsdag!!.erIPeriode(periode) && forventerInntekt()
            }
            return false
        }
    }

    internal inner class FørsteFraværsdagForskyvningsstrategi : InntektOgRefusjonMatchingstrategi {
        // Bruker matching på først fraværsdag, men forskyver første fraværsdag om vedtaksperioden(e) som treffes ikke forventer inntekt
        private var forskjøvetFørsteFraværsdag = førsteFraværsdag
        private var skjæringstidspunktForPeriodeMedFørsteFraværsdag: LocalDate? = null
        override fun matcher(skjæringstidspunkt: LocalDate, periode: Periode, forventerInntekt: () -> Boolean): Boolean {
            if (ingenArbeidsgiverperiode || førsteFraværsdagEtterArbeidsgiverperioden) {
                val førsteFraværsdagIPeriode = forskjøvetFørsteFraværsdag!!.erIPeriode(periode)
                if (!førsteFraværsdagIPeriode) return false
                if (førsteFraværsdag == forskjøvetFørsteFraværsdag) skjæringstidspunktForPeriodeMedFørsteFraværsdag = skjæringstidspunkt
                if (skjæringstidspunkt == skjæringstidspunktForPeriodeMedFørsteFraværsdag && forventerInntekt()) return true
                forskjøvetFørsteFraværsdag = periode.endInclusive.nesteDag
                return false
            }
            return false
        }
    }

    internal inner class FørsteDagEtterArbeidsgiverperiodenStrategi: InntektOgRefusjonMatchingstrategi {
        override fun matcher(skjæringstidspunkt: LocalDate, periode: Periode, forventerInntekt: () -> Boolean): Boolean {
            if (sisteDagIArbeidsgiverperioden == null) return false
            if (førsteFraværsdag?.isAfter(sisteDagIArbeidsgiverperioden) == true) return false
            val førsteDagEtterArbeidsgiverperioden = sisteDagIArbeidsgiverperioden.nesteDag
            val dagIPeriode = førsteDagEtterArbeidsgiverperioden.erIPeriode(periode)
            return dagIPeriode && forventerInntekt()
        }
    }

    internal inner class ArbeidsgiverperiodenStrategi: InntektOgRefusjonMatchingstrategi {
        override fun matcher(skjæringstidspunkt: LocalDate, periode: Periode, forventerInntekt: () -> Boolean): Boolean {
            if (sisteDagIArbeidsgiverperioden == null) return false
            if (førsteFraværsdag?.isAfter(sisteDagIArbeidsgiverperioden) == true) return false
            return arbeidsgiverperioden.any { it in periode } && forventerInntekt()
        }
    }

    internal inner class FørsteDagEtterArbeidsgiverperiodenForskyvningsstrategi : InntektOgRefusjonMatchingstrategi {
        // Bruker matching på første dag etter arbeidsgiverprioden, men forskyver dagen om vedtaksperioden(e) som treffes ikke forventer inntekt
        private val førsteDagEtterArbeidsgiverperioden = sisteDagIArbeidsgiverperioden?.nesteDag
        private var forskjøvetFørsteDagEtterArbeidsgiverperioden = førsteDagEtterArbeidsgiverperioden
        private var skjæringstidspunktForPeriodeMedFørsteDagEtterArbeidsgiverperioden: LocalDate? = null
        override fun matcher(skjæringstidspunkt: LocalDate, periode: Periode, forventerInntekt: () -> Boolean): Boolean {
            if (sisteDagIArbeidsgiverperioden == null) return false
            if (førsteFraværsdag?.isAfter(sisteDagIArbeidsgiverperioden) == true) return false
            val dagIPeriode = forskjøvetFørsteDagEtterArbeidsgiverperioden!!.erIPeriode(periode)
            if (!dagIPeriode) return false
            if (førsteDagEtterArbeidsgiverperioden == forskjøvetFørsteDagEtterArbeidsgiverperioden) skjæringstidspunktForPeriodeMedFørsteDagEtterArbeidsgiverperioden = skjæringstidspunkt
            if (skjæringstidspunkt == skjæringstidspunktForPeriodeMedFørsteDagEtterArbeidsgiverperioden && forventerInntekt()) return true
            forskjøvetFørsteDagEtterArbeidsgiverperioden = periode.endInclusive.nesteDag
            return false
        }
    }

    internal val strategier = listOf(
        FørsteDagEtterArbeidsgiverperiodenStrategi(),
        FørsteFraværsdagStrategi(),
        ArbeidsgiverperiodenStrategi(),
        FørsteDagEtterArbeidsgiverperiodenForskyvningsstrategi(),
        FørsteFraværsdagForskyvningsstrategi()
    )
}

internal interface InntektOgRefusjonMatchingstrategi {
    fun matcher(skjæringstidspunkt: LocalDate, periode: Periode, forventerInntekt: () -> Boolean): Boolean
}
