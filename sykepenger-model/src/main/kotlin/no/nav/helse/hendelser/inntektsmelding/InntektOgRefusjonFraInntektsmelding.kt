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

    internal fun meldingsreferanseId() = inntektsmelding.meldingsreferanseId()
    internal fun leggTil(hendelseIder: MutableSet<Dokumentsporing>) = inntektsmelding.leggTil(hendelseIder)
    internal fun nyeRefusjonsopplysninger(skjæringstidspunkt: LocalDate, person: Person) =
        person.nyeRefusjonsopplysninger(skjæringstidspunkt, inntektsmelding)

    internal fun valider(
        periode: Periode,
        skjæringstidspunkt: LocalDate
    ) =
        inntektsmelding.validerInntektOgRefusjon(periode, skjæringstidspunkt)

    internal fun addInntektsmelding(
        skjæringstidspunkt: LocalDate,
        arbeidsgiver: Arbeidsgiver,
        jurist: SubsumsjonObserver
    ) =
        arbeidsgiver.addInntektsmelding(skjæringstidspunkt, inntektsmelding, jurist)

    internal fun skalHåndteresAv(periode: Periode, strategy: InntektOgRefusjonMatchingStrategy, forventerInntekt: () -> Boolean)
        = strategy.skalHåndteresAv(periode, forventerInntekt)

    internal val gammelStrategy get() = GammelStrategy(førsteFraværsdag, sisteDagIArbeidsgiverperioden)
}

internal interface InntektOgRefusjonMatchingStrategy {
    fun skalHåndteresAv(periode: Periode, forventerInntekt: () -> Boolean): Boolean
}

internal class GammelStrategy(
    private val førsteFraværsdag: LocalDate?,
    private val sisteDagIArbeidsgiverperioden: LocalDate?
): InntektOgRefusjonMatchingStrategy {


    private val førsteFraværsdagEtterArbeidsgiverperioden =
        førsteFraværsdag != null && sisteDagIArbeidsgiverperioden != null && førsteFraværsdag > sisteDagIArbeidsgiverperioden
    private val ingenArbeidsgiverperiode = sisteDagIArbeidsgiverperioden == null

    private var nesteBestemmendeDag: LocalDate? = null
    private val bestemmendeDagFraInntektsmelding = when (ingenArbeidsgiverperiode || førsteFraværsdagEtterArbeidsgiverperioden) {
        true -> førsteFraværsdag!!
        false -> sisteDagIArbeidsgiverperioden!!.nesteDag
    }
    private fun bestemmendeDag() = nesteBestemmendeDag ?: bestemmendeDagFraInntektsmelding
    private var erHåndtert: Boolean = false

    override fun skalHåndteresAv(periode: Periode, forventerInntekt: () -> Boolean): Boolean {
        if (erHåndtert) return false
        if (førsteFraværsdag == null && sisteDagIArbeidsgiverperioden == null) return false
        val bestemmendeDag = bestemmendeDag()
        val bestemmendeDagIPeriode = bestemmendeDag in periode || bestemmendeDag.førsteArbeidsdag() in periode
        if (!bestemmendeDagIPeriode) return false
        // Nå vet vi at perioden matcher, men kan være f.eks. AUU med agp + ferie/ agp + helg
        if (forventerInntekt()) {
            erHåndtert = true
            return true
        }
        // Kan være at perioden rett etter oss skal håndtere inntekt
        nesteBestemmendeDag = periode.endInclusive.nesteDag
        return false
    }
}