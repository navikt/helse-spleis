package no.nav.helse.spleis.e2e

import no.nav.helse.ForventetFeil
import no.nav.helse.Organisasjonsnummer
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.person.*
import no.nav.helse.person.Bokstav.BOKSTAV_A
import no.nav.helse.person.Ledd.*
import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Paragraf.*
import no.nav.helse.person.Punktum.Companion.punktum
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.somFødselsnummer
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.util.*

internal class EtterlevelseTest : AbstractEndToEndTest() {

    @Test
    fun `§8-2 ledd 1 - opptjeningstid tilfredstilt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser()
        val arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER.toString(), 4.desember(2017), 31.januar))
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
                        "orgnummer" to ORGNUMMER.toString(),
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
        val arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER.toString(), 5.desember(2017), 31.januar))
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
                        "orgnummer" to ORGNUMMER.toString(),
                        "fom" to 5.desember(2017),
                        "tom" to 31.januar
                    )
                )
            ),
            outputdata = mapOf("antallOpptjeningsdager" to 27)
        )
    }

    @Test
    fun `§8-3 ledd 1 punktum 2 - fyller 70`() {
        val fnr = "20014835841".somFødselsnummer()
        createTestPerson(fnr)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterInntektsmelding(listOf(1.januar til 16.januar), fnr = fnr)
        håndterYtelser(fnr = fnr)
        håndterVilkårsgrunnlag(fnr = fnr)
        håndterYtelser(fnr = fnr)

        assertOppfylt(
            resultatvelger = 1.resultat,
            paragraf = PARAGRAF_8_3,
            ledd = LEDD_1,
            punktum = 2.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "syttiårsdagen" to 20.januar,
                "vurderingFom" to 1.januar,
                "vurderingTom" to 19.januar,
                "tidslinjeFom" to 1.januar,
                "tidslinjeTom" to 31.januar
            ),
            outputdata = mapOf(
                "avvisteDager" to emptyList<Periode>()
            )
        )

        assertIkkeOppfylt(
            resultatvelger = 2.resultat,
            paragraf = PARAGRAF_8_3,
            ledd = LEDD_1,
            punktum = 2.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "syttiårsdagen" to 20.januar,
                "vurderingFom" to 20.januar,
                "vurderingTom" to 31.januar,
                "tidslinjeFom" to 1.januar,
                "tidslinjeTom" to 31.januar
            ),
            outputdata = mapOf(
                "avvisteDager" to listOf(20.januar til 31.januar)
            )
        )
    }

    @Test
    fun `§8-3 ledd 1 punktum 2 - blir aldri 70`() {
        val fnr = "01024835841".somFødselsnummer()
        createTestPerson(fnr)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterInntektsmelding(listOf(1.januar til 16.januar), fnr = fnr)
        håndterYtelser(fnr = fnr)
        håndterVilkårsgrunnlag(fnr = fnr)
        håndterYtelser(fnr = fnr)

        assertOppfylt(
            paragraf = PARAGRAF_8_3,
            ledd = LEDD_1,
            punktum = 2.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "syttiårsdagen" to 1.februar,
                "vurderingFom" to 1.januar,
                "vurderingTom" to 31.januar,
                "tidslinjeFom" to 1.januar,
                "tidslinjeTom" to 31.januar
            ),
            outputdata = mapOf(
                "avvisteDager" to emptyList<Periode>()
            )
        )
    }

    @Test
    fun `§8-3 ledd 1 punktum 2 - er alltid 70`() {
        val fnr = "01014835841".somFødselsnummer()
        createTestPerson(fnr)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterInntektsmelding(listOf(1.januar til 16.januar), fnr = fnr)
        håndterYtelser(fnr = fnr)
        håndterVilkårsgrunnlag(fnr = fnr)
        håndterYtelser(fnr = fnr)

        assertIkkeOppfylt(
            paragraf = PARAGRAF_8_3,
            ledd = LEDD_1,
            punktum = 2.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "syttiårsdagen" to 1.januar,
                "vurderingFom" to 1.januar,
                "vurderingTom" to 31.januar,
                "tidslinjeFom" to 1.januar,
                "tidslinjeTom" to 31.januar
            ),
            outputdata = mapOf(
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
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), fnr = fnr)
        håndterInntektsmelding(listOf(1.januar til 16.januar), fnr = fnr)
        håndterYtelser(fnr = fnr)
        håndterVilkårsgrunnlag(fnr = fnr)
        håndterYtelser(fnr = fnr)

        assertIkkeOppfylt(
            paragraf = PARAGRAF_8_3,
            ledd = LEDD_1,
            punktum = 2.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "syttiårsdagen" to 1.januar,
                "vurderingFom" to 1.januar,
                "vurderingTom" to 16.januar,
                "tidslinjeFom" to 1.januar,
                "tidslinjeTom" to 16.januar
            ),
            outputdata = mapOf(
                "avvisteDager" to emptyList<Periode>()
            )
        )
    }

    @Test
    fun `§8-3 ledd 2 punktum 1 - har minimum inntekt halv G`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 46817.årlig)
        håndterYtelser()
        val arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER.toString(), 5.desember(2017), 31.januar))
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
        val arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER.toString(), 5.desember(2017), 31.januar))
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
    fun `§8-10 ledd 2 punktum 1 - vurderes ved overgang fra Infotrygd`() {
        val maksimumSykepengegrunnlag2018 = (93634 * 6).årlig // 6G
        val inntekt = maksimumSykepengegrunnlag2018.plus(1.årlig)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterUtbetalingshistorikk(
            1.vedtaksperiode,
            utbetalinger = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER.toString(), 1.januar, 31.januar, 100.prosent, inntekt)),
            inntektshistorikk = listOf(
                Inntektsopplysning(ORGNUMMER.toString(), 1.januar, inntekt, true)
            )
        )
        håndterYtelser()
        håndterSimulering()
        håndterUtbetalingsgodkjenning()
        håndterUtbetalt()
        assertVurdert(paragraf = PARAGRAF_8_10, ledd = LEDD_2, punktum = 1.punktum)
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
            sendtTilNAVEllerArbeidsgiver = 3.januar(2018)
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
    fun `§8-13 ledd 1 - Sykmeldte har 20 prosent uføregrad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 20.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 20.prosent, 80.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt(1.vedtaksperiode)

        assertOppfylt(
            paragraf = PARAGRAF_8_13,
            ledd = 1.ledd,
            punktum = (1..2).punktum,
            versjon = FOLKETRYGDLOVENS_OPPRINNELSESDATO,
            inputdata = mapOf(
                "avvisteDager" to emptyList<LocalDate>()
            ),
            outputdata = emptyMap()
        )
    }

    @Test
    fun `§8-13 ledd 1 - Sykmeldte har under 20 prosent uføregrad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 19.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 19.prosent, 81.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        val avvisteDager = (17..31)
            .map { it.januar }
            .filter { it.dayOfWeek != SATURDAY && it.dayOfWeek != SUNDAY }

        assertIkkeOppfylt(
            paragraf = PARAGRAF_8_13,
            ledd = 1.ledd,
            punktum = (1..2).punktum,
            versjon = FOLKETRYGDLOVENS_OPPRINNELSESDATO,
            inputdata = mapOf(
                "avvisteDager" to avvisteDager
            ),
            outputdata = emptyMap()
        )
    }

    @ForventetFeil("Mangler gruppering av hjemler i aktivitetsloggen")
    @Test
    fun `§8-16 ledd 1 - dekningsgrad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        assertOppfylt(
            PARAGRAF_8_16,
            ledd = 1.ledd,
            punktum = 1.punktum,
            versjon = FOLKETRYGDLOVENS_OPPRINNELSESDATO,
            inputdata = mapOf(
                "dekningsgrad" to 1.0,
                "inntekt" to 372000.0
            ),
            outputdata = mapOf(
                "dekningsgrunnlag" to 372000.0
            )
        )
    }

    @Test
    fun `§8-17 ledd 1 bokstav a - trygden yter sykepenger ved utløp av arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        assertOppfylt(
            paragraf = PARAGRAF_8_17,
            ledd = 1.ledd,
            punktum = 1.punktum,
            bokstaver = listOf(BOKSTAV_A),
            versjon = 1.januar,
            inputdata = mapOf(
                "førsteNavdag" to 17.januar,
                "arbeidsgiverperioder" to listOf(
                    mapOf(
                        "fom" to 1.januar,
                        "tom" to 16.januar
                    )
                )
            ),
            outputdata = emptyMap()
        )
    }

    @Test
    fun `§8-17 ledd 1 bokstav a - trygden yter sykepenger dersom arbeidsgiverperioden avslutter på en fredag`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(4.januar, 22.januar, 100.prosent))
        håndterInntektsmelding(listOf(4.januar til 19.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        assertOppfylt(
            paragraf = PARAGRAF_8_17,
            ledd = 1.ledd,
            punktum = 1.punktum,
            bokstaver = listOf(BOKSTAV_A),
            versjon = 1.januar,
            inputdata = mapOf(
                "førsteNavdag" to 22.januar,
                "arbeidsgiverperioder" to listOf(
                    mapOf(
                        "fom" to 4.januar,
                        "tom" to 19.januar
                    )
                )
            ),
            outputdata = emptyMap()
        )
    }

    @Test
    fun `§8-17 ledd 1 bokstav a - trygden yter ikke sykepenger dersom arbeidsgiverperioden ikke er fullført`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertIkkeVurdert(paragraf = PARAGRAF_8_17, ledd = 1.ledd)
    }

    @Test
    fun `§8-30 ledd 1 - sykepengegrunnlaget utgjør aktuell månedsinntekt omregnet til årsinntekt i kontekst av §8-30 ledd 1`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        assertOppfylt(
            PARAGRAF_8_30,
            1.ledd,
            1.punktum,
            versjon = 1.januar(2019),
            inputdata = mapOf(
                "beregnetMånedsinntektPerArbeidsgiver" to mapOf(
                    ORGNUMMER.toString() to 31000.0
                )
            ),
            outputdata = mapOf(
                "grunnlagForSykepengegrunnlag" to 372000.0
            )
        )
    }

    @Test
    fun `§8-30 ledd 1 - sykepengegrunnlaget utgjør aktuell månedsinntekt omregnet til årsinntekt i kontekst av §8-30 ledd 1 selv om beløpet overstiger 6G`() {
        val inntekt = 60000
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt.månedlig)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntekt.månedlig)
        håndterYtelser(1.vedtaksperiode)

        assertOppfylt(
            PARAGRAF_8_30,
            1.ledd,
            1.punktum,
            versjon = 1.januar(2019),
            inputdata = mapOf(
                "beregnetMånedsinntektPerArbeidsgiver" to mapOf(
                    ORGNUMMER.toString() to 60000.0
                )
            ),
            outputdata = mapOf(
                "grunnlagForSykepengegrunnlag" to 720000.0
            )
        )
    }

    @Test
    fun `§8-30 ledd 1 - sykepengegrunnlaget utgjør aktuell månedsinntekt omregnet til årsinntekt i kontekst av §8-30 ledd 1 - flere AG`() {
        val AG1 = "987654321".somOrganisasjonsnummer()
        val AG2 = "123456789".somOrganisasjonsnummer()
        val inntekt = 60000
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = AG2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = AG1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = AG2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt.månedlig, orgnummer = AG1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt.månedlig, orgnummer = AG2)
        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = AG1,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        AG1 inntekt inntekt
                        AG2 inntekt inntekt
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntektperioderForSykepengegrunnlag {
                    1.oktober(2017) til 1.desember(2017) inntekter {
                        AG1 inntekt inntekt
                        AG2 inntekt inntekt
                    }
                }, arbeidsforhold = emptyList())
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = AG1)

        assertOppfylt(
            PARAGRAF_8_30,
            1.ledd,
            1.punktum,
            versjon = 1.januar(2019),
            inputdata = mapOf(
                "beregnetMånedsinntektPerArbeidsgiver" to mapOf(
                    AG1.toString() to 60000.0,
                    AG2.toString() to 60000.0
                )
            ),
            outputdata = mapOf(
                "grunnlagForSykepengegrunnlag" to 1440000.0
            )
        )
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
            paragraf = PARAGRAF_8_30,
            ledd = 2.ledd,
            punktum = 1.punktum,
            versjon = LocalDate.of(2017, 4, 5),
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
            paragraf = PARAGRAF_8_30,
            ledd = 2.ledd,
            punktum = 1.punktum,
            versjon = LocalDate.of(2017, 4, 5),
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
            paragraf = PARAGRAF_8_30,
            ledd = 2.ledd,
            punktum = 1.punktum,
            versjon = LocalDate.of(2017, 4, 5),
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
        håndterInntektsmelding(listOf(1.januar til 16.januar))

        håndterSykmelding(Sykmeldingsperiode(2.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(2.januar, 31.januar, 100.prosent))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        assertIkkeVurdert(PARAGRAF_8_30, LEDD_2, vedtaksperiodeId = 1.vedtaksperiode)
        assertVurdert(PARAGRAF_8_30, LEDD_2, vedtaksperiodeId = 2.vedtaksperiode)
    }

    @Test
    fun `§8-51 ledd 2 - har minimum inntekt 2G - over 67 år`() {
        val GAMMEL = "01014500065".somFødselsnummer()
        createTestPerson(GAMMEL)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = GAMMEL)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), fnr = GAMMEL)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 187268.årlig, fnr = GAMMEL)
        håndterYtelser(fnr = GAMMEL)
        val arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER.toString(), 5.desember(2017), 31.januar))
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
        val GAMMEL = "01014500065".somFødselsnummer()
        createTestPerson(GAMMEL)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = GAMMEL)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), fnr = GAMMEL)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 187267.årlig, fnr = GAMMEL)
        håndterYtelser(fnr = GAMMEL)
        val arbeidsforhold = listOf(Arbeidsforhold(ORGNUMMER.toString(), 5.desember(2017), 31.januar))
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

    @Test
    fun `§8-51 ledd 3 - 60 sykedager etter fylte 67 år - syk 61 dager etter fylte 67 år`() {
        val GAMMEL = "01025100065".somFødselsnummer()
        createTestPerson(GAMMEL)
        nyttVedtak(1.januar, 31.januar, fnr = GAMMEL)
        forlengVedtak(1.februar, 28.februar, fnr = GAMMEL)
        forlengVedtak(1.mars, 31.mars, fnr = GAMMEL)
        forlengVedtak(1.april, 27.april, fnr = GAMMEL)

        assertOppfylt(
            resultatvelger = 1.resultat,
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "maksSykepengedagerOver67" to 60
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 61,
                "forbrukteSykedager" to 11,
                "maksdato" to 26.april
            )
        )

        assertOppfylt(
            resultatvelger = 2.resultat,
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "maksSykepengedagerOver67" to 60
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 41,
                "forbrukteSykedager" to 31,
                "maksdato" to 26.april
            )
        )

        assertOppfylt(
            resultatvelger = 3.resultat,
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "maksSykepengedagerOver67" to 60
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 19,
                "forbrukteSykedager" to 53,
                "maksdato" to 26.april
            )
        )

        assertIkkeOppfylt(
            resultatvelger = 4.resultat,
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "maksSykepengedagerOver67" to 60
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 0,
                "forbrukteSykedager" to 72,
                "maksdato" to 26.april
            )
        )

        assertOppfylt(
            resultatvelger = 5.resultat,
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "maksSykepengedagerOver67" to 60
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 0,
                "forbrukteSykedager" to 72,
                "maksdato" to 26.april
            )
        )
    }

    @Test
    fun `§8-51 ledd 3 - 60 sykedager etter fylte 67 år - frisk på 60-årsdagen så total sykedager blir en dag mindre uten at maksdato endres`() {
        val GAMMEL = "01025100065".somFødselsnummer()
        createTestPerson(GAMMEL)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = GAMMEL)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), fnr = GAMMEL)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, fnr = GAMMEL)
        håndterYtelser(1.vedtaksperiode, fnr = GAMMEL)
        håndterVilkårsgrunnlag(1.vedtaksperiode, fnr = GAMMEL)
        håndterYtelser(1.vedtaksperiode, fnr = GAMMEL)
        håndterSimulering(1.vedtaksperiode, fnr = GAMMEL)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, fnr = GAMMEL)
        håndterUtbetalt(1.vedtaksperiode, fnr = GAMMEL)

        håndterSykmelding(Sykmeldingsperiode(2.februar, 28.februar, 100.prosent), fnr = GAMMEL)
        håndterSøknad(Sykdom(2.februar, 28.februar, 100.prosent), fnr = GAMMEL)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 2.februar, fnr = GAMMEL)
        håndterYtelser(2.vedtaksperiode, fnr = GAMMEL)
        håndterVilkårsgrunnlag(2.vedtaksperiode, fnr = GAMMEL)
        håndterYtelser(2.vedtaksperiode, fnr = GAMMEL)
        håndterSimulering(2.vedtaksperiode, fnr = GAMMEL)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, fnr = GAMMEL)
        håndterUtbetalt(2.vedtaksperiode, fnr = GAMMEL)

        assertOppfylt(
            resultatvelger = 1.resultat,
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "maksSykepengedagerOver67" to 60
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 61,
                "forbrukteSykedager" to 11,
                "maksdato" to 26.april
            )
        )

        assertOppfylt(
            resultatvelger = 2.resultat,
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "maksSykepengedagerOver67" to 60
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 41,
                "forbrukteSykedager" to 30,
                "maksdato" to 26.april
            )
        )
    }

    @Test
    fun `§8-51 ledd 3 - 60 sykedager etter fylte 67 år - frisk dagen etter 60-årsdagen så maksdato flyttes en dag`() {
        val GAMMEL = "01025100065".somFødselsnummer()
        createTestPerson(GAMMEL)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 1.februar, 100.prosent), fnr = GAMMEL)
        håndterSøknad(Sykdom(1.januar, 1.februar, 100.prosent), fnr = GAMMEL)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, fnr = GAMMEL)
        håndterYtelser(1.vedtaksperiode, fnr = GAMMEL)
        håndterVilkårsgrunnlag(1.vedtaksperiode, fnr = GAMMEL)
        håndterYtelser(1.vedtaksperiode, fnr = GAMMEL)
        håndterSimulering(1.vedtaksperiode, fnr = GAMMEL)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, fnr = GAMMEL)
        håndterUtbetalt(1.vedtaksperiode, fnr = GAMMEL)

        håndterSykmelding(Sykmeldingsperiode(3.februar, 28.februar, 100.prosent), fnr = GAMMEL)
        håndterSøknad(Sykdom(3.februar, 28.februar, 100.prosent), fnr = GAMMEL)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 3.februar, fnr = GAMMEL)
        håndterYtelser(2.vedtaksperiode, fnr = GAMMEL)
        håndterVilkårsgrunnlag(2.vedtaksperiode, fnr = GAMMEL)
        håndterYtelser(2.vedtaksperiode, fnr = GAMMEL)
        håndterSimulering(2.vedtaksperiode, fnr = GAMMEL)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, fnr = GAMMEL)
        håndterUtbetalt(2.vedtaksperiode, fnr = GAMMEL)

        assertOppfylt(
            resultatvelger = 1.resultat,
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "maksSykepengedagerOver67" to 60
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 60,
                "forbrukteSykedager" to 12,
                "maksdato" to 26.april
            )
        )

        assertOppfylt(
            resultatvelger = 2.resultat,
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "maksSykepengedagerOver67" to 60
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 42,
                "forbrukteSykedager" to 30,
                "maksdato" to 27.april
            )
        )
    }

    @Test
    fun `§8-51 ledd 3 - 60 sykedager etter fylte 67 år - syk 60 dager etter fylte 67 år`() {
        val GAMMEL = "01025100065".somFødselsnummer()
        createTestPerson(GAMMEL)
        nyttVedtak(1.januar, 31.januar, fnr = GAMMEL)
        forlengVedtak(1.februar, 28.februar, fnr = GAMMEL)
        forlengVedtak(1.mars, 31.mars, fnr = GAMMEL)
        forlengVedtak(1.april, 26.april, fnr = GAMMEL)

        assertOppfylt(
            resultatvelger = 1.resultat,
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "maksSykepengedagerOver67" to 60
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 61,
                "forbrukteSykedager" to 11,
                "maksdato" to 26.april
            )
        )

        assertOppfylt(
            resultatvelger = 2.resultat,
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "maksSykepengedagerOver67" to 60
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 41,
                "forbrukteSykedager" to 31,
                "maksdato" to 26.april
            )
        )

        assertOppfylt(
            resultatvelger = 3.resultat,
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "maksSykepengedagerOver67" to 60
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 19,
                "forbrukteSykedager" to 53,
                "maksdato" to 26.april
            )
        )

        assertOppfylt(
            resultatvelger = 4.resultat,
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            inputdata = mapOf(
                "maksSykepengedagerOver67" to 60
            ),
            outputdata = mapOf(
                "gjenståendeSykedager" to 0,
                "forbrukteSykedager" to 72,
                "maksdato" to 26.april
            )
        )
    }

    private val Int.resultat: (List<Resultat>) -> Resultat get() = { it[this - 1] }

    private fun assertOppfylt(
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: List<Punktum>,
        bokstaver: List<Bokstav> = emptyList(),
        versjon: LocalDate,
        inputdata: Map<Any, Any?>,
        outputdata: Map<Any, Any?>,
        resultatvelger: (List<Resultat>) -> Resultat = { it.single() },
        inspektør: EtterlevelseInspektør = EtterlevelseInspektør(person.aktivitetslogg)
    ) {
        val resultat = inspektør.resultat(paragraf, ledd, punktum, bokstaver).let(resultatvelger)
        assertTrue(resultat.oppfylt) { "Forventet oppfylt $paragraf $ledd $punktum" }
        assertResultat(versjon, inputdata, outputdata, resultat)
    }

    private fun assertIkkeOppfylt(
        paragraf: Paragraf,
        ledd: Ledd,
        punktum: List<Punktum>,
        bokstaver: List<Bokstav> = emptyList(),
        versjon: LocalDate,
        inputdata: Map<Any, Any?>,
        outputdata: Map<Any, Any?>,
        resultatvelger: (List<Resultat>) -> Resultat = { it.single() },
        inspektør: EtterlevelseInspektør = EtterlevelseInspektør(person.aktivitetslogg)
    ) {
        val resultat = inspektør.resultat(paragraf, ledd, punktum, bokstaver).let(resultatvelger)
        assertFalse(resultat.oppfylt) { "Forventet ikke oppfylt $paragraf $ledd $punktum" }
        assertResultat(versjon, inputdata, outputdata, resultat)
    }

    private fun assertVurdert(
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: List<Punktum>? = null,
        bokstaver: List<Bokstav>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: Organisasjonsnummer = ORGNUMMER,
        inspektør: EtterlevelseInspektør = EtterlevelseInspektør(person.aktivitetslogg)
    ) {
        val resultat = inspektør.resultat(paragraf, ledd, punktum, bokstaver, vedtaksperiodeId?.invoke(organisasjonsnummer))
        assertTrue(resultat.isNotEmpty()) { "Forventet at $paragraf $ledd $punktum er vurdert" }
    }

    private fun assertIkkeVurdert(
        paragraf: Paragraf,
        ledd: Ledd? = null,
        punktum: List<Punktum>? = null,
        bokstav: List<Bokstav>? = null,
        vedtaksperiodeId: IdInnhenter? = null,
        organisasjonsnummer: Organisasjonsnummer = ORGNUMMER,
        inspektør: EtterlevelseInspektør = EtterlevelseInspektør(person.aktivitetslogg)
    ) {
        val resultat = inspektør.resultat(paragraf, ledd, punktum, bokstav, vedtaksperiodeId?.invoke(organisasjonsnummer))
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

        fun resultat(paragraf: Paragraf, ledd: Ledd?, punktum: List<Punktum>?, bokstav: List<Bokstav>?, vedtaksperiodeId: UUID? = null) =
            resultater.filter {
                it.paragraf == paragraf
                    && ledd?.equals(it.ledd) ?: true
                    && punktum?.equals(it.punktum) ?: true
                    && bokstav?.equals(it.bokstaver) ?: true
                    && vedtaksperiodeId?.equals(it.vedtaksperiodeIdFraKontekst()) ?: true
            }

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
            bokstaver: List<Bokstav>,
            outputdata: Map<Any, Any?>,
            inputdata: Map<Any, Any?>
        ) {
            resultater.add(Resultat(melding, oppfylt, versjon, paragraf, ledd, punktum, bokstaver, inputdata, outputdata, kontekster))
        }
    }

    private class Resultat(
        val melding: String,
        val oppfylt: Boolean,
        val versjon: LocalDate,
        val paragraf: Paragraf,
        val ledd: Ledd,
        val punktum: List<Punktum>,
        val bokstaver: List<Bokstav>,
        val inputdata: Map<Any, Any?>,
        val outputdata: Map<Any, Any?>,
        val kontekster: List<SpesifikkKontekst>
    ) {
        fun vedtaksperiodeIdFraKontekst(): UUID = UUID.fromString(kontekster.first { it.kontekstType == "Vedtaksperiode" }.kontekstMap["vedtaksperiodeId"])
    }
}
