package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.nesteDag
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi

internal data class InntekterForBeregning(
    private val inntekterPerInntektskilde: Map<String, Inntekter>,
    private val beregningsperiode: Periode,
    val `6G`: Inntekt
) {

    internal fun tilBeregning(organisasjonsnummer: String) = inntekterPerInntektskilde.getValue(organisasjonsnummer).let { it.fastsattÅrsinntekt to it.inntektstidslinje }

    internal fun hensyntattAlleInntektskilder(beregnedeUtbetalingstidslinjer: Map<String, List<Utbetalingstidslinje>>): Map<String, Utbetalingstidslinje> {
        return inntekterPerInntektskilde.mapValues { (inntektskilde, inntekter) ->
            val beregnedeUtbetalingstidslinjerForInntektskilde = beregnedeUtbetalingstidslinjer[inntektskilde] ?: emptyList()
            val beregendePerioderForInntektskilde = beregnedeUtbetalingstidslinjerForInntektskilde.map(Utbetalingstidslinje::periode)
            val uberegnedeDagerForArbeidsgiver = beregningsperiode.filterNot { dato -> beregendePerioderForInntektskilde.any { beregnetPeriode -> dato in beregnetPeriode} }
            val uberegnetUtbetalingstidslinjeForArbeidsgiver = inntekter.uberegnetUtbetalingstidslinje(uberegnedeDagerForArbeidsgiver, `6G`)
            beregnedeUtbetalingstidslinjerForInntektskilde.fold(uberegnetUtbetalingstidslinjeForArbeidsgiver, Utbetalingstidslinje::plus)
        }.filterValues { it.isNotEmpty() }
    }

    internal sealed interface Inntekter {
        val fastsattÅrsinntekt: Inntekt
        val inntektstidslinje: Beløpstidslinje
        fun inntektsendring(skjæringstidspunkt: LocalDate, inntektsendringer: Beløpstidslinje): Inntekter
        fun uberegnetUtbetalingstidslinje(uberegnedeDager: List<LocalDate>, `6G`: Inntekt): Utbetalingstidslinje
        fun arbeidsdager(dager: List<LocalDate>, `6G`: Inntekt) = with(Utbetalingstidslinje.Builder()) {
            dager.forEach { dato ->
                if (dato.erHelg()) addFridag(dato, Økonomi.ikkeBetalt())
                else addArbeidsdag(
                    dato = dato,
                    økonomi = Økonomi.ikkeBetalt().inntekt(
                        aktuellDagsinntekt = inntektstidslinje[dato].beløp,
                        beregningsgrunnlag = fastsattÅrsinntekt,
                        dekningsgrunnlag = INGEN,
                        `6G` = `6G`,
                        refusjonsbeløp = INGEN
                    )
                )
            }
            build()
        }

        data class FraInntektsgrunnlag(override val fastsattÅrsinntekt: Inntekt, override val inntektstidslinje: Beløpstidslinje): Inntekter {
            override fun inntektsendring(skjæringstidspunkt: LocalDate, inntektsendringer: Beløpstidslinje) = copy(
                // For arbeidsgiver i inntektsgrunnlaget kan ikke inntekten på skjæringstidspunktet endres av en inntektsendring.
                inntektstidslinje = this.inntektstidslinje + inntektsendringer.fraOgMed(skjæringstidspunkt.nesteDag)
            )
            override fun uberegnetUtbetalingstidslinje(uberegnedeDager: List<LocalDate>, `6G`: Inntekt): Utbetalingstidslinje {
                // Lager Utbetalingstidslinje for alle arbeidsgivere fra inntektsgrunnlaget
                // De som ikke har noen beregnede utbetalingstidslinjer er ghost for hele beregningsperioden
                return arbeidsdager(uberegnedeDager, `6G`)
            }
        }

        data class DeaktivertFraInntektsgrunnlag(override val inntektstidslinje: Beløpstidslinje): Inntekter {
            override val fastsattÅrsinntekt = INGEN

            override fun inntektsendring(skjæringstidspunkt: LocalDate, inntektsendringer: Beløpstidslinje) = copy(
                // For arbeidsgiver i inntektsgrunnlaget kan ikke inntekten på skjæringstidspunktet endres av en inntektsendring.
                // Inntektsendring for en deaktivert arbeidsgiver høres spennende ut. Men det kan vel sikkert skje? Hen var i sykepengegrunnlaget men var reelt tilkommen f.eks. ?
                inntektstidslinje = this.inntektstidslinje + inntektsendringer.fraOgMed(skjæringstidspunkt.nesteDag)
            )

            override fun uberegnetUtbetalingstidslinje(uberegnedeDager: List<LocalDate>, `6G`: Inntekt): Utbetalingstidslinje {
                // En deaktivert arbeidsgiver kan være brukt til beregning (der hvor saksbehandler i dag overstyrer/skjønnsmessig fastsetter til 0,- med burde deaktivert hen)
                // Legger kun til grunn uberegnede dager med inntektsendring
                val uberegnedeDagerMedInntektsendring = uberegnedeDager.filter { inntektstidslinje[it].beløp != INGEN }
                return arbeidsdager(uberegnedeDagerMedInntektsendring, `6G`)
            }
        }

        data class NyInntektskilde(override val inntektstidslinje: Beløpstidslinje = Beløpstidslinje()) : Inntekter {
            override val fastsattÅrsinntekt = INGEN
            override fun inntektsendring(skjæringstidspunkt: LocalDate, inntektsendringer: Beløpstidslinje) = copy(
                inntektstidslinje = this.inntektstidslinje + inntektsendringer
            )

            override fun uberegnetUtbetalingstidslinje(uberegnedeDager: List<LocalDate>, `6G`: Inntekt): Utbetalingstidslinje {
                // For en ny inntektskilde må vi ta utgangspunkt i inntektstidslinjen
                val dager = inntektstidslinje.filterIsInstance<Beløpsdag>().filter { it.dato in uberegnedeDager }.map(Beløpsdag::dato)
                return arbeidsdager(dager, `6G`)
            }
        }
    }

    internal class Builder(private val beregningsperiode: Periode, private val skjæringstidspunkt: LocalDate) {
        private val inntekterPerInntektskilde = mutableMapOf<String, Inntekter>()

        internal fun fraInntektsgrunnlag(organisasjonsnummer: String, fastsattÅrsinntekt: Inntekt, opplysningskilde: Kilde) {
            check(organisasjonsnummer !in inntekterPerInntektskilde) { "Organisasjonsnummer er allerede lagt til som ${inntekterPerInntektskilde.getValue(organisasjonsnummer)::class.simpleName}" }
            inntekterPerInntektskilde[organisasjonsnummer] = Inntekter.FraInntektsgrunnlag(
                fastsattÅrsinntekt = fastsattÅrsinntekt,
                inntektstidslinje = Beløpstidslinje.fra(beregningsperiode, fastsattÅrsinntekt, opplysningskilde)
            )
        }

        internal fun deaktivertFraInntektsgrunnlag(organisasjonsnummer: String, opplysningskilde: Kilde) {
            check(organisasjonsnummer !in inntekterPerInntektskilde) { "Organisasjonsnummer er allerede lagt til som ${inntekterPerInntektskilde.getValue(organisasjonsnummer)::class.simpleName}" }
            inntekterPerInntektskilde[organisasjonsnummer] = Inntekter.DeaktivertFraInntektsgrunnlag(
                inntektstidslinje = Beløpstidslinje.fra(beregningsperiode, INGEN, opplysningskilde)
            )
        }

        internal fun inntektsendringer(inntektskilde: String, inntektsendring: Beløpstidslinje) {
            // Om vi ikke kjenner til inntektskilden fra før kan vi være diverse snax. Derunder "tilkommen inntekt"
            if (inntektskilde !in inntekterPerInntektskilde) {
                inntekterPerInntektskilde[inntektskilde] = Inntekter.NyInntektskilde()
            }
            // Litt avhengig av inntekten som er lagt til grunn håndterer vi innteksendringer litt forskjellig
            inntekterPerInntektskilde[inntektskilde] = inntekterPerInntektskilde
                .getValue(inntektskilde)
                .inntektsendring(skjæringstidspunkt, inntektsendring.subset(beregningsperiode))
        }

        private lateinit var `6G`: Inntekt

        internal fun medGjeldende6G(gjeldende6G: Inntekt) {
            this.`6G` = gjeldende6G
        }

        internal fun build() = InntekterForBeregning(
            inntekterPerInntektskilde = inntekterPerInntektskilde.toMap(),
            beregningsperiode = beregningsperiode,
            `6G` = `6G`
        )
    }

    internal companion object {
        private val TøyseteMeldingsreferanseId = MeldingsreferanseId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
        private val TøyseteTidsstempel = LocalDate.EPOCH.atStartOfDay()
        private val TøyseteOpplysningskilde = Kilde(TøyseteMeldingsreferanseId, SYSTEM, TøyseteTidsstempel)
        private fun tøyseteAuu(periode: Periode) = INGEN to Beløpstidslinje.fra(periode, INGEN, TøyseteOpplysningskilde)

        fun forAuu(periode: Periode, skjæringstidspunkt: LocalDate, organisasjonsnummer: String, inntektsgrunnlag: Inntektsgrunnlag?): Pair<Inntekt, Beløpstidslinje> {
            if (inntektsgrunnlag == null) return tøyseteAuu(periode)
            return with(Builder(periode, skjæringstidspunkt)) {
                inntektsgrunnlag.beverte(this)
                build()
            }.tilBeregning(organisasjonsnummer)
        }

        fun Map<String, Utbetalingstidslinje>.checkLik(andreBeregning: Map<String, Utbetalingstidslinje>) {
            check(keys == andreBeregning.keys) { "Arbeidsgiverne er forskjellig. Var: $keys, nå: ${andreBeregning.keys}" }
            forEach { (arbeidsgiver, denneUtbetalingstidslinjen) ->
                val andreUtbetalingstidslinjen = andreBeregning.getValue(arbeidsgiver)
                check(denneUtbetalingstidslinjen.size == andreUtbetalingstidslinjen.size) { "Antall dager i utbetalingstidslinjen er forskjellig. Var: ${denneUtbetalingstidslinjen.size}, nå: ${andreUtbetalingstidslinjen.size}" }
                denneUtbetalingstidslinjen.forEach { denneUtbetalingsdagen ->
                    val andreUtbetalingsdagen = andreUtbetalingstidslinjen[denneUtbetalingsdagen.dato].takeUnless { it is Utbetalingsdag.UkjentDag } ?: error("Fant ikke dagen ${denneUtbetalingsdagen.dato}")
                    val denneØkonomi = denneUtbetalingsdagen.økonomi
                    val andreØkonomi = andreUtbetalingsdagen.økonomi

                    check(denneUtbetalingsdagen::class == andreUtbetalingsdagen::class) { "Utbetalingsdagtypen er forskjellig. Var: ${denneUtbetalingsdagen::class}, nå: ${andreUtbetalingsdagen::class}" }
                    check(denneØkonomi.grad == andreØkonomi.grad) { "Grad er forskjellig. Var: ${denneØkonomi.grad}, nå: ${andreØkonomi.grad}" }
                    check(denneØkonomi.totalGrad == andreØkonomi.totalGrad) { "Totalgrad er forskjellig. Var: ${denneØkonomi.totalGrad}, nå: ${andreØkonomi.totalGrad}" }
                    check(denneØkonomi.arbeidsgiverRefusjonsbeløp == andreØkonomi.arbeidsgiverRefusjonsbeløp) { "Arbeidsgiver refusjonsbeløp er forskjellig. Var: ${denneØkonomi.arbeidsgiverRefusjonsbeløp}, nå: ${andreØkonomi.arbeidsgiverRefusjonsbeløp}" }
                    check(denneØkonomi.aktuellDagsinntekt == andreØkonomi.aktuellDagsinntekt) { "Aktuell dagsinntekt er forskjellig. Var: ${denneØkonomi.aktuellDagsinntekt}, nå: ${andreØkonomi.aktuellDagsinntekt}" }
                    check(denneØkonomi.beregningsgrunnlag == andreØkonomi.beregningsgrunnlag) { "Beregningsgrunnlag er forskjellig. Var: ${denneØkonomi.beregningsgrunnlag}, nå: ${andreØkonomi.beregningsgrunnlag}" }
                    check(denneØkonomi.dekningsgrunnlag == andreØkonomi.dekningsgrunnlag) { "Dekningsgrunnlag er forskjellig. Var: ${denneØkonomi.dekningsgrunnlag}, nå: ${andreØkonomi.dekningsgrunnlag}" }
                    check(denneØkonomi.grunnbeløpgrense == andreØkonomi.grunnbeløpgrense) { "Grunnbeløpgrense er forskjellig. Var: ${denneØkonomi.grunnbeløpgrense}, nå: ${andreØkonomi.grunnbeløpgrense}" }
                    check(denneØkonomi.arbeidsgiverbeløp == andreØkonomi.arbeidsgiverbeløp) { "Arbeidsgiverbeløp er forskjellig. Var: ${denneØkonomi.arbeidsgiverbeløp}, nå: ${andreØkonomi.arbeidsgiverbeløp}" }
                    check(denneØkonomi.personbeløp == andreØkonomi.personbeløp) { "Personbeløp er forskjellig. Var: ${denneØkonomi.personbeløp}, nå: ${andreØkonomi.personbeløp}" }
                    check(denneØkonomi.er6GBegrenset == andreØkonomi.er6GBegrenset) { "Er6GBegrenset er forskjellig. Var: ${denneØkonomi.er6GBegrenset}, nå: ${andreØkonomi.er6GBegrenset}" }
                    check(denneØkonomi.tilstand::class == andreØkonomi.tilstand::class) { "Tilstandstype er forskjellig. Var: ${denneØkonomi.tilstand::class}, nå: ${andreØkonomi.tilstand::class}" }

                }
            }
        }
    }
}
