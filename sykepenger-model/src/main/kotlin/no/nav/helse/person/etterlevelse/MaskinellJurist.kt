package no.nav.helse.person.etterlevelse

import no.nav.helse.Fødselsnummer
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum.Companion.punktum
import no.nav.helse.person.etterlevelse.Subsumsjon.Utfall.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import java.time.LocalDate
import java.time.Year
import java.util.*

class MaskinellJurist private constructor(
    private val parent: MaskinellJurist?,
    private val kontekster: Map<String, String>
) : SubsumsjonObserver {

    private var subsumsjoner = listOf<Subsumsjon>()

    constructor(): this(null, emptyMap())

    private fun leggTil(subsumsjon: Subsumsjon) {
        subsumsjoner = subsumsjon.sammenstill(subsumsjoner)
        parent?.leggTil(subsumsjon)
    }

    private fun kontekster(): Map<String, String> = this.kontekster.toMap()

    fun medFødselsnummer(fødselsnummer: Fødselsnummer) = kopierMedKontekst(mapOf("fødselsnummer" to fødselsnummer.toString()))
    fun medOrganisasjonsnummer(organisasjonsnummer: String) = kopierMedKontekst(mapOf("organisasjonsnummer" to organisasjonsnummer))
    fun medVedtaksperiode(vedtaksperiodeId: UUID, hendelseIder: List<UUID>) = kopierMedKontekst(mapOf("vedtaksperiodeId" to vedtaksperiodeId.toString()))
    private fun kopierMedKontekst(kontekster: Map<String, String>) = MaskinellJurist(this, this.kontekster + kontekster)

    override fun `§2`(oppfylt: Boolean) {
        super.`§2`(oppfylt)
    }

    override fun `§8-2 ledd 1`(
        oppfylt: Boolean,
        skjæringstidspunkt: LocalDate,
        tilstrekkeligAntallOpptjeningsdager: Int,
        arbeidsforhold: List<Map<String, Any?>>,
        antallOpptjeningsdager: Int
    ) {
        leggTil(
            EnkelSubsumsjon(
                utfall = if (oppfylt) VILKAR_OPPFYLT else VILKAR_IKKE_OPPFYLT,
                versjon = LocalDate.of(2020, 6, 12),
                paragraf = Paragraf.PARAGRAF_8_2,
                ledd = 1.ledd,
                input = mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "tilstrekkeligAntallOpptjeningsdager" to tilstrekkeligAntallOpptjeningsdager,
                    "arbeidsforhold" to arbeidsforhold
                ),
                output = mapOf("antallOpptjeningsdager" to antallOpptjeningsdager),
                kontekster = kontekster()
            )
        )
    }

    override fun `§8-3 ledd 1 punktum 2`(
        oppfylt: Boolean,
        syttiårsdagen: LocalDate,
        utfallFom: LocalDate,
        utfallTom: LocalDate,
        tidslinjeFom: LocalDate,
        tidslinjeTom: LocalDate,
        avvisteDager: List<LocalDate>
    ) {
        leggTil(
            EnkelSubsumsjon(
                utfall = if (oppfylt) VILKAR_OPPFYLT else VILKAR_IKKE_OPPFYLT,
                versjon = LocalDate.of(2011, 12, 16),
                paragraf = Paragraf.PARAGRAF_8_3,
                ledd = 1.ledd,
                punktum = 2.punktum,
                input = mapOf(
                    "syttiårsdagen" to syttiårsdagen,
                    "utfallFom" to utfallFom,
                    "utfallTom" to utfallTom,
                    "tidslinjeFom" to tidslinjeFom,
                    "tidslinjeTom" to tidslinjeTom
                ),
                output = mapOf("avvisteDager" to avvisteDager.grupperSammenhengendePerioder()),
                kontekster = kontekster()
            )
        )
    }

    override fun `§8-3 ledd 2 punktum 1`(oppfylt: Boolean, skjæringstidspunkt: LocalDate, grunnlagForSykepengegrunnlag: Inntekt, minimumInntekt: Inntekt) {
        leggTil(
            EnkelSubsumsjon(
                utfall = if (oppfylt) VILKAR_OPPFYLT else VILKAR_IKKE_OPPFYLT,
                versjon = LocalDate.of(2011, 12, 16),
                paragraf = Paragraf.PARAGRAF_8_3,
                ledd = 2.ledd,
                punktum = 1.punktum,
                input = mapOf(
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                    "minimumInntekt" to minimumInntekt.reflection { årlig, _, _, _ -> årlig }
                ),
                output = emptyMap(),
                kontekster = kontekster()
            )
        )
    }

    override fun `§8-10 ledd 2 punktum 1`(
        funnetRelevant: Boolean,
        maksimaltSykepengegrunnlag: Inntekt,
        skjæringstidspunkt: LocalDate,
        grunnlagForSykepengegrunnlag: Inntekt
    ) {
        leggTil(
            BetingetSubsumsjon(
                funnetRelevant = funnetRelevant,
                utfall = VILKAR_BEREGNET,
                LocalDate.of(2020, 1, 1),
                Paragraf.PARAGRAF_8_10,
                2.ledd,
                1.punktum,
                input = mapOf(
                    "maksimaltSykepengegrunnlag" to maksimaltSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig }
                ),
                output = mapOf(),
                kontekster = kontekster()
            )
        )
    }

    override fun `§8-10 ledd 3`(oppfylt: Boolean) {
        super.`§8-10 ledd 3`(oppfylt)
    }

    override fun `§8-11 første ledd`() {
        super.`§8-11 første ledd`()
    }

    override fun `§8-12 ledd 1 punktum 1`(
        oppfylt: Boolean,
        fom: LocalDate,
        tom: LocalDate,
        tidslinjegrunnlag: List<List<Map<String, Any>>>,
        beregnetTidslinje: List<Map<String, Any>>,
        gjenståendeSykedager: Int,
        forbrukteSykedager: Int,
        maksdato: LocalDate,
        avvisteDager: List<LocalDate>
    ) {
        leggTil(
            EnkelSubsumsjon(
                utfall = if (oppfylt) VILKAR_OPPFYLT else VILKAR_IKKE_OPPFYLT,
                versjon = LocalDate.of(2021, 5, 21),
                paragraf = Paragraf.PARAGRAF_8_12,
                ledd = 1.ledd,
                punktum = 1.punktum,
                input = mapOf(
                    "fom" to fom,
                    "tom" to tom,
                    "tidslinjegrunnlag" to tidslinjegrunnlag,
                    "beregnetTidslinje" to beregnetTidslinje
                ),
                output = mapOf(
                    "gjenståendeSykedager" to gjenståendeSykedager,
                    "forbrukteSykedager" to forbrukteSykedager,
                    "maksdato" to maksdato,
                    "avvisteDager" to avvisteDager.grupperSammenhengendePerioder()
                ),
                kontekster = kontekster()
            )
        )
    }

    override fun `§8-12 ledd 2`(dato: LocalDate, tilstrekkeligOppholdISykedager: Int) {
        super.`§8-12 ledd 2`(dato, tilstrekkeligOppholdISykedager)
    }

    override fun `§8-13 ledd 1`(oppfylt: Boolean, avvisteDager: List<LocalDate>) {
        super.`§8-13 ledd 1`(oppfylt, avvisteDager)
    }

    override fun `§8-16 ledd 1`(dato: LocalDate, dekningsgrad: Double, inntekt: Double, dekningsgrunnlag: Double) {
        leggTil(
            GrupperbarSubsumsjon(
                dato = dato,
                input = mapOf("dekningsgrad" to dekningsgrad, "inntekt" to inntekt),
                output = mapOf("dekningsgrunnlag" to dekningsgrunnlag),
                utfall = VILKAR_BEREGNET,
                paragraf = Paragraf.PARAGRAF_8_16,
                ledd = 1.ledd,
                versjon = LocalDate.of(2020, 6, 12),
                kontekster = kontekster()
            )
        )
    }

    override fun `§8-17 ledd 1 bokstav a`(arbeidsgiverperiode: List<LocalDate>, førsteNavdag: LocalDate) {
        super.`§8-17 ledd 1 bokstav a`(arbeidsgiverperiode, førsteNavdag)
    }

    override fun `§8-17 ledd 2`(oppfylt: Boolean) {
        super.`§8-17 ledd 2`(oppfylt)
    }

    override fun `§8-28 ledd 3 bokstav a`(oppfylt: Boolean, grunnlagForSykepengegrunnlag: Inntekt) {
        super.`§8-28 ledd 3 bokstav a`(oppfylt, grunnlagForSykepengegrunnlag)
    }

    override fun `§8-30 ledd 1`(grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Inntekt>, grunnlagForSykepengegrunnlag: Inntekt) {
        super.`§8-30 ledd 1`(grunnlagForSykepengegrunnlagPerArbeidsgiver, grunnlagForSykepengegrunnlag)
    }

    override fun `§8-30 ledd 2 punktum 1`(
        oppfylt: Boolean,
        maksimaltTillattAvvikPåÅrsinntekt: Prosent,
        grunnlagForSykepengegrunnlag: Inntekt,
        sammenligningsgrunnlag: Inntekt,
        avvik: Prosent
    ) {
        super.`§8-30 ledd 2 punktum 1`(oppfylt, maksimaltTillattAvvikPåÅrsinntekt, grunnlagForSykepengegrunnlag, sammenligningsgrunnlag, avvik)
    }

    override fun `§8-33 ledd 1`() {
        super.`§8-33 ledd 1`()
    }

    override fun `§8-33 ledd 3`(grunnlagForFeriepenger: Int, opptjeningsår: Year, prosentsats: Double, alder: Int, feriepenger: Double) {
        super.`§8-33 ledd 3`(grunnlagForFeriepenger, opptjeningsår, prosentsats, alder, feriepenger)
    }

    override fun `§8-51 ledd 2`(oppfylt: Boolean, skjæringstidspunkt: LocalDate, grunnlagForSykepengegrunnlag: Inntekt, minimumInntekt: Inntekt) {
        super.`§8-51 ledd 2`(oppfylt, skjæringstidspunkt, grunnlagForSykepengegrunnlag, minimumInntekt)
    }

    override fun `§8-51 ledd 3`(oppfylt: Boolean, maksSykepengedagerOver67: Int, gjenståendeSykedager: Int, forbrukteSykedager: Int, maksdato: LocalDate) {
        super.`§8-51 ledd 3`(oppfylt, maksSykepengedagerOver67, gjenståendeSykedager, forbrukteSykedager, maksdato)
    }

    fun subsumsjoner() = subsumsjoner.toList()
}
