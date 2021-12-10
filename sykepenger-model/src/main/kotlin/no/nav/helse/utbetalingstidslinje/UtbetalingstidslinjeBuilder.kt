package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-17 ledd 1 bokstav a`
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-17 ledd 2`
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiodestrategi.Default
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilderException.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal fun interface IUtbetalingstidslinjeBuilder {
    fun result(sykdomstidslinje: Sykdomstidslinje, periode: Periode): Utbetalingstidslinje
}

/**
 *  Forstår opprettelsen av en Utbetalingstidslinje
 */

internal class UtbetalingstidslinjeBuilder internal constructor(
    private val skjæringstidspunkter: List<LocalDate>,
    private val inntektPerSkjæringstidspunkt: Map<LocalDate, Inntektshistorikk.Inntektsopplysning?>?,
    private val arbeidsgiverRegler: ArbeidsgiverRegler = NormalArbeidstaker,
    private val aktivitetslogg: IAktivitetslogg
) : SykdomstidslinjeVisitor, IUtbetalingstidslinjeBuilder, Arbeidsgiverperiodeteller.Observatør {
    private lateinit var teller: Arbeidsgiverperiodeteller
    private val tidslinje = Utbetalingstidslinje()

    init {
        teller(Forlengelsestrategi.Ingen)
    }

    private var harArbeidsgiverperiode = false

    override fun arbeidsgiverperiodeFerdig(arbeidsgiverperiode: Arbeidsgiverperiode, dagen: LocalDate) {
        harArbeidsgiverperiode = true
    }

    internal fun forlengelsestrategi(strategi: Forlengelsestrategi) {
        teller(strategi)
    }

    override fun result(sykdomstidslinje: Sykdomstidslinje, periode: Periode): Utbetalingstidslinje {
        sykdomstidslinje.fremTilOgMed(periode.endInclusive).accept(this)
        teller.avslutt()
        return tidslinje
    }

    private fun teller(strategi: Forlengelsestrategi) {
        this.teller = Arbeidsgiverperiodeteller(arbeidsgiverRegler, strategi).also {
            it.observatør(this)
        }
    }

    private fun inntektForDatoOrNull(dato: LocalDate) =
        skjæringstidspunkter
            .sorted()
            .lastOrNull { it <= dato }
            ?.let { skjæringstidspunkt ->
                finnInntekt(skjæringstidspunkt, dato)?.let { inntektsopplysning ->
                    skjæringstidspunkt to inntektsopplysning.grunnlagForSykepengegrunnlag()
                }
            }

    private fun finnInntekt(skjæringstidspunkt: LocalDate, dato: LocalDate): Inntektshistorikk.Inntektsopplysning? {
        return inntektPerSkjæringstidspunkt?.get(skjæringstidspunkt)
            ?: inntektPerSkjæringstidspunkt?.entries?.firstOrNull { (key) -> key in skjæringstidspunkt..dato }?.value
    }

    private fun inntektForDato(dato: LocalDate) =
        inntektForDatoOrNull(dato) ?: throw ManglerInntektException(dato, skjæringstidspunkter)

    private fun dekningsgrunnlag(inntekt: Inntekt, dagen: LocalDate, skjæringstidspunkt: LocalDate): Inntekt {
        val dekningsgrunnlag = inntekt.dekningsgrunnlag(arbeidsgiverRegler)
        if (dekningsgrunnlag < INGEN) {
            throw NegativDekningsgrunnlagException(dekningsgrunnlag, dagen, skjæringstidspunkt)
        }
        return dekningsgrunnlag
    }

    private fun Økonomi.inntektIfNotNull(dato: LocalDate) =
        inntektForDatoOrNull(dato)
            ?.let { (skjæringstidspunkt, inntekt) ->
                inntekt(
                    aktuellDagsinntekt = inntekt,
                    dekningsgrunnlag = dekningsgrunnlag(inntekt, dato, skjæringstidspunkt),
                    skjæringstidspunkt = skjæringstidspunkt
                )
            }
            ?: inntekt(aktuellDagsinntekt = INGEN, dekningsgrunnlag = INGEN, skjæringstidspunkt = dato)


    private fun sykedagIArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode, dato: LocalDate) {
        addArbeidsgiverdag(dato, arbeidsgiverperiode)
    }
    private fun sykedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi) {
        addNAVdag(dato, arbeidsgiverperiode, økonomi.arbeidsgiverperiode(arbeidsgiverperiode))
    }
    private fun sykHelgedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi) {
        addNAVHelgedag(dato, økonomi.arbeidsgiverperiode(arbeidsgiverperiode))
    }
    private fun foreldetSykedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi) {
        addForeldetDag(dato, økonomi.arbeidsgiverperiode(arbeidsgiverperiode))
    }

    private fun egenmeldingsdagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate) {
        addAvvistDag(dato, Økonomi.ikkeBetalt(arbeidsgiverperiode))
    }

    private fun fridagIArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode, dato: LocalDate) = sykedagIArbeidsgiverperioden(arbeidsgiverperiode, dato)

    private fun fridagUtenforArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate) {
        addFridag(dato, Økonomi.ikkeBetalt(arbeidsgiverperiode))
    }

    private fun arbeidsdag(dato: LocalDate) {
        addArbeidsdag(dato)
    }

    private fun sykedag(dato: LocalDate, økonomi: Økonomi) {
        teller.inkrementer(dato, Default({ sykedagIArbeidsgiverperioden(it, dato) }, { sykedagEtterArbeidsgiverperioden(it, dato, økonomi) }))
    }

    private fun sykHelgedag(dato: LocalDate, økonomi: Økonomi) {
        teller.inkrementer(dato, Default ({ sykedagIArbeidsgiverperioden(it, dato) }, { sykHelgedagEtterArbeidsgiverperioden(it, dato, økonomi) }))
    }

    private fun foreldetSykedag(dato: LocalDate, økonomi: Økonomi) {
        teller.inkrementer(dato, Default ({ sykedagIArbeidsgiverperioden(it, dato) }, { foreldetSykedagEtterArbeidsgiverperioden(it, dato, økonomi) }))
    }

    private fun fridag(dato: LocalDate) {
        teller.inkrementEllerDekrement(dato, Default ({ fridagIArbeidsgiverperioden(it, dato) }, { fridagUtenforArbeidsgiverperioden(it, dato) }))
    }

    private fun egenmeldingsdag(dato: LocalDate) {
        teller.inkrementer(dato, Default ({ sykedagIArbeidsgiverperioden(it, dato) }, { egenmeldingsdagEtterArbeidsgiverperioden(it, dato) }))
    }

    final override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        teller.dekrementer(dato)
        arbeidsdag(dato)
    }

    final override fun visitDag(
        dag: Dag.Arbeidsgiverdag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        egenmeldingsdag(dato)
    }

    final override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        teller.dekrementer(dato)
        arbeidsdag(dato)
    }

    final override fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        if (dato.erHelg()) return fridag(dato)
        teller.dekrementer(dato)
        arbeidsdag(dato)
    }

    final override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        // TODO: Bør flyttes dit hvor beslutningen om å ikke utbetale pga. fridag tas, når denne er gjort: https://trello.com/c/Wffztv11
        Aktivitetslogg().`§8-17 ledd 2`(true)
        fridag(dato)
    }

    final override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        // TODO: Bør flyttes dit hvor beslutningen om å ikke utbetale pga. fridag tas, når denne er gjort: https://trello.com/c/Wffztv11
        Aktivitetslogg().`§8-17 ledd 2`(true)
        fridag(dato)
    }

    final override fun visitDag(dag: Dag.AvslåttDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
        fridag(dato)
    }

    final override fun visitDag(
        dag: Dag.ArbeidsgiverHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        sykHelgedag(dato, økonomi)
    }

    final override fun visitDag(
        dag: Dag.Sykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        sykedag(dato, økonomi)
    }

    final override fun visitDag(
        dag: Dag.SykHelgedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        sykHelgedag(dato, økonomi)
    }

    final override fun visitDag(
        dag: Dag.ForeldetSykedag,
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) {
        foreldetSykedag(dato, økonomi)
    }

    final override fun visitDag(
        dag: Dag.ProblemDag,
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde,
        melding: String
    ) = throw UforventetDagException(dag, melding)

    private fun addForeldetDag(dagen: LocalDate, økonomi: Økonomi) {
        val (skjæringstidspunkt, inntekt) = inntektForDato(dagen)
        tidslinje.addForeldetDag(
            dagen, økonomi.inntekt(
                aktuellDagsinntekt = inntekt,
                dekningsgrunnlag = dekningsgrunnlag(inntekt, dagen, skjæringstidspunkt),
                skjæringstidspunkt = skjæringstidspunkt
            )
        )
    }

    private fun addArbeidsgiverdag(dato: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode) {
        tidslinje.addArbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt(arbeidsgiverperiode).inntektIfNotNull(dato))
    }

    private fun addNAVdag(dato: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode?, økonomi: Økonomi) {
        if (harArbeidsgiverperiode) {
            aktivitetslogg.`§8-17 ledd 1 bokstav a`(true, arbeidsgiverperiode?.toList() ?: emptyList(), førsteNavdag = dato)
            harArbeidsgiverperiode = false
        }

        val (skjæringstidspunkt, inntekt) = inntektForDato(dato)
        tidslinje.addNAVdag(
            dato,
            økonomi.inntekt(
                aktuellDagsinntekt = inntekt,
                dekningsgrunnlag = dekningsgrunnlag(inntekt, dato, skjæringstidspunkt),
                skjæringstidspunkt = skjæringstidspunkt
            )
        )
    }

    private fun addNAVHelgedag(dato: LocalDate, økonomi: Økonomi) {
        val skjæringstidspunkt = inntektForDatoOrNull(dato)?.let { (skjæringstidspunkt) -> skjæringstidspunkt } ?: dato
        tidslinje.addHelg(
            dato,
            økonomi.inntekt(aktuellDagsinntekt = INGEN, dekningsgrunnlag = INGEN, skjæringstidspunkt = skjæringstidspunkt)
        )
    }

    private fun addArbeidsdag(dato: LocalDate) {
        tidslinje.addArbeidsdag(dato, Økonomi.ikkeBetalt().inntektIfNotNull(dato))
    }

    private fun addAvvistDag(dato: LocalDate, økonomi: Økonomi) {
        tidslinje.addAvvistDag(dato, økonomi.inntektIfNotNull(dato), listOf(Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode))
    }

    private fun addFridag(dato: LocalDate, økonomi: Økonomi) {
        tidslinje.addFridag(dato, økonomi.inntektIfNotNull(dato))
    }
}

internal sealed class UtbetalingstidslinjeBuilderException(private val kort: String, message: String) : RuntimeException(message) {
    internal fun logg(aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Feilmelding: $message")
        aktivitetslogg.error("Feil ved utbetalingstidslinjebygging: $kort")
    }

    internal class ManglerInntektException(dagen: LocalDate, skjæringstidspunkter: List<LocalDate>) : UtbetalingstidslinjeBuilderException(
        "Mangler inntekt for dag",
        "Fant ikke inntekt for $dagen med skjæringstidspunkter $skjæringstidspunkter"
    )

    internal class UforventetDagException(dag: Dag, melding: String) : UtbetalingstidslinjeBuilderException(
        "Forventet ikke ${dag::class.simpleName}",
        "Forventet ikke ${dag::class.simpleName} i utbetalingstidslinjen. Melding: $melding"
    )

    internal class NegativDekningsgrunnlagException(dekningsgrunnlag: Inntekt, dagen: LocalDate, skjæringstidspunkt: LocalDate) : UtbetalingstidslinjeBuilderException(
        "Dekningsgrunnlag er negativ",
        "Dekningsgrunnlag for $dagen med skjæringstidspunkt $skjæringstidspunkt gir negativt beløp: $dekningsgrunnlag"
    )
}
