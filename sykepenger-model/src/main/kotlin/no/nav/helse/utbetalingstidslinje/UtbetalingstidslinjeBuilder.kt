package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.til
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_3
import no.nav.helse.person.inntekt.Refusjonsopplysning
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
        private val `6G`: Inntekt,
        private val inntekter: List<FaktaavklartInntekt>
    ) {
        internal companion object {
            fun List<VilkårsprøvdSkjæringstidspunkt>.finnSkjæringstidspunkt(dato: LocalDate) = this
                .filter { it.skjæringstidspunkt <= dato }
                .maxByOrNull { it.skjæringstidspunkt }
        }

        internal fun ghosttidslinje(organisasjonsnummer: String, sisteDag: LocalDate): Sykdomstidslinje? {
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

internal class UtbetalingstidslinjeBuilder(
    private val faktaavklarteInntekter: FaktaavklarteInntekter,
    private val hendelse: IAktivitetslogg,
    private val organisasjonsnummer: String,
    private val regler: ArbeidsgiverRegler,
    private val subsumsjonslogg: Subsumsjonslogg,
    private val beregningsperiode: Periode
) : ArbeidsgiverperiodeMediator {
    private val builder = Utbetalingstidslinje.Builder()
    private val kildeSykmelding = mutableSetOf<LocalDate>()

    internal fun result(): Utbetalingstidslinje {
        check(kildeSykmelding.isEmpty()) { "Kan ikke opprette utbetalingsdager med kilde Sykmelding: ${kildeSykmelding.grupperSammenhengendePerioder()}" }
        return builder.build()
    }

    override fun fridag(dato: LocalDate) {
        builder.addFridag(dato, faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, Økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }

    override fun fridagOppholdsdag(dato: LocalDate) {
        builder.addFridag(dato, faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, Økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }

    override fun arbeidsdag(dato: LocalDate) {
        builder.addArbeidsdag(dato, faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, Økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }

    override fun arbeidsgiverperiodedag(
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
        builder.addArbeidsgiverperiodedag(dato, faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }

    override fun arbeidsgiverperiodedagNav(
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
        val medUtbetalingsopplysninger = when (dato in beregningsperiode) {
            true -> faktaavklarteInntekter.medUtbetalingsopplysninger(hendelse, organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
            false -> faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
        }
        builder.addArbeidsgiverperiodedagNav(dato, medUtbetalingsopplysninger)
    }

    override fun ukjentDag(dato: LocalDate) {
        builder.addUkjentDag(dato)
    }

    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (dato.erHelg()) return builder.addHelg(dato, faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, økonomi.ikkeBetalt(), regler, subsumsjonslogg))
        val medUtbetalingsopplysninger = when (dato in beregningsperiode) {
            true -> faktaavklarteInntekter.medUtbetalingsopplysninger(hendelse, organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
            false -> faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
        }
        builder.addNAVdag(dato, medUtbetalingsopplysninger)
    }

    override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {
        builder.addForeldetDag(dato, faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg))
    }

    override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse, økonomi: Økonomi) {
        builder.addAvvistDag(dato, faktaavklarteInntekter.medInntekt(organisasjonsnummer, dato, økonomi.ikkeBetalt(), regler, subsumsjonslogg), listOf(begrunnelse))
    }
}
