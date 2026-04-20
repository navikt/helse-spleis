package no.nav.helse.spleis.e2e

import java.time.LocalDate
import no.nav.helse.Toggle
import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.selvstendig
import no.nav.helse.hendelser.SelvstendigForsikring
import no.nav.helse.hendelser.SelvstendigForsikring.Forsikringstype.HundreProsentFraDagEn
import no.nav.helse.hendelser.SelvstendigForsikring.Forsikringstype.HundreProsentFraDagSytten
import no.nav.helse.hendelser.Søknad
import no.nav.helse.januar
import no.nav.helse.oktober
import no.nav.helse.person.Behandlinger.Behandling.Endring.Arbeidssituasjon
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Test

internal class SelvstendigEndaEnGodkjenningsbehovTest : AbstractDslTest() {

    @Test
    fun `SelvstendigFaktaavklartInntekt - enda en godkjenningsbehov`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = emptyList())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertGodkjenningsbehov(
                behovsoppsamler = testperson.behovsoppsamler,
                tags = setOf("Førstegangsbehandling", "Personutbetaling", "Innvilget", "EnArbeidsgiver"),
                forbrukteSykedager = 11,
                gjenståendeSykedager = 237,
                foreløpigBeregnetSluttPåSykepenger = 28.desember,
                utbetalingsdager = listOf(
                    utbetalingsdag(1.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(2.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(3.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(4.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(5.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(6.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(7.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(8.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(9.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(10.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(11.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(12.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(13.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(14.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(15.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(16.januar, "Ventetidsdag", 0, 100, 80),
                    utbetalingsdag(17.januar, "NavDag", 1417, 100, 80),
                    utbetalingsdag(18.januar, "NavDag", 1417, 100, 80),
                    utbetalingsdag(19.januar, "NavDag", 1417, 100, 80),
                    utbetalingsdag(20.januar, "NavHelgDag", 0, 100, 80),
                    utbetalingsdag(21.januar, "NavHelgDag", 0, 100, 80),
                    utbetalingsdag(22.januar, "NavDag", 1417, 100, 80),
                    utbetalingsdag(23.januar, "NavDag", 1417, 100, 80),
                    utbetalingsdag(24.januar, "NavDag", 1417, 100, 80),
                    utbetalingsdag(25.januar, "NavDag", 1417, 100, 80),
                    utbetalingsdag(26.januar, "NavDag", 1417, 100, 80),
                    utbetalingsdag(27.januar, "NavHelgDag", 0, 100, 80),
                    utbetalingsdag(28.januar, "NavHelgDag", 0, 100, 80),
                    utbetalingsdag(29.januar, "NavDag", 1417, 100, 80),
                    utbetalingsdag(30.januar, "NavDag", 1417, 100, 80),
                    utbetalingsdag(31.januar, "NavDag", 1417, 100, 80)
                ),
                sykepengegrunnlagsfakta = mapOf(
                    "sykepengegrunnlag" to 460_589.0,
                    "6G" to 561_804.0,
                    "fastsatt" to "EtterHovedregel",
                    "arbeidsgivere" to emptyList<Map<String, Any>>(),
                    "selvstendig" to mapOf(
                        "pensjonsgivendeInntekter" to listOf(
                            mapOf(
                                "årstall" to 2017,
                                "beløp" to 450_000.0
                            ),
                            mapOf(
                                "årstall" to 2016,
                                "beløp" to 450_000.0
                            ),
                            mapOf(
                                "årstall" to 2015,
                                "beløp" to 450_000.0
                            )
                        ),
                        "beregningsgrunnlag" to 460589.0,

                        ),
                ),
                inntektskilde = "EN_ARBEIDSGIVER",
                arbeidssituasjon = Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE
            )
        }
    }

    @Test
    fun `SelvstendigFaktaavklartInntekt - enda en godkjenningsbehov med hundre prosent forsikring fra dag en`() = Toggle.SelvstendigForsikring.enable {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = emptyList())
            håndterYtelserSelvstendig(1.vedtaksperiode, selvstendigForsikring = SelvstendigForsikring(14.oktober(2017), null, HundreProsentFraDagEn, 450000.årlig))
            håndterSimulering(1.vedtaksperiode)
            assertGodkjenningsbehov(
                behovsoppsamler = testperson.behovsoppsamler,
                tags = setOf("Førstegangsbehandling", "Personutbetaling", "Innvilget", "EnArbeidsgiver"),
                forbrukteSykedager = 11,
                gjenståendeSykedager = 237,
                foreløpigBeregnetSluttPåSykepenger = 28.desember,
                utbetalingsdager = listOf(
                    utbetalingsdag(1.januar, "Ventetidsdag", 1771, 100, 100),
                    utbetalingsdag(2.januar, "Ventetidsdag", 1771, 100, 100),
                    utbetalingsdag(3.januar, "Ventetidsdag", 1771, 100, 100),
                    utbetalingsdag(4.januar, "Ventetidsdag", 1771, 100, 100),
                    utbetalingsdag(5.januar, "Ventetidsdag", 1771, 100, 100),
                    utbetalingsdag(6.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(7.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(8.januar, "Ventetidsdag", 1771, 100, 100),
                    utbetalingsdag(9.januar, "Ventetidsdag", 1771, 100, 100),
                    utbetalingsdag(10.januar, "Ventetidsdag", 1771, 100, 100),
                    utbetalingsdag(11.januar, "Ventetidsdag", 1771, 100, 100),
                    utbetalingsdag(12.januar, "Ventetidsdag", 1771, 100, 100),
                    utbetalingsdag(13.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(14.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(15.januar, "Ventetidsdag", 1771, 100, 100),
                    utbetalingsdag(16.januar, "Ventetidsdag", 1771, 100, 100),
                    utbetalingsdag(17.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(18.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(19.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(20.januar, "NavHelgDag", 0, 100, 100),
                    utbetalingsdag(21.januar, "NavHelgDag", 0, 100, 100),
                    utbetalingsdag(22.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(23.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(24.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(25.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(26.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(27.januar, "NavHelgDag", 0, 100, 100),
                    utbetalingsdag(28.januar, "NavHelgDag", 0, 100, 100),
                    utbetalingsdag(29.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(30.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(31.januar, "NavDag", 1771, 100, 100)
                ),
                sykepengegrunnlagsfakta = mapOf(
                    "sykepengegrunnlag" to 460_589.0,
                    "6G" to 561_804.0,
                    "fastsatt" to "EtterHovedregel",
                    "arbeidsgivere" to emptyList<Map<String, Any>>(),
                    "selvstendig" to mapOf(
                        "pensjonsgivendeInntekter" to listOf(
                            mapOf(
                                "årstall" to 2017,
                                "beløp" to 450_000.0
                            ),
                            mapOf(
                                "årstall" to 2016,
                                "beløp" to 450_000.0
                            ),
                            mapOf(
                                "årstall" to 2015,
                                "beløp" to 450_000.0
                            )
                        ),
                        "beregningsgrunnlag" to 460589.0,

                        ),
                ),
                inntektskilde = "EN_ARBEIDSGIVER",
                arbeidssituasjon = Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE
            )
            assertVarsler(1.vedtaksperiode, Varselkode.RV_AN_6)
        }
    }

    @Test
    fun `SelvstendigFaktaavklartInntekt - enda en godkjenningsbehov med hundre prosent forsikring fra dag sytten`() = Toggle.SelvstendigForsikring.enable {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
            håndterVilkårsgrunnlag(1.vedtaksperiode, skatteinntekter = emptyList())
            håndterYtelserSelvstendig(1.vedtaksperiode, selvstendigForsikring = SelvstendigForsikring(14.oktober(2017), null, HundreProsentFraDagSytten, 450000.årlig))
            håndterSimulering(1.vedtaksperiode)
            assertGodkjenningsbehov(
                behovsoppsamler = testperson.behovsoppsamler,
                tags = setOf("Førstegangsbehandling", "Personutbetaling", "Innvilget", "EnArbeidsgiver"),
                forbrukteSykedager = 11,
                gjenståendeSykedager = 237,
                foreløpigBeregnetSluttPåSykepenger = 28.desember,
                utbetalingsdager = listOf(
                    utbetalingsdag(1.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(2.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(3.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(4.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(5.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(6.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(7.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(8.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(9.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(10.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(11.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(12.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(13.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(14.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(15.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(16.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(17.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(18.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(19.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(20.januar, "NavHelgDag", 0, 100, 100),
                    utbetalingsdag(21.januar, "NavHelgDag", 0, 100, 100),
                    utbetalingsdag(22.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(23.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(24.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(25.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(26.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(27.januar, "NavHelgDag", 0, 100, 100),
                    utbetalingsdag(28.januar, "NavHelgDag", 0, 100, 100),
                    utbetalingsdag(29.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(30.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(31.januar, "NavDag", 1771, 100, 100)
                ),
                sykepengegrunnlagsfakta = mapOf(
                    "sykepengegrunnlag" to 460_589.0,
                    "6G" to 561_804.0,
                    "fastsatt" to "EtterHovedregel",
                    "arbeidsgivere" to emptyList<Map<String, Any>>(),
                    "selvstendig" to mapOf(
                        "pensjonsgivendeInntekter" to listOf(
                            mapOf(
                                "årstall" to 2017,
                                "beløp" to 450_000.0
                            ),
                            mapOf(
                                "årstall" to 2016,
                                "beløp" to 450_000.0
                            ),
                            mapOf(
                                "årstall" to 2015,
                                "beløp" to 450_000.0
                            )
                        ),
                        "beregningsgrunnlag" to 460589.0,

                        ),
                ),
                inntektskilde = "EN_ARBEIDSGIVER",
                arbeidssituasjon = Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE
            )
            assertVarsler(1.vedtaksperiode, Varselkode.RV_AN_6)
        }
    }

    @Test
    fun `Godkjenningsbehov for jordbruker ser ut som forventet`() = Toggle.Jordbruker.enable {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar, arbeidssituasjon = Søknad.Arbeidssituasjon.JORDBRUKER)
            håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
            håndterYtelserSelvstendig(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertGodkjenningsbehov(
                behovsoppsamler = testperson.behovsoppsamler,
                tags = setOf("Førstegangsbehandling", "Personutbetaling", "Innvilget", "EnArbeidsgiver"),
                forbrukteSykedager = 11,
                gjenståendeSykedager = 237,
                foreløpigBeregnetSluttPåSykepenger = 28.desember,
                utbetalingsdager = listOf(
                    utbetalingsdag(1.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(2.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(3.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(4.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(5.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(6.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(7.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(8.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(9.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(10.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(11.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(12.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(13.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(14.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(15.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(16.januar, "Ventetidsdag", 0, 100, 100),
                    utbetalingsdag(17.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(18.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(19.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(20.januar, "NavHelgDag", 0, 100, 100),
                    utbetalingsdag(21.januar, "NavHelgDag", 0, 100, 100),
                    utbetalingsdag(22.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(23.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(24.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(25.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(26.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(27.januar, "NavHelgDag", 0, 100, 100),
                    utbetalingsdag(28.januar, "NavHelgDag", 0, 100, 100),
                    utbetalingsdag(29.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(30.januar, "NavDag", 1771, 100, 100),
                    utbetalingsdag(31.januar, "NavDag", 1771, 100, 100)
                ),
                sykepengegrunnlagsfakta = mapOf(
                    "sykepengegrunnlag" to 460_589.0,
                    "6G" to 561_804.0,
                    "fastsatt" to "EtterHovedregel",
                    "arbeidsgivere" to emptyList<Map<String, Any>>(),
                    "selvstendig" to mapOf(
                        "pensjonsgivendeInntekter" to listOf(
                            mapOf(
                                "årstall" to 2017,
                                "beløp" to 450_000.0
                            ),
                            mapOf(
                                "årstall" to 2016,
                                "beløp" to 450_000.0
                            ),
                            mapOf(
                                "årstall" to 2015,
                                "beløp" to 450_000.0
                            )
                        ),
                        "beregningsgrunnlag" to 460589.0,

                        ),
                ),
                inntektskilde = "EN_ARBEIDSGIVER",
                arbeidssituasjon = Arbeidssituasjon.JORDBRUKER
            )
            assertVarsler(1.vedtaksperiode, Varselkode.RV_SØ_55)
        }
    }

    private fun utbetalingsdag(dato: LocalDate, type: String, beløpTilBruker: Int, sykdomsgrad: Int, dekningsgrad: Int, begrunnelser: List<String> = emptyList()) =
        utbetalingsdag(dato, type, 0, beløpTilBruker, sykdomsgrad, dekningsgrad, begrunnelser)
}
