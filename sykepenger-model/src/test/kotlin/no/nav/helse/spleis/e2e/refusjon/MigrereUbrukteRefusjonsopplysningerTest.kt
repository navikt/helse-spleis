package no.nav.helse.spleis.e2e.refusjon

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.person.refusjon.RefusjonsservitørView
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class MigrereUbrukteRefusjonsopplysningerTest : AbstractDslTest() {

    private lateinit var forrigeUbrukteRefusjonsopplysninger: RefusjonsservitørView
    private val meldingsreferanseId1 = UUID.randomUUID()
    private val mottatt1 = LocalDate.EPOCH.atStartOfDay()
    private val meldingsreferanseId2 = UUID.randomUUID()
    private val mottatt2 = mottatt1.plusYears(1)

    private fun setup1og2() {
        håndterSøknad(januar)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, opphørsdato = 28.februar),
            beregnetInntekt = INNTEKT,
            id = meldingsreferanseId1,
            mottatt = mottatt1
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
    }

    @Test
    @Order(1)
    fun `Endring i refusjon frem i tid fra inntektsmelding - med toggle`() =
        Toggle.LagreUbrukteRefusjonsopplysninger.enable {
            a1 {
                setup1og2()
                forrigeUbrukteRefusjonsopplysninger = inspektør.ubrukteRefusjonsopplysninger
            }
        }

    @Test
    @Order(2)
    fun `Endring i refusjon frem i tid fra inntektsmelding - uten toggle`() =
        Toggle.LagreUbrukteRefusjonsopplysninger.disable {
            a1 {
                setup1og2()
                migrerUbrukteRefusjonsopplysninger()
                assertEquals(
                    forrigeUbrukteRefusjonsopplysninger,
                    inspektør.ubrukteRefusjonsopplysninger
                )
            }
        }

    private fun setup3og4() {
        håndterSøknad(januar)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT * 2, opphørsdato = 28.februar),
            beregnetInntekt = INNTEKT,
            id = meldingsreferanseId1,
            mottatt = mottatt1
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT / 2, opphørsdato = 28.februar),
            beregnetInntekt = INNTEKT,
            id = meldingsreferanseId2,
            mottatt = mottatt2
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)
    }

    @Test
    @Order(3)
    fun `Endring i refusjon frem i tid fra flere inntektsmeldinger - med toggle`() =
        Toggle.LagreUbrukteRefusjonsopplysninger.enable {
            a1 {
                setup3og4()
                forrigeUbrukteRefusjonsopplysninger = inspektør.ubrukteRefusjonsopplysninger
            }
        }

    @Test
    @Order(4)
    fun `Endring i refusjon frem i tid fra flere inntektsmeldinger - uten toggle`() =
        Toggle.LagreUbrukteRefusjonsopplysninger.disable {
            a1 {
                setup3og4()
                migrerUbrukteRefusjonsopplysninger()
                assertEquals(
                    forrigeUbrukteRefusjonsopplysninger,
                    inspektør.ubrukteRefusjonsopplysninger
                )
            }
        }

    private fun setup5og6() {
        håndterSøknad(januar)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, opphørsdato = 28.februar),
            beregnetInntekt = INNTEKT,
            id = meldingsreferanseId1,
            mottatt = mottatt1,
            harOpphørAvNaturalytelser = true
        )
    }

    @Test
    @Order(5)
    fun `Inntektsmeldingen støttes ikke - med toggle`() =
        Toggle.LagreUbrukteRefusjonsopplysninger.enable {
            a1 {
                setup5og6()
                forrigeUbrukteRefusjonsopplysninger = inspektør.ubrukteRefusjonsopplysninger
            }
        }

    @Test
    @Order(6)
    fun `Inntektsmeldingen støttes ikke - uten toggle`() =
        Toggle.LagreUbrukteRefusjonsopplysninger.disable {
            a1 {
                setup5og6()
                migrerUbrukteRefusjonsopplysninger()
                assertEquals(
                    forrigeUbrukteRefusjonsopplysninger,
                    inspektør.ubrukteRefusjonsopplysninger
                )
            }
        }

    private fun setup7og8() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, opphørsdato = 28.februar),
                beregnetInntekt = INNTEKT,
                id = meldingsreferanseId1,
                mottatt = mottatt1
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                ArbeidsgiverUtbetalingsperiode(
                    a1,
                    1.januar,
                    31.januar,
                    100.prosent,
                    INNTEKT
                )
            )
        }
    }

    @Test
    @Order(7)
    fun `Periode er kastet og utbetalt i Infotrygd - med toggle`() =
        Toggle.LagreUbrukteRefusjonsopplysninger.enable {
            a1 {
                setup7og8()
                forrigeUbrukteRefusjonsopplysninger = inspektør.ubrukteRefusjonsopplysninger
            }
        }

    @Test
    @Order(8)
    fun `Periode er kastet og utbetalt i Infotrygd - uten toggle`() =
        Toggle.LagreUbrukteRefusjonsopplysninger.disable {
            a1 {
                setup7og8()
                migrerUbrukteRefusjonsopplysninger()
                assertEquals(
                    forrigeUbrukteRefusjonsopplysninger,
                    inspektør.ubrukteRefusjonsopplysninger
                )
            }
        }

    private fun setup9og10() {
        a1 {
            håndterSøknad(januar)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT,
                id = meldingsreferanseId1,
                mottatt = mottatt1
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrArbeidsgiveropplysninger(
                skjæringstidspunkt = 1.januar,
                overstyringer = listOf(
                    OverstyrtArbeidsgiveropplysning(
                        orgnummer = a1,
                        inntekt = INNTEKT,
                        forklaring = "foo",
                        subsumsjon = null,
                        refusjonsopplysninger = listOf(
                            Triple(1.januar, 31.januar, INNTEKT / 2),
                            Triple(1.februar, null, INGEN)
                        )
                    )
                ),
                hendelseId = meldingsreferanseId2,
                tidsstempel = mottatt2
            )
        }
    }

    @Test
    @Order(9)
    fun `Saksbehandler overstyrer refusjon frem i tid - med toggle`() =
        Toggle.LagreUbrukteRefusjonsopplysninger.enable {
            a1 {
                setup9og10()
                forrigeUbrukteRefusjonsopplysninger = inspektør.ubrukteRefusjonsopplysninger
            }
        }

    @Test
    @Order(10)
    fun `Saksbehandler overstyrer refusjon frem i tid - uten toggle`() =
        Toggle.LagreUbrukteRefusjonsopplysninger.disable {
            a1 {
                setup9og10()
                migrerUbrukteRefusjonsopplysninger()
                assertEquals(
                    forrigeUbrukteRefusjonsopplysninger,
                    inspektør.ubrukteRefusjonsopplysninger
                )
            }
        }

    private fun setup11og12() {
        a1 {
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT,
                id = meldingsreferanseId1,
                mottatt = mottatt1
            )
        }
    }

    @Test
    @Order(11)
    fun `Har kun fått IM - med toggle`() = Toggle.LagreUbrukteRefusjonsopplysninger.enable {
        a1 {
            setup11og12()
            forrigeUbrukteRefusjonsopplysninger = inspektør.ubrukteRefusjonsopplysninger
        }
    }

    @Test
    @Order(12)
    fun `Har kun fått IM - uten toggle`() = Toggle.LagreUbrukteRefusjonsopplysninger.disable {
        a1 {
            setup11og12()
            migrerUbrukteRefusjonsopplysninger()
            assertEquals(
                forrigeUbrukteRefusjonsopplysninger,
                inspektør.ubrukteRefusjonsopplysninger
            )
        }
    }

    @Test
    fun `Hensyntar Infotrygd-utbetaling ved ubrukte refusjonsopplysninger i inntektsgrunnlaget`() =
        Toggle.LagreUbrukteRefusjonsopplysninger.disable {
            a1 {
                nyttVedtak(januar)
                håndterOverstyrArbeidsgiveropplysninger(
                    skjæringstidspunkt = 1.januar,
                    overstyringer = listOf(
                        OverstyrtArbeidsgiveropplysning(
                            orgnummer = a1,
                            inntekt = INNTEKT,
                            forklaring = "foo",
                            subsumsjon = null,
                            refusjonsopplysninger = listOf(
                                Triple(1.januar, 31.januar, INNTEKT),
                                Triple(1.februar, null, INGEN)
                            )
                        )
                    ),
                )
                håndterUtbetalingshistorikkEtterInfotrygdendring(
                    PersonUtbetalingsperiode(
                        a1,
                        1.februar,
                        10.februar,
                        100.prosent,
                        INNTEKT
                    )
                )

                assertEquals(
                    RefusjonsservitørView(emptyMap()),
                    inspektør.ubrukteRefusjonsopplysninger
                )

                migrerUbrukteRefusjonsopplysninger()

                assertEquals(
                    RefusjonsservitørView(emptyMap()),
                    inspektør.ubrukteRefusjonsopplysninger
                )
            }
        }

    @Test
    fun `Perioder som ikke er vilkårsprøvd må anses som ubrukte refusjonsopplysninger`() =
        Toggle.LagreUbrukteRefusjonsopplysninger.disable {
            a1 {
                tilGodkjenning(januar)
                håndterSøknad(mars)
                håndterInntektsmelding(
                    listOf(1.mars til 16.mars),
                    id = meldingsreferanseId1,
                    mottatt = mottatt1
                )

                assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
                assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
                assertEquals(
                    RefusjonsservitørView(emptyMap()),
                    inspektør.ubrukteRefusjonsopplysninger
                )

                migrerUbrukteRefusjonsopplysninger()

                assertEquals(
                    RefusjonsservitørView(
                        mapOf(
                            1.mars to Beløpstidslinje.fra(
                                1.mars.somPeriode(),
                                INNTEKT,
                                Kilde(meldingsreferanseId1, ARBEIDSGIVER, mottatt1)
                            )
                        )
                    ), inspektør.ubrukteRefusjonsopplysninger
                )
            }
        }

    @Test
    fun `Når siste periode er AUU anser vi kun refusjonsopplysninger etter denne perioden som ubrukte`() =
        Toggle.LagreUbrukteRefusjonsopplysninger.disable {
            a1 {
                håndterSøknad(1.januar til 10.januar)
                håndterInntektsmelding(
                    listOf(1.januar til 16.januar),
                    refusjon = Inntektsmelding.Refusjon(INNTEKT, 25.januar),
                    id = meldingsreferanseId1,
                    mottatt = mottatt1
                )

                assertEquals(
                    RefusjonsservitørView(emptyMap()),
                    inspektør.ubrukteRefusjonsopplysninger
                )

                migrerUbrukteRefusjonsopplysninger()

                val beløpstidslinje = Beløpstidslinje.fra(
                    11.januar til 25.januar,
                    INNTEKT,
                    Kilde(meldingsreferanseId1, ARBEIDSGIVER, mottatt1)
                ) +
                    Beløpstidslinje.fra(
                        26.januar.somPeriode(),
                        INGEN,
                        Kilde(meldingsreferanseId1, ARBEIDSGIVER, mottatt1)
                    )

                assertEquals(
                    RefusjonsservitørView(mapOf(1.januar to beløpstidslinje)),
                    inspektør.ubrukteRefusjonsopplysninger
                )
            }
        }
}
