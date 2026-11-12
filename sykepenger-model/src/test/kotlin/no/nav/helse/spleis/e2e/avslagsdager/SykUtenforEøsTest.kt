package no.nav.helse.spleis.e2e.avslagsdager

import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.selvstendig
import no.nav.helse.etterlevelse.Ledd.LEDD_1
import no.nav.helse.etterlevelse.Paragraf.PARAGRAF_8_9
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.GraderteAndreYtelserForBeregning
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Utlandsopphold
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.SubsumsjonInspektør
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_13
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidstaker
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class SykUtenforEøsTest : AbstractDslTest() {
    private val utenlandsopphold = 22.januar til 28.januar
    private val hverdagerUtenforEøs = 22.januar til 26.januar
    private val helgUtenforEøs = 27.januar til 28.januar

    @Test
    fun `utenlandsopphold i søknaden gir syk utenfor EØS som utbetales som sykedager`() =
        Toggle.SykUtenforEøs.enable {
            a1 {
                håndterSykmelding(januar)
                håndterSøknad(
                    Sykdom(1.januar, 31.januar, 50.prosent),
                    Utlandsopphold(utenlandsopphold.start, utenlandsopphold.endInclusive),
                )
                tilGodkjenning()

                assertEquals(utenlandsopphold.toList(), dagerUtenforEøs())
                utenlandsopphold.forEach { dato ->
                    assertEquals(50, inspektør.sykdomstidslinje.inspektør.grader[dato])
                }
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { utbetalingstidslinje ->
                    assertEquals(
                        hverdagerUtenforEøs.toList(),
                        utbetalingstidslinje.navdager.map { it.dato }.filter { it in utenlandsopphold },
                    )
                    assertEquals(
                        helgUtenforEøs.toList(),
                        utbetalingstidslinje.navHelgdager.map { it.dato }.filter { it in utenlandsopphold },
                    )
                    assertEquals(11, utbetalingstidslinje.navDagTeller)
                }
                assertVarsler(emptyList(), 1.vedtaksperiode.filter())
                SubsumsjonInspektør(jurist).assertIkkeVurdert(paragraf = PARAGRAF_8_9, versjon = 1.juni(2021), ledd = LEDD_1)
                assertGjenoppbygget(dto())
            }
        }

    @Test
    fun `ferie samme dag som utenlandsopphold vinner`() =
        Toggle.SykUtenforEøs.enable {
            a1 {
                håndterSykmelding(januar)
                håndterSøknad(
                    Sykdom(1.januar, 31.januar, 100.prosent),
                    Utlandsopphold(utenlandsopphold.start, utenlandsopphold.endInclusive),
                    Ferie(25.januar, 28.januar),
                )

                assertEquals((22.januar til 24.januar).toList(), dagerUtenforEøs())
                (25.januar til 28.januar).forEach { dato ->
                    assertEquals(Dag.Feriedag::class, inspektør.sykdomstidslinje[dato]::class)
                }
            }
        }

    @Test
    fun `saksbehandler kan overstyre fra syk utenfor EØS`() =
        Toggle.SykUtenforEøs.enable {
            a1 {
                håndterSykmelding(januar)
                håndterSøknad(
                    Sykdom(1.januar, 31.januar, 100.prosent),
                    Utlandsopphold(utenlandsopphold.start, utenlandsopphold.endInclusive),
                )
                tilGodkjenning()

                håndterOverstyrTidslinje(
                    listOf(
                        ManuellOverskrivingDag(22.januar, Dagtype.Feriedag),
                        ManuellOverskrivingDag(23.januar, Dagtype.Sykedag, 100),
                    ),
                )

                assertEquals(Dag.Feriedag::class, inspektør.sykdomstidslinje[22.januar]::class)
                assertEquals(Dag.Sykedag::class, inspektør.sykdomstidslinje[23.januar]::class)
                assertEquals((24.januar til 28.januar).toList(), dagerUtenforEøs())
            }
        }

    @Test
    fun `korrigert søknad uten utenlandsopphold gir sykedager`() =
        Toggle.SykUtenforEøs.enable {
            a1 {
                val søknadId = UUID.randomUUID()
                håndterSykmelding(januar)
                håndterSøknad(
                    Sykdom(1.januar, 31.januar, 100.prosent),
                    Utlandsopphold(utenlandsopphold.start, utenlandsopphold.endInclusive),
                    søknadId = søknadId,
                )
                assertEquals(utenlandsopphold.toList(), dagerUtenforEøs())

                håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), korrigerer = søknadId)

                assertEquals(emptyList<LocalDate>(), dagerUtenforEøs())
                assertEquals(Dag.Sykedag::class, inspektør.sykdomstidslinje[22.januar]::class)
                assertEquals(Dag.SykHelgedag::class, inspektør.sykdomstidslinje[27.januar]::class)
            }
        }

    @Test
    fun `varsel når korrigert søknad endrer sykdomsgraden etter fordeling på syk utenfor EØS`() =
        Toggle.SykUtenforEøs.enable {
            a1 {
                val graderteAndreYtelser = listOf(graderteAndreYtelser(hverdagerUtenforEøs, trekkFraSykdomsgrad = 30))
                håndterSykmelding(januar)
                håndterSøknad(
                    Sykdom(1.januar, 31.januar, 100.prosent),
                    Utlandsopphold(utenlandsopphold.start, utenlandsopphold.endInclusive),
                )
                håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
                håndterVilkårsgrunnlag(1.vedtaksperiode)
                håndterYtelser(1.vedtaksperiode, graderteAndreYtelser = graderteAndreYtelser)
                håndterSimulering(1.vedtaksperiode)
                håndterUtbetalingsgodkjenning(1.vedtaksperiode)
                håndterUtbetalt()

                håndterSøknad(
                    Sykdom(1.januar, 31.januar, 80.prosent),
                    Utlandsopphold(utenlandsopphold.start, utenlandsopphold.endInclusive),
                )
                håndterYtelser(1.vedtaksperiode, graderteAndreYtelser = graderteAndreYtelser)

                assertEquals(utenlandsopphold.toList(), dagerUtenforEøs())
                assertVarsel(RV_AY_13, 1.vedtaksperiode.filter())
                assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
            }
        }

    @Test
    fun `selvstendig næringsdrivende får syk utenfor EØS`() =
        Toggle.SykUtenforEøs.enable {
            selvstendig {
                håndterFørstegangssøknadSelvstendig(
                    Sykdom(1.januar, 31.januar, 100.prosent),
                    Utlandsopphold(utenlandsopphold.start, utenlandsopphold.endInclusive),
                )
                håndterVilkårsgrunnlagSelvstendig(1.vedtaksperiode)
                håndterYtelserSelvstendig(1.vedtaksperiode)
                håndterSimulering(1.vedtaksperiode)
                assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)

                assertEquals(utenlandsopphold.toList(), dagerUtenforEøs())
                inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { utbetalingstidslinje ->
                    assertEquals(
                        hverdagerUtenforEøs.toList(),
                        utbetalingstidslinje.navdager.map { it.dato }.filter { it in utenlandsopphold },
                    )
                    assertEquals(
                        helgUtenforEøs.toList(),
                        utbetalingstidslinje.navHelgdager.map { it.dato }.filter { it in utenlandsopphold },
                    )
                }
                assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            }
        }

    @Test
    fun `uten togglen blir utenlandsopphold sykedager med varsel`() =
        Toggle.SykUtenforEøs.disable {
            a1 {
                håndterSykmelding(januar)
                håndterSøknad(
                    Sykdom(1.januar, 31.januar, 100.prosent),
                    Utlandsopphold(utenlandsopphold.start, utenlandsopphold.endInclusive),
                )

                assertEquals(emptyList<LocalDate>(), dagerUtenforEøs())
                assertEquals(Dag.Sykedag::class, inspektør.sykdomstidslinje[22.januar]::class)
                assertEquals(Dag.SykHelgedag::class, inspektør.sykdomstidslinje[27.januar]::class)
                assertVarsler(listOf(RV_SØ_8), 1.vedtaksperiode.filter())
                SubsumsjonInspektør(jurist).assertIkkeOppfylt(
                    paragraf = PARAGRAF_8_9,
                    versjon = 1.juni(2021),
                    ledd = LEDD_1,
                    input =
                        mapOf(
                            "soknadsPerioder" to
                                listOf(
                                    mapOf("fom" to 1.januar, "tom" to 31.januar, "type" to "sykdom"),
                                    mapOf("fom" to 22.januar, "tom" to 28.januar, "type" to "utlandsopphold"),
                                ),
                        ),
                    output = mapOf("perioder" to listOf(mapOf("fom" to 22.januar, "tom" to 28.januar))),
                )
            }
        }

    private fun no.nav.helse.dsl.TestPerson.TestArbeidsgiver.tilGodkjenning() {
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
    }

    private fun graderteAndreYtelser(
        periode: Periode,
        trekkFraSykdomsgrad: Int,
    ) = GraderteAndreYtelserForBeregning(
        yrkesaktivitet = Arbeidstaker(a1),
        versjon = UUID.randomUUID(),
        dager = periode.map { dato -> GraderteAndreYtelserForBeregning.Dag(dato, trekkFraSykdomsgrad, 100) },
    )

    private fun no.nav.helse.dsl.TestPerson.TestArbeidsgiver.dagerUtenforEøs() =
        inspektør.sykdomstidslinje
            .filterIsInstance<Dag.SykUtenforEØS>()
            .map { it.dato }
}
