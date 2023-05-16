package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import java.time.LocalDate.EPOCH
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.lagStandardSammenligningsgrunnlag
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.SykdomstidslinjedagType
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagType
import no.nav.helse.serde.api.dto.Utbetalingtype
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DelvisRevurderingTest : AbstractDslTest() {

    @Test
    fun `ag1 revurderes, ag2 ikke utbetalt ennå - endrer inntekt for ag1`() {
        nyPeriode(1.januar til 31.januar, a1, a2)
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a2 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode,
                inntektsvurdering = lagStandardSammenligningsgrunnlag(listOf(a1 to INNTEKT, a2 to INNTEKT), 1.januar),
                inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(listOf(a1 to INNTEKT, a2 to INNTEKT), 1.januar),
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, EPOCH),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, EPOCH)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(
                OverstyrtArbeidsgiveropplysning(a1, INNTEKT + 100.daglig, "", null, listOf(
                    Triple(1.januar, null, INNTEKT + 250.daglig)
                ))
            ))

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
        val speil = serializeForSpeil()
        val ag2Periode = speil.arbeidsgivere
            .single { it.organisasjonsnummer == a2 }
            .generasjoner
            .single()
            .perioder
            .single()
        assertForventetFeil(
            forklaring = "ag2 har ikke fått laget utbetaling, og har dermed ingen utbetalingstidslinjedager",
            nå = {
                assertTrue(ag2Periode.sammenslåttTidslinje.all { it.utbetalingstidslinjedagtype == UtbetalingstidslinjedagType.UkjentDag })
            },
            ønsket = {
                assertTrue(ag2Periode.sammenslåttTidslinje.none { it.utbetalingstidslinjedagtype == UtbetalingstidslinjedagType.UkjentDag })
            }
        )
    }

    @Test
    fun `ag1 revurderes, ag2 ikke utbetalt ennå - endrer sykdomstidslinje ag2`() {
        nyPeriode(1.januar til 31.januar, a1, a2)
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a2 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a1 {
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                inntektsvurdering = lagStandardSammenligningsgrunnlag(listOf(a1 to INNTEKT, a2 to INNTEKT), 1.januar),
                inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(
                    listOf(
                        a1 to INNTEKT,
                        a2 to INNTEKT
                    ), 1.januar
                ),
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, EPOCH),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, EPOCH)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrTidslinje(listOf(
                ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)
            ))
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
        val speil = serializeForSpeil()
        val ag2Periode = speil.arbeidsgivere
            .single { it.organisasjonsnummer == a2 }
            .generasjoner
            .single()
            .perioder
            .single()
        assertForventetFeil(
            forklaring = "ag2 har ikke fått laget utbetaling, og har dermed ingen utbetalingstidslinjedager",
            nå = {
                assertEquals(SykdomstidslinjedagType.SYKEDAG, ag2Periode.sammenslåttTidslinje.single { it.dagen == 31.januar }.sykdomstidslinjedagtype)
                assertTrue(ag2Periode.sammenslåttTidslinje.all { it.utbetalingstidslinjedagtype == UtbetalingstidslinjedagType.UkjentDag })
            },
            ønsket = {
                assertEquals(SykdomstidslinjedagType.FERIEDAG, ag2Periode.sammenslåttTidslinje.single { it.dagen == 31.januar }.sykdomstidslinjedagtype)
                assertTrue(ag2Periode.sammenslåttTidslinje.none { it.utbetalingstidslinjedagtype == UtbetalingstidslinjedagType.UkjentDag })
            }
        )
    }

    @Test
    fun `ag1 var ghost og utbetales, ag2 er utbetalt`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
            håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
        }
        a2 {
            håndterVilkårsgrunnlag(
                2.vedtaksperiode,
                inntektsvurdering = lagStandardSammenligningsgrunnlag(listOf(a1 to INNTEKT, a2 to INNTEKT), 1.januar),
                inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(
                    listOf(
                        a1 to INNTEKT,
                        a2 to INNTEKT
                    ), 1.januar
                ),
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, EPOCH),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, EPOCH)
                )
            )
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode)
            håndterUtbetalt()

            håndterOverstyrTidslinje(listOf(
                ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)
            ))
        }
        a1 {
            håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
        }
        val speil = serializeForSpeil()
        val ag1Periode = speil.arbeidsgivere
            .single { it.organisasjonsnummer == a2 }
            .generasjoner
            .single()
            .perioder
            .first() as BeregnetPeriode
        assertEquals(SykdomstidslinjedagType.FERIEDAG, ag1Periode.sammenslåttTidslinje.single { it.dagen == 31.januar }.sykdomstidslinjedagtype)
        assertForventetFeil(
            forklaring = "ag2 har ikke fått laget revurdering, og har dermed ingen utbetalingstidslinjedager",
            nå = {
                assertEquals(Utbetalingtype.UTBETALING, ag1Periode.utbetaling.type)
            },
            ønsket = {
                assertEquals(Utbetalingtype.REVURDERING, ag1Periode.utbetaling.type)
            }
        )
    }
}