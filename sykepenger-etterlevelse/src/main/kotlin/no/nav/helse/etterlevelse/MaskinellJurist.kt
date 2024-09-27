package no.nav.helse.etterlevelse

import java.io.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.helse.etterlevelse.Bokstav.BOKSTAV_A
import no.nav.helse.etterlevelse.Bokstav.BOKSTAV_B
import no.nav.helse.etterlevelse.Bokstav.BOKSTAV_C
import no.nav.helse.etterlevelse.Inntektsubsumsjon.Companion.subsumsjonsformat
import no.nav.helse.etterlevelse.Ledd.Companion.ledd
import no.nav.helse.etterlevelse.Ledd.LEDD_1
import no.nav.helse.etterlevelse.Ledd.LEDD_2
import no.nav.helse.etterlevelse.Ledd.LEDD_3
import no.nav.helse.etterlevelse.Ledd.LEDD_5
import no.nav.helse.etterlevelse.MaskinellJurist.SubsumsjonEvent.Companion.paragrafVersjonFormaterer
import no.nav.helse.etterlevelse.Paragraf.KJENNELSE_2006_4023
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_10
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_11
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_12
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_13
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_15
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_16
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_17
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_19
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_2
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_28
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_29
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_3
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_48
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_51
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_9
import no.nav.helse.etterlevelse.Punktum.Companion.punktum
import no.nav.helse.etterlevelse.RangeIterator.Companion.trim
import no.nav.helse.etterlevelse.Subsumsjon.Utfall
import no.nav.helse.etterlevelse.Subsumsjon.Utfall.VILKAR_BEREGNET
import no.nav.helse.etterlevelse.Subsumsjon.Utfall.VILKAR_IKKE_OPPFYLT
import no.nav.helse.etterlevelse.Subsumsjon.Utfall.VILKAR_OPPFYLT
import no.nav.helse.etterlevelse.Tidslinjedag.Companion.dager

class MaskinellJurist private constructor(
    private val parent: MaskinellJurist?,
    private val kontekster: List<Subsumsjonskontekst>,
    vedtaksperiode: ClosedRange<LocalDate>? = null
) : Subsumsjonslogg {

    private val periode: () -> ClosedRange<LocalDate>

    init {
        // Når periode blir kalt av en subsumsjon skal vi være i kontekst av en vedtaksperiode.
        periode =  { checkNotNull(vedtaksperiode){"MaksinellJurist må være i kontekst av en vedtaksperiode for å registrere subsumsjonen"} }
    }

    private val subsumsjoner = mutableListOf<Subsumsjon>()

    constructor() : this(null, emptyList())

    override fun logg(subsumsjon: Subsumsjon) {
        sjekkKontekster()
        if (tomPeriode(subsumsjon)) return
        leggTil(subsumsjon.copy(kontekster = kontekster))
    }

    private fun tomPeriode(subsumsjon: Subsumsjon): Boolean {
        if (subsumsjon.type != Subsumsjon.Subsumsjonstype.PERIODISERT) return false
        if ("perioder" !in subsumsjon.output) return true
        val perioder = subsumsjon.output["perioder"] as List<*>
        return perioder.isEmpty()
    }

    private fun leggTil(subsumsjon: Subsumsjon) {
        subsumsjoner.add(subsumsjon)
        parent?.leggTil(subsumsjon)
    }

    private fun sjekkKontekster() {
        val kritiskeTyper = setOf(KontekstType.Fødselsnummer, KontekstType.Organisasjonsnummer)
        check(kritiskeTyper.all { kritiskType ->
            kontekster.count { it.type == kritiskType } == 1
        }) {
            "en av $kritiskeTyper mangler/har duplikat:\n${kontekster.joinToString(separator = "\n")}"
        }
        // todo: sjekker for mindre enn 1 også ettersom noen subsumsjoner skjer på arbeidsgivernivå. det burde vi forsøke å flytte/fikse slik at
        // alt kan subsummeres i kontekst av en behandling.
        check(kontekster.count { it.type == KontekstType.Vedtaksperiode } <= 1) {
            "det er flere kontekster av ${KontekstType.Vedtaksperiode}:\n${kontekster.joinToString(separator = "\n")}"
        }
    }

    fun medFødselsnummer(personidentifikator: String) =
        kopierMedKontekst(listOf(Subsumsjonskontekst(KontekstType.Fødselsnummer, personidentifikator)))

    fun medOrganisasjonsnummer(organisasjonsnummer: String) =
        kopierMedKontekst(listOf(Subsumsjonskontekst(KontekstType.Organisasjonsnummer, organisasjonsnummer)))

    fun medVedtaksperiode(vedtaksperiodeId: UUID, hendelseIder: List<Subsumsjonskontekst>, periode: ClosedRange<LocalDate>) =
        kopierMedKontekst(listOf(Subsumsjonskontekst(KontekstType.Vedtaksperiode, vedtaksperiodeId.toString())) + hendelseIder, periode)

    fun medInntektsmelding(inntektsmeldingId: UUID) = kopierMedKontekst(listOf(Subsumsjonskontekst(KontekstType.Inntektsmelding, inntektsmeldingId.toString())))

    private fun kopierMedKontekst(kontekster: List<Subsumsjonskontekst>, periode: ClosedRange<LocalDate>? = null) =
        MaskinellJurist(this, this.kontekster + kontekster, periode)

    override fun `§ 8-15`(
        skjæringstidspunkt: LocalDate,
        organisasjonsnummer: String,
        inntekterSisteTreMåneder: List<Inntektsubsumsjon>,
        forklaring: String,
        oppfylt: Boolean
    ) {
        leggTil(
            Subsumsjon.enkelSubsumsjon(
                utfall = if (oppfylt) VILKAR_OPPFYLT else VILKAR_IKKE_OPPFYLT,
                lovverk = "folketrygdloven",
                versjon = LocalDate.of(1998, 12, 18),
                paragraf = PARAGRAF_8_15,
                ledd = null,
                punktum = null,
                bokstav = null,
                input = mapOf(
                    "organisasjonsnummer" to organisasjonsnummer,
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "inntekterSisteTreMåneder" to inntekterSisteTreMåneder.subsumsjonsformat(),
                    "forklaring" to forklaring
                ),
                kontekster = kontekster,
                output = if (oppfylt) {
                    mapOf("arbeidsforholdAvbrutt" to organisasjonsnummer)
                } else {
                    mapOf("aktivtArbeidsforhold" to organisasjonsnummer)
                }
            )
        )
    }

    override fun `§ 8-16 ledd 1`(dato: Collection<ClosedRange<LocalDate>>, dekningsgrad: Double, inntekt: Double, dekningsgrunnlag: Double) {
        if (dato.isEmpty()) return
        leggTil(
            Subsumsjon.periodisertSubsumsjon(
                perioder = dato,
                lovverk = "folketrygdloven",
                input = mapOf("dekningsgrad" to dekningsgrad, "inntekt" to inntekt),
                output = mapOf("dekningsgrunnlag" to dekningsgrunnlag),
                utfall = VILKAR_BEREGNET,
                paragraf = PARAGRAF_8_16,
                ledd = 1.ledd,
                versjon = FOLKETRYGDLOVENS_OPPRINNELSESDATO,
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-17 ledd 1 bokstav a`(
        oppfylt: Boolean,
        dagen: Collection<ClosedRange<LocalDate>>,
        sykdomstidslinje: List<Tidslinjedag>
    ) {
        if (dagen.isEmpty()) return
        leggTil(
            Subsumsjon.periodisertSubsumsjon(
                perioder = dagen,
                utfall = if (oppfylt) VILKAR_OPPFYLT else VILKAR_IKKE_OPPFYLT,
                lovverk = "folketrygdloven",
                versjon = LocalDate.of(2018, 1, 1),
                paragraf = PARAGRAF_8_17,
                ledd = 1.ledd,
                bokstav = BOKSTAV_A,
                input = mapOf("sykdomstidslinje" to sykdomstidslinje.dager(periode())),
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-17 ledd 1`(dato: Collection<ClosedRange<LocalDate>>) {
        if (dato.isEmpty()) return
        leggTil(
            Subsumsjon.periodisertSubsumsjon(
                perioder = dato,
                lovverk = "folketrygdloven",
                versjon = LocalDate.of(2018, 1, 1),
                utfall = VILKAR_OPPFYLT,
                paragraf = PARAGRAF_8_17,
                ledd = LEDD_1,
                input = emptyMap(),
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-17 ledd 1 bokstav a - arbeidsgiversøknad`(
        periode: ClosedRange<LocalDate>,
        sykdomstidslinje: List<Tidslinjedag>
    ) {
        `§ 8-17 ledd 1 bokstav a`(false, listOf(periode), sykdomstidslinje)
    }

    override fun `§ 8-17 ledd 2`(dato: Collection<ClosedRange<LocalDate>>, sykdomstidslinje: List<Tidslinjedag>) {
        if (dato.isEmpty()) return
        leggTil(
            Subsumsjon.periodisertSubsumsjon(
                perioder = dato,
                lovverk = "folketrygdloven",
                versjon = LocalDate.of(2018, 1, 1),
                utfall = VILKAR_IKKE_OPPFYLT,
                paragraf = PARAGRAF_8_17,
                ledd = LEDD_2,
                input = mapOf(
                    "beregnetTidslinje" to sykdomstidslinje.dager(periode())
                ),
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-19 første ledd`(dato: LocalDate, beregnetTidslinje: List<Tidslinjedag>) {
        leggTil(
            Subsumsjon.enkelSubsumsjon(
                utfall = VILKAR_BEREGNET,
                lovverk = "folketrygdloven",
                versjon = LocalDate.of(2001, 1, 1),
                paragraf = PARAGRAF_8_19,
                ledd = 1.ledd,
                input = mapOf(
                    "beregnetTidslinje" to beregnetTidslinje.dager()
                ),
                output = mapOf(
                    "sisteDagIArbeidsgiverperioden" to dato
                ),
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-19 andre ledd`(dato: Collection<ClosedRange<LocalDate>>, beregnetTidslinje: List<Tidslinjedag>) {
        if (dato.isEmpty()) return
        leggTil(
            Subsumsjon.periodisertSubsumsjon(
                perioder = dato,
                lovverk = "folketrygdloven",
                utfall = VILKAR_BEREGNET,
                versjon = LocalDate.of(2001, 1, 1),
                paragraf = PARAGRAF_8_19,
                ledd = 2.ledd,
                input = mapOf(
                    "beregnetTidslinje" to beregnetTidslinje.dager()
                ),
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-19 tredje ledd`(dato: Collection<LocalDate>, beregnetTidslinje: List<Tidslinjedag>) {
        if (dato.isEmpty()) return
        leggTil(
            Subsumsjon.periodisertSubsumsjon(
                perioder = dato.map { it..it },
                lovverk = "folketrygdloven",
                utfall = VILKAR_BEREGNET,
                versjon = LocalDate.of(2001, 1, 1),
                paragraf = PARAGRAF_8_19,
                ledd = 3.ledd,
                input = mapOf(
                    "beregnetTidslinje" to beregnetTidslinje.dager()
                ),
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-19 fjerde ledd`(dato: LocalDate, beregnetTidslinje: List<Tidslinjedag>) {
        leggTil(
            Subsumsjon.periodisertSubsumsjon(
                perioder = listOf(dato.rangeTo(dato)),
                lovverk = "folketrygdloven",
                utfall = VILKAR_BEREGNET,
                versjon = LocalDate.of(2001, 1, 1),
                paragraf = PARAGRAF_8_19,
                ledd = 4.ledd,
                input = mapOf(
                    "beregnetTidslinje" to beregnetTidslinje.dager()
                ),
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-28 ledd 3 bokstav a`(
        organisasjonsnummer: String,
        inntekterSisteTreMåneder: List<Inntektsubsumsjon>,
        grunnlagForSykepengegrunnlagÅrlig: Double,
        grunnlagForSykepengegrunnlagMånedlig: Double,
        skjæringstidspunkt: LocalDate
    ) {
        leggTil(
            Subsumsjon.enkelSubsumsjon(
                utfall = VILKAR_BEREGNET,
                lovverk = "folketrygdloven",
                versjon = LocalDate.of(2019, 1, 1),
                paragraf = PARAGRAF_8_28,
                ledd = LEDD_3,
                bokstav = BOKSTAV_A,
                input = mapOf(
                    "organisasjonsnummer" to organisasjonsnummer,
                    "inntekterSisteTreMåneder" to inntekterSisteTreMåneder.subsumsjonsformat(),
                    "skjæringstidspunkt" to skjæringstidspunkt
                ),
                output = mapOf(
                    "beregnetGrunnlagForSykepengegrunnlagPrÅr" to grunnlagForSykepengegrunnlagÅrlig,
                    "beregnetGrunnlagForSykepengegrunnlagPrMåned" to grunnlagForSykepengegrunnlagMånedlig
                ),
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-28 ledd 3 bokstav b`(
        organisasjonsnummer: String,
        startdatoArbeidsforhold: LocalDate,
        overstyrtInntektFraSaksbehandler: Map<String, Any>,
        skjæringstidspunkt: LocalDate,
        forklaring: String,
        grunnlagForSykepengegrunnlagÅrlig: Double,
        grunnlagForSykepengegrunnlagMånedlig: Double
    ) {
        leggTil(
            Subsumsjon.enkelSubsumsjon(
                utfall = VILKAR_BEREGNET,
                lovverk = "folketrygdloven",
                versjon = LocalDate.of(2019, 1, 1),
                paragraf = PARAGRAF_8_28,
                ledd = LEDD_3,
                bokstav = BOKSTAV_B,
                input = mapOf(
                    "organisasjonsnummer" to organisasjonsnummer,
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "startdatoArbeidsforhold" to startdatoArbeidsforhold,
                    "overstyrtInntektFraSaksbehandler" to overstyrtInntektFraSaksbehandler,
                    "forklaring" to forklaring
                ),
                output = mapOf(
                    "beregnetGrunnlagForSykepengegrunnlagPrÅr" to grunnlagForSykepengegrunnlagÅrlig,
                    "beregnetGrunnlagForSykepengegrunnlagPrMåned" to grunnlagForSykepengegrunnlagMånedlig
                ),
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-28 ledd 3 bokstav c`(
        organisasjonsnummer: String,
        overstyrtInntektFraSaksbehandler: Map<String, Any>,
        skjæringstidspunkt: LocalDate,
        forklaring: String,
        grunnlagForSykepengegrunnlagÅrlig: Double,
        grunnlagForSykepengegrunnlagMånedlig: Double
    ) {
        leggTil(
            Subsumsjon.enkelSubsumsjon(
                utfall = VILKAR_BEREGNET,
                lovverk = "folketrygdloven",
                versjon = LocalDate.of(2019, 1, 1),
                paragraf = PARAGRAF_8_28,
                ledd = LEDD_3,
                bokstav = BOKSTAV_C,
                input = mapOf(
                    "organisasjonsnummer" to organisasjonsnummer,
                    "overstyrtInntektFraSaksbehandler" to overstyrtInntektFraSaksbehandler,
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "forklaring" to forklaring
                ),
                output = mapOf(
                    "beregnetGrunnlagForSykepengegrunnlagPrÅr" to grunnlagForSykepengegrunnlagÅrlig,
                    "beregnetGrunnlagForSykepengegrunnlagPrMåned" to grunnlagForSykepengegrunnlagMånedlig
                ),
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-28 ledd 5`(
        organisasjonsnummer: String,
        overstyrtInntektFraSaksbehandler: Map<String, Any>,
        skjæringstidspunkt: LocalDate,
        forklaring: String,
        grunnlagForSykepengegrunnlagÅrlig: Double,
        grunnlagForSykepengegrunnlagMånedlig: Double,
    ) {
        leggTil(
            Subsumsjon.enkelSubsumsjon(
                utfall = VILKAR_BEREGNET,
                lovverk = "folketrygdloven",
                versjon = LocalDate.of(2019, 1, 1),
                paragraf = PARAGRAF_8_28,
                ledd = LEDD_5,
                input = mapOf(
                    "organisasjonsnummer" to organisasjonsnummer,
                    "overstyrtInntektFraSaksbehandler" to overstyrtInntektFraSaksbehandler,
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "forklaring" to forklaring
                ),
                output = mapOf(
                    "beregnetGrunnlagForSykepengegrunnlagPrÅr" to grunnlagForSykepengegrunnlagÅrlig,
                    "beregnetGrunnlagForSykepengegrunnlagPrMåned" to grunnlagForSykepengegrunnlagMånedlig
                ),
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-29`(
        skjæringstidspunkt: LocalDate,
        grunnlagForSykepengegrunnlagÅrlig: Double,
        inntektsopplysninger: List<Inntektsubsumsjon>,
        organisasjonsnummer: String
    ) {
        leggTil(
            Subsumsjon.enkelSubsumsjon(
                utfall = VILKAR_BEREGNET,
                lovverk = "folketrygdloven",
                versjon = LocalDate.of(2019, 1, 1),
                paragraf = PARAGRAF_8_29,
                ledd = null,
                input = mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "organisasjonsnummer" to organisasjonsnummer,
                    "inntektsopplysninger" to inntektsopplysninger.subsumsjonsformat()
                ),
                output = mapOf(
                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlagÅrlig
                ),
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-33 ledd 1`() {
        super.`§ 8-33 ledd 1`()
    }

    override fun `§ 8-33 ledd 3`(grunnlagForFeriepenger: Int, opptjeningsår: Year, prosentsats: Double, alder: Int, feriepenger: Double) {
        super.`§ 8-33 ledd 3`(grunnlagForFeriepenger, opptjeningsår, prosentsats, alder, feriepenger)
    }

    override fun `§ 8-51 ledd 2`(
        oppfylt: Boolean,
        skjæringstidspunkt: LocalDate,
        alderPåSkjæringstidspunkt: Int,
        beregningsgrunnlagÅrlig: Double,
        minimumInntektÅrlig: Double
    ) {
        leggTil(
            Subsumsjon.enkelSubsumsjon(
                utfall = if (oppfylt) VILKAR_OPPFYLT else VILKAR_IKKE_OPPFYLT,
                lovverk = "folketrygdloven",
                versjon = LocalDate.of(2011, 12, 16),
                paragraf = PARAGRAF_8_51,
                ledd = LEDD_2,
                input = mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "alderPåSkjæringstidspunkt" to alderPåSkjæringstidspunkt,
                    "grunnlagForSykepengegrunnlag" to beregningsgrunnlagÅrlig,
                    "minimumInntekt" to minimumInntektÅrlig
                ),
                output = emptyMap(),
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-51 ledd 2`(
        oppfylt: Boolean,
        utfallFom: LocalDate,
        utfallTom: LocalDate,
        periodeFom: LocalDate,
        periodeTom: LocalDate,
        sekstisyvårsdag: LocalDate,
        beregningsgrunnlagÅrlig: Double,
        minimumInntektÅrlig: Double
    ) {
        leggTil(
            Subsumsjon.enkelSubsumsjon(
                utfall = if (oppfylt) VILKAR_OPPFYLT else VILKAR_IKKE_OPPFYLT,
                lovverk = "folketrygdloven",
                versjon = LocalDate.of(2011, 12, 16),
                paragraf = PARAGRAF_8_51,
                ledd = LEDD_2,
                input = mapOf(
                    "sekstisyvårsdag" to sekstisyvårsdag,
                    "utfallFom" to utfallFom,
                    "utfallTom" to utfallTom,
                    "periodeFom" to periodeFom,
                    "periodeTom" to periodeTom,
                    "grunnlagForSykepengegrunnlag" to beregningsgrunnlagÅrlig,
                    "minimumInntekt" to minimumInntektÅrlig
                ),
                output = emptyMap(),
                kontekster = kontekster
            )
        )
    }

    override fun `§ 8-51 ledd 3`(
        periode: ClosedRange<LocalDate>,
        tidslinjegrunnlag: List<List<Tidslinjedag>>,
        beregnetTidslinje: List<Tidslinjedag>,
        gjenståendeSykedager: Int,
        forbrukteSykedager: Int,
        maksdato: LocalDate,
        startdatoSykepengerettighet: LocalDate?
    ) {
        if (startdatoSykepengerettighet == null) return
        val iterator = RangeIterator(periode).subsetFom(startdatoSykepengerettighet)
        val (dagerOppfylt, dagerIkkeOppfylt) = iterator.asSequence().partition { it <= maksdato }

        fun logg(utfall: Utfall, utfallFom: LocalDate, utfallTom: LocalDate) {
            leggTil(
                Subsumsjon.enkelSubsumsjon(
                    utfall = utfall,
                    versjon = LocalDate.of(2011, 12, 16),
                    lovverk = "folketrygdloven",
                    paragraf = PARAGRAF_8_51,
                    ledd = LEDD_3,
                    input = mapOf(
                        "fom" to periode.start,
                        "tom" to periode.endInclusive,
                        "utfallFom" to utfallFom,
                        "utfallTom" to utfallTom,
                        "tidslinjegrunnlag" to tidslinjegrunnlag.map { it.dager() },
                        "beregnetTidslinje" to beregnetTidslinje.dager()
                    ),
                    output = mapOf(
                        "gjenståendeSykedager" to gjenståendeSykedager,
                        "forbrukteSykedager" to forbrukteSykedager,
                        "maksdato" to maksdato,
                    ),
                    kontekster = kontekster
                )
            )
        }
        if (dagerOppfylt.isNotEmpty()) logg(VILKAR_OPPFYLT, dagerOppfylt.first(), dagerOppfylt.last())
        if (dagerIkkeOppfylt.isNotEmpty()) logg(VILKAR_IKKE_OPPFYLT, dagerIkkeOppfylt.first(), dagerIkkeOppfylt.last())
    }

    override fun `§ 22-13 ledd 3`(avskjæringsdato: LocalDate, perioder: Collection<ClosedRange<LocalDate>>) {
        if (perioder.isEmpty()) return
        leggTil(Subsumsjon.periodisertSubsumsjon(
            perioder = perioder,
            utfall = VILKAR_IKKE_OPPFYLT,
            lovverk = "folketrygdloven",
            versjon = LocalDate.of(2011, 12, 16),
            paragraf = Paragraf.PARAGRAF_22_13,
            ledd = LEDD_3,
            input = mapOf(
                "avskjæringsdato" to avskjæringsdato
            ),
            kontekster = kontekster
        ))
    }

    override fun `fvl § 35 ledd 1`() {
        leggTil(Subsumsjon.enkelSubsumsjon(
            utfall = VILKAR_OPPFYLT,
            lovverk = "forvaltningsloven",
            versjon = LocalDate.of(2021, 6, 1),
            paragraf = Paragraf.PARAGRAF_35,
            ledd = LEDD_1,
            input = mapOf(
                "stadfesting" to true
            ),
            output = emptyMap(),
            kontekster = kontekster
        ))
    }

    override fun `§ 8-48 ledd 2 punktum 2`(dato: Collection<ClosedRange<LocalDate>>, sykdomstidslinje: List<Tidslinjedag>) {
        if (dato.isEmpty()) return
        leggTil(Subsumsjon.periodisertSubsumsjon(
            perioder = dato,
            utfall = VILKAR_IKKE_OPPFYLT,
            lovverk = "folketrygdloven",
            versjon = LocalDate.parse("2021-05-21"),
            paragraf = PARAGRAF_8_48,
            ledd = LEDD_2,
            punktum = Punktum.PUNKTUM_2,
            input = mapOf("sykdomstidslinje" to sykdomstidslinje.dager(periode())),
            kontekster = kontekster
        ))
    }

    override fun `Trygderettens kjennelse 2006-4023`(dato: Collection<ClosedRange<LocalDate>>, sykdomstidslinje: List<Tidslinjedag>) {
        if (dato.isEmpty()) return
        leggTil(Subsumsjon.periodisertSubsumsjon(
            perioder = dato,
            utfall = VILKAR_IKKE_OPPFYLT,
            lovverk = "trygderetten",
            versjon = LocalDate.parse("2007-03-02"),
            paragraf = KJENNELSE_2006_4023,
            ledd = null,
            input = mapOf("sykdomstidslinje" to sykdomstidslinje.dager(periode())),
            kontekster = kontekster
        ))
    }

    fun subsumsjoner() = subsumsjoner.toList()

    fun events() = subsumsjoner.map { subsumsjon ->
        SubsumsjonEvent(
            sporing = subsumsjon.kontekster
                .filterNot { it.type == KontekstType.Fødselsnummer }
                .groupBy({ it.type }) { it.verdi },
            lovverk = subsumsjon.lovverk,
            ikrafttredelse = paragrafVersjonFormaterer.format(subsumsjon.versjon),
            paragraf = subsumsjon.paragraf.ref,
            ledd = subsumsjon.ledd?.nummer,
            punktum = subsumsjon.punktum?.nummer,
            bokstav = subsumsjon.bokstav?.ref,
            input = subsumsjon.input,
            output = subsumsjon.output,
            utfall = subsumsjon.utfall.name
        )
    }

    data class SubsumsjonEvent(
        val id: UUID = UUID.randomUUID(),
        val sporing: Map<KontekstType, List<String>>,
        val lovverk: String,
        val ikrafttredelse: String,
        val paragraf: String,
        val ledd: Int?,
        val punktum: Int?,
        val bokstav: Char?,
        val input: Map<String, Any>,
        val output: Map<String, Any>,
        val utfall: String,
    ) {

        companion object {
            val paragrafVersjonFormaterer = DateTimeFormatter.ISO_DATE
        }
    }
}

internal class RangeIterator(start: LocalDate, private val end: LocalDate): Iterator<LocalDate> {
    private var currentDate = start
    constructor(range: ClosedRange<LocalDate>) : this(range.start, range.endInclusive)
    fun subsetFom(fom: LocalDate) = apply {
        currentDate = maxOf(currentDate, fom)
    }
    override fun hasNext() = end >= currentDate
    override fun next(): LocalDate {
        check(hasNext())
        return currentDate.also {
            currentDate = it.plusDays(1)
        }
    }

    internal companion object {
        // forutsetter at <other> er sortert
        fun Collection<ClosedRange<LocalDate>>.trim(other: ClosedRange<LocalDate>): Collection<ClosedRange<LocalDate>> {
            return fold(listOf(other)) { result, trimperiode ->
                result.dropLast(1) + (result.lastOrNull()?.trim(trimperiode) ?: emptyList())
            }
        }

        private fun ClosedRange<LocalDate>.trim(periodeSomSkalTrimmesBort: ClosedRange<LocalDate>): Collection<ClosedRange<LocalDate>> {
            // fullstendig overlapp
            if (periodeSomSkalTrimmesBort.start <= this.start && periodeSomSkalTrimmesBort.endInclusive >= this.endInclusive) return emptyList()
            // <periodeSomSkalTrimmesBort> kan nå enten trimme bort hale, snuten eller midten av <this>. i sistnevnte
            // situasjon så vil resultatet være to perioder.
            val result = mutableListOf<ClosedRange<LocalDate>>()
            if (this.start < periodeSomSkalTrimmesBort.start) result.add(this.start..periodeSomSkalTrimmesBort.start.minusDays(1))
            if (this.endInclusive > periodeSomSkalTrimmesBort.endInclusive) result.add(periodeSomSkalTrimmesBort.endInclusive.plusDays(1)..this.endInclusive)
            return result
        }
        // utvider perioden hvis <dato> ligger inntil, foran eller bak, (inkl. helg)
        fun ClosedRange<LocalDate>.utvide(dato: LocalDate): ClosedRange<LocalDate>? {
            if (dato in this) return this
            val dagen = dato.dayOfWeek
            return when {
                (endInclusive == dato.minusDays(1)) ||
                (endInclusive in dato.minusDays(2)..dato.minusDays(1) && dagen == DayOfWeek.SUNDAY) ||
                (endInclusive in dato.minusDays(3)..dato.minusDays(1) && dagen == DayOfWeek.MONDAY) ->
                    start.rangeTo(dato)
                (start == dato.plusDays(1)) ||
                (start in dato.plusDays(1)..dato.plusDays(2) && dagen == DayOfWeek.SATURDAY) ||
                (start in dato.plusDays(1)..dato.plusDays(3) && dagen == DayOfWeek.FRIDAY) ->
                    dato.rangeTo(endInclusive)
                else -> null
            }
        }
        fun Collection<ClosedRange<LocalDate>>.merge(): Collection<ClosedRange<LocalDate>> {
            if (this.size <= 1) return this
            return this
                .sortedBy { it.start }
                .fold(emptyList()) { result, range ->
                    val ny = result.lastOrNull()?.utvide(range.start)
                    if (result.isEmpty()) listOf(range)
                    else if (ny != null) result.dropLast(1).plusElement(ny)
                    else result.plusElement(range)
                }
        }
        fun ClosedRange<LocalDate>.iterator(): Iterator<LocalDate> = RangeIterator(this)
        fun ClosedRange<LocalDate>.forEach(block: (LocalDate) -> Unit) {
            iterator().forEach(block)
        }
    }
}