package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_3
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.utbetalingstidslinje.FaktaavklarteInntekter.VilkårsprøvdSkjæringstidspunkt.Companion.finnSkjæringstidspunkt
import no.nav.helse.utbetalingstidslinje.FaktaavklarteInntekter.VilkårsprøvdSkjæringstidspunkt.FaktaavklartInntekt.Companion.finnArbeidsgiver
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi

internal sealed class UtbetalingstidslinjeBuilderException(message: String) : RuntimeException(message) {
    internal fun logg(aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Feilmelding: $message")
        aktivitetslogg.funksjonellFeil(RV_UT_3)
    }

    internal class ProblemdagException(melding: String) : UtbetalingstidslinjeBuilderException(
        "Forventet ikke ProblemDag i utbetalingstidslinjen. Melding: $melding"
    )
}

internal class FaktaavklarteInntekter(
    private val skjæringstidspunkter: List<VilkårsprøvdSkjæringstidspunkt>
) {
    internal fun ghosttidslinje(organisasjonsnummer: String, sisteDag: LocalDate): Sykdomstidslinje {
        return skjæringstidspunkter.mapNotNull { it.ghosttidslinje(organisasjonsnummer, sisteDag) }.merge()
    }

    internal fun medInntekt(
        organisasjonsnummer: String,
        dato: LocalDate,
        økonomi: Økonomi,
        regler: ArbeidsgiverRegler,
        subsumsjonslogg: Subsumsjonslogg
    ): Økonomi {
        val skjæringstidspunkt = skjæringstidspunkter.finnSkjæringstidspunkt(dato)
            ?: return økonomi.inntekt(aktuellDagsinntekt = INGEN, dekningsgrunnlag = INGEN, `6G` = INGEN, refusjonsbeløp = INGEN)

        return skjæringstidspunkt.medInntekt(organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
    }

    internal fun medUtbetalingsopplysninger(
        hendelse: IAktivitetslogg,
        organisasjonsnummer: String,
        dato: LocalDate,
        økonomi: Økonomi,
        regler: ArbeidsgiverRegler,
        subsumsjonslogg: Subsumsjonslogg,
    ): Økonomi {
        val skjæringstidspunkt = skjæringstidspunkter.finnSkjæringstidspunkt(dato)
        if (skjæringstidspunkt == null) {
            hendelse.info("Fant ikke vilkårsgrunnlag for $dato. Må ha et vilkårsgrunnlag for å legge til utbetalingsopplysninger.")
            hendelse.varsel(RV_IV_8)
            return økonomi.inntekt(aktuellDagsinntekt = INGEN, dekningsgrunnlag = INGEN, `6G` = INGEN, refusjonsbeløp = INGEN)
        }
        return skjæringstidspunkt.medUtbetalingsopplysninger(organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
    }

    internal class VilkårsprøvdSkjæringstidspunkt(
        private val skjæringstidspunkt: LocalDate,
        private val vurdertIInfotrygd: Boolean,
        private val `6G`: Inntekt,
        private val inntekter: List<FaktaavklartInntekt>
    ) {
        internal companion object {
            fun List<VilkårsprøvdSkjæringstidspunkt>.finnSkjæringstidspunkt(dato: LocalDate) = this
                .filter { it.skjæringstidspunkt <= dato }
                .maxByOrNull { it.skjæringstidspunkt }
        }

        internal fun ghosttidslinje(organisasjonsnummer: String, sisteDag: LocalDate): Sykdomstidslinje? {
            if (vurdertIInfotrygd) return null
            return inntekter.finnArbeidsgiver(organisasjonsnummer)?.ghosttidslinje(sisteDag)
        }

        internal fun medInntekt(
            organisasjonsnummer: String,
            dato: LocalDate,
            økonomi: Økonomi,
            regler: ArbeidsgiverRegler,
            subsumsjonslogg: Subsumsjonslogg
        ): Økonomi {
            return medInntekt(
                organisasjonsnummer,
                dato,
                økonomi,
                regler,
                subsumsjonslogg,
                inntektFinnesIkkeStrategi = {
                    økonomi.inntekt(aktuellDagsinntekt = INGEN, dekningsgrunnlag = INGEN, `6G` = `6G`, refusjonsbeløp = INGEN)
                },
                refusjonsopplysningFinnesIkkeStrategi = { aktuellDagsinntekt -> aktuellDagsinntekt }
            )
        }

        internal fun medUtbetalingsopplysninger(
            organisasjonsnummer: String,
            dato: LocalDate,
            økonomi: Økonomi,
            regler: ArbeidsgiverRegler,
            subsumsjonslogg: Subsumsjonslogg
        ): Økonomi {
            return medInntekt(
                organisasjonsnummer = organisasjonsnummer,
                dato = dato,
                økonomi = økonomi,
                regler = regler,
                subsumsjonslogg = subsumsjonslogg, inntektFinnesIkkeStrategi = {
                    error("""Arbeidsgiver $organisasjonsnummer mangler i sykepengegrunnlaget ved utbetaling av $dato. 
                Arbeidsgiveren må være i sykepengegrunnlaget for å legge til utbetalingsopplysninger.""")
                },
                refusjonsopplysningFinnesIkkeStrategi = {
                    error("Har ingen refusjonsopplysninger på vilkårsgrunnlag for utbetalingsdag $dato")
                }
            )
        }

        private fun medInntekt(
            organisasjonsnummer: String,
            dato: LocalDate,
            økonomi: Økonomi,
            regler: ArbeidsgiverRegler,
            subsumsjonslogg: Subsumsjonslogg,
            inntektFinnesIkkeStrategi: () -> Økonomi,
            refusjonsopplysningFinnesIkkeStrategi: (Inntekt) -> Inntekt
        ): Økonomi {
            val faktaavklartInntekt = inntekter.finnArbeidsgiver(organisasjonsnummer) ?: return inntektFinnesIkkeStrategi()
            return faktaavklartInntekt.medInntekt(skjæringstidspunkt, dato, økonomi, `6G`, regler, subsumsjonslogg, refusjonsopplysningFinnesIkkeStrategi)
        }

        internal class FaktaavklartInntekt(
            private val organisasjonsnummer: String,
            private val fastsattÅrsinntekt: Inntekt,
            private val gjelder: Periode,
            private val refusjonsopplysninger: Refusjonsopplysning.Refusjonsopplysninger
        ) {
            internal companion object {
                fun List<FaktaavklartInntekt>.finnArbeidsgiver(organisasjonsnummer: String) = this
                    .singleOrNull { it.organisasjonsnummer == organisasjonsnummer }
            }

            internal fun ghosttidslinje(sisteDag: LocalDate): Sykdomstidslinje {
                if (sisteDag < gjelder.start) return Sykdomstidslinje()
                return Sykdomstidslinje.ghostdager(gjelder.start til sisteDag)
            }

            private fun fastsattÅrsinntekt(dagen: LocalDate): Inntekt {
                if (dagen > gjelder.endInclusive) return INGEN
                return fastsattÅrsinntekt
            }

            private fun beregningsgrunnlag(skjæringstidspunkt: LocalDate): Inntekt {
                if (!gjelderPåSkjæringstidspunktet(skjæringstidspunkt)) return INGEN
                return fastsattÅrsinntekt
            }
            private fun gjelderPåSkjæringstidspunktet(skjæringstidspunkt: LocalDate) = skjæringstidspunkt == gjelder.start

            internal fun medInntekt(skjæringstidspunkt: LocalDate, dato: LocalDate, økonomi: Økonomi, `6G`: Inntekt, regler: ArbeidsgiverRegler, subsumsjonslogg: Subsumsjonslogg, refusjonsopplysningFinnesIkkeStrategi: (Inntekt) -> Inntekt): Økonomi {
                val aktuellDagsinntekt = fastsattÅrsinntekt(dato)
                val refusjonsbeløp = refusjonsopplysninger.refusjonsbeløpOrNull(dato) ?: refusjonsopplysningFinnesIkkeStrategi(aktuellDagsinntekt)
                return økonomi.inntekt(
                    aktuellDagsinntekt = aktuellDagsinntekt,
                    beregningsgrunnlag = beregningsgrunnlag(skjæringstidspunkt),
                    dekningsgrunnlag = aktuellDagsinntekt.dekningsgrunnlag(dato, regler, subsumsjonslogg),
                    `6G` = `6G`,
                    refusjonsbeløp = refusjonsbeløp
                )
            }
        }
    }
}

internal data class ArbeidsgiverperiodeForVedtaksperiode(
    val vedtaksperiode: Periode,
    val arbeidsgiverperioder: List<Periode>
)

/**
 * val sykdomstidslinje = ...
 * val builder = UtbetalingstidslinjeBuilderNy(..)
 * sykdomstidslinje.accept(builder)
 *
 * val result = builder.result()
 */
internal class UtbetalingstidslinjeBuilderNy(
    private val faktaavklarteInntekter: FaktaavklarteInntekter,
    private val hendelse: IAktivitetslogg,
    private val organisasjonsnummer: String,
    private val regler: ArbeidsgiverRegler,
    private val subsumsjonslogg: Subsumsjonslogg,
    private val beregningsperiode: Periode,
    private val arbeidsgiverperioder: List<ArbeidsgiverperiodeForVedtaksperiode>,
    private val utbetaltePerioderInfotrygd: List<Periode>
) : SykdomstidslinjeVisitor {
    private val builder = Utbetalingstidslinje.Builder()

    internal fun result(): Utbetalingstidslinje {
        return builder.build()
    }

    private fun erAGP(dato: LocalDate) = arbeidsgiverperioder.any { vedtaksperiode ->
        vedtaksperiode.arbeidsgiverperioder.any { dato in it }
    }
    private fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
        builder.addArbeidsgiverperiodedag(dato, faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }
    private fun avvistDag(dato: LocalDate, økonomi: Økonomi, begrunnelse: Begrunnelse) {
        builder.addAvvistDag(dato, faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg), listOf(begrunnelse))
    }
    private fun helg(dato: LocalDate, økonomi: Økonomi) {
        builder.addHelg(dato, faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }
    private fun navDag(dato: LocalDate, økonomi: Økonomi) {
        builder.addNAVdag(dato, when (dato in beregningsperiode) {
            true -> faktaavklarteInntekter.medUtbetalingsopplysninger(hendelse, organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
            false -> faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
        })
    }
    private fun fridag(dato: LocalDate) {
        builder.addFridag(dato, faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, Økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }
    private fun arbeidsdag(dato: LocalDate) {
        builder.addArbeidsdag(dato, faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, Økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }

    /** <potensielt arbeidsgiverperiode-dager> **/
    override fun visitDag(dag: Dag.Arbeidsgiverdag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, økonomi)
        else avvistDag(dato, økonomi.ikkeBetalt(), Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode)
    }

    override fun visitDag(dag: Dag.ArbeidsgiverHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, økonomi)
        else helg(dato, økonomi)
    }

    override fun visitDag(dag: Dag.Sykedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, økonomi)
        else navDag(dato, økonomi)
    }

    override fun visitDag(dag: Dag.SykHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, økonomi)
        else helg(dato, økonomi)
    }

    override fun visitDag(dag: Dag.SykedagNav, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (erAGP(dato)) builder.addArbeidsgiverperiodedagNav(dato, when (dato in beregningsperiode) {
            true -> faktaavklarteInntekter.medUtbetalingsopplysninger(hendelse, organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
            false -> faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
        })
        else when (dato.erHelg()) {
            true -> helg(dato, økonomi)
            false -> navDag(dato, økonomi)
        }
    }

    override fun visitDag(dag: Dag.AndreYtelser, dato: LocalDate, kilde: Hendelseskilde, ytelse: Dag.AndreYtelser.AnnenYtelse) {
        // andreytelse-dagen er fridag hvis den overlapper med en agp-dag, eller om vedtaksperioden ikke har noen agp -- fordi andre ytelsen spiser opp alt
        val vedtaksperiode = arbeidsgiverperioder.single { dato in it.vedtaksperiode }
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt())
        else if (vedtaksperiode.arbeidsgiverperioder.isEmpty() || dato < vedtaksperiode.arbeidsgiverperioder.first().start) fridag(dato)
        else {
            val begrunnelse = when(ytelse) {
                Dag.AndreYtelser.AnnenYtelse.AAP -> Begrunnelse.AndreYtelserAap
                Dag.AndreYtelser.AnnenYtelse.Dagpenger -> Begrunnelse.AndreYtelserDagpenger
                Dag.AndreYtelser.AnnenYtelse.Foreldrepenger -> Begrunnelse.AndreYtelserForeldrepenger
                Dag.AndreYtelser.AnnenYtelse.Omsorgspenger -> Begrunnelse.AndreYtelserOmsorgspenger
                Dag.AndreYtelser.AnnenYtelse.Opplæringspenger -> Begrunnelse.AndreYtelserOpplaringspenger
                Dag.AndreYtelser.AnnenYtelse.Pleiepenger -> Begrunnelse.AndreYtelserPleiepenger
                Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger -> Begrunnelse.AndreYtelserSvangerskapspenger
            }
            avvistDag(dato, Økonomi.ikkeBetalt(), begrunnelse)

        }
    }

    override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt())
        else fridag(dato)
    }

    override fun visitDag(dag: Dag.ForeldetSykedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, økonomi)
        else builder.addForeldetDag(dato, faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg))
    }

    override fun visitDag(dag: Dag.ArbeidIkkeGjenopptattDag, dato: LocalDate, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt())
        else fridag(dato)
    }

    override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt())
        else fridag(dato)
    }
    /** </potensielt arbeidsgiverperiode-dager> **/

    override fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: Hendelseskilde) {
        if (utbetaltePerioderInfotrygd.any { dato in it }) return builder.addUkjentDag(dato)
        when (dato.erHelg()) {
            true -> fridag(dato)
            false -> arbeidsdag(dato)
        }
    }

    override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) {
        arbeidsdag(dato)
    }

    override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) {
        arbeidsdag(dato)
    }

    override fun visitDag(dag: Dag.ProblemDag, dato: LocalDate, kilde: Hendelseskilde, other: Hendelseskilde?, melding: String) {
        // den andre builderen kaster egentlig exception her, men trenger vi det –– sånn egentlig?
        fridag(dato)
    }
}
