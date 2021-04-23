package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import java.time.LocalDate
import java.time.LocalDateTime
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
    internal val arbeidsforholdId: String?,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: String?,
    private val harOpphørAvNaturalytelser: Boolean = false,
    mottatt: LocalDateTime
) : SykdomstidslinjeHendelse(meldingsreferanseId, mottatt) {

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

    private val arbeidsgiverperiode: Periode?
    private var sykdomstidslinje: Sykdomstidslinje

    init {
        if (arbeidsgiverperioder.isEmpty() && førsteFraværsdag == null) severe("Arbeidsgiverperiode er tom og førsteFraværsdag er null")
        val arbeidsgivertidslinje = arbeidsgivertidslinje()
        arbeidsgiverperiode = arbeidsgivertidslinje.periode()
        sykdomstidslinje = listOf(arbeidsgivertidslinje, førsteFraværsdagGaptidslinje(arbeidsgiverperiode)).merge(beste)
    }

    private fun arbeidsgivertidslinje(): Sykdomstidslinje {
        val tidslinje = arbeidsgiverperioder.map(::asArbeidsgivertidslinje).merge()
        val periode = tidslinje.periode() ?: return tidslinje
        val resultat = Sykdomstidslinje.arbeidsdager(periode, kilde).merge(tidslinje, replace)
        if (!førsteFraværsdagKantIKant(periode)) return resultat
        return resultat + førsteFraværsdagtidslinje()
    }

    private fun førsteFraværsdagKantIKant(periode: Periode?): Boolean {
        if (periode == null) return false
        return førsteFraværsdag == periode.endInclusive.plusDays(1)
    }

    private fun førsteFraværsdagGaptidslinje(arbeidsgiverperiode: Periode?): Sykdomstidslinje {
        if (førsteFraværsdag == null || førsteFraværsdagKantIKant(arbeidsgiverperiode)) return Sykdomstidslinje()
        val tidslinje = førsteFraværsdagtidslinje()
        if (arbeidsgiverperiode == null) return tidslinje
        if (!arbeidsgiverperiode.erRettFør(førsteFraværsdag)) return tidslinje
        val gapdager = arbeidsgiverperiode.periodeMellom(førsteFraværsdag) ?: return tidslinje
        return Sykdomstidslinje.arbeidsdager(gapdager, kilde) + tidslinje
    }

    private fun førsteFraværsdagtidslinje(): Sykdomstidslinje {
        if (førsteFraværsdag == null) return Sykdomstidslinje()
        return Sykdomstidslinje.arbeidsgiverdager(førsteFraværsdag, førsteFraværsdag, 100.prosent, kilde)
    }

    private fun asArbeidsgivertidslinje(periode: Periode) = Sykdomstidslinje.arbeidsgiverdager(periode.start, periode.endInclusive, 100.prosent, kilde)

    override fun sykdomstidslinje() = sykdomstidslinje

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

    override fun erRelevantMed(other: Periode): Boolean {
        if (førsteFraværsdagErEtterArbeidsgiverperioden()) return førsteFraværsdag in other
        return arbeidsgiverperiode?.overlapperMed(other) ?: false
    }

    private fun førsteFraværsdagErEtterArbeidsgiverperioden(): Boolean {
        if (førsteFraværsdag == null) return false
        return arbeidsgiverperiode?.slutterEtter(førsteFraværsdag) != true
    }

    override fun valider(periode: Periode): IAktivitetslogg {
        refusjon.valider(this, periode, beregnetInntekt)
        if (arbeidsgiverperioder.isEmpty()) warn("Inntektsmelding inneholder ikke arbeidsgiverperiode. Vurder om  arbeidsgiverperioden beregnes riktig")
        if (arbeidsforholdId != null && arbeidsforholdId.isNotBlank()) warn("ArbeidsforholdsID er fylt ut i inntektsmeldingen. Kontroller om brukeren har flere arbeidsforhold i samme virksomhet. Flere arbeidsforhold støttes ikke av systemet foreløpig.")
        begrunnelseForReduksjonEllerIkkeUtbetalt?.takeIf(String::isNotBlank)?.also {
            warn(
                "Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: %s. Vurder om dette har betydning for rett til sykepenger og beregning av arbeidsgiverperiode",
                it
            )
        }
        if (harOpphørAvNaturalytelser) error("Brukeren har opphold i naturalytelser")
        return this
    }

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    override fun organisasjonsnummer() = orgnummer

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = arbeidsgiver.håndter(this)

    internal fun addInntekt(inntektshistorikk: Inntektshistorikk, skjæringstidspunktVedtaksperiode: LocalDate) {
        val skjæringstidspunkt = (sykdomstidslinje.skjæringstidspunkt() ?: return).takeUnless {
            førsteFraværsdagErEtterArbeidsgiverperioden() && it > skjæringstidspunktVedtaksperiode
        } ?: skjæringstidspunktVedtaksperiode

        if (skjæringstidspunkt != førsteFraværsdag) {
            warn("Første fraværsdag i inntektsmeldingen er ulik skjæringstidspunktet. Kontrollér at inntektsmeldingen er knyttet til riktig periode.")
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

    internal fun cacheRefusjon(arbeidsgiver: Arbeidsgiver){
        refusjon.cacheRefusjon(arbeidsgiver, beregnetInntekt, sykdomstidslinje.førsteDag())
    }

    class Refusjon(
        private val opphørsdato: LocalDate?,
        private val inntekt: Inntekt?,
        private val endringerIRefusjon: List<LocalDate> = emptyList()
    ) {

        internal fun valider(
            aktivitetslogg: IAktivitetslogg,
            periode: Periode,
            beregnetInntekt: Inntekt
        ): IAktivitetslogg {
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

        internal fun cacheRefusjon(arbeidsgiver: Arbeidsgiver, beregnetInntekt: Inntekt, førsteDagIArbeidsgiverperioden: LocalDate) {
            val refusjonsopphørsdato =
                if (beregnetInntekt != inntekt) førsteDagIArbeidsgiverperioden
                else (endringerIRefusjon + opphørsdato).filterNotNull().minOrNull()
            arbeidsgiver.cacheRefusjon(refusjonsopphørsdato)
        }
    }
}
