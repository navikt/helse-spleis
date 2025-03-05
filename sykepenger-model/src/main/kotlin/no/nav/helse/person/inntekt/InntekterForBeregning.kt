package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.dto.InntektskildeDto
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.nesteDag
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi

@JvmInline
value class Inntektskilde(val id: String) {
    fun dto() = InntektskildeDto(id)

    companion object {
        fun gjenopprett(dto: InntektskildeDto) = Inntektskilde(dto.id)
    }
}

internal data class InntekterForBeregning(
    private val inntekterPerInntektskilde: Map<Inntektskilde, Inntekter>,
    private val beregningsperiode: Periode,
    val `6G`: Inntekt
) {

    internal fun tilBeregning(organisasjonsnummer: String) = inntekterPerInntektskilde.getValue(Inntektskilde(organisasjonsnummer)).let { it.fastsattÅrsinntekt to it.inntektstidslinje }

    internal fun forPeriode(periode: Periode) = inntekterPerInntektskilde
        .mapValues { (_, inntekter) -> inntekter.inntektstidslinje.subset(periode) }
        .filterValues { inntektstidslinje -> inntektstidslinje.isNotEmpty() }

    internal fun hensyntattAlleInntektskilder(beregnedeUtbetalingstidslinjer: Map<String, List<Utbetalingstidslinje>>): Map<String, Utbetalingstidslinje> {
        return inntekterPerInntektskilde.mapValues { (inntektskilde, inntekter) ->
            val beregnedeUtbetalingstidslinjerForInntektskilde = beregnedeUtbetalingstidslinjer[inntektskilde.id] ?: emptyList()
            val beregendePerioderForInntektskilde = beregnedeUtbetalingstidslinjerForInntektskilde.map(Utbetalingstidslinje::periode)
            val uberegnedeDagerForArbeidsgiver = beregningsperiode.uten(beregendePerioderForInntektskilde).flatten()
            val uberegnetUtbetalingstidslinjeForArbeidsgiver = inntekter.uberegnetUtbetalingstidslinje(uberegnedeDagerForArbeidsgiver, `6G`)
            beregnedeUtbetalingstidslinjerForInntektskilde.fold(uberegnetUtbetalingstidslinjeForArbeidsgiver, Utbetalingstidslinje::plus)
        }.mapKeys { (inntktskilde, _) ->inntktskilde.id }.filterValues { it.isNotEmpty() }
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
                inntektstidslinje = this.inntektstidslinje.erstatt(inntektsendringer.fraOgMed(skjæringstidspunkt.nesteDag))
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
                // Inntektsendring for en deaktivert arbeidsgiver høres spennende ut. Men det kan vel sikkert skje? Hen var i sykepengegrunnlaget men var reelt tilkommen f.eks. ?
                // Her tillater vi å endre beløpet på skjæringstidspunktet ettersom arbeidsgiveren ikke er en del av sykepengegrunnlaget.
                // Skulle den ble aktivert igjen så er det beløpet fra inntektsgrunnlaget som igjen vil bli brukt (Da er vi FraInntektsgrunnlag, ikke DeaktivertFraInntektsgrunnlag)
                inntektstidslinje = this.inntektstidslinje.erstatt(inntektsendringer)
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
                inntektstidslinje = this.inntektstidslinje.erstatt(inntektsendringer)
            )

            override fun uberegnetUtbetalingstidslinje(uberegnedeDager: List<LocalDate>, `6G`: Inntekt): Utbetalingstidslinje {
                // For en ny inntektskilde må vi ta utgangspunkt i inntektstidslinjen
                val dager = uberegnedeDager.filter { inntektstidslinje[it] is Beløpsdag }
                return arbeidsdager(dager, `6G`)
            }
        }
    }

    internal class Builder(private val beregningsperiode: Periode, private val skjæringstidspunkt: LocalDate) {
        private val inntekterFraInntektsgrunnlaget = mutableMapOf<Inntektskilde, Inntekter>()
        private val inntektsendringer = mutableMapOf<Inntektskilde, Beløpstidslinje>()

        internal fun fraInntektsgrunnlag(organisasjonsnummer: String, fastsattÅrsinntekt: Inntekt, opplysningskilde: Kilde) {
            val inntektskilde = Inntektskilde(organisasjonsnummer)
            check(inntektskilde !in inntekterFraInntektsgrunnlaget) { "Organisasjonsnummer er allerede lagt til som ${inntekterFraInntektsgrunnlaget.getValue(inntektskilde)::class.simpleName}" }
            inntekterFraInntektsgrunnlaget[inntektskilde] = Inntekter.FraInntektsgrunnlag(
                fastsattÅrsinntekt = fastsattÅrsinntekt,
                inntektstidslinje = Beløpstidslinje.fra(beregningsperiode, fastsattÅrsinntekt, opplysningskilde)
            )
        }

        internal fun deaktivertFraInntektsgrunnlag(organisasjonsnummer: String, opplysningskilde: Kilde) {
            val inntektskilde = Inntektskilde(organisasjonsnummer)
            check(inntektskilde !in inntekterFraInntektsgrunnlaget) { "Organisasjonsnummer er allerede lagt til som ${inntekterFraInntektsgrunnlaget.getValue(inntektskilde)::class.simpleName}" }
            inntekterFraInntektsgrunnlaget[inntektskilde] = Inntekter.DeaktivertFraInntektsgrunnlag(
                inntektstidslinje = Beløpstidslinje.fra(beregningsperiode, INGEN, opplysningskilde)
            )
        }

        internal fun inntektsendringer(inntektskilde: Inntektskilde, fom: LocalDate, tom: LocalDate?, inntekt: Inntekt, meldingsreferanseId: MeldingsreferanseId) {
            val periode = fom til listOfNotNull(tom, beregningsperiode.endInclusive).min()
            val kilde = Kilde(meldingsreferanseId, SYSTEM, LocalDateTime.now()) // TODO: TilkommenV4 smak litt på denne
            val inntektsendring = Beløpstidslinje.fra(periode, inntekt, kilde)
            inntektsendringer.compute(inntektskilde) { _, eksisterende ->
                ((eksisterende ?: Beløpstidslinje()).erstatt(inntektsendring)).subset(beregningsperiode)
            }
        }

        private lateinit var `6G`: Inntekt

        internal fun medGjeldende6G(gjeldende6G: Inntekt) {
            this.`6G` = gjeldende6G
        }

        internal fun build(): InntekterForBeregning {
            // Tar utgangspunkt i inntektene fra inntektsgrunnlaget
            val inntekterPerInntektskilde = inntekterFraInntektsgrunnlaget.toMutableMap()

            // Og fletter inn inntektsendringene
            inntektsendringer.forEach { (inntektskilde, inntektsendring) ->
                // Om vi ikke kjenner til inntektskilden fra før kan vi være diverse snax. Derunder "tilkommen inntekt"
                if (inntektskilde !in inntekterPerInntektskilde) {
                    inntekterPerInntektskilde[inntektskilde] = Inntekter.NyInntektskilde()
                }
                // Litt avhengig av inntekten som er lagt til grunn håndterer vi innteksendringer litt forskjellig
                inntekterPerInntektskilde[inntektskilde] = inntekterPerInntektskilde
                    .getValue(inntektskilde)
                    .inntektsendring(skjæringstidspunkt, inntektsendring)
            }
            return InntekterForBeregning(
                inntekterPerInntektskilde = inntekterPerInntektskilde.toMap(),
                beregningsperiode = beregningsperiode,
                `6G` = `6G`
            )
        }
    }

    internal companion object {
        private val TøyseteMeldingsreferanseId = MeldingsreferanseId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
        private val TøyseteTidsstempel = LocalDate.EPOCH.atStartOfDay()
        private val TøyseteOpplysningskilde = Kilde(TøyseteMeldingsreferanseId, SYSTEM, TøyseteTidsstempel)

        fun forAuu(periode: Periode, skjæringstidspunkt: LocalDate, organisasjonsnummer: String, inntektsgrunnlag: Inntektsgrunnlag?): Triple<Inntekt, Beløpstidslinje, InntekterForBeregning> {
            if (inntektsgrunnlag == null) {
                val inntekterForBeregning = with(Builder(periode, skjæringstidspunkt)) {
                    medGjeldende6G(Grunnbeløp.`6G`.beløp(skjæringstidspunkt))
                    build()
                }
                // Når vi skal lage en utbetalingstidslinje på en AUU hvor det ikke finnes noe inntektsgrunnlag vil vi ikke lagre de tøysete verdiene på behandlingen, bare bruke dem for å lage utbetalingstidslinje
                return Triple(INGEN, Beløpstidslinje.fra(periode, INGEN, TøyseteOpplysningskilde), inntekterForBeregning)
            }

            val inntekterForBeregning = with(Builder(periode, skjæringstidspunkt)) {
                inntektsgrunnlag.beverte(this)
                build()
            }

            val (fastsattÅrsinntekt, inntektstidslinje) = inntekterForBeregning.inntekterPerInntektskilde[Inntektskilde(organisasjonsnummer)]?.let { it.fastsattÅrsinntekt to it.inntektstidslinje } ?: error(
                "Det er en arbeidsgiver som ikke inngår i SP: $organisasjonsnummer som har søknader: $periode.\n" +
                "Burde ikke arbeidsgiveren være kjent i sykepengegrunnlaget, enten i form av en skatteinntekt eller en tilkommet?"
            )

            // Når vi skal lage en utbetalingstidslinje på en AUU hvor det finnes inntektsgrunnlag så lagrer vi ned de ekte inntektene
            return Triple(fastsattÅrsinntekt, inntektstidslinje, inntekterForBeregning)
        }
    }
}
