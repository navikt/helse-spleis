package no.nav.helse.hendelser

import no.nav.helse.hendelser.Periode.Companion.slåSammen
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.InntekthistorikkVol2
import no.nav.helse.person.InntekthistorikkVol2.Inntektsendring.Kilde.INNTEKTSMELDING
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.util.*

class Inntektsmelding(
    meldingsreferanseId: UUID,
    private val refusjon: Refusjon,
    private val orgnummer: String,
    private val fødselsnummer: String,
    private val aktørId: String,
    førsteFraværsdag: LocalDate?,
    internal val beregnetInntekt: Inntekt,
    private val arbeidsgiverperioder: List<Periode>,
    ferieperioder: List<Periode>,
    private val arbeidsforholdId: String?,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: String?
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private var beingQualified = false
    internal val førsteFraværsdag: LocalDate?

    private val beste = { venstre: Dag, høyre: Dag ->
        when {
            venstre::class == høyre::class -> venstre
            venstre is UkjentDag -> høyre
            høyre is UkjentDag -> venstre
            venstre is Arbeidsgiverdag || venstre is ArbeidsgiverHelgedag -> venstre
            høyre is Arbeidsgiverdag || høyre is ArbeidsgiverHelgedag -> høyre
            venstre is Sykedag -> venstre
            høyre is Sykedag -> høyre
            venstre is Feriedag && høyre is Arbeidsdag -> venstre
            høyre is Feriedag && venstre is Arbeidsdag -> høyre
            venstre is Feriedag && høyre is FriskHelgedag -> venstre
            høyre is Feriedag && venstre is FriskHelgedag -> høyre
            else -> høyre.problem(venstre)
        }
    }

    private var sykdomstidslinje: Sykdomstidslinje = (
        arbeidsgivertidslinje(arbeidsgiverperioder)
            + ferietidslinje(ferieperioder)
            + nyFørsteFraværsdagtidslinje(førsteFraværsdag)
        ).merge(beste)

    init {
        if (arbeidsgiverperioder.isEmpty() && førsteFraværsdag == null) severe("Arbeidsgiverperiode er tom og førsteFraværsdag er null")
        this.førsteFraværsdag =
            slåSammenArbeidsgiverperiodeMedPåfølgendeFerie(ferieperioder).slåSammen().lastOrNull()?.start
                ?.takeIf { it.senereEnn(førsteFraværsdag) } ?: førsteFraværsdag
    }

    private fun slåSammenArbeidsgiverperiodeMedPåfølgendeFerie(ferieperioder: List<Periode>): List<Periode> {
        return arbeidsgiverperioder.map { arbeidsgiverperiode ->
            ferieperioder.fold(arbeidsgiverperiode) { utvidetPeriode, ferie ->
                if (utvidetPeriode.erRettFør(ferie)) utvidetPeriode.merge(ferie)
                else utvidetPeriode
            }
        }
    }

    private fun LocalDate.senereEnn(other: LocalDate?): Boolean {
        if (other == null) return false
        return this > other
    }

    private fun arbeidsgivertidslinje(arbeidsgiverperioder: List<Periode>): List<Sykdomstidslinje> {
        val arbeidsgiverdager = arbeidsgiverperioder.map { it.asArbeidsgivertidslinje() }.merge(beste)

        return listOfNotNull(arbeidsgiverdager, Sykdomstidslinje.arbeidsdager(arbeidsgiverdager.periode(), kilde))
    }

    private fun ferietidslinje(ferieperioder: List<Periode>): List<Sykdomstidslinje> =
        ferieperioder.map { it.asFerietidslinje() }

    private fun nyFørsteFraværsdagtidslinje(førsteFraværsdag: LocalDate?): List<Sykdomstidslinje> =
        listOf(førsteFraværsdag?.let { Sykdomstidslinje.arbeidsgiverdager(it, it, 100, kilde) } ?: Sykdomstidslinje())

    private fun Periode.asArbeidsgivertidslinje() = Sykdomstidslinje.arbeidsgiverdager(start, endInclusive, 100, kilde)
    private fun Periode.asFerietidslinje() = Sykdomstidslinje.feriedager(start, endInclusive, kilde)

    override fun sykdomstidslinje() = sykdomstidslinje

    override fun periode() =
        super.periode().let {
            Periode(
                listOfNotNull(sykdomstidslinje.førsteSykedagEtter(it.start), it.start).max()!!,
                it.endInclusive
            )
        }

    // Pad days prior to employer-paid days with assumed work days
    override fun padLeft(dato: LocalDate) {
        if (arbeidsgiverperioder.isEmpty()) return  // No justification to pad
        if (dato >= sykdomstidslinje.førsteDag()) return  // No need to pad if sykdomstidslinje early enough
        sykdomstidslinje += Sykdomstidslinje.arbeidsdager(
            dato,
            sykdomstidslinje.førsteDag().minusDays(1),
            this.kilde
        )
    }

    override fun valider(periode: Periode): Aktivitetslogg {
        refusjon.valider(aktivitetslogg, periode, beregnetInntekt)
        if (arbeidsgiverperioder.isEmpty()) aktivitetslogg.warn("Inntektsmelding inneholder ikke arbeidsgiverperiode. Kontroller at det kun er én arbeidsgiver. Flere arbeidsforhold støttes ikke av systemet")
        if (arbeidsforholdId != null && arbeidsforholdId.isNotBlank()) aktivitetslogg.warn("ArbeidsforholdsID er fylt ut i inntektsmeldingen. Kontroller om brukeren har flere arbeidsforhold i samme virksomhet. Flere arbeidsforhold støttes ikke av systemet foreløpig.")
        begrunnelseForReduksjonEllerIkkeUtbetalt?.takeIf(String::isNotBlank)?.also {
            aktivitetslogg.warn(
                "Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: %s. Vurder om dette har betydning for rett til sykepenger og beregning av arbeidsgiverperiode",
                it
            )
        }
        return aktivitetslogg
    }

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    override fun organisasjonsnummer() = orgnummer

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = arbeidsgiver.håndter(this)

    internal fun addInntekt(inntekthistorikk: InntekthistorikkVol2) {
        if (førsteFraværsdag == null) return
        inntekthistorikk.add(
            førsteFraværsdag.minusDays(1),  // Assuming salary is the day before the first sykedag
            meldingsreferanseId(),
            beregnetInntekt,
            INNTEKTSMELDING
        )
    }

    internal fun beingQualified() {
        beingQualified = true
    }

    fun isNotQualified() = !beingQualified

    class Refusjon(
        private val opphørsdato: LocalDate?,
        private val inntekt: Inntekt?,
        private val endringerIRefusjon: List<LocalDate> = emptyList()
    ) {

        internal fun valider(
            aktivitetslogg: Aktivitetslogg,
            periode: Periode,
            beregnetInntekt: Inntekt
        ): Aktivitetslogg {
            when {
                inntekt == null -> aktivitetslogg.error("Arbeidsgiver forskutterer ikke (krever ikke refusjon)")
                inntekt != beregnetInntekt -> aktivitetslogg.error("Inntektsmelding inneholder beregnet inntekt og refusjon som avviker med hverandre")
                opphørerRefusjon(periode) -> aktivitetslogg.error("Arbeidsgiver opphører refusjon i perioden")
                opphørsdato != null -> aktivitetslogg.error("Arbeidsgiver opphører refusjon")
                endrerRefusjon(periode) -> aktivitetslogg.error("Arbeidsgiver endrer refusjon i perioden")
                endringerIRefusjon.isNotEmpty() -> aktivitetslogg.error("Arbeidsgiver har endringer i refusjon")
            }
            return aktivitetslogg
        }

        private fun opphørerRefusjon(periode: Periode) =
            opphørsdato?.let { it in periode } ?: false

        private fun endrerRefusjon(periode: Periode) =
            endringerIRefusjon.any { it in periode }
    }
}
