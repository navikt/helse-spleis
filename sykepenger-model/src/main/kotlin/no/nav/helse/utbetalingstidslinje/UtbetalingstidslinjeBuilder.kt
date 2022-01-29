package no.nav.helse.utbetalingstidslinje

import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilderException.ManglerInntektException
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilderException.NegativDekningsgrunnlagException
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal interface IUtbetalingstidslinjeBuilder : IArbeidsgiverperiodetelling {
    fun result(): Utbetalingstidslinje
}

/**
 *  Forstår opprettelsen av en Utbetalingstidslinje
 */
internal class UtbetalingstidslinjeBuilder internal constructor(
    private val skjæringstidspunkter: List<LocalDate>,
    private val inntektPerSkjæringstidspunkt: Map<LocalDate, Inntektshistorikk.Inntektsopplysning?>?,
    arbeidsgiverRegler: ArbeidsgiverRegler = NormalArbeidstaker,
    private val subsumsjonObserver: SubsumsjonObserver
) : AbstractArbeidsgiverperiodetelling(arbeidsgiverRegler), IUtbetalingstidslinjeBuilder {
    private val tidslinje = Utbetalingstidslinje()
    private var harArbeidsgiverperiode = false

    override fun arbeidsgiverperiodeFerdig(arbeidsgiverperiode: Arbeidsgiverperiode, dagen: LocalDate) {
        harArbeidsgiverperiode = true
    }

    override fun result() = tidslinje

    private fun inntektForDatoOrNull(dato: LocalDate) =
        skjæringstidspunkter
            .sorted()
            .lastOrNull { it <= dato }
            ?.let { skjæringstidspunkt ->
                finnInntekt(skjæringstidspunkt, dato)?.let { inntektsopplysning ->
                    skjæringstidspunkt to inntektsopplysning.grunnlagForSykepengegrunnlag()
                }
            }

    private fun finnInntekt(skjæringstidspunkt: LocalDate, dato: LocalDate) = inntektPerSkjæringstidspunkt?.get(skjæringstidspunkt)
        ?: inntektPerSkjæringstidspunkt?.entries?.firstOrNull { (key) -> key in skjæringstidspunkt..dato }?.value

    private fun inntektForDato(dato: LocalDate) = inntektForDatoOrNull(dato)
        ?: throw ManglerInntektException(dato, skjæringstidspunkter)

    private fun dekningsgrunnlag(inntekt: Inntekt, dagen: LocalDate, skjæringstidspunkt: LocalDate): Inntekt {
        val dekningsgrunnlag = inntekt.dekningsgrunnlag(dagen, regler, subsumsjonObserver)
        if (dekningsgrunnlag < INGEN) throw NegativDekningsgrunnlagException(dekningsgrunnlag, dagen, skjæringstidspunkt)
        return dekningsgrunnlag
    }

    private fun Økonomi.inntektIfNotNull(dato: LocalDate) =
        inntektForDatoOrNull(dato)?.let { (skjæringstidspunkt, inntekt) ->
            inntekt(
                aktuellDagsinntekt = inntekt,
                dekningsgrunnlag = dekningsgrunnlag(inntekt, dato, skjæringstidspunkt),
                skjæringstidspunkt = skjæringstidspunkt
            )
        } ?: this


    override fun sykedagIArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode, dato: LocalDate) {
        addArbeidsgiverdag(dato, arbeidsgiverperiode)
    }
    override fun sykedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi) {
        addNAVdag(dato, arbeidsgiverperiode, økonomi)
    }
    override fun sykHelgedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi) {
        addNAVHelgedag(dato, arbeidsgiverperiode, økonomi)
    }
    override fun foreldetSykedagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate, økonomi: Økonomi) {
        addForeldetDag(dato, arbeidsgiverperiode, økonomi)
    }

    override fun egenmeldingsdagEtterArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate) {
        val økonomi = Økonomi.ikkeBetalt(arbeidsgiverperiode)
        addAvvistDag(dato, økonomi)
    }

    override fun fridagUtenforArbeidsgiverperioden(arbeidsgiverperiode: Arbeidsgiverperiode?, dato: LocalDate) {
        val økonomi = Økonomi.ikkeBetalt(arbeidsgiverperiode)
        addFridag(dato, økonomi)
    }

    override fun arbeidsdag(dato: LocalDate) {
        addArbeidsdag(dato)
    }

    private fun addForeldetDag(dagen: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode?, økonomi: Økonomi) {
        val (skjæringstidspunkt, inntekt) = inntektForDato(dagen)
        tidslinje.addForeldetDag(dagen, økonomi.inntekt(
            aktuellDagsinntekt = inntekt,
            dekningsgrunnlag = dekningsgrunnlag(inntekt, dagen, skjæringstidspunkt),
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiverperiode = arbeidsgiverperiode
        ))
    }

    private fun addArbeidsgiverdag(dato: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode) {
        tidslinje.addArbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt(arbeidsgiverperiode).inntektIfNotNull(dato))
    }

    private fun addNAVdag(dato: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode?, økonomi: Økonomi) {
        if (harArbeidsgiverperiode) {
            subsumsjonObserver.`§8-17 ledd 1 bokstav a`(arbeidsgiverperiode?.toList() ?: emptyList(), førsteNavdag = dato)
            harArbeidsgiverperiode = false
        }

        val (skjæringstidspunkt, inntekt) = inntektForDato(dato)
        tidslinje.addNAVdag(dato, økonomi.inntekt(
            aktuellDagsinntekt = inntekt,
            dekningsgrunnlag = dekningsgrunnlag(inntekt, dato, skjæringstidspunkt),
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiverperiode = arbeidsgiverperiode
        ))
    }

    private fun addNAVHelgedag(dato: LocalDate, arbeidsgiverperiode: Arbeidsgiverperiode?, økonomi: Økonomi) {
        val skjæringstidspunkt = inntektForDatoOrNull(dato)?.let { (skjæringstidspunkt) -> skjæringstidspunkt } ?: dato
        tidslinje.addHelg(dato, økonomi.inntekt(
            aktuellDagsinntekt = INGEN,
            dekningsgrunnlag = INGEN,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiverperiode = arbeidsgiverperiode
        ))
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
