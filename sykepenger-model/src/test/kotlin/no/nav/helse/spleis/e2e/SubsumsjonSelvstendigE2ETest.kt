package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.time.Year
import no.nav.helse.Toggle
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.selvstendig
import no.nav.helse.etterlevelse.FOLKETRYGDLOVENS_OPPRINNELSESDATO
import no.nav.helse.etterlevelse.Ledd
import no.nav.helse.etterlevelse.Ledd.LEDD_2
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_12
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_34
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_11
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_35
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerfilter.Companion.TILSTREKKELIG_OPPHOLD_I_SYKEDAGER
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SubsumsjonSelvstendigE2ETest : AbstractDslTest() {

    @Test
    fun `§ 8-12 ledd 2 - Vurdering av ny rett til sykepenger`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            nyttVedtak(1.mai(2017) til 31.mai(2017))
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
                Søknad.Søknadsperiode.Venteperiode(1.januar til 16.januar),
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig)
                )
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = emptyList())
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
                            mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTEPERIODEDAG", "grad" to 100),
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
                        mapOf("fom" to 1.januar, "tom" to 16.januar, "dagtype" to "VENTEPERIODEDAG", "grad" to 100),
                        mapOf("fom" to 17.januar, "tom" to 31.januar, "dagtype" to "NAVDAG", "grad" to 100)
                    )
                ),
                output = emptyMap()
            )

            assertVarsel(Varselkode.RV_SØ_45, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `§ 8-35 ledd 2 - selvstendignæringsdrivende sykepengegrunnlag`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
                Søknad.Søknadsperiode.Venteperiode(1.januar til 16.januar),
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig)
                )
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = emptyList())
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

            assertVarsel(Varselkode.RV_SØ_45, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Subsumerer § 8-11 - utbetaler ikke helg`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
                Søknad.Søknadsperiode.Venteperiode(1.januar til 16.januar),
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig)
                )
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = emptyList())
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

            assertVarsel(Varselkode.RV_SØ_45, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `Subsumerer 8-34 ledd 1 for selvstendig uten forsikring`() = Toggle.SelvstendigNæringsdrivende.enable {
        selvstendig {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
                Søknad.Søknadsperiode.Venteperiode(1.januar til 16.januar),
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig)
                )
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = emptyList())
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
            assertVarsel(Varselkode.RV_SØ_45, 1.vedtaksperiode.filter())
        }
    }
}
