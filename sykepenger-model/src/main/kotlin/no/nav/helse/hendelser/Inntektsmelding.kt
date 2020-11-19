package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.InntektshistorikkVol2
import no.nav.helse.sykdomstidslinje.*
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.util.*

class Inntektsmelding(
    meldingsreferanseId: UUID,
    private val refusjon: Refusjon,
    private val orgnummer: String,
    private val fødselsnummer: String,
    private val aktørId: String,
    internal val førsteFraværsdag: LocalDate?,
    internal val beregnetInntekt: Inntekt,
    private val arbeidsgiverperioder: List<Periode>,
    ferieperioder: List<Periode>,
    private val arbeidsforholdId: String?,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    private val harOpphørAvNaturalytelser: Boolean = false
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

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
        arbeidsgivertidslinje(arbeidsgiverperioder, førsteFraværsdag)
            + ferietidslinje(ferieperioder)
            + nyFørsteFraværsdagtidslinje(førsteFraværsdag)
        ).merge(beste)

    init {
        if (arbeidsgiverperioder.isEmpty() && førsteFraværsdag == null) severe("Arbeidsgiverperiode er tom og førsteFraværsdag er null")
    }

    private fun arbeidsgivertidslinje(
        arbeidsgiverperioder: List<Periode>,
        førsteFraværsdag: LocalDate?
    ): List<Sykdomstidslinje> {
        val arbeidsgiverdager = arbeidsgiverperioder.map { it.asArbeidsgivertidslinje() }.merge(beste)

        var arbeidsdager = Sykdomstidslinje.arbeidsdager(arbeidsgiverdager.periode(), kilde)

        if (førsteFraværsdag?.let {
                arbeidsgiverdager.periode()?.endInclusive?.plusDays(1)?.erHelgedagRettFør(it)
            } == true) {
            arbeidsdager +=
                Sykdomstidslinje.arbeidsdager(
                    arbeidsgiverdager.sisteDag().plusDays(1),
                    førsteFraværsdag.minusDays(1),
                    kilde
                )
        }

        return listOfNotNull(arbeidsgiverdager, arbeidsdager)
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
                listOfNotNull(sykdomstidslinje.førsteSykedagEtter(it.start), it.start).maxOrNull()!!,
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

    override fun erRelevant(periode: Periode) =
        tidslinjeOverlapperMed(periode) && (førsteFraværsdag == null || førsteFraværsdagErIArbeidsgiverperioden() || førsteFraværsdagErFørEllerIPerioden(
            periode
        ))

    private fun tidslinjeOverlapperMed(periode: Periode) =
        (arbeidsgivertidslinje(arbeidsgiverperioder, førsteFraværsdag)
            + nyFørsteFraværsdagtidslinje(førsteFraværsdag)).merge(beste).periode()?.overlapperMed(periode) == true

    private fun førsteFraværsdagErIArbeidsgiverperioden() =
        arbeidsgiverperioder.isNotEmpty() && (requireNotNull(førsteFraværsdag) in arbeidsgiverperioder ||
            førsteFraværsdag.isEqual(
                arbeidsgivertidslinje(arbeidsgiverperioder, null).merge(beste).periode()?.endInclusive?.plusDays(1)
            ))

    private fun førsteFraværsdagErEtterArbeidsgiverperioden() =
        arbeidsgiverperioder.isNotEmpty() && (requireNotNull(førsteFraværsdag) !in arbeidsgiverperioder &&
            førsteFraværsdag.isAfter(
                arbeidsgivertidslinje(arbeidsgiverperioder, null).merge(beste).periode()?.endInclusive?.plusDays(1)
            ))

    private fun førsteFraværsdagErFørEllerIPerioden(periode: Periode) =
        requireNotNull(førsteFraværsdag) <= periode.endInclusive

    override fun valider(periode: Periode): Aktivitetslogg {
        refusjon.valider(aktivitetslogg, periode, beregnetInntekt)
        if (arbeidsgiverperioder.isEmpty()) aktivitetslogg.warn("Inntektsmelding inneholder ikke arbeidsgiverperiode. Vurder om  arbeidsgiverperioden beregnes riktig")
        if (arbeidsforholdId != null && arbeidsforholdId.isNotBlank()) aktivitetslogg.warn("ArbeidsforholdsID er fylt ut i inntektsmeldingen. Kontroller om brukeren har flere arbeidsforhold i samme virksomhet. Flere arbeidsforhold støttes ikke av systemet foreløpig.")
        begrunnelseForReduksjonEllerIkkeUtbetalt?.takeIf(String::isNotBlank)?.also {
            aktivitetslogg.warn(
                "Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: %s. Vurder om dette har betydning for rett til sykepenger og beregning av arbeidsgiverperiode",
                it
            )
        }
        if (harOpphørAvNaturalytelser) aktivitetslogg.error("Brukeren har opphold i naturalytelser")
        return aktivitetslogg
    }

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    override fun organisasjonsnummer() = orgnummer

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = arbeidsgiver.håndter(this)

    internal fun addInntekt(inntektshistorikk: Inntektshistorikk, skjæringstidspunktVedtaksperiode: LocalDate) {
        val skjæringstidspunkt = sykdomstidslinje.skjæringstidspunkt()
            ?.let { if (førsteFraværsdagErEtterArbeidsgiverperioden() && it.isAfter(skjæringstidspunktVedtaksperiode)) skjæringstidspunktVedtaksperiode else it }
            ?: return

        if (skjæringstidspunkt != førsteFraværsdag) {
            warn("Første fraværsdag oppgitt i inntektsmeldingen er ulik den systemet har beregnet. Vurder hvilken inntektsmelding som skal legges til grunn, og utbetal kun hvis dagsatsen er korrekt i forhold til denne.")
        }

        inntektshistorikk.add(
            skjæringstidspunkt.minusDays(1),  // Assuming salary is the day before the first sykedag
            meldingsreferanseId(),
            beregnetInntekt,
            Inntektshistorikk.Inntektsendring.Kilde.INNTEKTSMELDING
        )
    }

    internal fun addInntekt(inntektshistorikk: InntektshistorikkVol2, skjæringstidspunktVedtaksperiode: LocalDate) {
        val skjæringstidspunkt = sykdomstidslinje.skjæringstidspunkt()
            ?.let { if (førsteFraværsdagErEtterArbeidsgiverperioden() && it.isAfter(skjæringstidspunktVedtaksperiode)) skjæringstidspunktVedtaksperiode else it }
            ?: return

        if (skjæringstidspunkt != førsteFraværsdag) {
            warn("Første fraværsdag oppgitt i inntektsmeldingen er ulik den systemet har beregnet. Vurder hvilken inntektsmelding som skal legges til grunn, og utbetal kun hvis dagsatsen er korrekt i forhold til denne.")
        }

        inntektshistorikk {
            addInntektsmelding(
                skjæringstidspunkt,
                meldingsreferanseId(),
                beregnetInntekt
            )
        }
    }

    internal fun inntektenGjelderFor(periode: Periode) = sykdomstidslinje.skjæringstidspunkt() in periode

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
