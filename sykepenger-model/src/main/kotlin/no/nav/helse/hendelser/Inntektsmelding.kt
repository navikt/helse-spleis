package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Inntekthistorikk
import no.nav.helse.sykdomstidslinje.NyDag
import no.nav.helse.sykdomstidslinje.NyDag.*
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.merge
import java.time.LocalDate
import java.util.*

class Inntektsmelding(
    meldingsreferanseId: UUID,
    private val refusjon: Refusjon,
    private val orgnummer: String,
    private val fødselsnummer: String,
    private val aktørId: String,
    internal val førsteFraværsdag: LocalDate?,
    internal val beregnetInntekt: Double,
    private val arbeidsgiverperioder: List<Periode>,
    ferieperioder: List<Periode>,
    private val arbeidsforholdId: String?,
    private val begrunnelseForReduksjonEllerIkkeUtbetalt: String?
) : SykdomstidslinjeHendelse(meldingsreferanseId) {

    private var beingQualified = false
    private var nyForrigeTom: LocalDate? = null

    private val beste = { venstre: NyDag, høyre: NyDag ->
        when {
            venstre::class == høyre::class -> venstre
            venstre is NyUkjentDag -> høyre
            høyre is NyUkjentDag -> venstre
            venstre is NyArbeidsgiverdag || venstre is NyArbeidsgiverHelgedag -> venstre
            høyre is NyArbeidsgiverdag || høyre is NyArbeidsgiverHelgedag -> høyre
            venstre is NySykedag -> venstre
            høyre is NySykedag -> høyre
            venstre is NyFeriedag && høyre is NyArbeidsdag -> venstre
            høyre is NyFeriedag && venstre is NyArbeidsdag -> høyre
            venstre is NyFeriedag && høyre is NyFriskHelgedag -> venstre
            høyre is NyFeriedag && venstre is NyFriskHelgedag -> høyre
            else -> høyre.problem(venstre)
        }
    }

    private var nySykdomstidslinje: NySykdomstidslinje = (
        arbeidsgivertidslinje(arbeidsgiverperioder)
            + ferietidslinje(ferieperioder)
            + nyFørsteFraværsdagtidslinje(førsteFraværsdag)
        ).merge(beste)

    init {
        if (arbeidsgiverperioder.isEmpty() && førsteFraværsdag == null) severe("Arbeidsgiverperiode er tom og førsteFraværsdag er null")
    }

    private fun arbeidsgivertidslinje(arbeidsgiverperioder: List<Periode>): List<NySykdomstidslinje> {
        val arbeidsgiverdager = arbeidsgiverperioder.map { it.asArbeidsgivertidslinje() }.merge(beste)

        return listOfNotNull(arbeidsgiverdager, NySykdomstidslinje.arbeidsdager(arbeidsgiverdager.periode(), kilde))
    }

    private fun ferietidslinje(ferieperioder: List<Periode>): List<NySykdomstidslinje> =
        ferieperioder.map { it.asFerietidslinje() }

    private fun nyFørsteFraværsdagtidslinje(førsteFraværsdag: LocalDate?): List<NySykdomstidslinje> =
        listOf(førsteFraværsdag?.let { NySykdomstidslinje.arbeidsgiverdager(it, it, kilde = kilde) } ?: NySykdomstidslinje())

    private fun Periode.asArbeidsgivertidslinje() = NySykdomstidslinje.arbeidsgiverdager(start, endInclusive, kilde = kilde)
    private fun Periode.asFerietidslinje() = NySykdomstidslinje.feriedager(start, endInclusive, kilde)

    override fun nySykdomstidslinje() = nySykdomstidslinje

    internal fun nyTrimLeft(dato: LocalDate) { nyForrigeTom = dato }

    override fun nySykdomstidslinje(tom: LocalDate): NySykdomstidslinje {
        require(nyForrigeTom == null || (nyForrigeTom != null && tom > nyForrigeTom)) { "Kalte metoden flere ganger med samme eller en tidligere dato" }

        return (nyForrigeTom?.let { nySykdomstidslinje.subset(Periode(it.plusDays(1), tom))} ?: nySykdomstidslinje.kutt(tom))
            .also { nyTrimLeft(tom) }
            .also { it.periode() ?: severe("Ugyldig subsetting av tidslinjen til inntektsmeldingen") }
    }

    // Pad days prior to employer-paid days with assumed work days
    override fun nyPadLeft(dato: LocalDate) {
        if (arbeidsgiverperioder.isEmpty()) return  // No justification to pad
        if (dato >= nySykdomstidslinje.førsteDag()) return  // No need to pad if sykdomstidslinje early enough
        nySykdomstidslinje += NySykdomstidslinje.Companion.arbeidsdager(
            dato,
            nySykdomstidslinje.førsteDag().minusDays(1),
            this.kilde
        )
    }

    override fun valider(periode: Periode): Aktivitetslogg {
        refusjon.valider(aktivitetslogg, periode, beregnetInntekt)
        if (arbeidsgiverperioder.isEmpty()) aktivitetslogg.warn("Inntektsmelding inneholder ikke arbeidsgiverperiode")
        if (arbeidsforholdId != null && arbeidsforholdId.isNotBlank()) aktivitetslogg.warn("ArbeidsforholdsID fra inntektsmeldingen er utfylt")
        begrunnelseForReduksjonEllerIkkeUtbetalt?.takeIf(String::isNotBlank)?.also {
            aktivitetslogg.warn(
                "Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: %s", it
            )
        }
        return aktivitetslogg
    }

    override fun aktørId() = aktørId

    override fun fødselsnummer() = fødselsnummer

    override fun organisasjonsnummer() = orgnummer

    override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = arbeidsgiver.håndter(this)

    internal fun addInntekt(inntekthistorikk: Inntekthistorikk) {
        if (førsteFraværsdag == null) return
        inntekthistorikk.add(
            førsteFraværsdag.minusDays(1),  // Assuming salary is the day before the first sykedag
            meldingsreferanseId(),
            beregnetInntekt.toBigDecimal()
        )
    }

    internal fun beingQualified() {
        beingQualified = true
    }

    fun isNotQualified() = !beingQualified

    class Refusjon(
        private val opphørsdato: LocalDate?,
        private val beløpPrMåned: Double?,
        private val endringerIRefusjon: List<LocalDate> = emptyList()
    ) {

        internal fun valider(aktivitetslogg: Aktivitetslogg, periode: Periode, beregnetInntekt: Double): Aktivitetslogg {
            when {
                beløpPrMåned == null -> aktivitetslogg.error("Arbeidsgiver forskutterer ikke (krever ikke refusjon)")
                beløpPrMåned != beregnetInntekt -> aktivitetslogg.error("Inntektsmelding inneholder beregnet inntekt og refusjon som avviker med hverandre")
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
