package no.nav.helse.spleis.e2e

import no.nav.helse.hendelser.Arbeidsforhold
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.person.*
import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Ledd.LEDD_1
import no.nav.helse.person.Ledd.LEDD_2
import no.nav.helse.person.Paragraf.*
import no.nav.helse.person.Punktum.Companion.punktum
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class EtterlevelseTest : AbstractEndToEndTest() {

    @Test
    fun `§8-2 ledd 1 - opptjeningstid tilfredstilt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser()
        val arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER, 4.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold)

        assertOppfylt(
            paragraf = PARAGRAF_8_2,
            ledd = LEDD_1,
            punktum = 1.punktum,
            versjon = 12.juni(2020),
            inputdata = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "tilstrekkeligAntallOpptjeningsdager" to 28,
                "arbeidsforhold" to listOf(
                    mapOf(
                        "orgnummer" to ORGNUMMER,
                        "fom" to 4.desember(2017),
                        "tom" to 31.januar
                    )
                )
            ),
            outputdata = mapOf("antallOpptjeningsdager" to 28)
        )
    }

    @Test
    fun `§8-2 ledd 1 - opptjeningstid ikke tilfredstilt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser()
        val arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER, 5.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold)

        assertIkkeOppfylt(
            paragraf = PARAGRAF_8_2,
            ledd = LEDD_1,
            punktum = 1.punktum,
            versjon = 12.juni(2020),
            inputdata = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "tilstrekkeligAntallOpptjeningsdager" to 28,
                "arbeidsforhold" to listOf(
                    mapOf(
                        "orgnummer" to ORGNUMMER,
                        "fom" to 5.desember(2017),
                        "tom" to 31.januar
                    )
                )
            ),
            outputdata = mapOf("antallOpptjeningsdager" to 27)
        )
    }

    @Test
    fun `§8-3 ledd 2 punktum 1 - har minimum inntekt halv G`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 46817.årlig)
        håndterYtelser()
        val arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER, 5.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold)

        assertOppfylt(
            paragraf = PARAGRAF_8_3,
            ledd = LEDD_2,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "grunnlagForSykepengegrunnlag" to 46817.0,
                "minimumInntekt" to 46817.0
            ),
            outputdata = emptyMap()
        )
        assertIkkeVurdert(PARAGRAF_8_51, LEDD_2, 1.punktum)
    }

    @Test
    fun `§8-3 ledd 2 punktum 1 - har inntekt mindre enn en halv G`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 46816.årlig)
        håndterYtelser()
        val arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER, 5.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold)

        assertIkkeOppfylt(
            paragraf = PARAGRAF_8_3,
            ledd = LEDD_2,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "grunnlagForSykepengegrunnlag" to 46816.0,
                "minimumInntekt" to 46817.0
            ),
            outputdata = emptyMap()
        )
        assertIkkeVurdert(PARAGRAF_8_51, LEDD_2, 1.punktum)
    }

    @Test
    fun `§8-10 ledd 2 punktum 1 - inntekt overstiger ikke maksimum sykepengegrunnlag`() {
        val maksimumSykepengegrunnlag2018 = (93634 * 6).årlig // 6G
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = maksimumSykepengegrunnlag2018)
        håndterYtelser()
        håndterVilkårsgrunnlag(inntekt = maksimumSykepengegrunnlag2018)
        assertOppfylt(
            paragraf = PARAGRAF_8_10,
            ledd = LEDD_2,
            punktum = 1.punktum,
            versjon = 1.januar(2020),
            inputdata = mapOf(
                "maksimaltSykepengegrunnlag" to 561804.0,
                "skjæringstidspunkt" to 1.januar,
                "grunnlagForSykepengegrunnlag" to 561804.0
            ),
            outputdata = mapOf("funnetRelevant" to false)
        )
    }

    @Test
    fun `§8-10 ledd 2 punktum 1 - inntekt overstiger maksimum sykepengegrunnlag`() {
        val maksimumSykepengegrunnlag2018 = (93634 * 6).årlig // 6G
        val inntekt = maksimumSykepengegrunnlag2018.plus(1.årlig)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt)
        håndterYtelser()
        håndterVilkårsgrunnlag(inntekt = inntekt)
        assertOppfylt(
            paragraf = PARAGRAF_8_10,
            ledd = LEDD_2,
            punktum = 1.punktum,
            versjon = 1.januar(2020),
            inputdata = mapOf(
                "maksimaltSykepengegrunnlag" to 561804.0,
                "skjæringstidspunkt" to 1.januar,
                "grunnlagForSykepengegrunnlag" to 561805.0
            ),
            outputdata = mapOf("funnetRelevant" to true)
        )
    }

    @Test
    fun `§8-10 ledd 2 punktum 1 - vurderes ikke ved overgang fra Infotrygd`() {
        val maksimumSykepengegrunnlag2018 = (93634 * 6).årlig // 6G
        val inntekt = maksimumSykepengegrunnlag2018.plus(1.årlig)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 1.januar, 31.januar, 100.prosent, inntekt)),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER, 1.januar, inntekt, true)
            )
        )
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()
        assertIkkeVurdert(paragraf = PARAGRAF_8_10, ledd = LEDD_2, punktum = 1.punktum)
    }

    @Test
    fun `§8-12 ledd 1 - Brukt færre enn 248 dager`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 50.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        assertOppfylt(
            paragraf = PARAGRAF_8_12,
            ledd = LEDD_1,
            punktum = 1.punktum,
            versjon = 21.mai(2021),
            inputdata = mapOf(
                "fom" to 19.januar,
                "tom" to 26.januar,
                "tidslinjegrunnlag" to listOf(listOf(mapOf("fom" to 19.januar, "tom" to 26.januar, "dagtype" to "NAVDAG")), emptyList()),
                "beregnetTidslinje" to listOf(mapOf("fom" to 19.januar, "tom" to 26.januar, "dagtype" to "NAVDAG"))
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 242,
                "forbrukteSykedager" to 6,
                "maksdato" to 1.januar(2019),
                "avvisteDager" to emptyList<Periode>()
            )
        )
    }

    @Test
    fun `§8-12 ledd 1 - Brukt flere enn 248 dager`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar(2018), 11.januar(2019), 50.prosent))
        håndterSøknadMedValidering(
            1.vedtaksperiode,
            Sykdom(3.januar(2018), 11.januar(2019), 50.prosent, 50.prosent),
            sendtTilNav = 3.januar(2018)
        )
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar(2018), 18.januar(2018))))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        assertOppfylt(
            resultatvelger = 1.resultat,
            paragraf = PARAGRAF_8_12,
            ledd = LEDD_1,
            punktum = 1.punktum,
            versjon = 21.mai(2021),
            inputdata = mapOf(
                "fom" to 19.januar,
                "tom" to 1.januar(2019),
                "tidslinjegrunnlag" to listOf(listOf(mapOf("fom" to 19.januar, "tom" to 11.januar(2019), "dagtype" to "NAVDAG")), emptyList()),
                "beregnetTidslinje" to listOf(mapOf("fom" to 19.januar, "tom" to 11.januar(2019), "dagtype" to "NAVDAG"))
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 0,
                "forbrukteSykedager" to 248,
                "maksdato" to 1.januar(2019),
                "avvisteDager" to emptyList<Periode>()
            )
        )

        assertIkkeOppfylt(
            resultatvelger = 2.resultat,
            paragraf = PARAGRAF_8_12,
            ledd = LEDD_1,
            punktum = 1.punktum,
            versjon = 21.mai(2021),
            inputdata = mapOf(
                "fom" to 2.januar(2019),
                "tom" to 11.januar(2019),
                "tidslinjegrunnlag" to listOf(listOf(mapOf("fom" to 19.januar, "tom" to 11.januar(2019), "dagtype" to "NAVDAG")), emptyList()),
                "beregnetTidslinje" to listOf(mapOf("fom" to 19.januar, "tom" to 11.januar(2019), "dagtype" to "NAVDAG"))
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 0,
                "forbrukteSykedager" to 248,
                "maksdato" to 1.januar(2019),
                "avvisteDager" to listOf(2.januar(2019) til 4.januar(2019), 7.januar(2019) til 11.januar(2019))
            )
        )
    }

    @Test
    fun `§8-12 ledd 2 - Bruker har vært arbeidsfør i 26 uker`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 50.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)


        håndterSykmelding(Sykmeldingsperiode(17.juli, 31.august, 50.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(17.juli, 31.august, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(17.juli, 1.august)))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)


        assertOppfylt(
            resultatvelger = 1.resultat,
            paragraf = PARAGRAF_8_12,
            ledd = LEDD_2,
            punktum = (1..2).punktum,
            versjon = 21.mai(2021),
            inputdata = mapOf(
                "dato" to 1.august,
                "tilstrekkeligOppholdISykedager" to 182, //26 uker * 7 dager
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG"),
                        mapOf("fom" to 2.august, "tom" to 31.august, "dagtype" to "NAVDAG")
                    ), emptyList()
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG"),
                    mapOf("fom" to 2.august, "tom" to 31.august, "dagtype" to "NAVDAG")
                )
            ),
            outputdata = emptyMap()
        )
    }

    @Test
    fun `§8-12 ledd 2 - Bruker har ikke vært arbeidsfør i 26 uker`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 50.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)


        håndterSykmelding(Sykmeldingsperiode(16.juli, 31.august, 50.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(16.juli, 31.august, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(16.juli, 31.juli)))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertIkkeVurdert(paragraf = PARAGRAF_8_12, ledd = LEDD_2)
    }

    @Test
    fun `§8-30 ledd 2 punktum 1 - under 25 prosent avvik`() {
        val beregnetInntekt = 31000.0
        val sammenligningsgrunnlag = 31000.0
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = beregnetInntekt.månedlig)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, sammenligningsgrunnlag.månedlig)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertOppfylt(
            PARAGRAF_8_30,
            2.ledd,
            1.punktum,
            LocalDate.of(2017, 4, 5),
            inputdata = mapOf(
                "maksimaltTillattAvvikPåÅrsinntekt" to 25.0,
                "grunnlagForSykepengegrunnlag" to beregnetInntekt * 12,
                "sammenligningsgrunnlag" to sammenligningsgrunnlag * 12
            ),
            outputdata = mapOf(
                "avvik" to 0.0
            )
        )
    }

    @Test
    fun `§8-30 ledd 2 punktum 1 - akkurat 25 prosent avvik`() {
        val beregnetInntekt = 38750.0
        val sammenligningsgrunnlag = 31000.0
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = beregnetInntekt.månedlig)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, sammenligningsgrunnlag.månedlig)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertOppfylt(
            PARAGRAF_8_30,
            2.ledd,
            1.punktum,
            LocalDate.of(2017, 4, 5),
            inputdata = mapOf(
                "maksimaltTillattAvvikPåÅrsinntekt" to 25.0,
                "grunnlagForSykepengegrunnlag" to beregnetInntekt * 12,
                "sammenligningsgrunnlag" to sammenligningsgrunnlag * 12
            ),
            outputdata = mapOf(
                "avvik" to 25.0
            )
        )
    }

    @Test
    fun `§8-30 ledd 2 punktum 1 - over 25 prosent avvik`() {
        val beregnetInntekt = 38781.0
        val sammenligningsgrunnlag = 31000.0
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = beregnetInntekt.månedlig)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, sammenligningsgrunnlag.månedlig)

        assertIkkeOppfylt(
            PARAGRAF_8_30,
            2.ledd,
            1.punktum,
            LocalDate.of(2017, 4, 5),
            inputdata = mapOf(
                "maksimaltTillattAvvikPåÅrsinntekt" to 25.0,
                "grunnlagForSykepengegrunnlag" to beregnetInntekt * 12,
                "sammenligningsgrunnlag" to sammenligningsgrunnlag * 12
            ),
            outputdata = mapOf(
                "avvik" to 25.1
            )
        )
    }

    @Test
    fun `§8-30 ledd 2 - gjør ikke ny juridisk vurdering ved sjekk av inntektsavvik i forlengelse av kort periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 1.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 1.januar, 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser()
        håndterVilkårsgrunnlag()
        håndterPåminnelse(1.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK, LocalDateTime.MIN)

        håndterSykmelding(Sykmeldingsperiode(2.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingReplay(inntektsmeldingId, 2.vedtaksperiode(ORGNUMMER))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertVurdert(PARAGRAF_8_30, LEDD_2, vedtaksperiodeId = 1.vedtaksperiode)
        assertIkkeVurdert(PARAGRAF_8_30, LEDD_2, vedtaksperiodeId = 2.vedtaksperiode)
    }

    @Test
    fun `§8-51 ledd 2 - har minimum inntekt 2G - over 67 år`() {
        val GAMMEL = "01014500065"
        createTestPerson(GAMMEL)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = GAMMEL)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), fnr = GAMMEL)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 187268.årlig, fnr = GAMMEL)
        håndterYtelser(fnr = GAMMEL)
        val arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER, 5.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold, fnr = GAMMEL)

        assertOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_2,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "grunnlagForSykepengegrunnlag" to 187268.0,
                "minimumInntekt" to 187268.0
            ),
            outputdata = emptyMap()
        )
        assertIkkeVurdert(PARAGRAF_8_3, ledd = LEDD_2, 1.punktum)
    }

    @Test
    fun `§8-51 ledd 2 - har inntekt mindre enn 2G - over 67 år`() {
        val GAMMEL = "01014500065"
        createTestPerson(GAMMEL)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = GAMMEL)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), fnr = GAMMEL)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 187267.årlig, fnr = GAMMEL)
        håndterYtelser(fnr = GAMMEL)
        val arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER, 5.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold, fnr = GAMMEL)

        assertIkkeOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_2,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "grunnlagForSykepengegrunnlag" to 187267.0,
                "minimumInntekt" to 187268.0
            ),
            outputdata = emptyMap()
        )
        assertIkkeVurdert(PARAGRAF_8_3, ledd = LEDD_2, 1.punktum)
    }

    private val Int.resultat: (List<Resultat>) -> Resultat get() = { it[this - 1] }

    private fun assertOppfylt(
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: List<Punktum>,
        versjon: LocalDate,
        inputdata: Map<Any, Any?>,
        outputdata: Map<Any, Any?>,
        resultatvelger: (List<Resultat>) -> Resultat = { it.single() },
        inspektør: EtterlevelseInspektør = EtterlevelseInspektør(person.aktivitetslogg)
    ) {
        val resultat = inspektør.resultat(paragraf, ledd, punktum).let(resultatvelger)
        assertTrue(resultat.oppfylt) { "Forventet oppfylt $paragraf $ledd $punktum" }
        assertResultat(versjon, inputdata, outputdata, resultat)
    }

    private fun assertIkkeOppfylt(
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: List<Punktum>,
        versjon: LocalDate,
        inputdata: Map<Any, Any?>,
        outputdata: Map<Any, Any?>,
        resultatvelger: (List<Resultat>) -> Resultat = { it.single() },
        inspektør: EtterlevelseInspektør = EtterlevelseInspektør(person.aktivitetslogg)
    ) {
        val resultat = inspektør.resultat(paragraf, ledd, punktum).let(resultatvelger)
        assertFalse(resultat.oppfylt) { "Forventet ikke oppfylt $paragraf $ledd $punktum" }
        assertResultat(versjon, inputdata, outputdata, resultat)
    }

    private fun assertVurdert(
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: List<Punktum>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = ORGNUMMER,
        inspektør: EtterlevelseInspektør = EtterlevelseInspektør(person.aktivitetslogg)
    ) {
        val resultat = inspektør.resultat(paragraf, ledd, punktum, vedtaksperiodeId?.invoke(organisasjonsnummer))
        assertTrue(resultat.isNotEmpty()) { "Forventet at $paragraf $ledd $punktum er vurdert" }
    }

    private fun assertIkkeVurdert(
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: List<Punktum>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: String = ORGNUMMER,
        inspektør: EtterlevelseInspektør = EtterlevelseInspektør(person.aktivitetslogg)
    ) {
        val resultat = inspektør.resultat(paragraf, ledd, punktum, vedtaksperiodeId?.invoke(organisasjonsnummer))
        assertTrue(resultat.isEmpty()) { "Forventet at $paragraf $ledd $punktum ikke er vurdert" }
    }

    private fun assertResultat(
        versjon: LocalDate,
        inputdata: Map<Any, Any?>,
        outputdata: Map<Any, Any?>,
        resultat: Resultat
    ) {
        assertEquals(versjon, resultat.versjon)
        assertEquals(inputdata, resultat.inputdata)
        assertEquals(outputdata, resultat.outputdata)
    }

    private class EtterlevelseInspektør(aktivitetslogg: Aktivitetslogg) : AktivitetsloggVisitor {
        private val resultater = mutableListOf<Resultat>()

        fun resultat(paragraf: Paragraf, ledd: Ledd?, punktum: List<Punktum>?, vedtaksperiodeId: UUID? = null) =
            resultater.filter { it.paragraf == paragraf && ledd?.equals(it.ledd) ?: true && punktum?.equals(it.punktum) ?: true && vedtaksperiodeId?.equals(it.vedtaksperiodeIdFraKontekst()) ?: true}

        init {
            aktivitetslogg.accept(this)
        }

        private lateinit var melding: String
        private lateinit var kontekster: List<SpesifikkKontekst>

        override fun preVisitEtterlevelse(
            kontekster: List<SpesifikkKontekst>,
            aktivitet: Aktivitetslogg.Aktivitet.Etterlevelse,
            melding: String,
            vurderingsresultat: Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat,
            tidsstempel: String
        ) {
            this.kontekster = kontekster
            this.melding = melding
        }

        override fun visitVurderingsresultat(
            oppfylt: Boolean,
            versjon: LocalDate,
            paragraf: Paragraf,
            ledd: Ledd,
            punktum: List<Punktum>,
            inputdata: Map<Any, Any?>,
            outputdata: Map<Any, Any?>
        ) {
            resultater.add(Resultat(melding, oppfylt, versjon, paragraf, ledd, punktum, inputdata, outputdata, kontekster))
        }
    }

    private class Resultat(
        val melding: String,
        val oppfylt: Boolean,
        val versjon: LocalDate,
        val paragraf: Paragraf,
        val ledd: Ledd,
        val punktum: List<Punktum>,
        val inputdata: Map<Any, Any?>,
        val outputdata: Map<Any, Any?>,
        val kontekster: List<SpesifikkKontekst>
    ) {
        fun vedtaksperiodeIdFraKontekst(): UUID = UUID.fromString(kontekster.first { it.kontekstType == "Vedtaksperiode" }.kontekstMap["vedtaksperiodeId"])
    }
}
