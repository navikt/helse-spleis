package no.nav.helse.person.etterlevelse

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.*
import no.nav.helse.person.Inntektshistorikk.Skatt
import no.nav.helse.person.Inntektshistorikk.Skatt.Inntekttype.*
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Tidslinjedag.Tidslinjeperiode.Companion.dager
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.*
import kotlin.math.roundToInt
import kotlin.properties.Delegates

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


    fun `§ 8-9 ledd 1`(oppfylt: Boolean, periode: Periode) {}

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
    fun `§ 8-10 ledd 2 punktum 1`(
        erBegrenset: Boolean,
        maksimaltSykepengegrunnlag: Inntekt,
        skjæringstidspunkt: LocalDate,
        grunnlagForSykepengegrunnlag: Inntekt
    ) {}

    /**
     * Beregning av inntekt pr. dag
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-10)
     *
     * @param årsinntekt inntekt oppgitt fra inntektsmelding omregnet til årlig
     * @param inntektOmregnetTilDaglig årsinntekt omregnet til daglig inntekt
     */
    fun `§ 8-10 ledd 3`(årsinntekt: Double, inntektOmregnetTilDaglig: Double) {}

    /**
     * Trygden yter ikke sykepenger i lørdag og søndag
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-11)
     *
     * @param dato dagen vilkåret ikke er oppfylt for
     */
    fun `§ 8-11 første ledd`(dato: LocalDate) {}

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
        tidslinjegrunnlag: List<List<Tidslinjedag>>,
        beregnetTidslinje: List<Tidslinjedag>,
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
        tidslinjegrunnlag: List<List<Tidslinjedag>>,
        beregnetTidslinje: List<Tidslinjedag>
    ) {}

    /**
     * Vurdering av graderte sykepenger
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-13)
     *
     * @param periode perioden vilkåret vurderes for
     * @param avvisteDager dager som vilkåret ikke er oppfylt for, hvis noen
     * @param tidslinjer alle tidslinjer på tvers av arbeidsgivere
     */
    fun `§ 8-13 ledd 1`(periode: Periode, avvisteDager: List<LocalDate>, tidslinjer: List<List<Tidslinjedag>>) {}

    /**
     * Vurdering av sykepengenes størrelse
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-13)
     *
     * @param periode perioden vilkåret vurderes for
     * @param tidslinjer alle tidslinjer på tvers av arbeidsgivere
     * @param grense grense brukt til å vurdere [dagerUnderGrensen]
     * @param dagerUnderGrensen dager som befinner seg under tilstrekkelig uføregrad, gitt av [grense]
     */
    fun `§ 8-13 ledd 2`(periode: Periode, tidslinjer: List<List<Tidslinjedag>>, grense: Double, dagerUnderGrensen: List<LocalDate>) {}

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
     * @param oppfylt **true** dersom [dagen] er etter arbeidsgiverperioden
     * @param dagen aktuelle dagen for vurdering
     */
    fun `§ 8-17 ledd 1 bokstav a`(oppfylt: Boolean, dagen: LocalDate) {}

    /**
     * Vurdering av når utbetaling av sykepenger tidligst skal starte
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-17)
     *
     * @param periode arbeidsgiversøknad-perioden
     */
    fun `§ 8-17 ledd 1 bokstav a - arbeidsgiversøknad`(periode: Iterable<LocalDate>) {}

    /**
     * Trygden yter ikke sykepenger for lovpålagt ferie og permisjon
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-17)
     *
     * @param dato dagen vilkåret blir vurdert for
     */
    fun `§ 8-17 ledd 2`(dato: LocalDate, sykdomstidslinje: List<Tidslinjedag>) {}

    /**
     * Arbeidsgiverperioden teller 16 sykedager
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-19)
     *
     * @param dato dagen vilkåret blir vurdert for
     * @param beregnetTidslinje tidslinje som ligger til grunn for beregning av agp
     */
    fun `§ 8-19 første ledd`(dato: LocalDate, beregnetTidslinje: List<Tidslinjedag>) {}

    /**
     * Arbeidsgiverperioden regnes fra og med første hele fraværsdag
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-19)
     *
     * @param dato for en dag som anses som en agp-dag
     * @param beregnetTidslinje tidslinje som ligger til grunn for beregning av agp
     */
    fun `§ 8-19 andre ledd`(dato: LocalDate, beregnetTidslinje: List<Tidslinjedag>) {}

    /**
     * Når det er gått mindre enn 16 kalenderdager siden forrige sykefravær,
     * skal et nytt sykefravær regnes med i samme arbeidsgiverperiode.
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-19)
     *
     * @param dato for en dag som anses som en agp-dag
     * @param beregnetTidslinje tidslinje som ligger til grunn for beregning av agp
     */
    fun `§ 8-19 tredje ledd`(dato: LocalDate, beregnetTidslinje: List<Tidslinjedag>) {}

    /**
     * Inntekt som legges til grunn dersom sykdom ved en arbeidsgiver starter senere enn skjæringstidspunktet tilsvarer
     * innrapportert inntekt til a-ordningen for de tre siste månedene før skjæringstidspunktet
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-28)
     *
     * @param organisasjonsnummer arbeidsgiveren [grunnlagForSykepengegrunnlag] er beregnet for
     * @param inntekterSisteTreMåneder månedlig inntekt for de tre siste måneder før skjæringstidspunktet
     * @param skjæringstidspunkt dato som [grunnlagForSykepengegrunnlag] beregnes relativt til
     * @param grunnlagForSykepengegrunnlag beregnet grunnlag basert på [inntekterSisteTreMåneder]
     */
    fun `§ 8-28 ledd 3 bokstav a`(
        organisasjonsnummer: String,
        inntekterSisteTreMåneder: List<Map<String, Any>>,
        grunnlagForSykepengegrunnlag: Inntekt,
        skjæringstidspunkt: LocalDate
    ) {}

    /**
     * Inntekter som legges til grunn for beregning av sykepengegrunnlag
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-29)
     *
     * @param organisasjonsnummer arbeidsgiveren [grunnlagForSykepengegrunnlag] er beregnet for
     * @param inntektsopplysninger inntekter som ligger til grunn for beregning av [grunnlagForSykepengegrunnlag]
     * @param skjæringstidspunkt dato som [grunnlagForSykepengegrunnlag] beregnes relativt til
     * @param grunnlagForSykepengegrunnlag beregnet grunnlag basert på [inntektsopplysninger]
     */
    fun `§ 8-29`(
        skjæringstidspunkt: LocalDate,
        grunnlagForSykepengegrunnlag: Inntekt,
        inntektsopplysninger: List<Map<String, Any>>,
        organisasjonsnummer: String
    ) {}

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
     * @param maksimaltTillattAvvikPåÅrsinntekt margin
     * @param grunnlagForSykepengegrunnlag beregnet inntekt på tvers av arbeidsgivere
     * @param sammenligningsgrunnlag innrapportert inntekt til a-ordningen
     * @param avvik beregnet avvik mellom omregnet årsinntekt og innrapportert inntekt til a-ordningen
     */
    fun `§ 8-30 ledd 2 punktum 1`(
        maksimaltTillattAvvikPåÅrsinntekt: Prosent,
        grunnlagForSykepengegrunnlag: Inntekt,
        sammenligningsgrunnlag: Inntekt,
        avvik: Prosent
    ) {}

    /**
     * Beregning av sammenligningsgrunnlaget
     *
     * Lovdata: [lenke](https://lovdata.no/lov/1997-02-28-19/%C2%A78-30)
     *
     * @param skjæringstidspunkt dato inntekter fra a-ordningen er hentet relativt til
     * @param sammenligningsgrunnlag inneholder beregnet sammenligningsgrunnlag samt hver enkelt inntekt som er innregnet i sammenligningsgrunnlaget
     */
    fun `§ 8-30 ledd 2`(skjæringstidspunkt: LocalDate, sammenligningsgrunnlag: SammenligningsgrunnlagDTO) {}


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
        tidslinjegrunnlag: List<List<Tidslinjedag>>,
        beregnetTidslinje: List<Tidslinjedag>,
        gjenståendeSykedager: Int,
        forbrukteSykedager: Int,
        maksdato: LocalDate,
        startdatoSykepengerettighet: LocalDate
    ) {}

    class Tidslinjedag(
        private val dato: LocalDate,
        private val dagtype: String,
        private val grad: Int?
    ) {
        private fun hørerTil(tidslinjeperiode: Tidslinjeperiode) = tidslinjeperiode.hørerTil(dato, dagtype, grad)

        internal fun erRettFør(dato: LocalDate) = this.dato.plusDays(1) == dato

        internal fun erAvvistDag() = dagtype == "AVVISTDAG"

        companion object {
            fun List<Tidslinjedag>.dager(periode: Periode? = null): List<Map<String, Any?>> {
                return this
                    .filter { it.dato >= (periode?.start ?: LocalDate.MIN) && it.dato <= (periode?.endInclusive ?: LocalDate.MAX) }
                    .sortedBy { it.dato }
                    .fold(mutableListOf<Tidslinjeperiode>()) { acc, nesteDag ->
                        if (acc.isNotEmpty() && nesteDag.hørerTil(acc.last())) {
                            acc.last().utvid(nesteDag.dato)
                        } else {
                            acc.add(Tidslinjeperiode(nesteDag.dato, nesteDag.dato, nesteDag.dagtype, nesteDag.grad))
                        }
                        acc
                    }.dager()
            }
        }

        private class Tidslinjeperiode(
            private val fom: LocalDate,
            private var tom: LocalDate,
            private val dagtype: String,
            private val grad: Int?
        ) {
            fun utvid(dato: LocalDate) {
                this.tom = dato
            }

            fun hørerTil(dato: LocalDate, dagtype: String, grad: Int?) = tom.plusDays(1) == dato && this.dagtype == dagtype && this.grad == grad

            companion object {
                fun List<Tidslinjeperiode>.dager() = map {
                    mapOf(
                        "fom" to it.fom,
                        "tom" to it.tom,
                        "dagtype" to it.dagtype,
                        "grad" to it.grad
                    )
                }
            }
        }
    }

    private class SykdomstidslinjeBuilder(sykdomstidslinje: Sykdomstidslinje) : SykdomstidslinjeVisitor {
        private val navdager = mutableListOf<Tidslinjedag>()

        init {
            sykdomstidslinje.accept(this)
        }

        fun dager() = navdager.toList()

        override fun visitDag(dag: Dag.Sykedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            visit(dato, "SYKEDAG", økonomi)
        }

        override fun visitDag(dag: Dag.SykHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            visit(dato, "SYKEDAG", økonomi)
        }

        override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            visit(dato, "FERIEDAG", null)
        }

        override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
            visit(dato, "PERMISJONSDAG", null)
        }

        private fun visit(dato: LocalDate, dagtype: String, økonomi: Økonomi?) {
            val grad = økonomi?.medData { grad, _, _ -> grad }
            navdager.add(Tidslinjedag(dato, dagtype, grad?.roundToInt()))
        }
    }

    private class UtbetalingstidslinjeBuilder(utbetalingstidslinje: Utbetalingstidslinje) : UtbetalingsdagVisitor {
        private val navdager = mutableListOf<Tidslinjedag>()

        init {
            utbetalingstidslinje.accept(this)
        }

        fun dager() = navdager.toList()

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag, dato: LocalDate, økonomi: Økonomi) {
            visit(dato, "NAVDAG", økonomi)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag, dato: LocalDate, økonomi: Økonomi) {
            visit(dato, "AGPDAG", økonomi)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag, dato: LocalDate, økonomi: Økonomi) {
            if (navdager.isNotEmpty() && navdager.last().erAvvistDag()) visit(dato, "AVVISTDAG", økonomi) else visit(dato, "NAVDAG", økonomi)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag, dato: LocalDate, økonomi: Økonomi) {
            // Dersom vi er inne i en oppholdsperiode ønsker vi ikke å ta med vanlige helger
            if (navdager.isNotEmpty() && navdager.last().erRettFør(dato)) visit(dato, "FRIDAG", økonomi)
        }

        override fun visit(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag, dato: LocalDate, økonomi: Økonomi) {
            visit(dato, "AVVISTDAG", økonomi)
        }

        private fun visit(dato: LocalDate, dagtype: String, økonomi: Økonomi?) {
            val grad = økonomi?.medData { grad, _, _ -> grad }
            navdager.add(Tidslinjedag(dato, dagtype, grad?.roundToInt()))
        }
    }

    private class SkattBuilder(skatt: Skatt) : InntekthistorikkVisitor {
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
            inntekt = mapOf(
                "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
                "årMåned" to måned,
                "type" to type.fromInntekttype(),
                "fordel" to fordel,
                "beskrivelse" to beskrivelse
            )
        }

        private fun Skatt.Inntekttype.fromInntekttype() = when (this) {
            LØNNSINNTEKT -> "LØNNSINNTEKT"
            NÆRINGSINNTEKT -> "NÆRINGSINNTEKT"
            PENSJON_ELLER_TRYGD -> "PENSJON_ELLER_TRYGD"
            YTELSE_FRA_OFFENTLIGE -> "YTELSE_FRA_OFFENTLIGE"
        }
    }

    private class SammenligningsgrunnlagBuilder(sammenligningsgrunnlag: Sammenligningsgrunnlag) : VilkårsgrunnlagHistorikkVisitor {
        private var sammenligningsgrunnlag by Delegates.notNull<Double>()
        private val inntekter = mutableMapOf<String, List<Map<String, Any>>>()
        private lateinit var inntektliste: MutableList<Map<String, Any>>

        init {
            sammenligningsgrunnlag.accept(this)
        }

        fun build() = SammenligningsgrunnlagDTO(sammenligningsgrunnlag, inntekter)

        override fun preVisitSammenligningsgrunnlag(sammenligningsgrunnlag1: Sammenligningsgrunnlag, sammenligningsgrunnlag: Inntekt) {
            this.sammenligningsgrunnlag = sammenligningsgrunnlag.reflection { årlig, _, _, _ -> årlig }
        }

        override fun preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
            inntektliste = mutableListOf()
            inntekter[orgnummer] = inntektliste
        }

        override fun visitSkattSammenligningsgrunnlag(
            sammenligningsgrunnlag: Skatt.Sammenligningsgrunnlag,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime
        ) {
            inntektliste.add(
                mapOf(
                    "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
                    "årMåned" to måned,
                    "type" to type.fromInntekttype(),
                    "fordel" to fordel,
                    "beskrivelse" to beskrivelse
                )
            )
        }

        private fun Skatt.Inntekttype.fromInntekttype() = when (this) {
            LØNNSINNTEKT -> "LØNNSINNTEKT"
            NÆRINGSINNTEKT -> "NÆRINGSINNTEKT"
            PENSJON_ELLER_TRYGD -> "PENSJON_ELLER_TRYGD"
            YTELSE_FRA_OFFENTLIGE -> "YTELSE_FRA_OFFENTLIGE"
        }
    }

    class SammenligningsgrunnlagDTO(
        val sammenligningsgrunnlag: Double,
        val inntekterFraAOrdningen: Map<String, List<Map<String, Any>>>
    )

    companion object {
        internal fun List<Utbetalingstidslinje>.subsumsjonsformat(): List<List<Tidslinjedag>> = map { it.subsumsjonsformat() }.filter { it.isNotEmpty() }
        internal fun Utbetalingstidslinje.subsumsjonsformat(): List<Tidslinjedag> = UtbetalingstidslinjeBuilder(this).dager()
        internal fun Sykdomstidslinje.subsumsjonsformat(): List<Tidslinjedag> = SykdomstidslinjeBuilder(this).dager()
        internal fun Iterable<Skatt>.subsumsjonsformat(): List<Map<String, Any>> = map { SkattBuilder(it).inntekt() }
        internal fun Sammenligningsgrunnlag.subsumsjonsformat(): SammenligningsgrunnlagDTO = SammenligningsgrunnlagBuilder(this).build()
    }
}
