package no.nav.helse.hendelser.inntektsmelding

import java.time.LocalDate
import no.nav.helse.førsteArbeidsdag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.nesteDag
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Person
import no.nav.helse.person.etterlevelse.SubsumsjonObserver

internal class InntektOgRefusjonFraInntektsmelding(
    private val inntektsmelding: Inntektsmelding,
    private val førsteFraværsdag: LocalDate?,
    arbeidsgiverperioder: List<Periode>
): IAktivitetslogg by inntektsmelding {

    private val sisteDagIArbeidsgiverperioden = arbeidsgiverperioder
        .flatten()
        .sorted()
        .take(16)
        .maxOrNull()

    private val førsteFraværsdagEtterArbeidsgiverperioden =
        førsteFraværsdag != null && sisteDagIArbeidsgiverperioden != null && førsteFraværsdag > sisteDagIArbeidsgiverperioden
    private val ingenArbeidsgiverperiode = sisteDagIArbeidsgiverperioden == null

    internal fun meldingsreferanseId() = inntektsmelding.meldingsreferanseId()
    internal fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) = inntektsmelding.leggTil(hendelseIder)
    internal fun nyeRefusjonsopplysninger(skjæringstidspunkt: LocalDate, person: Person) =
        person.nyeRefusjonsopplysninger(skjæringstidspunkt, inntektsmelding)

    internal fun valider(periode: Periode, skjæringstidspunkt: LocalDate) =
        inntektsmelding.validerInntektOgRefusjon(periode, skjæringstidspunkt)

    internal fun addInntektsmelding(skjæringstidspunkt: LocalDate, arbeidsgiver: Arbeidsgiver, jurist: SubsumsjonObserver) =
        arbeidsgiver.addInntektsmelding(skjæringstidspunkt, inntektsmelding, jurist)

    private var håndtert: Boolean = false
    internal fun skalHåndteresAv(periode: Periode, strategy: InntektOgRefusjonMatchingstrategi, forventerInntekt: () -> Boolean): Boolean {
        if (håndtert) return false
        if (!strategy.matcher(periode, forventerInntekt)) return false
        håndtert = true
        return true
    }

    private fun LocalDate.erIPeriode(periode: Periode) = this in periode || this.førsteArbeidsdag() in periode

    internal val førsteFraværsdagStrategi get() = FørsteFraværsdagStrategi()
    internal inner class FørsteFraværsdagStrategi : InntektOgRefusjonMatchingstrategi {
        override fun matcher(periode: Periode, forventerInntekt: () -> Boolean): Boolean {
            if (ingenArbeidsgiverperiode || førsteFraværsdagEtterArbeidsgiverperioden) {
                return førsteFraværsdag!!.erIPeriode(periode) && forventerInntekt()
            }
            return false
        }
    }

    internal val førsteFraværsdagForskyvningsstragi get() = FørsteFraværsdagForskyvningsstragi()
    internal inner class FørsteFraværsdagForskyvningsstragi : InntektOgRefusjonMatchingstrategi {
        // Bruker matching på først fraværsdag, men forskyver første fraværsdag om vedtaksperioden(e) som treffes ikke forventer inntekt
        private var forskjøvetFørsteFraværsdag = førsteFraværsdag
        override fun matcher(periode: Periode, forventerInntekt: () -> Boolean): Boolean {
            if (ingenArbeidsgiverperiode || førsteFraværsdagEtterArbeidsgiverperioden) {
                val førsteFraværsdagIPeriode = forskjøvetFørsteFraværsdag!!.erIPeriode(periode)
                if (!førsteFraværsdagIPeriode) return false
                if (forventerInntekt()) return true
                forskjøvetFørsteFraværsdag = periode.endInclusive.nesteDag
                return false
            }
            return false
        }
    }

    internal val førsteDagEtterArbeidsgiverperiodenStrategi get() = FørsteDagEtterArbeidsgiverperiodenStrategi()
    internal inner class FørsteDagEtterArbeidsgiverperiodenStrategi: InntektOgRefusjonMatchingstrategi {
        override fun matcher(periode: Periode, forventerInntekt: () -> Boolean): Boolean {
            if (sisteDagIArbeidsgiverperioden == null) return false
            if (førsteFraværsdag?.isAfter(sisteDagIArbeidsgiverperioden) == true) return false
            val førsteDagEtterArbeidsgiverperioden = sisteDagIArbeidsgiverperioden.nesteDag
            val dagIPeriode = førsteDagEtterArbeidsgiverperioden in periode || førsteDagEtterArbeidsgiverperioden.førsteArbeidsdag() in periode
            return dagIPeriode && forventerInntekt()
        }
    }

    internal val førsteDagEtterArbeidsgiverperiodenForskyvningsstragi get() = FørsteDagEtterArbeidsgiverperiodenForskyvningsstragi()
    internal inner class FørsteDagEtterArbeidsgiverperiodenForskyvningsstragi : InntektOgRefusjonMatchingstrategi {
        // Bruker matching på første dag etter arbeidsgiverprioden, men forskyver dagen om vedtaksperioden(e) som treffes ikke forventer inntekt
        private var forskjøvetFørsteDagEtterArbeidsgiverperioden = sisteDagIArbeidsgiverperioden?.nesteDag
        override fun matcher(periode: Periode, forventerInntekt: () -> Boolean): Boolean {
            if (sisteDagIArbeidsgiverperioden == null) return false
            if (førsteFraværsdag?.isAfter(sisteDagIArbeidsgiverperioden) == true) return false

            val dagIPeriode = forskjøvetFørsteDagEtterArbeidsgiverperioden!!.erIPeriode(periode)
            if (!dagIPeriode) return false
            if (forventerInntekt()) return true
            forskjøvetFørsteDagEtterArbeidsgiverperioden = periode.endInclusive.nesteDag
            return false
        }
    }

    internal val stragier get() = listOf(førsteFraværsdagStrategi, førsteDagEtterArbeidsgiverperiodenStrategi, førsteFraværsdagForskyvningsstragi, førsteDagEtterArbeidsgiverperiodenForskyvningsstragi)
}

internal interface InntektOgRefusjonMatchingstrategi {
    fun matcher(periode: Periode, forventerInntekt: () -> Boolean): Boolean
}
