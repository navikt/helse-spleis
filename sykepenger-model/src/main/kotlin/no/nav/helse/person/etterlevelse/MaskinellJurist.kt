package no.nav.helse.person.etterlevelse

import no.nav.helse.person.KontekstObserver
import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum.Companion.punktum
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import java.time.LocalDate
import java.time.Year

class MaskinellJurist : EtterlevelseObserver, KontekstObserver {
    private lateinit var fødselsnummer: String
    private lateinit var aktørId: String
    private lateinit var organisasjonsnummer: String
    private var vedtaksperiodeId: String? = null
    private var utbetalingId: String? = null

    private var vurderinger = listOf<JuridiskVurdering>()

    private fun leggTil(vurdering: JuridiskVurdering) {
        vurderinger = vurdering.sammenstill(vurderinger)
    }

    private fun kontekster(): Map<String, String> = mutableMapOf(
        "fødselsnummer" to fødselsnummer,
        "aktørId" to aktørId,
        "organisasjonsnummer" to organisasjonsnummer
    ).apply {
        compute("vedtaksperiodeId") { _, _ -> vedtaksperiodeId }
        compute("utbetalingId") { _, _ -> utbetalingId }
    }

    override fun nyPersonKontekst(fødselsnummer: String, aktørId: String) {
        this.fødselsnummer = fødselsnummer
        this.aktørId = aktørId
    }

    override fun nyVedtaksperiodeKontekst(vedtaksperiodeId: String) {
        this.vedtaksperiodeId = vedtaksperiodeId
    }

    override fun nyArbeidsgiverKontekst(organisasjonsnummer: String) {
        this.organisasjonsnummer = organisasjonsnummer
    }

    override fun nyUtbetalingKontekst(utbetalingId: String) {
        this.utbetalingId = utbetalingId
    }

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
            EnkelVurdering(
                oppfylt = oppfylt,
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
        vurderingFom: LocalDate,
        vurderingTom: LocalDate,
        tidslinjeFom: LocalDate,
        tidslinjeTom: LocalDate,
        avvisteDager: List<LocalDate>
    ) {
        super.`§8-3 ledd 1 punktum 2`(oppfylt, syttiårsdagen, vurderingFom, vurderingTom, tidslinjeFom, tidslinjeTom, avvisteDager)
    }

    override fun `§8-3 ledd 2 punktum 1`(oppfylt: Boolean, skjæringstidspunkt: LocalDate, grunnlagForSykepengegrunnlag: Inntekt, minimumInntekt: Inntekt) {
        super.`§8-3 ledd 2 punktum 1`(oppfylt, skjæringstidspunkt, grunnlagForSykepengegrunnlag, minimumInntekt)
    }

    override fun `§8-10 ledd 2 punktum 1`(
        oppfylt: Boolean,
        funnetRelevant: Boolean,
        maksimaltSykepengegrunnlag: Inntekt,
        skjæringstidspunkt: LocalDate,
        grunnlagForSykepengegrunnlag: Inntekt
    ) {
        leggTil(
            BetingetVurdering(
                funnetRelevant = funnetRelevant,
                oppfylt = oppfylt,
                LocalDate.of(2020, 1, 1),
                Paragraf.PARAGRAF_8_10,
                2.ledd,
                1.punktum,
                input = mapOf(
                    "maksimaltSykepengegrunnlag" to maksimaltSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                    "skjæringstidspunkt" to skjæringstidspunkt,
                    "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig }
                ),
                output = mapOf("funnetRelevant" to funnetRelevant),
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
        gjenståendeSykedager: Int,
        forbrukteSykedager: Int,
        maksdato: LocalDate,
        avvisteDager: List<LocalDate>
    ) {
        super.`§8-12 ledd 1 punktum 1`(oppfylt, fom, tom, gjenståendeSykedager, forbrukteSykedager, maksdato, avvisteDager)
    }

    override fun `§8-12 ledd 2`(dato: LocalDate, tilstrekkeligOppholdISykedager: Int) {
        super.`§8-12 ledd 2`(dato, tilstrekkeligOppholdISykedager)
    }

    override fun `§8-13 ledd 1`(oppfylt: Boolean, avvisteDager: List<LocalDate>) {
        super.`§8-13 ledd 1`(oppfylt, avvisteDager)
    }

    override fun `§8-16 ledd 1`(dato: LocalDate, dekningsgrad: Double, inntekt: Double, dekningsgrunnlag: Double) {
        leggTil(
            GrupperbarVurdering(
                dato = dato,
                input = mapOf("dekningsgrad" to dekningsgrad, "inntekt" to inntekt),
                output = mapOf("dekningsgrunnlag" to dekningsgrunnlag),
                oppfylt = true,
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

    override fun vurderinger() = vurderinger.toList()
}
