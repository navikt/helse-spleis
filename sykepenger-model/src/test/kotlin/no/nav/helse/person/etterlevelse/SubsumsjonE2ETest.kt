package no.nav.helse.person.etterlevelse

import no.nav.helse.*
import no.nav.helse.hendelser.*
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.person.Ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum.Companion.punktum
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.spleis.e2e.*
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class SubsumsjonE2ETest : AbstractEndToEndTest() {

    @Test
    fun `§8-2 ledd 1 - opptjeningstid tilfredstilt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser()
        val arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 4.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = Paragraf.PARAGRAF_8_2,
            ledd = Ledd.LEDD_1,
            versjon = 12.juni(2020),
            input = mapOf(
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
            output = mapOf("antallOpptjeningsdager" to 28)
        )
    }

    @Test
    fun `§8-2 ledd 1 - opptjeningstid ikke tilfredstilt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser()
        val arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER.toString(), 5.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold)

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = Paragraf.PARAGRAF_8_2,
            ledd = Ledd.LEDD_1,
            versjon = 12.juni(2020),
            input = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "tilstrekkeligAntallOpptjeningsdager" to 28,
                "arbeidsforhold" to listOf(
                    mapOf(
                        "orgnummer" to ORGNUMMER.toString(),
                        "fom" to 5.desember(2017),
                        "tom" to 31.januar
                    )
                )
            ),
            output = mapOf("antallOpptjeningsdager" to 27)
        )
    }

    @Test
    fun `§8-3 ledd 1 punktum 2 - fyller 70`() {
        val fnr = "20014835841".somFødselsnummer()
        createTestPerson(fnr)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterInntektsmelding(listOf(1.januar til 16.januar), fnr = fnr)
        håndterYtelser(fnr = fnr)
        håndterVilkårsgrunnlag(fnr = fnr)
        håndterYtelser(fnr = fnr)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = Paragraf.PARAGRAF_8_3,
            ledd = Ledd.LEDD_1,
            punktum = 2.punktum,
            versjon = 16.desember(2011),
            input = mapOf(
                "syttiårsdagen" to 20.januar,
                "utfallFom" to 1.januar,
                "utfallTom" to 19.januar,
                "tidslinjeFom" to 1.januar,
                "tidslinjeTom" to 31.januar
            ),
            output = mapOf(
                "avvisteDager" to emptyList<Periode>()
            )
        )

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = Paragraf.PARAGRAF_8_3,
            ledd = Ledd.LEDD_1,
            punktum = 2.punktum,
            versjon = 16.desember(2011),
            input = mapOf(
                "syttiårsdagen" to 20.januar,
                "utfallFom" to 20.januar,
                "utfallTom" to 31.januar,
                "tidslinjeFom" to 1.januar,
                "tidslinjeTom" to 31.januar
            ),
            output = mapOf(
                "avvisteDager" to listOf(20.januar til 31.januar)
            )
        )
    }

    @Test
    fun `§8-3 ledd 1 punktum 2 - blir aldri 70`() {
        val fnr = "01024835841".somFødselsnummer()
        createTestPerson(fnr)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterInntektsmelding(listOf(1.januar til 16.januar), fnr = fnr)
        håndterYtelser(fnr = fnr)
        håndterVilkårsgrunnlag(fnr = fnr)
        håndterYtelser(fnr = fnr)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = Paragraf.PARAGRAF_8_3,
            ledd = Ledd.LEDD_1,
            punktum = 2.punktum,
            versjon = 16.desember(2011),
            input = mapOf(
                "syttiårsdagen" to 1.februar,
                "utfallFom" to 1.januar,
                "utfallTom" to 31.januar,
                "tidslinjeFom" to 1.januar,
                "tidslinjeTom" to 31.januar
            ),
            output = mapOf(
                "avvisteDager" to emptyList<Periode>()
            )
        )
    }

    @Test
    fun `§8-3 ledd 1 punktum 2 - er alltid 70`() {
        val fnr = "01014835841".somFødselsnummer()
        createTestPerson(fnr)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterInntektsmelding(listOf(1.januar til 16.januar), fnr = fnr)
        håndterYtelser(fnr = fnr)
        håndterVilkårsgrunnlag(fnr = fnr)
        håndterYtelser(fnr = fnr)

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = Paragraf.PARAGRAF_8_3,
            ledd = Ledd.LEDD_1,
            punktum = 2.punktum,
            versjon = 16.desember(2011),
            input = mapOf(
                "syttiårsdagen" to 1.januar,
                "utfallFom" to 1.januar,
                "utfallTom" to 31.januar,
                "tidslinjeFom" to 1.januar,
                "tidslinjeTom" to 31.januar
            ),
            output = mapOf(
                "avvisteDager" to listOf(17.januar til 31.januar)
            )
        )
    }

    @ForventetFeil("Perioden avsluttes automatisk -- usikker på hva vi ønsker av etterlevelse da")
    @Test
    fun `§8-3 ledd 1 punktum 2 - er alltid 70 uten NAVdager`() {
        val fnr = "01014835841".somFødselsnummer()
        createTestPerson(fnr)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent), fnr = fnr)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 16.januar, 100.prosent), fnr = fnr)
        håndterInntektsmelding(listOf(1.januar til 16.januar), fnr = fnr)
        håndterYtelser(fnr = fnr)
        håndterVilkårsgrunnlag(fnr = fnr)
        håndterYtelser(fnr = fnr)

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = Paragraf.PARAGRAF_8_3,
            ledd = Ledd.LEDD_1,
            punktum = 2.punktum,
            versjon = 16.desember(2011),
            input = mapOf(
                "syttiårsdagen" to 1.januar,
                "utfallFom" to 1.januar,
                "utfallTom" to 16.januar,
                "tidslinjeFom" to 1.januar,
                "tidslinjeTom" to 16.januar
            ),
            output = mapOf(
                "avvisteDager" to emptyList<Periode>()
            )
        )
    }

    @Test
    fun `§8-3 ledd 2 punktum 1 - har minimum inntekt halv G`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 46817.årlig)
        håndterYtelser()
        val arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 5.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = Paragraf.PARAGRAF_8_3,
            ledd = Ledd.LEDD_2,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            input = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "grunnlagForSykepengegrunnlag" to 46817.0,
                "minimumInntekt" to 46817.0
            ),
            output = emptyMap()
        )
        SubsumsjonInspektør(jurist).assertIkkeVurdert(Paragraf.PARAGRAF_8_51, Ledd.LEDD_2, 1.punktum)
    }

    @Test
    fun `§8-3 ledd 2 punktum 1 - har inntekt mindre enn en halv G`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 46816.årlig)
        håndterYtelser()
        val arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 5.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold)

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = Paragraf.PARAGRAF_8_3,
            ledd = Ledd.LEDD_2,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            input = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "grunnlagForSykepengegrunnlag" to 46816.0,
                "minimumInntekt" to 46817.0
            ),
            output = emptyMap()
        )
        SubsumsjonInspektør(jurist).assertIkkeVurdert(Paragraf.PARAGRAF_8_51, Ledd.LEDD_2, 1.punktum)
    }


    @Test
    fun `§8-10 ledd 2 punktum 1 - inntekt overstiger ikke maksimum sykepengegrunnlag`() {
        val maksimumSykepengegrunnlag2018 = (93634 * 6).årlig // 6G
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = maksimumSykepengegrunnlag2018)
        håndterYtelser()
        håndterVilkårsgrunnlag(inntekt = maksimumSykepengegrunnlag2018)
        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = Paragraf.PARAGRAF_8_10,
            ledd = Ledd.LEDD_2,
            punktum = 1.punktum,
            versjon = 1.januar(2020),
            input = mapOf(
                "maksimaltSykepengegrunnlag" to 561804.0,
                "skjæringstidspunkt" to 1.januar,
                "grunnlagForSykepengegrunnlag" to 561804.0
            ),
            output = mapOf(
                "erBegrenset" to false
            )
        )
    }

    @Test
    fun `§8-10 ledd 2 punktum 1 - inntekt overstiger maksimum sykepengegrunnlag`() {
        val maksimumSykepengegrunnlag2018 = (93634 * 6).årlig // 6G
        val inntekt = maksimumSykepengegrunnlag2018.plus(1.årlig)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt)
        håndterYtelser()
        håndterVilkårsgrunnlag(inntekt = inntekt)
        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = Paragraf.PARAGRAF_8_10,
            ledd = Ledd.LEDD_2,
            punktum = 1.punktum,
            versjon = 1.januar(2020),
            input = mapOf(
                "maksimaltSykepengegrunnlag" to 561804.0,
                "skjæringstidspunkt" to 1.januar,
                "grunnlagForSykepengegrunnlag" to 561805.0
            ),
            output = mapOf(
                "erBegrenset" to true
            )
        )
    }

    @Test
    fun `§8-10 ledd 2 punktum 1 - vurderes ved overgang fra Infotrygd`() {
        val maksimumSykepengegrunnlag2018 = (93634 * 6).årlig // 6G
        val inntekt = maksimumSykepengegrunnlag2018.plus(1.årlig)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
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
        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = Paragraf.PARAGRAF_8_10,
            ledd = Ledd.LEDD_2,
            punktum = 1.punktum,
            versjon = 1.januar(2020),
            input = mapOf(
                "maksimaltSykepengegrunnlag" to 561804.0,
                "skjæringstidspunkt" to 1.januar,
                "grunnlagForSykepengegrunnlag" to 561805.0
            ),
            output = mapOf(
                "erBegrenset" to true
            )
        )
    }


    @Test
    fun `§8-12 ledd 1 punktum 1 - Brukt færre enn 248 dager`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 50.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = Paragraf.PARAGRAF_8_12,
            ledd = Ledd.LEDD_1,
            punktum = 1.punktum,
            versjon = 21.mai(2021),
            input = mapOf(
                "fom" to 19.januar,
                "tom" to 26.januar,
                "tidslinjegrunnlag" to listOf(listOf(mapOf("fom" to 19.januar, "tom" to 26.januar, "dagtype" to "NAVDAG")), emptyList()),
                "beregnetTidslinje" to listOf(mapOf("fom" to 19.januar, "tom" to 26.januar, "dagtype" to "NAVDAG"))
            ),
            output = mapOf(
                "gjenståendeSykedager" to 242,
                "forbrukteSykedager" to 6,
                "maksdato" to 1.januar(2019),
                "avvisteDager" to emptyList<Periode>()
            )
        )
    }

    @Test
    fun `§8-12 ledd 1 punktum 1 - Brukt flere enn 248 dager`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar(2018), 11.januar(2019), 50.prosent))
        håndterSøknadMedValidering(
            1.vedtaksperiode,
            Søknad.Søknadsperiode.Sykdom(3.januar(2018), 11.januar(2019), 50.prosent, 50.prosent),
            sendtTilNAVEllerArbeidsgiver = 3.januar(2018)
        )
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar(2018), 18.januar(2018))))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = Paragraf.PARAGRAF_8_12,
            ledd = Ledd.LEDD_1,
            punktum = 1.punktum,
            versjon = 21.mai(2021),
            input = mapOf(
                "fom" to 19.januar,
                "tom" to 1.januar(2019),
                "tidslinjegrunnlag" to listOf(listOf(mapOf("fom" to 19.januar, "tom" to 11.januar(2019), "dagtype" to "NAVDAG")), emptyList()),
                "beregnetTidslinje" to listOf(mapOf("fom" to 19.januar, "tom" to 11.januar(2019), "dagtype" to "NAVDAG"))
            ),
            output = mapOf(
                "gjenståendeSykedager" to 0,
                "forbrukteSykedager" to 248,
                "maksdato" to 1.januar(2019),
                "avvisteDager" to emptyList<Periode>()
            )
        )

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = Paragraf.PARAGRAF_8_12,
            ledd = Ledd.LEDD_1,
            punktum = 1.punktum,
            versjon = 21.mai(2021),
            input = mapOf(
                "fom" to 2.januar(2019),
                "tom" to 11.januar(2019),
                "tidslinjegrunnlag" to listOf(listOf(mapOf("fom" to 19.januar, "tom" to 11.januar(2019), "dagtype" to "NAVDAG")), emptyList()),
                "beregnetTidslinje" to listOf(mapOf("fom" to 19.januar, "tom" to 11.januar(2019), "dagtype" to "NAVDAG"))
            ),
            output = mapOf(
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
        håndterSøknadMedValidering(1.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(17.juli, 31.august, 50.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Søknad.Søknadsperiode.Sykdom(17.juli, 31.august, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(17.juli, 1.august)))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = Paragraf.PARAGRAF_8_12,
            ledd = Ledd.LEDD_2,
            punktum = emptyList(), // TODO: Avklare at det er ok å droppe punktum range
            versjon = 21.mai(2021),
            input = mapOf(
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
            output = emptyMap()
        )
    }
}
