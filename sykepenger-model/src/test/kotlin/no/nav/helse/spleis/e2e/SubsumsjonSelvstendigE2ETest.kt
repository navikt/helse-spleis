package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.Year
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.selvstendig
import no.nav.helse.etterlevelse.FOLKETRYGDLOVENS_OPPRINNELSESDATO
import no.nav.helse.etterlevelse.Ledd
import no.nav.helse.etterlevelse.Ledd.LEDD_2
import no.nav.helse.etterlevelse.Ledd.LEDD_3
import no.nav.helse.etterlevelse.Paragraf.KJENNELSE_2006_4023
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_22_13
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_11
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_12
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_3
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_34
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_35
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_48
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_51
import no.nav.helse.etterlevelse.Punktum.Companion.punktum
import no.nav.helse.etterlevelse.Subsumsjon.Utfall.VILKAR_IKKE_OPPFYLT
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerfilter.Companion.TILSTREKKELIG_OPPHOLD_I_SYKEDAGER
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SubsumsjonSelvstendigE2ETest : AbstractDslTest() {
    private val FYLLER_68_1_JANUAR = 1.januar(1950)

    @Test
    fun `22-13 ledd 3 - Vurdering av foreldelse`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar, sendtTilNAVEllerArbeidsgiver = LocalDate.of(2018, 5, 1).atStartOfDay())

            SubsumsjonInspektør(jurist).assertIkkeOppfylt(
                paragraf = PARAGRAF_22_13,
                ledd = LEDD_3,
                versjon = LocalDate.of(2011, 12, 16),
                input = mapOf("avskjæringsdato" to 1.februar),
                output = mapOf(
                    "perioder" to
                        listOf(
                            mapOf(
                                "fom" to 1.januar,
                                "tom" to 5.januar,
                            ),
                            mapOf(
                                "fom" to 8.januar,
                                "tom" to 12.januar,
                            ),
                            mapOf(
                                "fom" to 15.januar,
                                "tom" to 19.januar,
                            ),
                            mapOf(
                                "fom" to 22.januar,
                                "tom" to 26.januar,
                            ),
                            mapOf(
                                "fom" to 29.januar,
                                "tom" to 31.januar,
                            ),
                        )
                )
            )

            assertVarsel(Varselkode.RV_SØ_2, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `§ 8-12 ledd 2 - Vurdering av ny rett til sykepenger`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            nyttVedtak(1.mai(2017) til 31.mai(2017))
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            SubsumsjonInspektør(jurist).assertOppfylt(
                paragraf = PARAGRAF_8_12,
                ledd = LEDD_2,
                versjon = 21.mai(2021),
                input = mapOf(
                    "dato" to LocalDate.of(2017, 11, 29),
                    "tilstrekkeligOppholdISykedager" to TILSTREKKELIG_OPPHOLD_I_SYKEDAGER,
                    "tidslinjegrunnlag" to listOf(
                        listOf(
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                            mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100),
                        ),
                        listOf(
                            mapOf("fom" to 1.mai(2017), "tom" to 16.mai(2017), "dagtype" to "AGPDAG", "grad" to 100),
                            mapOf("fom" to 17.mai(2017), "tom" to 31.mai(2017), "dagtype" to "NAVDAG", "grad" to 100)
                        ),
                    ),
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.mai(2017), "tom" to 16.mai(2017), "dagtype" to "AGPDAG", "grad" to 100),
                        mapOf("fom" to 17.mai(2017), "tom" to 31.mai(2017), "dagtype" to "NAVDAG", "grad" to 100),
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                        mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                output = emptyMap()
            )

        }
    }

    @Test
    fun `§ 8-35 ledd 2 - selvstendignæringsdrivende sykepengegrunnlag`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            SubsumsjonInspektør(jurist).assertBeregnet(
                paragraf = PARAGRAF_8_35,
                ledd = LEDD_2,
                versjon = 20.desember(2018),
                input = mapOf(
                    "skjæringstidspunkt" to 1.januar,
                    "pensjonsgivendeInntekter" to listOf(
                        mapOf<String, Any>("pensjonsgivendeInntekt" to 450000.0, "årstall" to Year.of(2017), "gjennomsnittligG" to 93281.0),
                        mapOf<String, Any>("pensjonsgivendeInntekt" to 450000.0, "årstall" to Year.of(2016), "gjennomsnittligG" to 91740.0),
                        mapOf<String, Any>("pensjonsgivendeInntekt" to 450000.0, "årstall" to Year.of(2015), "gjennomsnittligG" to 89502.0)
                    ),
                    "nåværendeGrunnbeløp" to 93634.0
                ),
                output = mapOf("sykepengegrunnlag" to 460589.0)
            )

        }
    }

    @Test
    fun `Subsumerer § 8-11 - utbetaler ikke helg`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            SubsumsjonInspektør(jurist).assertIkkeOppfylt(
                paragraf = PARAGRAF_8_11,
                versjon = FOLKETRYGDLOVENS_OPPRINNELSESDATO,
                input = mapOf(
                    "periode" to mapOf("fom" to 1.januar, "tom" to 31.januar)
                ),
                output = mapOf(
                    "perioder" to listOf(
                        mapOf("fom" to 20.januar, "tom" to 21.januar),
                        mapOf("fom" to 27.januar, "tom" to 28.januar)
                    )
                )
            )

        }
    }

    @Test
    fun `Subsumerer 8-34 ledd 1 for selvstendig uten forsikring`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            val antallSubsumsjoner = { subsumsjonInspektør: SubsumsjonInspektør ->
                subsumsjonInspektør.antallSubsumsjoner(
                    paragraf = PARAGRAF_8_34,
                    versjon = 1.januar(2019),
                    ledd = Ledd.LEDD_1,
                    punktum = null,
                    bokstav = null
                )
            }
            assertSubsumsjoner { assertEquals(1, antallSubsumsjoner(this)) }

        }
    }

    @Test
    fun `§ 8-51 ledd 2 - er ikke over 67 år`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)

            SubsumsjonInspektør(jurist).assertIkkeVurdert(PARAGRAF_8_51, ledd = LEDD_2)

        }
    }

    @Test
    fun `§ 8-51 ledd 2 - har minimum inntekt over 2G - over 67 år`() = Toggle.SelvstendigNæringsdrivende.enable {
        medFødselsdato(FYLLER_68_1_JANUAR)
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            SubsumsjonInspektør(jurist).assertOppfylt(
                paragraf = PARAGRAF_8_51,
                versjon = 16.desember(2011),
                ledd = LEDD_2,
                input = mapOf(
                    "sekstisyvårsdag" to 1.januar(2017),
                    "utfallFom" to 1.januar,
                    "utfallTom" to 31.januar,
                    "periodeFom" to 1.januar,
                    "periodeTom" to 31.januar,
                    "grunnlagForSykepengegrunnlag" to 460589.0,
                    "minimumInntekt" to 187268.0
                ),
                output = emptyMap()
            )
            SubsumsjonInspektør(jurist).assertIkkeVurdert(PARAGRAF_8_3, ledd = LEDD_2, 1.punktum)

        }
    }

    @Test
    fun `§ 8-51 ledd 2 - har minimum inntekt under 2G - over 67 år`() = Toggle.SelvstendigNæringsdrivende.enable {
        medFødselsdato(FYLLER_68_1_JANUAR)
        selvstendig {
            håndterSøknadSelvstendig(
                periode = januar,
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 100_000.årlig, INGEN, INGEN, INGEN),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 100_000.årlig, INGEN, INGEN, INGEN),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 100_000.årlig, INGEN, INGEN, INGEN)
                )
            )
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)

            SubsumsjonInspektør(jurist).assertIkkeOppfylt(
                paragraf = PARAGRAF_8_51,
                versjon = 16.desember(2011),
                ledd = LEDD_2,
                input = mapOf(
                    "sekstisyvårsdag" to 1.januar(2017),
                    "utfallFom" to 1.januar,
                    "utfallTom" to 31.januar,
                    "periodeFom" to 1.januar,
                    "periodeTom" to 31.januar,
                    "grunnlagForSykepengegrunnlag" to 102353.0,
                    "minimumInntekt" to 187268.0
                ),
                output = emptyMap()
            )
            SubsumsjonInspektør(jurist).assertIkkeVurdert(PARAGRAF_8_3, ledd = LEDD_2, 1.punktum)
            assertVarsler(1.vedtaksperiode, Varselkode.RV_SV_1)
        }
    }

    @Test
    fun `§ 8-51 ledd 2 - avslag subsumeres når person blir 67 år underveis i sykefraværstilfellet og tjener mindre enn 2G`() = Toggle.SelvstendigNæringsdrivende.enable {
        val blir67Underveis = 5.februar(1951)
        medFødselsdato(blir67Underveis)
        selvstendig {
            // inntekt mellom 0.5G og 2G - slik at kravet er oppfylt før personen fylte 67, men ikke etter
            håndterSøknadSelvstendig(
                periode = januar,
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 100_000.årlig, INGEN, INGEN, INGEN),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 100_000.årlig, INGEN, INGEN, INGEN),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 100_000.årlig, INGEN, INGEN, INGEN)
                )
            )
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()


            SubsumsjonInspektør(jurist).assertOppfylt(
                paragraf = PARAGRAF_8_3,
                ledd = LEDD_2,
                punktum = 1.punktum,
                input = null,
                versjon = 16.desember(2011)
            )

            håndterSøknadSelvstendig(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()
            assertVarsler(2.vedtaksperiode, Varselkode.RV_SV_1)

            SubsumsjonInspektør(jurist).assertIkkeOppfylt(
                paragraf = PARAGRAF_8_51,
                ledd = LEDD_2,
                versjon = 16.desember(2011),
                input = mapOf(
                    "sekstisyvårsdag" to 5.februar(2018),
                    "utfallFom" to 6.februar,
                    "utfallTom" to 28.februar,
                    "periodeFom" to 1.februar,
                    "periodeTom" to 28.februar,
                    "grunnlagForSykepengegrunnlag" to 102353.0,
                    "minimumInntekt" to 187268.0
                ),
                output = emptyMap()
            )

            håndterSøknadSelvstendig(mars)
            håndterYtelser(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            assertVarsler(3.vedtaksperiode, Varselkode.RV_SV_1)

            assertEquals(
                2, SubsumsjonInspektør(jurist).antallSubsumsjoner(
                paragraf = PARAGRAF_8_3,
                ledd = LEDD_2,
                punktum = 1.punktum,
                versjon = 16.desember(2011),
                bokstav = null
            )
            )
            SubsumsjonInspektør(jurist).assertPaaIndeks(
                paragraf = PARAGRAF_8_51,
                ledd = LEDD_2,
                versjon = 16.desember(2011),
                input = mapOf(
                    "sekstisyvårsdag" to 5.februar(2018),
                    "utfallFom" to 1.mars,
                    "utfallTom" to 31.mars,
                    "periodeFom" to 1.mars,
                    "periodeTom" to 31.mars,
                    "grunnlagForSykepengegrunnlag" to 102353.0,
                    "minimumInntekt" to 187268.0
                ),
                index = 1,
                output = emptyMap(),
                forventetAntall = 2,
                utfall = VILKAR_IKKE_OPPFYLT
            )
        }
    }

    @Test
    fun `§ 8-51 ledd 3 - 60 sykedager etter fylte 67 år - frisk på 67-årsdagen så total sykedager blir en dag mindre uten at maksdato endres`() = Toggle.SelvstendigNæringsdrivende.enable {
        val blir67Underveis = 1.februar(1951)
        medFødselsdato(blir67Underveis)
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()


            håndterSøknadSelvstendig(2.februar til 28.februar)
            håndterVilkårsgrunnlagSelvstendig(2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()


            SubsumsjonInspektør(jurist).assertOppfylt(
                paragraf = PARAGRAF_8_51,
                versjon = 16.desember(2011),
                ledd = LEDD_3,
                input = mapOf(
                    "fom" to 1.januar,
                    "tom" to 31.januar,
                    "utfallFom" to 17.januar,
                    "utfallTom" to 31.januar,
                    "tidslinjegrunnlag" to listOf(
                        listOf(
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                            mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
                        )
                    ),
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
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
                versjon = 16.desember(2011),
                ledd = LEDD_3,
                input = mapOf(
                    "fom" to 2.februar,
                    "tom" to 28.februar,
                    "utfallFom" to 2.februar,
                    "utfallTom" to 28.februar,
                    "tidslinjegrunnlag" to listOf(
                        listOf(
                            mapOf("fom" to 2.februar, "tom" to 28.februar, "dagtype" to "NAVDAG", "grad" to 100)
                        ),
                        listOf(
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                            mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
                        )
                    ),
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
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
    }

    @Test
    fun `§ 8-51 ledd 3 - 60 sykedager etter fylte 67 år - frisk dagen etter 67-årsdagen så maksdato flyttes en dag`() = Toggle.SelvstendigNæringsdrivende.enable {
        val blir67Underveis = 1.februar(1951)
        medFødselsdato(blir67Underveis)
        selvstendig {
            håndterSøknadSelvstendig(1.januar til 1.februar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()


            håndterSøknadSelvstendig(3.februar til 28.februar)
            håndterVilkårsgrunnlag(2.vedtaksperiode, skatteinntekter = emptyList())
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()


            SubsumsjonInspektør(jurist).assertOppfylt(
                paragraf = PARAGRAF_8_51,
                versjon = 16.desember(2011),
                ledd = LEDD_3,
                input = mapOf(
                    "fom" to 1.januar,
                    "tom" to 1.februar,
                    "utfallFom" to 17.januar,
                    "utfallTom" to 1.februar,
                    "tidslinjegrunnlag" to listOf(
                        listOf(
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                            mapOf("fom" to 17.januar, "tom" to 1.februar, "dagtype" to "NAVDAG", "grad" to 100)
                        )
                    ),
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                        mapOf("fom" to 17.januar, "tom" to 1.februar, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                output = mapOf(
                    "gjenståendeSykedager" to 60,
                    "forbrukteSykedager" to 12,
                    "maksdato" to 26.april
                ),
                vedtaksperiodeId = 1.vedtaksperiode,
            )

            SubsumsjonInspektør(jurist).assertOppfylt(
                paragraf = PARAGRAF_8_51,
                versjon = 16.desember(2011),
                ledd = LEDD_3,
                input = mapOf(
                    "fom" to 3.februar,
                    "tom" to 28.februar,
                    "utfallFom" to 3.februar,
                    "utfallTom" to 28.februar,
                    "tidslinjegrunnlag" to listOf(
                        listOf(
                            mapOf("fom" to 3.februar, "tom" to 28.februar, "dagtype" to "NAVDAG", "grad" to 100)
                        ),
                        listOf(
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                            mapOf("fom" to 17.januar, "tom" to 1.februar, "dagtype" to "NAVDAG", "grad" to 100),
                        )
                    ),
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
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
    }

    @Test
    fun `§ 8-51 ledd 3 - 60 sykedager etter fylte 67 år - syk 60 dager etter fylte 67 år`() = Toggle.SelvstendigNæringsdrivende.enable {
        val blir67Underveis = 1.februar(1951)
        medFødselsdato(blir67Underveis)
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknadSelvstendig(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknadSelvstendig(mars)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknadSelvstendig(1.april til 26.april)
            håndterYtelser(4.vedtaksperiode)
            håndterSimulering(4.vedtaksperiode)
            håndterUtbetalingsgodkjenning(4.vedtaksperiode)
            håndterUtbetalt()

            SubsumsjonInspektør(jurist).assertOppfylt(
                paragraf = PARAGRAF_8_51,
                versjon = 16.desember(2011),
                ledd = LEDD_3,
                input = mapOf(
                    "fom" to 1.januar,
                    "tom" to 31.januar,
                    "utfallFom" to 17.januar,
                    "utfallTom" to 31.januar,
                    "tidslinjegrunnlag" to listOf(
                        listOf(
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                            mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
                        )
                    ),
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
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
                versjon = 16.desember(2011),
                ledd = LEDD_3,
                input = mapOf(
                    "fom" to 1.februar,
                    "tom" to 28.februar,
                    "utfallFom" to 1.februar,
                    "utfallTom" to 28.februar,
                    "tidslinjegrunnlag" to listOf(
                        listOf(
                            mapOf("fom" to 1.februar, "tom" to 28.februar, "dagtype" to "NAVDAG", "grad" to 100)
                        ),
                        listOf(
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                            mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100),
                        )
                    ),
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
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
                versjon = 16.desember(2011),
                ledd = LEDD_3,
                input = mapOf(
                    "fom" to 1.mars,
                    "tom" to 31.mars,
                    "utfallFom" to 1.mars,
                    "utfallTom" to 31.mars,
                    "tidslinjegrunnlag" to listOf(
                        listOf(
                            mapOf("fom" to 1.mars, "tom" to 31.mars, "dagtype" to "NAVDAG", "grad" to 100)
                        ),
                        listOf(
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                            mapOf("fom" to 17.januar, "tom" to 28.februar, "dagtype" to "NAVDAG", "grad" to 100)
                        )
                    ),
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
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
                versjon = 16.desember(2011),
                ledd = LEDD_3,
                input = mapOf(
                    "fom" to 1.april,
                    "tom" to 26.april,
                    "utfallFom" to 1.april,
                    "utfallTom" to 26.april,
                    "tidslinjegrunnlag" to listOf(
                        listOf(
                            mapOf("fom" to 1.april, "tom" to 26.april, "dagtype" to "NAVDAG", "grad" to 100)
                        ),
                        listOf(
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                            mapOf("fom" to 17.januar, "tom" to 31.mars, "dagtype" to "NAVDAG", "grad" to 100)
                        )
                    ),
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
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
    }

    @Test
    fun `§ 8-51 ledd 3 - 60 sykedager etter fylte 67 år - syk 61 dager etter fylte 67 år`() = Toggle.SelvstendigNæringsdrivende.enable {
        val blir67Underveis = 1.februar(1951)
        medFødselsdato(blir67Underveis)
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()


            håndterSøknadSelvstendig(februar)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()


            håndterSøknadSelvstendig(mars)
            håndterYtelser(3.vedtaksperiode)
            håndterSimulering(3.vedtaksperiode)
            håndterUtbetalingsgodkjenning(3.vedtaksperiode)
            håndterUtbetalt()

            håndterSøknadSelvstendig(1.april til 27.april)
            håndterYtelser(4.vedtaksperiode)
            håndterSimulering(4.vedtaksperiode)
            håndterUtbetalingsgodkjenning(4.vedtaksperiode)
            håndterUtbetalt()

            SubsumsjonInspektør(jurist).assertOppfylt(
                paragraf = PARAGRAF_8_51,
                versjon = 16.desember(2011),
                ledd = LEDD_3,
                input = mapOf(
                    "fom" to 1.januar,
                    "tom" to 31.januar,
                    "utfallFom" to 17.januar,
                    "utfallTom" to 31.januar,
                    "tidslinjegrunnlag" to listOf(
                        listOf(
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                            mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
                        )
                    ),
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
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
                versjon = 16.desember(2011),
                ledd = LEDD_3,
                input = mapOf(
                    "fom" to 1.februar,
                    "tom" to 28.februar,
                    "utfallFom" to 1.februar,
                    "utfallTom" to 28.februar,
                    "tidslinjegrunnlag" to listOf(
                        listOf(
                            mapOf("fom" to 1.februar, "tom" to 28.februar, "dagtype" to "NAVDAG", "grad" to 100)
                        ),
                        listOf(
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                            mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
                        )
                    ),
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
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
                versjon = 16.desember(2011),
                ledd = LEDD_3,
                input = mapOf(
                    "fom" to 1.mars,
                    "tom" to 31.mars,
                    "utfallFom" to 1.mars,
                    "utfallTom" to 31.mars,
                    "tidslinjegrunnlag" to listOf(
                        listOf(
                            mapOf("fom" to 1.mars, "tom" to 31.mars, "dagtype" to "NAVDAG", "grad" to 100)
                        ),
                        listOf(
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                            mapOf("fom" to 17.januar, "tom" to 28.februar, "dagtype" to "NAVDAG", "grad" to 100)
                        )
                    ),
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
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
                versjon = 16.desember(2011),
                ledd = LEDD_3,
                input = mapOf(
                    "fom" to 1.april,
                    "tom" to 27.april,
                    "utfallFom" to 1.april,
                    "utfallTom" to 26.april,
                    "tidslinjegrunnlag" to listOf(
                        listOf(
                            mapOf("fom" to 1.april, "tom" to 27.april, "dagtype" to "NAVDAG", "grad" to 100)
                        ),
                        listOf(
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                            mapOf("fom" to 17.januar, "tom" to 31.mars, "dagtype" to "NAVDAG", "grad" to 100)
                        )
                    ),
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
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
                versjon = 16.desember(2011),
                ledd = LEDD_3,
                input = mapOf(
                    "fom" to 1.april,
                    "tom" to 27.april,
                    "utfallFom" to 27.april,
                    "utfallTom" to 27.april,
                    "tidslinjegrunnlag" to listOf(
                        listOf(
                            mapOf("fom" to 1.april, "tom" to 27.april, "dagtype" to "NAVDAG", "grad" to 100)
                        ),
                        listOf(
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
                            mapOf("fom" to 17.januar, "tom" to 31.mars, "dagtype" to "NAVDAG", "grad" to 100)
                        )
                    ),
                    "beregnetTidslinje" to listOf(
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTETIDSDAG", "grad" to 100),
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

    @Test
    fun `andre ytelser i halen`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknadSelvstendig(januar)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)


            håndterOverstyrTidslinje(
                overstyringsdager = listOf(
                    ManuellOverskrivingDag(24.januar, Dagtype.Foreldrepengerdag),
                    ManuellOverskrivingDag(25.januar, Dagtype.Pleiepengerdag),
                    ManuellOverskrivingDag(26.januar, Dagtype.Omsorgspengerdag),
                    ManuellOverskrivingDag(27.januar, Dagtype.Svangerskapspengerdag),
                    ManuellOverskrivingDag(28.januar, Dagtype.Opplaringspengerdag),
                    ManuellOverskrivingDag(29.januar, Dagtype.AAPdag),
                    ManuellOverskrivingDag(30.januar, Dagtype.Dagpengerdag),
                    ManuellOverskrivingDag(31.januar, Dagtype.AAPdag),
                )
            )
            håndterYtelser(1.vedtaksperiode)
            val forventetInput = mapOf(
                "sykdomstidslinje" to listOf(
                    mapOf("fom" to 1.januar, "tom" to 23.januar, "dagtype" to "SYKEDAG", "grad" to 100),
                    mapOf("fom" to 24.januar, "tom" to 24.januar, "dagtype" to "FORELDREPENGER", "grad" to null),
                    mapOf("fom" to 25.januar, "tom" to 25.januar, "dagtype" to "PLEIEPENGER", "grad" to null),
                    mapOf("fom" to 26.januar, "tom" to 26.januar, "dagtype" to "OMSORGSPENGER", "grad" to null),
                    mapOf("fom" to 27.januar, "tom" to 27.januar, "dagtype" to "SVANGERSKAPSPENGER", "grad" to null),
                    mapOf("fom" to 28.januar, "tom" to 28.januar, "dagtype" to "OPPLÆRINGSPENGER", "grad" to null),
                    mapOf("fom" to 29.januar, "tom" to 29.januar, "dagtype" to "ARBEIDSAVKLARINGSPENGER", "grad" to null),
                    mapOf("fom" to 30.januar, "tom" to 30.januar, "dagtype" to "DAGPENGER", "grad" to null),
                    mapOf("fom" to 31.januar, "tom" to 31.januar, "dagtype" to "ARBEIDSAVKLARINGSPENGER", "grad" to null)
                ),
            )
            // Alt utenom Arbeidsavklaringspenger
            SubsumsjonInspektør(jurist).assertIkkeOppfylt(
                lovverk = "trygderetten",
                versjon = 2.mars(2007),
                paragraf = KJENNELSE_2006_4023,
                input = forventetInput,
                output = mapOf(
                    "perioder" to listOf(
                        mapOf("fom" to 24.januar, "tom" to 28.januar),
                        mapOf("fom" to 30.januar, "tom" to 30.januar),
                    )
                )
            )
            // Arbeidsavklaringspenger
            SubsumsjonInspektør(jurist).assertIkkeOppfylt(
                lovverk = "folketrygdloven",
                versjon = 21.mai(2021),
                paragraf = PARAGRAF_8_48,
                input = forventetInput,
                output = mapOf(
                    "perioder" to listOf(
                        mapOf("fom" to 29.januar, "tom" to 29.januar),
                        mapOf("fom" to 31.januar, "tom" to 31.januar),
                    )
                )
            )
        }
    }
}
