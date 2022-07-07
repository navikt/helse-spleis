package no.nav.helse.person.etterlevelse

import java.time.LocalDate
import java.time.YearMonth
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utlandsopphold
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.oktober
import no.nav.helse.person.Bokstav.BOKSTAV_A
import no.nav.helse.person.FOLKETRYGDLOVENS_OPPRINNELSESDATO
import no.nav.helse.person.Ledd.Companion.ledd
import no.nav.helse.person.Ledd.LEDD_1
import no.nav.helse.person.Ledd.LEDD_2
import no.nav.helse.person.Ledd.LEDD_3
import no.nav.helse.person.Paragraf.PARAGRAF_8_10
import no.nav.helse.person.Paragraf.PARAGRAF_8_11
import no.nav.helse.person.Paragraf.PARAGRAF_8_12
import no.nav.helse.person.Paragraf.PARAGRAF_8_13
import no.nav.helse.person.Paragraf.PARAGRAF_8_16
import no.nav.helse.person.Paragraf.PARAGRAF_8_17
import no.nav.helse.person.Paragraf.PARAGRAF_8_19
import no.nav.helse.person.Paragraf.PARAGRAF_8_2
import no.nav.helse.person.Paragraf.PARAGRAF_8_28
import no.nav.helse.person.Paragraf.PARAGRAF_8_29
import no.nav.helse.person.Paragraf.PARAGRAF_8_3
import no.nav.helse.person.Paragraf.PARAGRAF_8_30
import no.nav.helse.person.Paragraf.PARAGRAF_8_51
import no.nav.helse.person.Paragraf.PARAGRAF_8_9
import no.nav.helse.person.Punktum.Companion.punktum
import no.nav.helse.person.TilstandType
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.finnSkjæringstidspunkt
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.grunnlag
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterInntektsmeldingMedValidering
import no.nav.helse.spleis.e2e.håndterInntektsmeldingReplay
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterSøknadMedValidering
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikk
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.repeat
import no.nav.helse.spleis.e2e.sammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.testhelpers.inntektperioderForSykepengegrunnlag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class SubsumsjonE2ETest : AbstractEndToEndTest() {

    @Test
    fun `§ 8-2 ledd 1 - opptjeningstid tilfredstilt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser()
        val arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 4.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_2,
            ledd = LEDD_1,
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
    fun `§ 8-2 ledd 1 - opptjeningstid ikke tilfredstilt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser()
        val arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 5.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold)

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = PARAGRAF_8_2,
            ledd = LEDD_1,
            versjon = 12.juni(2020),
            input = mapOf(
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
            output = mapOf("antallOpptjeningsdager" to 27)
        )
    }

    @Test
    fun `§ 8-3 ledd 1 punktum 2 - fyller 70`() {
        val fnr = "20014835841".somFødselsnummer()
        createTestPerson(fnr)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterInntektsmelding(listOf(1.januar til 16.januar), fnr = fnr)
        håndterYtelser(fnr = fnr)
        håndterVilkårsgrunnlag(fnr = fnr)
        håndterYtelser(fnr = fnr)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_3,
            ledd = LEDD_1,
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
            paragraf = PARAGRAF_8_3,
            ledd = LEDD_1,
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
    fun `§ 8-3 ledd 1 punktum 2 - blir aldri 70`() {
        val fnr = "01024835841".somFødselsnummer()
        createTestPerson(fnr)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterInntektsmelding(listOf(1.januar til 16.januar), fnr = fnr)
        håndterYtelser(fnr = fnr)
        håndterVilkårsgrunnlag(fnr = fnr)
        håndterYtelser(fnr = fnr)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_3,
            ledd = LEDD_1,
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
    fun `§ 8-3 ledd 1 punktum 2 - er alltid 70`() {
        val fnr = "01014835841".somFødselsnummer()
        createTestPerson(fnr)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), fnr = fnr)
        håndterInntektsmelding(listOf(1.januar til 16.januar), fnr = fnr)
        håndterYtelser(fnr = fnr)
        håndterVilkårsgrunnlag(fnr = fnr)
        håndterYtelser(fnr = fnr)

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = PARAGRAF_8_3,
            ledd = LEDD_1,
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

    @Test
    fun `§ 8-3 ledd 1 punktum 2 - er alltid 70 uten NAVdager`() {
        val fnr = "01014835841".somFødselsnummer()
        createTestPerson(fnr)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent), fnr = fnr)
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), fnr = fnr)
        håndterInntektsmelding(listOf(1.januar til 16.januar), fnr = fnr)

        assertForventetFeil(
            forklaring = "Perioden avsluttes automatisk -- usikker på hva vi ønsker av etterlevelse da",
            nå = {
                assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET_UTEN_UTBETALING)
                SubsumsjonInspektør(jurist).assertIkkeVurdert(paragraf = PARAGRAF_8_3)
            },
            ønsket = {
                håndterYtelser(fnr = fnr)
                håndterVilkårsgrunnlag(fnr = fnr)
                håndterYtelser(fnr = fnr)

                SubsumsjonInspektør(jurist).assertIkkeOppfylt(
                    paragraf = PARAGRAF_8_3,
                    ledd = LEDD_1,
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
        )
    }

    @Test
    fun `§ 8-3 ledd 2 punktum 1 - har minimum inntekt halv G`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 46817.årlig)
        håndterYtelser()
        val arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 5.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_3,
            ledd = LEDD_2,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            input = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "grunnlagForSykepengegrunnlag" to 46817.0,
                "minimumInntekt" to 46817.0
            ),
            output = emptyMap()
        )
        SubsumsjonInspektør(jurist).assertIkkeVurdert(PARAGRAF_8_51, LEDD_2, 1.punktum)
    }

    @Test
    fun `§ 8-3 ledd 2 punktum 1 - har inntekt mindre enn en halv G`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 46816.årlig)
        håndterYtelser()
        val arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 5.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold)

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = PARAGRAF_8_3,
            ledd = LEDD_2,
            punktum = 1.punktum,
            versjon = 16.desember(2011),
            input = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "grunnlagForSykepengegrunnlag" to 46816.0,
                "minimumInntekt" to 46817.0
            ),
            output = emptyMap()
        )
        SubsumsjonInspektør(jurist).assertIkkeVurdert(PARAGRAF_8_51, LEDD_2, 1.punktum)
    }

    @Test
    fun `§ 8-9 ledd 1 - ikke vurdert dersom det ikke er oppgitt utenlandsopphold`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(20.januar, 31.januar))
        SubsumsjonInspektør(jurist).assertIkkeVurdert(
            paragraf = PARAGRAF_8_9,
            versjon = 1.juni(2021),
            ledd = LEDD_1,
        )
    }

    @Test
    fun `§ 8-9 ledd 1 - avslag ved utenlandsopphold`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Utlandsopphold(20.januar, 31.januar))
        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = PARAGRAF_8_9,
            versjon = 1.juni(2021),
            ledd = LEDD_1,
            input = emptyMap(),
            output = mapOf(
                "perioder" to listOf(
                    mapOf(
                        "fom" to 20.januar,
                        "tom" to 31.januar
                    )
                )
            )
        )
    }

    @Test
    fun `§ 8-9 ledd 1 - avslag ved utenlandsopphold, selv om utenlandsoppholdet er helt innenfor en ferie`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Utlandsopphold(20.januar, 31.januar), Ferie(20.januar, 31.januar))
        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = PARAGRAF_8_9,
            versjon = 1.juni(2021),
            ledd = LEDD_1,
            input = emptyMap(),
            output = mapOf(
                "perioder" to listOf(
                    mapOf(
                        "fom" to 20.januar,
                        "tom" to 31.januar
                    )
                )
            )
        )
    }

    @Test
    fun `§ 8-10 ledd 2 punktum 1 - inntekt overstiger ikke maksimum sykepengegrunnlag`() {
        val maksimumSykepengegrunnlag2018 = (93634 * 6).årlig // 6G
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = maksimumSykepengegrunnlag2018)
        håndterYtelser()
        håndterVilkårsgrunnlag(inntekt = maksimumSykepengegrunnlag2018)
        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_10,
            ledd = LEDD_2,
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
    fun `§ 8-10 ledd 2 punktum 1 - inntekt overstiger maksimum sykepengegrunnlag`() {
        val maksimumSykepengegrunnlag2018 = (93634 * 6).årlig // 6G
        val inntekt = maksimumSykepengegrunnlag2018.plus(1.årlig)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt)
        håndterYtelser()
        håndterVilkårsgrunnlag(inntekt = inntekt)
        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_10,
            ledd = LEDD_2,
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
    fun `§ 8-10 ledd 3 - årlig inntekt omregnet til daglig`() {
        val inntekt = 260000.årlig
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt)

        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_10,
            ledd = LEDD_3,
            versjon = 1.januar(2020),
            input = mapOf("årligInntekt" to 260000.0),
            output = mapOf("dagligInntekt" to 1000.0)
        )
    }

    @Test
    fun `§ 8-11 ledd 1 - yter ikke sykepenger i helgedager`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = PARAGRAF_8_11,
            ledd = LEDD_1,
            versjon = FOLKETRYGDLOVENS_OPPRINNELSESDATO,
            input = emptyMap(),
            output = mapOf(
                "perioder" to listOf(
                    mapOf("fom" to 20.januar, "tom" to 21.januar),
                    mapOf("fom" to 27.januar, "tom" to 28.januar)
                )
            )
        )
    }

    @Test
    fun `§ 8-12 ledd 1 punktum 1 - Brukt færre enn 248 dager`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar, 50.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_12,
            ledd = LEDD_1,
            punktum = 1.punktum,
            versjon = 21.mai(2021),
            input = mapOf(
                "fom" to 3.januar,
                "tom" to 26.januar,
                "utfallFom" to 19.januar,
                "utfallTom" to 26.januar,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 3.januar, "tom" to 18.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 19.januar, "tom" to 26.januar, "dagtype" to "NAVDAG", "grad" to 50)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 3.januar, "tom" to 18.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 19.januar, "tom" to 26.januar, "dagtype" to "NAVDAG", "grad" to 50)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 242,
                "forbrukteSykedager" to 6,
                "maksdato" to 1.januar(2019)
            )
        )
    }

    @Test
    fun `§ 8-12 ledd 1 punktum 1 - Brukt flere enn 248 dager`() {
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

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_12,
            ledd = LEDD_1,
            punktum = 1.punktum,
            versjon = 21.mai(2021),
            input = mapOf(
                "fom" to 3.januar,
                "tom" to 11.januar(2019),
                "utfallFom" to 19.januar,
                "utfallTom" to 1.januar(2019),
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 3.januar, "tom" to 18.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 19.januar, "tom" to 11.januar(2019), "dagtype" to "NAVDAG", "grad" to 50)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 3.januar, "tom" to 18.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 19.januar, "tom" to 11.januar(2019), "dagtype" to "NAVDAG", "grad" to 50)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 0,
                "forbrukteSykedager" to 248,
                "maksdato" to 1.januar(2019)
            )
        )

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = PARAGRAF_8_12,
            ledd = LEDD_1,
            punktum = 1.punktum,
            versjon = 21.mai(2021),
            input = mapOf(
                "fom" to 3.januar,
                "tom" to 11.januar(2019),
                "utfallFom" to 2.januar(2019),
                "utfallTom" to 11.januar(2019),
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 3.januar, "tom" to 18.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 19.januar, "tom" to 11.januar(2019), "dagtype" to "NAVDAG", "grad" to 50)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 3.januar, "tom" to 18.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 19.januar, "tom" to 11.januar(2019), "dagtype" to "NAVDAG", "grad" to 50)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 0,
                "forbrukteSykedager" to 248,
                "maksdato" to 1.januar(2019)
            )
        )
    }

    @Test
    fun `§8-12 ledd 1 punktum 1 - Blir kun vurdert en gang etter ny periode med ny rett til sykepenger`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar(2019), 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))
        håndterSøknad(Sykdom(1.januar, 31.januar(2019), 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.januar(2018))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(16.juni(2019), 31.juli(2019), 50.prosent))
        håndterSøknad(Sykdom(16.juni(2019), 31.juli(2019), 50.prosent, 50.prosent))
        håndterInntektsmelding(listOf(Periode(16.juni(2019), 31.juli(2019))))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        SubsumsjonInspektør(jurist).assertVurdert(
            paragraf = PARAGRAF_8_12,
            ledd = 1.ledd,
            punktum = 1.punktum,
            versjon = LocalDate.of(2021, 5, 21),
            vedtaksperiodeId = 2.vedtaksperiode
        )
    }

    @Test
    fun `§ 8-12 ledd 2 - Bruker har vært arbeidsfør i 26 uker`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 50.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(17.juli, 31.august, 50.prosent))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(17.juli, 31.august, 50.prosent, 50.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(17.juli, 1.august)))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()


        SubsumsjonInspektør(jurist).assertIkkeVurdert(
            paragraf = PARAGRAF_8_12,
            ledd = LEDD_2,
            versjon = 21.mai(2021),
            vedtaksperiodeId = 1.vedtaksperiode
        )
        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_12,
            ledd = LEDD_2,
            versjon = 21.mai(2021),
            input = mapOf(
                "dato" to 1.august,
                "tilstrekkeligOppholdISykedager" to 182, //26 uker * 7 dager
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 50),
                        mapOf("fom" to 17.juli, "tom" to 1.august, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 2.august, "tom" to 31.august, "dagtype" to "NAVDAG", "grad" to 50)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 50),
                    mapOf("fom" to 17.juli, "tom" to 1.august, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 2.august, "tom" to 31.august, "dagtype" to "NAVDAG", "grad" to 50)
                )
            ),
            output = emptyMap(),
            vedtaksperiodeId = 2.vedtaksperiode
        )
    }

    @Test
    fun `§8-12 ledd 2 - Bruker har ikke vært arbeidsfør i 26 uker`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar(2018), 31.desember(2018), 100.prosent))
        val inntektsmeldingId = håndterInntektsmelding(listOf(Periode(1.januar(2018), 16.januar(2018))))
        håndterSøknad(Sykdom(1.januar(2018), 31.desember(2018), 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.januar(2018))
        håndterInntektsmeldingReplay(inntektsmeldingId, 1.vedtaksperiode.id(ORGNUMMER))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.januar(2019), 31.januar(2019), 100.prosent))
        håndterSøknad(Sykdom(1.januar(2019), 31.januar(2019), 100.prosent), sendtTilNAVEllerArbeidsgiver = 31.januar(2019))
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt()

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = PARAGRAF_8_12,
            ledd = LEDD_2,
            versjon = 21.mai(2021),
            input = mapOf(
                "dato" to 28.desember,
                "tilstrekkeligOppholdISykedager" to 182,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 31.januar(2019), "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 31.januar(2019), "dagtype" to "NAVDAG", "grad" to 100)
                )
            ),
            output = emptyMap(),
            vedtaksperiodeId = 2.vedtaksperiode
        )
    }

    @Test
    fun `§ 8-13 ledd 1 - Sykmeldte har 20 prosent uføregrad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 20.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 20.prosent, 80.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_13,
            ledd = 1.ledd,
            versjon = FOLKETRYGDLOVENS_OPPRINNELSESDATO,
            input = mapOf(
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 20)
                    )
                ),
            ),
            output = mapOf(
                "perioder" to listOf(
                    mapOf(
                        "fom" to 1.januar,
                        "tom" to 31.januar
                    )
                )

            )
        )
    }

    @Test
    fun `§ 8-13 ledd 1 - Sykmeldte har under 20 prosent uføregrad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 19.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 19.prosent, 81.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = PARAGRAF_8_13,
            ledd = 1.ledd,
            versjon = FOLKETRYGDLOVENS_OPPRINNELSESDATO,
            input = mapOf(
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 19)
                    )
                ),
            ),
            output = mapOf(
                "perioder" to listOf(
                    mapOf(
                        "fom" to 17.januar,
                        "tom" to 31.januar
                    )
                )
            )
        )
    }

    @Test
    fun `§ 8-13 ledd 2 - Sykmeldte har 20 prosent uføregrad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 20.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 20.prosent, 80.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_13,
            ledd = 2.ledd,
            versjon = FOLKETRYGDLOVENS_OPPRINNELSESDATO,
            input = mapOf(
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 20)
                    )
                ),
                "grense" to 20.0
            ),
            output = mapOf(
                "perioder" to listOf(
                    mapOf(
                        "fom" to 1.januar,
                        "tom" to 31.januar
                    )
                ),
                "dagerUnderGrensen" to listOf(
                    mapOf(
                        "fom" to 1.januar,
                        "tom" to 16.januar
                    )
                )
            )
        )
    }

    @Test
    fun `§ 8-13 ledd 2 - Sykmeldte har under 20 prosent uføregrad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 19.prosent))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 19.prosent, 81.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 16.januar)))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_13,
            ledd = 2.ledd,
            versjon = FOLKETRYGDLOVENS_OPPRINNELSESDATO,
            input = mapOf(
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 19)
                    )
                ),
                "grense" to 20.0
            ),
            output = mapOf(
                "perioder" to listOf(
                    mapOf(
                        "fom" to 1.januar,
                        "tom" to 31.januar
                    )
                ),
                "dagerUnderGrensen" to listOf(
                    mapOf(
                        "fom" to 1.januar,
                        "tom" to 31.januar
                    )
                )
            )
        )
    }

    @Test
    fun `§ 8-16 ledd 1 - dekningsgrad`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_16,
            ledd = 1.ledd,
            versjon = FOLKETRYGDLOVENS_OPPRINNELSESDATO,
            input = mapOf(
                "dekningsgrad" to 1.0,
                "inntekt" to 372000.0,
            ),
            output = mapOf(
                "dekningsgrunnlag" to 372000.0,
                "perioder" to listOf(
                    mapOf(
                        "fom" to 1.januar,
                        "tom" to 31.januar,
                    )
                )
            )
        )
    }

    @Test
    fun `§ 8-17 ledd 1 bokstav a - trygden yter sykepenger ved utløp av arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_17,
            ledd = 1.ledd,
            bokstav = BOKSTAV_A,
            versjon = 1.januar,
            input = emptyMap(),
            output = mapOf(
                "perioder" to listOf(mapOf("fom" to 17.januar, "tom" to 17.januar))
            )
        )
    }

    @Test
    fun `§ 8-17 ledd 1 bokstav a - trygden yter sykepenger dersom arbeidsgiverperioden avslutter på en fredag`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(4.januar, 22.januar, 100.prosent))
        håndterInntektsmelding(listOf(4.januar til 19.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_17,
            ledd = 1.ledd,
            bokstav = BOKSTAV_A,
            versjon = 1.januar,
            input = emptyMap(),
            output = mapOf(
                "perioder" to listOf(mapOf("fom" to 22.januar, "tom" to 22.januar))
            )
        )
    }

    @Test
    fun `§ 8-17 ledd 1 bokstav a - trygden yter ikke sykepenger dersom arbeidsgiverperioden ikke er fullført`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET_UTEN_UTBETALING)
        SubsumsjonInspektør(jurist).assertFlereIkkeOppfylt(
            antall = 2,
            paragraf = PARAGRAF_8_17,
            ledd = 1.ledd,
            bokstav = BOKSTAV_A,
            versjon = 1.januar(2018),
            input = emptyMap(),
            output = mapOf(
                "perioder" to listOf(mapOf("fom" to 1.januar, "tom" to 16.januar))
            )
        )
    }

    @Test
    fun `§ 8-17 ledd 1 bokstav a - ikke-oppfylt innenfor arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = PARAGRAF_8_17,
            ledd = 1.ledd,
            bokstav = BOKSTAV_A,
            versjon = 1.januar(2018),
            input = emptyMap(),
            output = mapOf(
                "perioder" to listOf(mapOf("fom" to 1.januar, "tom" to 16.januar))
            )
        )
    }

    @Test
    fun `§ 8-17 ledd 1 bokstav a - opphold inne i arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 10.januar, 12.januar til 17.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = PARAGRAF_8_17,
            ledd = 1.ledd,
            bokstav = BOKSTAV_A,
            versjon = 1.januar(2018),
            input = emptyMap(),
            output = mapOf(
                "perioder" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 10.januar),
                    mapOf("fom" to 12.januar, "tom" to 17.januar)
                )
            )
        )
    }

    @Test
    fun `§ 8-17 ledd 2 - trygden yter ikke sykepenger for feriedager og permisjonsdager`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(30.januar, 31.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            versjon = 1.januar(2018),
            paragraf = PARAGRAF_8_17,
            ledd = 2.ledd,
            input = mapOf(
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 29.januar, "dagtype" to "SYKEDAG", "grad" to 100),
                    mapOf("fom" to 30.januar, "tom" to 31.januar, "dagtype" to "FERIEDAG", "grad" to null)
                ),
            ),
            output = mapOf(
                "perioder" to listOf(mapOf("fom" to 30.januar, "tom" to 31.januar))
            )
        )
    }

    @Test
    fun `§ 8-19 første ledd - arbeidsgiverperioden varer i 16 dager`() {
        håndterSykmelding(Sykmeldingsperiode(4.januar, 22.januar, 100.prosent))
        håndterSøknad(Sykdom(4.januar, 22.januar, 100.prosent))
        håndterInntektsmelding(listOf(4.januar til 19.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_19,
            versjon = 1.januar(2001),
            ledd = 1.ledd,
            input = mapOf(
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 4.januar, "tom" to 22.januar, "dagtype" to "SYKEDAG", "grad" to 100),
                ),
            ),
            output = mapOf(
                "sisteDagIArbeidsgiverperioden" to 19.januar,
            )
        )
    }

    @Test
    fun `§ 8-19 andre ledd - arbeidsgiverperioden regnes fra og med første hele fraværsdag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_19,
            ledd = 2.ledd,
            versjon = 1.januar(2001),
            input = mapOf(
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 31.januar, "dagtype" to "SYKEDAG", "grad" to 100),
                ),
            ),
            output = mapOf(
                "perioder" to listOf(
                    mapOf(
                        "fom" to 1.januar,
                        "tom" to 16.januar
                    )
                ),
            )
        )
    }

    @Test
    fun `§ 8-19 tredje ledd - opphold i AGP`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 3.januar, 5.januar til 10.januar, 12.januar til 17.januar))
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_19,
            ledd = 3.ledd,
            versjon = 1.januar(2001),
            input = mapOf(
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 3.januar, "dagtype" to "SYKEDAG", "grad" to 100),
                    mapOf("fom" to 5.januar, "tom" to 10.januar, "dagtype" to "SYKEDAG", "grad" to 100),
                    mapOf("fom" to 12.januar, "tom" to 31.januar, "dagtype" to "SYKEDAG", "grad" to 100),
                ),
            ),
            output = mapOf(
                "perioder" to listOf(
                    mapOf("fom" to 5.januar, "tom" to 5.januar),
                    mapOf("fom" to 12.januar, "tom" to 12.januar)
                )
            )
        )
    }

    @Test
    fun `§ 8-19 fjerde ledd - ny agp etter tilstrekkelig opphold`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars, 100.prosent))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(1.mars til 16.mars))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)


        SubsumsjonInspektør(jurist).apply {
            assertIkkeVurdert(PARAGRAF_8_19, 4.ledd, vedtaksperiodeId = 1.vedtaksperiode)
            assertBeregnet(
                paragraf = PARAGRAF_8_19,
                ledd = 4.ledd,
                versjon = 1.januar(2001),
                input = mapOf(
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.januar, "tom" to 31.januar, "dagtype" to "SYKEDAG", "grad" to 100),
                        mapOf("fom" to 1.mars, "tom" to 31.mars, "dagtype" to "SYKEDAG", "grad" to 100),
                    ),
                ),
                output = mapOf(
                    "perioder" to listOf(
                        mapOf("fom" to 16.februar, "tom" to 16.februar),
                    )
                ),
                vedtaksperiodeId = 2.vedtaksperiode
            )
        }
    }

    @Test
    fun `§ 8-28 tredje ledd bokstav a - legger tre siste innraporterte inntekter til grunn for andre arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12))
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        SubsumsjonInspektør(jurist).assertBeregnet(
            versjon = 1.januar(2019),
            paragraf = PARAGRAF_8_28,
            ledd = 3.ledd,
            bokstav = BOKSTAV_A,
            input = mapOf(
                "organisasjonsnummer" to a2,
                "skjæringstidspunkt" to 1.januar,
                "inntekterSisteTreMåneder" to listOf(
                    mapOf(
                        "årMåned" to YearMonth.of(2017, 10),
                        "beløp" to 32000.0,
                        "type" to "LØNNSINNTEKT",
                        "fordel" to "Juidy inntekt",
                        "beskrivelse" to "Juidy fordel"
                    ),
                    mapOf(
                        "årMåned" to YearMonth.of(2017, 11),
                        "beløp" to 32000.0,
                        "type" to "LØNNSINNTEKT",
                        "fordel" to "Juidy inntekt",
                        "beskrivelse" to "Juidy fordel"
                    ),
                    mapOf(
                        "årMåned" to YearMonth.of(2017, 12),
                        "beløp" to 32000.0,
                        "type" to "LØNNSINNTEKT",
                        "fordel" to "Juidy inntekt",
                        "beskrivelse" to "Juidy fordel"
                    )
                )
            ),
            output = mapOf(
                "beregnetGrunnlagForSykepengegrunnlagPrÅr" to 384000.0,
                "beregnetGrunnlagForSykepengegrunnlagPrMåned" to 32000.0
            )
        )
    }

    @Test
    fun `§ 8-29 - filter for inntekter som skal medregnes ved beregning av sykepengegrunnlaget for arbeidsforhold hvor sykdom ikke starter på skjæringstidspunktet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterSøknad(Sykdom(1.januar, 15.mars, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjon = Inntektsmelding.Refusjon(31000.månedlig, null, emptyList()),
            orgnummer = a1
        )

        val inntekter = listOf(
            grunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(3)),
            grunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(3))
        )

        val arbeidsforhold = listOf(
            Vilkårsgrunnlag.Arbeidsforhold(a1, LocalDate.EPOCH, null),
            Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, null)
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 31000.månedlig.repeat(12)),
                    sammenligningsgrunnlag(a2, finnSkjæringstidspunkt(a1, 1.vedtaksperiode), 32000.månedlig.repeat(12))
                )
            ),
            orgnummer = a1,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekter, arbeidsforhold = emptyList()),
            arbeidsforhold = arbeidsforhold
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        SubsumsjonInspektør(jurist).assertBeregnet(
            versjon = 1.januar(2019),
            paragraf = PARAGRAF_8_29,
            ledd = null,
            input = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "organisasjonsnummer" to a2,
                "inntektsopplysninger" to listOf(
                    mapOf(
                        "årMåned" to YearMonth.of(2017, 10),
                        "beløp" to 32000.0,
                        "type" to "LØNNSINNTEKT",
                        "fordel" to "Juidy inntekt",
                        "beskrivelse" to "Juidy fordel"
                    ),
                    mapOf(
                        "årMåned" to YearMonth.of(2017, 11),
                        "beløp" to 32000.0,
                        "type" to "LØNNSINNTEKT",
                        "fordel" to "Juidy inntekt",
                        "beskrivelse" to "Juidy fordel"
                    ),
                    mapOf(
                        "årMåned" to YearMonth.of(2017, 12),
                        "beløp" to 32000.0,
                        "type" to "LØNNSINNTEKT",
                        "fordel" to "Juidy inntekt",
                        "beskrivelse" to "Juidy fordel"
                    )
                )
            ),
            output = mapOf(
                "grunnlagForSykepengegrunnlag" to 384000.0
            )
        )
    }

    @Test
    fun `§ 8-30 ledd 1 - sykepengegrunnlaget utgjør aktuell månedsinntekt omregnet til årsinntekt i kontekst av § 8-30 ledd 1`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_30,
            ledd = 1.ledd,
            versjon = 1.januar(2019),
            input = mapOf("beregnetMånedsinntektPerArbeidsgiver" to mapOf(ORGNUMMER to 31000.0)),
            output = mapOf("grunnlagForSykepengegrunnlag" to 372000.0)
        )
    }

    @Test
    fun `§ 8-30 ledd 1 - sykepengegrunnlaget utgjør aktuell månedsinntekt omregnet til årsinntekt i kontekst av § 8-30 ledd 1 selv om beløpet overstiger 6G`() {
        val inntekt = 60000
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt.månedlig)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, inntekt.månedlig)
        håndterYtelser(1.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_30,
            ledd = 1.ledd,
            versjon = 1.januar(2019),
            input = mapOf("beregnetMånedsinntektPerArbeidsgiver" to mapOf(ORGNUMMER to 60000.0)),
            output = mapOf("grunnlagForSykepengegrunnlag" to 720000.0)
        )
    }

    @Test
    fun `§ 8-30 ledd 1 - sykepengegrunnlaget utgjør aktuell månedsinntekt omregnet til årsinntekt i kontekst av § 8-30 ledd 1 - flere AG`() {
        val ag1 = "987654321"
        val ag2 = "123456789"
        val inntekt = 60000
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = ag1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = ag2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = ag1)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = ag2)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt.månedlig, orgnummer = ag1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = inntekt.månedlig, orgnummer = ag2)
        håndterYtelser(1.vedtaksperiode, orgnummer = ag1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            orgnummer = ag1,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        ag1 inntekt inntekt
                        ag2 inntekt inntekt
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntektperioderForSykepengegrunnlag {
                    1.oktober(2017) til 1.desember(2017) inntekter {
                        ag1 inntekt inntekt
                        ag2 inntekt inntekt
                    }
                }, arbeidsforhold = emptyList()
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = ag1)

        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_30,
            ledd = 1.ledd,
            versjon = 1.januar(2019),
            input = mapOf(
                "beregnetMånedsinntektPerArbeidsgiver" to mapOf(
                    ag1 to 60000.0,
                    ag2 to 60000.0
                )
            ),
            output = mapOf("grunnlagForSykepengegrunnlag" to 1440000.0)
        )
    }

    @Test
    fun `§ 8-30 ledd 2 punktum 1 - under 25 prosent avvik`() {
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
        håndterUtbetalt()

        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_30,
            ledd = 2.ledd,
            punktum = 1.punktum,
            versjon = LocalDate.of(2017, 4, 5),
            input = mapOf(
                "maksimaltTillattAvvikPåÅrsinntekt" to 25.0,
                "grunnlagForSykepengegrunnlag" to beregnetInntekt * 12,
                "sammenligningsgrunnlag" to sammenligningsgrunnlag * 12
            ),
            output = mapOf(
                "avviksprosent" to 0.0
            )
        )
    }

    @Test
    fun `§ 8-30 ledd 2 punktum 1 - akkurat 25 prosent avvik`() {
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
        håndterUtbetalt()

        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_30,
            ledd = 2.ledd,
            punktum = 1.punktum,
            versjon = LocalDate.of(2017, 4, 5),
            input = mapOf(
                "maksimaltTillattAvvikPåÅrsinntekt" to 25.0,
                "grunnlagForSykepengegrunnlag" to beregnetInntekt * 12,
                "sammenligningsgrunnlag" to sammenligningsgrunnlag * 12
            ),
            output = mapOf(
                "avviksprosent" to 25.0
            )
        )
    }

    @Test
    fun `§ 8-30 ledd 2 punktum 1 - over 25 prosent avvik`() {
        val beregnetInntekt = 38781.0
        val sammenligningsgrunnlag = 31000.0
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = beregnetInntekt.månedlig)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, sammenligningsgrunnlag.månedlig)

        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_30,
            ledd = 2.ledd,
            punktum = 1.punktum,
            versjon = LocalDate.of(2017, 4, 5),
            input = mapOf(
                "maksimaltTillattAvvikPåÅrsinntekt" to 25.0,
                "grunnlagForSykepengegrunnlag" to beregnetInntekt * 12,
                "sammenligningsgrunnlag" to sammenligningsgrunnlag * 12
            ),
            output = mapOf(
                "avviksprosent" to 25.1
            )
        )
    }

    @Test
    fun `§ 8-30 ledd 2 punktum 1 - gjør ikke ny juridisk vurdering ved sjekk av inntektsavvik i forlengelse av kort periode`() {
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
        håndterUtbetalt()

        SubsumsjonInspektør(jurist).assertIkkeVurdert(PARAGRAF_8_30, LEDD_2, punktum = 1.punktum, vedtaksperiodeId = 1.vedtaksperiode)
        SubsumsjonInspektør(jurist).assertVurdert(PARAGRAF_8_30, LEDD_2, punktum = 1.punktum, vedtaksperiodeId = 2.vedtaksperiode)
    }

    @Test
    fun `§ 8-30 ledd 2 - beregning av sammenligningsgrunnlaget`() {
        val beregnetInntekt = 38750.0
        val sammenligningsgrunnlag = 31000.0
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = beregnetInntekt.månedlig)
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode, sammenligningsgrunnlag.månedlig)
        håndterYtelser(1.vedtaksperiode)

        SubsumsjonInspektør(jurist).assertBeregnet(
            paragraf = PARAGRAF_8_30,
            ledd = 2.ledd,
            versjon = 1.januar(2019),
            input = mapOf(
                "skjæringstidspunkt" to 1.januar(2018),
                "inntekterFraAOrdningen" to mapOf(
                    ORGNUMMER to listOf(
                        mapOf(
                            "beløp" to 31000.0,
                            "årMåned" to YearMonth.of(2017, 1),
                            "type" to "LØNNSINNTEKT",
                            "fordel" to "kontantytelse",
                            "beskrivelse" to "fastloenn"
                        ),
                        mapOf(
                            "beløp" to 31000.0,
                            "årMåned" to YearMonth.of(2017, 2),
                            "type" to "LØNNSINNTEKT",
                            "fordel" to "kontantytelse",
                            "beskrivelse" to "fastloenn"
                        ),
                        mapOf(
                            "beløp" to 31000.0,
                            "årMåned" to YearMonth.of(2017, 3),
                            "type" to "LØNNSINNTEKT",
                            "fordel" to "kontantytelse",
                            "beskrivelse" to "fastloenn"
                        ),
                        mapOf(
                            "beløp" to 31000.0,
                            "årMåned" to YearMonth.of(2017, 4),
                            "type" to "LØNNSINNTEKT",
                            "fordel" to "kontantytelse",
                            "beskrivelse" to "fastloenn"
                        ),
                        mapOf(
                            "beløp" to 31000.0,
                            "årMåned" to YearMonth.of(2017, 5),
                            "type" to "LØNNSINNTEKT",
                            "fordel" to "kontantytelse",
                            "beskrivelse" to "fastloenn"
                        ),
                        mapOf(
                            "beløp" to 31000.0,
                            "årMåned" to YearMonth.of(2017, 6),
                            "type" to "LØNNSINNTEKT",
                            "fordel" to "kontantytelse",
                            "beskrivelse" to "fastloenn"
                        ),
                        mapOf(
                            "beløp" to 31000.0,
                            "årMåned" to YearMonth.of(2017, 7),
                            "type" to "LØNNSINNTEKT",
                            "fordel" to "kontantytelse",
                            "beskrivelse" to "fastloenn"
                        ),
                        mapOf(
                            "beløp" to 31000.0,
                            "årMåned" to YearMonth.of(2017, 8),
                            "type" to "LØNNSINNTEKT",
                            "fordel" to "kontantytelse",
                            "beskrivelse" to "fastloenn"
                        ),
                        mapOf(
                            "beløp" to 31000.0,
                            "årMåned" to YearMonth.of(2017, 9),
                            "type" to "LØNNSINNTEKT",
                            "fordel" to "kontantytelse",
                            "beskrivelse" to "fastloenn"
                        ),
                        mapOf(
                            "beløp" to 31000.0,
                            "årMåned" to YearMonth.of(2017, 10),
                            "type" to "LØNNSINNTEKT",
                            "fordel" to "kontantytelse",
                            "beskrivelse" to "fastloenn"
                        ),
                        mapOf(
                            "beløp" to 31000.0,
                            "årMåned" to YearMonth.of(2017, 11),
                            "type" to "LØNNSINNTEKT",
                            "fordel" to "kontantytelse",
                            "beskrivelse" to "fastloenn"
                        ),
                        mapOf(
                            "beløp" to 31000.0,
                            "årMåned" to YearMonth.of(2017, 12),
                            "type" to "LØNNSINNTEKT",
                            "fordel" to "kontantytelse",
                            "beskrivelse" to "fastloenn"
                        )
                    )
                )
            ),
            output = mapOf(
                "sammenligningsgrunnlag" to 372000.0
            )
        )
    }

    @Test
    fun `§ 8-51 ledd 2 - er ikke over 67 år`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 187268.årlig)
        håndterYtelser()
        val arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 5.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold)

        SubsumsjonInspektør(jurist).assertIkkeVurdert(PARAGRAF_8_51, ledd = LEDD_2)
    }

    @Test
    fun `§ 8-51 ledd 2 - har minimum inntekt 2G - over 67 år`() {
        val personOver67år = "01014500065".somFødselsnummer()
        createTestPerson(personOver67år)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = personOver67år)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), fnr = personOver67år)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 187268.årlig, fnr = personOver67år)
        håndterYtelser(fnr = personOver67år)
        val arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 5.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold, fnr = personOver67år)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_2,
            versjon = 16.desember(2011),
            input = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "alderPåSkjæringstidspunkt" to 73,
                "grunnlagForSykepengegrunnlag" to 187268.0,
                "minimumInntekt" to 187268.0
            ),
            output = emptyMap()
        )
        SubsumsjonInspektør(jurist).assertIkkeVurdert(PARAGRAF_8_3, ledd = LEDD_2, 1.punktum)
    }

    @Test
    fun `§ 8-51 ledd 2 - har inntekt mindre enn 2G - over 67 år`() {
        val personOver67år = "01014500065".somFødselsnummer()
        createTestPerson(personOver67år)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = personOver67år)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), fnr = personOver67år)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 187267.årlig, fnr = personOver67år)
        håndterYtelser(fnr = personOver67år)
        val arbeidsforhold = listOf(Vilkårsgrunnlag.Arbeidsforhold(ORGNUMMER, 5.desember(2017), 31.januar))
        håndterVilkårsgrunnlag(arbeidsforhold = arbeidsforhold, fnr = personOver67år)

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_2,
            versjon = 16.desember(2011),
            input = mapOf(
                "skjæringstidspunkt" to 1.januar,
                "alderPåSkjæringstidspunkt" to 73,
                "grunnlagForSykepengegrunnlag" to 187267.0,
                "minimumInntekt" to 187268.0
            ),
            output = emptyMap()
        )
        SubsumsjonInspektør(jurist).assertIkkeVurdert(PARAGRAF_8_3, ledd = LEDD_2, 1.punktum)
    }

    @Test
    fun `§ 8-51 ledd 3 - 60 sykedager etter fylte 67 år - frisk på 60-årsdagen så total sykedager blir en dag mindre uten at maksdato endres`() {
        val personOver67år = "01025100065".somFødselsnummer()
        createTestPerson(personOver67år)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), fnr = personOver67år)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), fnr = personOver67år)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, fnr = personOver67år)
        håndterYtelser(1.vedtaksperiode, fnr = personOver67år)
        håndterVilkårsgrunnlag(1.vedtaksperiode, fnr = personOver67år)
        håndterYtelser(1.vedtaksperiode, fnr = personOver67år)
        håndterSimulering(1.vedtaksperiode, fnr = personOver67år)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, fnr = personOver67år)
        håndterUtbetalt(fnr = personOver67år)

        håndterSykmelding(Sykmeldingsperiode(2.februar, 28.februar, 100.prosent), fnr = personOver67år)
        håndterSøknad(Sykdom(2.februar, 28.februar, 100.prosent), fnr = personOver67år)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 2.februar, fnr = personOver67år)
        håndterYtelser(2.vedtaksperiode, fnr = personOver67år)
        håndterVilkårsgrunnlag(2.vedtaksperiode, fnr = personOver67år)
        håndterYtelser(2.vedtaksperiode, fnr = personOver67år)
        håndterSimulering(2.vedtaksperiode, fnr = personOver67år)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, fnr = personOver67år)
        håndterUtbetalt(fnr = personOver67år)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            versjon = 16.desember(2011),
            input = mapOf(
                "fom" to 1.januar,
                "tom" to 31.januar,
                "utfallFom" to 17.januar,
                "utfallTom" to 31.januar,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 61,
                "forbrukteSykedager" to 11,
                "maksdato" to 26.april
            ),
            vedtaksperiodeId = 1.vedtaksperiode
        )

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            versjon = 16.desember(2011),
            input = mapOf(
                "fom" to 2.februar,
                "tom" to 28.februar,
                "utfallFom" to 2.februar,
                "utfallTom" to 28.februar,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100),
                        mapOf("fom" to 2.februar, "tom" to 28.februar, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100),
                    mapOf("fom" to 2.februar, "tom" to 28.februar, "dagtype" to "NAVDAG", "grad" to 100)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 41,
                "forbrukteSykedager" to 30,
                "maksdato" to 26.april
            ),
            vedtaksperiodeId = 2.vedtaksperiode
        )
    }

    @Test
    fun `§ 8-51 ledd 3 - 60 sykedager etter fylte 67 år - frisk dagen etter 67-årsdagen så maksdato flyttes en dag`() {
        val personOver67år = "01025100065".somFødselsnummer()
        createTestPerson(personOver67år)

        håndterSykmelding(Sykmeldingsperiode(1.januar, 1.februar, 100.prosent), fnr = personOver67år)
        håndterSøknad(Sykdom(1.januar, 1.februar, 100.prosent), fnr = personOver67år)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, fnr = personOver67år)
        håndterYtelser(1.vedtaksperiode, fnr = personOver67år)
        håndterVilkårsgrunnlag(1.vedtaksperiode, fnr = personOver67år)
        håndterYtelser(1.vedtaksperiode, fnr = personOver67år)
        håndterSimulering(1.vedtaksperiode, fnr = personOver67år)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, fnr = personOver67år)
        håndterUtbetalt(fnr = personOver67år)

        håndterSykmelding(Sykmeldingsperiode(3.februar, 28.februar, 100.prosent), fnr = personOver67år)
        håndterSøknad(Sykdom(3.februar, 28.februar, 100.prosent), fnr = personOver67år)
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 3.februar, fnr = personOver67år)
        håndterYtelser(2.vedtaksperiode, fnr = personOver67år)
        håndterVilkårsgrunnlag(2.vedtaksperiode, fnr = personOver67år)
        håndterYtelser(2.vedtaksperiode, fnr = personOver67år)
        håndterSimulering(2.vedtaksperiode, fnr = personOver67år)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, fnr = personOver67år)
        håndterUtbetalt(fnr = personOver67år)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            versjon = 16.desember(2011),
            input = mapOf(
                "fom" to 1.januar,
                "tom" to 1.februar,
                "utfallFom" to 17.januar,
                "utfallTom" to 1.februar,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 1.februar, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 1.februar, "dagtype" to "NAVDAG", "grad" to 100)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 60,
                "forbrukteSykedager" to 12,
                "maksdato" to 26.april
            ),
            vedtaksperiodeId = 1.vedtaksperiode
        )

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            versjon = 16.desember(2011),
            input = mapOf(
                "fom" to 3.februar,
                "tom" to 28.februar,
                "utfallFom" to 3.februar,
                "utfallTom" to 28.februar,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 1.februar, "dagtype" to "NAVDAG", "grad" to 100),
                        mapOf("fom" to 3.februar, "tom" to 28.februar, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 1.februar, "dagtype" to "NAVDAG", "grad" to 100),
                    mapOf("fom" to 3.februar, "tom" to 28.februar, "dagtype" to "NAVDAG", "grad" to 100)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 42,
                "forbrukteSykedager" to 30,
                "maksdato" to 27.april
            ),
            vedtaksperiodeId = 2.vedtaksperiode
        )
    }

    @Test
    fun `§ 8-51 ledd 3 - 60 sykedager etter fylte 67 år - syk 60 dager etter fylte 67 år`() {
        val personOver67år = "01025100065".somFødselsnummer()
        createTestPerson(personOver67år)
        nyttVedtak(1.januar, 31.januar, fnr = personOver67år)
        forlengVedtak(1.februar, 28.februar, fnr = personOver67år)
        forlengVedtak(1.mars, 31.mars, fnr = personOver67år)
        forlengVedtak(1.april, 26.april, fnr = personOver67år)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            versjon = 16.desember(2011),
            input = mapOf(
                "fom" to 1.januar,
                "tom" to 31.januar,
                "utfallFom" to 17.januar,
                "utfallTom" to 31.januar,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 61,
                "forbrukteSykedager" to 11,
                "maksdato" to 26.april
            ),
            vedtaksperiodeId = 1.vedtaksperiode
        )

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            versjon = 16.desember(2011),
            input = mapOf(
                "fom" to 1.februar,
                "tom" to 28.februar,
                "utfallFom" to 1.februar,
                "utfallTom" to 28.februar,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 28.februar, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 28.februar, "dagtype" to "NAVDAG", "grad" to 100)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 41,
                "forbrukteSykedager" to 31,
                "maksdato" to 26.april
            ),
            vedtaksperiodeId = 2.vedtaksperiode
        )

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            versjon = 16.desember(2011),
            input = mapOf(
                "fom" to 1.mars,
                "tom" to 31.mars,
                "utfallFom" to 1.mars,
                "utfallTom" to 31.mars,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 31.mars, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 31.mars, "dagtype" to "NAVDAG", "grad" to 100)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 19,
                "forbrukteSykedager" to 53,
                "maksdato" to 26.april
            ),
            vedtaksperiodeId = 3.vedtaksperiode
        )

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            versjon = 16.desember(2011),
            input = mapOf(
                "fom" to 1.april,
                "tom" to 26.april,
                "utfallFom" to 1.april,
                "utfallTom" to 26.april,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 26.april, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 26.april, "dagtype" to "NAVDAG", "grad" to 100)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 0,
                "forbrukteSykedager" to 72,
                "maksdato" to 26.april
            ),
            vedtaksperiodeId = 4.vedtaksperiode
        )
    }

    @Test
    fun `§ 8-51 ledd 3 - 60 sykedager etter fylte 67 år - syk 61 dager etter fylte 67 år`() {
        val personOver67år = "01025100065".somFødselsnummer()
        createTestPerson(personOver67år)
        nyttVedtak(1.januar, 31.januar, fnr = personOver67år)
        forlengVedtak(1.februar, 28.februar, fnr = personOver67år)
        forlengVedtak(1.mars, 31.mars, fnr = personOver67år)
        forlengVedtak(1.april, 27.april, fnr = personOver67år)

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            versjon = 16.desember(2011),
            input = mapOf(
                "fom" to 1.januar,
                "tom" to 31.januar,
                "utfallFom" to 17.januar,
                "utfallTom" to 31.januar,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 61,
                "forbrukteSykedager" to 11,
                "maksdato" to 26.april
            ),
            vedtaksperiodeId = 1.vedtaksperiode
        )

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            versjon = 16.desember(2011),
            input = mapOf(
                "fom" to 1.februar,
                "tom" to 28.februar,
                "utfallFom" to 1.februar,
                "utfallTom" to 28.februar,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 28.februar, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 28.februar, "dagtype" to "NAVDAG", "grad" to 100)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 41,
                "forbrukteSykedager" to 31,
                "maksdato" to 26.april
            ),
            vedtaksperiodeId = 2.vedtaksperiode
        )

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            versjon = 16.desember(2011),
            input = mapOf(
                "fom" to 1.mars,
                "tom" to 31.mars,
                "utfallFom" to 1.mars,
                "utfallTom" to 31.mars,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 31.mars, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 31.mars, "dagtype" to "NAVDAG", "grad" to 100)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 19,
                "forbrukteSykedager" to 53,
                "maksdato" to 26.april
            ),
            vedtaksperiodeId = 3.vedtaksperiode
        )

        SubsumsjonInspektør(jurist).assertOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            versjon = 16.desember(2011),
            input = mapOf(
                "fom" to 1.april,
                "tom" to 27.april,
                "utfallFom" to 1.april,
                "utfallTom" to 26.april,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 27.april, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 27.april, "dagtype" to "NAVDAG", "grad" to 100)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 0,
                "forbrukteSykedager" to 72,
                "maksdato" to 26.april
            ),
            vedtaksperiodeId = 4.vedtaksperiode
        )

        SubsumsjonInspektør(jurist).assertIkkeOppfylt(
            paragraf = PARAGRAF_8_51,
            ledd = LEDD_3,
            versjon = 16.desember(2011),
            input = mapOf(
                "fom" to 1.april,
                "tom" to 27.april,
                "utfallFom" to 27.april,
                "utfallTom" to 27.april,
                "tidslinjegrunnlag" to listOf(
                    listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                        mapOf("fom" to 17.januar, "tom" to 27.april, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                "beregnetTidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "AGPDAG", "grad" to 0),
                    mapOf("fom" to 17.januar, "tom" to 27.april, "dagtype" to "NAVDAG", "grad" to 100)
                )
            ),
            output = mapOf(
                "gjenståendeSykedager" to 0,
                "forbrukteSykedager" to 72,
                "maksdato" to 26.april
            ),
            vedtaksperiodeId = 4.vedtaksperiode
        )
    }
}
