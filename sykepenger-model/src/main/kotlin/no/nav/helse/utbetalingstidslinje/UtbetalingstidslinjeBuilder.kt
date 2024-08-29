package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_3
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
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

internal class UtbetalingstidslinjeBuilder(
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
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
        builder.addFridag(dato, vilkårsgrunnlagHistorikk.medInntekt(organisasjonsnummer, dato, Økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }

    override fun fridagOppholdsdag(dato: LocalDate) {
        builder.addFridag(dato, vilkårsgrunnlagHistorikk.medInntekt(organisasjonsnummer, dato, Økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }

    override fun arbeidsdag(dato: LocalDate) {
        builder.addArbeidsdag(dato, vilkårsgrunnlagHistorikk.medInntekt(organisasjonsnummer, dato, Økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }

    override fun arbeidsgiverperiodedag(
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
        builder.addArbeidsgiverperiodedag(dato, vilkårsgrunnlagHistorikk.medInntekt(organisasjonsnummer, dato, økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }

    override fun arbeidsgiverperiodedagNav(
        dato: LocalDate,
        økonomi: Økonomi,
        kilde: Hendelseskilde
    ) {
        val medUtbetalingsopplysninger = when (dato in beregningsperiode) {
            true -> vilkårsgrunnlagHistorikk.medUtbetalingsopplysninger(hendelse, organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
            false -> vilkårsgrunnlagHistorikk.medInntekt(organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
        }
        builder.addArbeidsgiverperiodedagNav(dato, medUtbetalingsopplysninger)
    }

    override fun ukjentDag(dato: LocalDate) {
        builder.addUkjentDag(dato)
    }

    override fun utbetalingsdag(dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (dato.erHelg()) return builder.addHelg(dato, vilkårsgrunnlagHistorikk.medInntekt(organisasjonsnummer, dato, økonomi.ikkeBetalt(), regler, subsumsjonslogg))
        val medUtbetalingsopplysninger = when (dato in beregningsperiode) {
            true -> vilkårsgrunnlagHistorikk.medUtbetalingsopplysninger(hendelse, organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
            false -> vilkårsgrunnlagHistorikk.medInntekt(organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg)
        }
        builder.addNAVdag(dato, medUtbetalingsopplysninger)
    }

    override fun foreldetDag(dato: LocalDate, økonomi: Økonomi) {
        builder.addForeldetDag(dato, vilkårsgrunnlagHistorikk.medInntekt(organisasjonsnummer, dato, økonomi, regler, subsumsjonslogg))
    }

    override fun avvistDag(dato: LocalDate, begrunnelse: Begrunnelse, økonomi: Økonomi) {
        builder.addAvvistDag(dato, vilkårsgrunnlagHistorikk.medInntekt(organisasjonsnummer, dato, økonomi.ikkeBetalt(), regler, subsumsjonslogg), listOf(begrunnelse))
    }
}
