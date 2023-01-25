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
    arbeidsgiverperioder: List<Periode>,
    dagerFraInntektsmelding: DagerFraInntektsmelding
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
    internal fun nyeRefusjonsopplysninger(skjæringstidspunkt: LocalDate, person: Person, subsumsjonObserver: SubsumsjonObserver) =
        person.nyeArbeidsgiverInntektsopplysninger(skjæringstidspunkt, inntektsmelding, subsumsjonObserver)

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

    internal inner class FørsteFraværsdagStrategi : InntektOgRefusjonMatchingstrategi {
        override fun matcher(periode: Periode, forventerInntekt: () -> Boolean): Boolean {
            if (ingenArbeidsgiverperiode || førsteFraværsdagEtterArbeidsgiverperioden) {
                return førsteFraværsdag!!.erIPeriode(periode) && forventerInntekt()
            }
            return false
        }
    }

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

    internal inner class FørsteDagEtterArbeidsgiverperiodenStrategi: InntektOgRefusjonMatchingstrategi {
        override fun matcher(periode: Periode, forventerInntekt: () -> Boolean): Boolean {
            if (sisteDagIArbeidsgiverperioden == null) return false
            if (førsteFraværsdag?.isAfter(sisteDagIArbeidsgiverperioden) == true) return false
            val førsteDagEtterArbeidsgiverperioden = sisteDagIArbeidsgiverperioden.nesteDag
            val dagIPeriode = førsteDagEtterArbeidsgiverperioden.erIPeriode(periode)
            return dagIPeriode && forventerInntekt()
        }
    }

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

    internal inner class HarHåndtertDagerFraInntektsmeldingenStrategi(private val dagerFraInntektsmelding: DagerFraInntektsmelding) : InntektOgRefusjonMatchingstrategi {
        // Denne er en slags fallback-strategi hvis ingen av de andre strategiene fungerer, for å beholde dagens oppførsel
        override fun matcher(periode: Periode, forventerInntekt: () -> Boolean): Boolean {
            if (førsteFraværsdag?.isAfter(periode.endInclusive) == true) return false
            return dagerFraInntektsmelding.harBlittHåndtertAv(periode) && forventerInntekt()
        }

    }

    internal val strategier = listOf(
        FørsteFraværsdagStrategi(),
        FørsteDagEtterArbeidsgiverperiodenStrategi(),
        FørsteFraværsdagForskyvningsstragi(),
        FørsteDagEtterArbeidsgiverperiodenForskyvningsstragi(),
        HarHåndtertDagerFraInntektsmeldingenStrategi(dagerFraInntektsmelding)
    )
}

internal interface InntektOgRefusjonMatchingstrategi {
    fun matcher(periode: Periode, forventerInntekt: () -> Boolean): Boolean
}
