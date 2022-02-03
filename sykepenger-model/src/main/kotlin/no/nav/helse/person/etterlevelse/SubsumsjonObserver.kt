package no.nav.helse.person.etterlevelse

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.person.Inntektshistorikk.Skatt
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.UtbetalingstidslinjeVisitor.Periode.Companion.dager
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.*

interface SubsumsjonObserver {

    /**
     * Vurdering av opptjeningstid
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-2)
     *
     * @param oppfylt hvorvidt sykmeldte har oppfylt krav om opptjeningstid
     * @param skjæringstidspunkt dato som antall opptjeningsdager regnes mot
     * @param tilstrekkeligAntallOpptjeningsdager antall opptjeningsdager som kreves for at vilkåret skal være [oppfylt]
     * @param arbeidsforhold hvilke arbeidsforhold det er tatt utgangspunkt i ved beregning av opptjeningstid
     * @param antallOpptjeningsdager antall opptjeningsdager sykmeldte faktisk har på [skjæringstidspunkt]
     */
    fun `§ 8-2 ledd 1`(
        oppfylt: Boolean,
        skjæringstidspunkt: LocalDate,
        tilstrekkeligAntallOpptjeningsdager: Int,
        arbeidsforhold: List<Map<String, Any?>>,
        antallOpptjeningsdager: Int
    ) {}

    /**
     * Vurdering av rett til sykepenger ved fylte 70 år
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-3)
     *
     * @param oppfylt hvorvidt sykmeldte har fylt 70 år. Oppfylt så lenge sykmeldte ikke er 70 år eller eldre
     * @param syttiårsdagen dato sykmeldte fyller 70 år
     * @param utfallFom fra-og-med-dato [oppfylt]-vurderingen gjelder for
     * @param utfallTom til-og-med-dato [oppfylt]-vurderingen gjelder for
     * @param tidslinjeFom fra-og-med-dato vurderingen gjøres for
     * @param tidslinjeTom til-og-med-dato vurderingen gjøres for
     * @param avvisteDager alle dager vurderingen ikke er [oppfylt] for. Tom dersom sykmeldte ikke fyller 70 år mellom [tidslinjeFom] og [tidslinjeTom]
     */
    fun `§ 8-3 ledd 1 punktum 2`(
        oppfylt: Boolean,
        syttiårsdagen: LocalDate,
        utfallFom: LocalDate,
        utfallTom: LocalDate,
        tidslinjeFom: LocalDate,
        tidslinjeTom: LocalDate,
        avvisteDager: List<LocalDate>
    ) {}

    /**
     * Vurdering av krav til minimum inntekt
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-3)
     *
     * @param oppfylt hvorvidt sykmeldte har inntekt lik eller større enn minimum inntekt
     * @param skjæringstidspunkt dato det tas utgangspunkt i ved vurdering av minimum inntekt
     * @param grunnlagForSykepengegrunnlag total inntekt på tvers av alle relevante arbeidsgivere
     * @param minimumInntekt minimum beløp [grunnlagForSykepengegrunnlag] må være lik eller større enn for at vilkåret skal være [oppfylt]
     */
    fun `§ 8-3 ledd 2 punktum 1`(oppfylt: Boolean, skjæringstidspunkt: LocalDate, grunnlagForSykepengegrunnlag: Inntekt, minimumInntekt: Inntekt) {}

    /**
     * Vurdering av maksimalt sykepengegrunnlag
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-10)
     *
     * @param erBegrenset dersom hjemlen slår inn ved at [grunnlagForSykepengegrunnlag] blir begrenset til [maksimaltSykepengegrunnlag]
     * @param maksimaltSykepengegrunnlag maksimalt årlig beløp utbetaling skal beregnes ut fra
     * @param skjæringstidspunkt dato [maksimaltSykepengegrunnlag] settes ut fra
     * @param grunnlagForSykepengegrunnlag total inntekt på tvers av alle relevante arbeidsgivere
     */
    fun `§ 8-10 ledd 2 punktum 1`(erBegrenset: Boolean, maksimaltSykepengegrunnlag: Inntekt, skjæringstidspunkt: LocalDate, grunnlagForSykepengegrunnlag: Inntekt) {}

    /**
     * Beregning av inntekt pr. dag
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-10)
     *
     * @param årsinntekt inntekt oppgitt fra inntektsmelding omregnet til årlig
     * @param inntektOmregnetTilDaglig årsinntekt omregnet til daglig inntekt
     */
    fun `§ 8-10 ledd 3`(årsinntekt: Double, inntektOmregnetTilDaglig: Double) {}

    fun `§ 8-11 første ledd`() {
        // versjon = FOLKETRYGDLOVENS_OPPRINNELSESDATO
    }

    /**
     * Vurdering av maksimalt antall sykepengedager
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-12)
     *
     * @param periode aktuell periode som vilkårsprøves
     * @param tidslinjegrunnlag alle tidslinjer det tas utgangspunkt i inklusiv potensielt utbetalte dager fra Infotrygd
     * @param beregnetTidslinje sammenslått tidslinje det tas utgangspunkt i når man beregner [gjenståendeSykedager], [forbrukteSykedager] og [maksdato]
     * @param gjenståendeSykedager antall gjenstående sykepengedager ved siste utbetalte dag i [periode].
     * @param forbrukteSykedager antall forbrukte sykepengedager ved siste utbetalte dag i [periode].
     * @param maksdato dato for opphør av rett til sykepenger
     * @param startdatoSykepengerettighet første NAV-dag i siste 248-dagers sykeforløp
     */
    fun `§ 8-12 ledd 1 punktum 1`(
        periode: Periode,
        tidslinjegrunnlag: List<List<Map<String, Any>>>,
        beregnetTidslinje: List<Map<String, Any>>,
        gjenståendeSykedager: Int,
        forbrukteSykedager: Int,
        maksdato: LocalDate,
        startdatoSykepengerettighet: LocalDate
    ) {}

    /**
     * Vurdering av ny rett til sykepenger
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-12)
     *
     * @param oppfylt **true** dersom det har vært tilstrekelig opphold
     * @param dato dato vurdering av hjemmel gjøres
     * @param tilstrekkeligOppholdISykedager antall dager med opphold i ytelsen som nødvendig for å oppnå ny rett til sykepenger
     * @param tidslinjegrunnlag alle tidslinjer det tas utgangspunkt i ved bygging av [beregnetTidslinje]
     * @param beregnetTidslinje tidslinje det tas utgangspunkt i ved utbetaling for aktuell vedtaksperiode
     */
    fun `§ 8-12 ledd 2`(
        oppfylt: Boolean,
        dato: LocalDate,
        gjenståendeSykepengedager: Int,
        beregnetAntallOppholdsdager: Int,
        tilstrekkeligOppholdISykedager: Int,
        tidslinjegrunnlag: List<List<Map<String, Any>>>,
        beregnetTidslinje: List<Map<String, Any>>
    ) {}

    /**
     * Vurdering av graderte sykepenger
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-13)
     *
     * @param oppfylt **true** dersom uføregraden er minst 20%
     * @param avvisteDager dager som ikke møter kriterie for [oppfylt]
     */
    fun `§ 8-13 ledd 1`(oppfylt: Boolean, avvisteDager: List<LocalDate>) {}

    /**
     * Fastsettelse av dekningsgrunnlag
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-16)
     *
     * @param dato dagen [dekningsgrunnlag] beregnes for
     * @param dekningsgrad hvor stor andel av inntekten det ytes sykepenger av
     * @param inntekt inntekt for aktuell arbeidsgiver
     * @param dekningsgrunnlag maks dagsats før reduksjon til 6G og reduksjon for sykmeldingsgrad
     */
    fun `§ 8-16 ledd 1`(dato: LocalDate, dekningsgrad: Double, inntekt: Double, dekningsgrunnlag: Double) {}

    /**
     * Vurdering av når utbetaling av sykepenger tidligst skal starte
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-17)
     *
     * @param arbeidsgiverperiode alle arbeidsgiverperiode-dager
     * @param førsteNavdag første dag NAV skal utbetale
     */
    fun `§ 8-17 ledd 1 bokstav a`(arbeidsgiverperiode: List<LocalDate>, førsteNavdag: LocalDate) {}

    /**
     * Trygden yter ikke sykepenger for lovpålagt ferie og permisjon
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-17)
     *
     * @param dato dagen vilkåret blir vurdert for
     */
    fun `§ 8-17 ledd 2`(dato: LocalDate) {}

    /**
     * Inntekt som legges til grunn dersom sykdom ved en arbeidsgiver starter senere enn skjæringstidspunktet tilsvarer
     * innrapportert inntekt til a-ordningen for de tre siste månedene før skjæringstidspunktet
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-28)
     *
     * @param inntekterSisteTreMåneder månedlig inntekt for de tre siste måneder før skjæringstidspunktet
     * @param resterendeInntekter alle andre innrapporterte inntekter fra a-ordningen
     * @param grunnlagForSykepengegrunnlag beregnet grunnlag basert på [inntekterSisteTreMåneder]
     */
    fun `§ 8-28 ledd 3 bokstav a`(
        inntekterSisteTreMåneder: List<Map<String, Any>>,
        resterendeInntekter: List<Map<String, Any>>,
        grunnlagForSykepengegrunnlag: Inntekt
    ) {
    }

    /**
     * Fastsettelse av sykepengegrunnlaget
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-30)
     *
     * Merk: Alltid oppfylt
     *
     * @param grunnlagForSykepengegrunnlagPerArbeidsgiver beregnet inntekt per arbeidsgiver
     * @param grunnlagForSykepengegrunnlag beregnet inntekt på tvers av arbeidsgivere
     */
    fun `§ 8-30 ledd 1`(grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Inntekt>, grunnlagForSykepengegrunnlag: Inntekt) {}

    /**
     * Vurdering av avvik mellom omregnet årsinntekt og innrapportert inntekt til a-ordningen
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-30)
     *
     * @param oppfylt dersom avviket er innenfor gitt margin
     * @param maksimaltTillattAvvikPåÅrsinntekt margin
     * @param grunnlagForSykepengegrunnlag beregnet inntekt på tvers av arbeidsgivere
     * @param sammenligningsgrunnlag innrapportert inntekt til a-ordningen
     * @param avvik beregnet avvik mellom omregnet årsinntekt og innrapportert inntekt til a-ordningen
     */
    fun `§ 8-30 ledd 2 punktum 1`(
        oppfylt: Boolean,
        maksimaltTillattAvvikPåÅrsinntekt: Prosent,
        grunnlagForSykepengegrunnlag: Inntekt,
        sammenligningsgrunnlag: Inntekt,
        avvik: Prosent
    ) {}

    fun `§ 8-33 ledd 1`() {}

    @Suppress("UNUSED_PARAMETER")
    fun `§ 8-33 ledd 3`(grunnlagForFeriepenger: Int, opptjeningsår: Year, prosentsats: Double, alder: Int, feriepenger: Double) {}

    /**
     * Vurdering av krav til minimum inntekt ved alder mellom 67 og 70 år
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-51)
     *
     * @param oppfylt dersom vedkommende har inntekt større enn eller lik to ganger grunnbeløpet. Det er en forutsetning at vedkommende er mellom 67 og 70 år
     * @param skjæringstidspunkt dato det tas utgangspunkt i ved vurdering av minimum inntekt
     * @param alderPåSkjæringstidspunkt alder på skjæringstidspunktet
     * @param grunnlagForSykepengegrunnlag total inntekt på tvers av alle relevante arbeidsgivere
     * @param minimumInntekt minimum beløp [grunnlagForSykepengegrunnlag] må være lik eller større enn for at vilkåret skal være [oppfylt]
     */
    fun `§ 8-51 ledd 2`(
        oppfylt: Boolean,
        skjæringstidspunkt: LocalDate,
        alderPåSkjæringstidspunkt: Int,
        grunnlagForSykepengegrunnlag: Inntekt,
        minimumInntekt: Inntekt
    ) {}

    /**
     * Vurdering av maksimalt antall sykepengedager ved fytle 67 år
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-51)
     *
     * @param periode aktuell periode som vilkårsprøves
     * @param tidslinjegrunnlag alle tidslinjer det tas utgangspunkt i inklusiv potensielt utbetalte dager fra Infotrygd
     * @param beregnetTidslinje sammenslått tidslinje det tas utgangspunkt i når man beregner [gjenståendeSykedager], [forbrukteSykedager] og [maksdato]
     * @param gjenståendeSykedager antall gjenstående sykepengedager ved siste utbetalte dag i [periode].
     * @param forbrukteSykedager antall forbrukte sykepengedager ved siste utbetalte dag i [periode].
     * @param maksdato dato for opphør av rett til sykepenger
     * @param startdatoSykepengerettighet første NAV-dag i siste 248-dagers sykeforløp
     */
    fun `§ 8-51 ledd 3`(
        periode: Periode,
        tidslinjegrunnlag: List<List<Map<String, Any>>>,
        beregnetTidslinje: List<Map<String, Any>>,
        gjenståendeSykedager: Int,
        forbrukteSykedager: Int,
        maksdato: LocalDate,
        startdatoSykepengerettighet: LocalDate
    ) {}

    private class UtbetalingstidslinjeVisitor(utbetalingstidslinje: Utbetalingstidslinje) : UtbetalingsdagVisitor {
        private val navdager = mutableListOf<Periode>()
        private var forrigeDato: LocalDate? = null

        private class Periode(
            val fom: LocalDate,
            var tom: LocalDate,
            val dagtype: String
        ) {
            companion object {
                fun List<Periode>.dager() = map {
                    mapOf(
                        "fom" to it.fom,
                        "tom" to it.tom,
                        "dagtype" to it.dagtype
                    )
                }
            }
        }

        init {
            utbetalingstidslinje.accept(this)
        }

        fun dager() = navdager.dager()

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag, dato: LocalDate, økonomi: Økonomi) {
            visit(dato, "NAVDAG")
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag, dato: LocalDate, økonomi: Økonomi) {
            visit(dato, "NAVDAG")
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag, dato: LocalDate, økonomi: Økonomi) {
            if (forrigeDato != null && forrigeDato?.plusDays(1) == dato) visit(dato, "FRIDAG")
        }

        private fun visit(dato: LocalDate, dagtype: String) {
            forrigeDato = dato
            if (navdager.isEmpty() || dagtype != navdager.last().dagtype || navdager.last().tom.plusDays(1) != dato) {
                navdager.add(Periode(dato, dato, dagtype))
            } else {
                navdager.last().tom = dato
            }
        }
    }

    private class SkattVisitor(skatt: Skatt) : InntekthistorikkVisitor {
        private lateinit var inntekt: Map<String, Any>

        init {
            skatt.accept(this)
        }

        fun inntekt() = inntekt

        override fun visitSkattSykepengegrunnlag(
            sykepengegrunnlag: Skatt.Sykepengegrunnlag,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime
        ) {
            inntekt = mapOf("årMåned" to måned, "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig })
        }
    }

    companion object {
        internal fun List<Utbetalingstidslinje>.toSubsumsjonFormat(): List<List<Map<String, Any>>> = map { it.toSubsumsjonFormat() }.filter { it.isNotEmpty() }
        internal fun Utbetalingstidslinje.toSubsumsjonFormat(): List<Map<String, Any>> = UtbetalingstidslinjeVisitor(this).dager()
        internal fun Iterable<Skatt>.toSubsumsjonFormat(): List<Map<String, Any>> = map { SkattVisitor(it).inntekt() }
    }
}
