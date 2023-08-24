package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import java.time.LocalDate.EPOCH
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.lagStandardSammenligningsgrunnlag
import no.nav.helse.dsl.lagStandardSykepengegrunnlag
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.SykdomstidslinjedagType
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagType
import no.nav.helse.serde.api.dto.Utbetalingtype
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
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
                    Vilkårsgrunnlag.Arbeidsforhold(a1, EPOCH, type = Arbeidsforholdtype.ORDINÆRT),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, EPOCH, type = Arbeidsforholdtype.ORDINÆRT)
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
        assertTrue(ag2Periode.sammenslåttTidslinje.none { it.utbetalingstidslinjedagtype == UtbetalingstidslinjedagType.UkjentDag })
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
                    Vilkårsgrunnlag.Arbeidsforhold(a1, EPOCH, type = Arbeidsforholdtype.ORDINÆRT),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, EPOCH, type = Arbeidsforholdtype.ORDINÆRT)
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
        assertEquals(SykdomstidslinjedagType.FERIEDAG, ag2Periode.sammenslåttTidslinje.single { it.dagen == 31.januar }.sykdomstidslinjedagtype)
        assertTrue(ag2Periode.sammenslåttTidslinje.none { it.utbetalingstidslinjedagtype == UtbetalingstidslinjedagType.UkjentDag })
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
                    Vilkårsgrunnlag.Arbeidsforhold(a1, EPOCH, type = Arbeidsforholdtype.ORDINÆRT),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, EPOCH, type = Arbeidsforholdtype.ORDINÆRT)
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
        val ag2Periode = speil.arbeidsgivere
            .single { it.organisasjonsnummer == a2 }
            .generasjoner
            .first()
            .perioder
            .first() as BeregnetPeriode
        assertEquals(SykdomstidslinjedagType.FERIEDAG, ag2Periode.sammenslåttTidslinje.single { it.dagen == 31.januar }.sykdomstidslinjedagtype)
        assertEquals(Utbetalingtype.REVURDERING, ag2Periode.utbetaling.type)
    }

    @Test
    fun `To arbeidsgivere gikk inn i en bar - og første arbeidsgiver ble ferdig behandlet før vi mottok sykmelding på neste arbeidsgiver`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 30000.månedlig)
            håndterVilkårsgrunnlag(
                1.vedtaksperiode,
                inntektsvurdering = lagStandardSammenligningsgrunnlag(listOf(a1 to 30000.månedlig, a2 to 35000.månedlig), 1.januar),
                inntektsvurderingForSykepengegrunnlag = lagStandardSykepengegrunnlag(listOf(a1 to 30000.månedlig, a2 to 35000.månedlig), 1.januar),
                arbeidsforhold = listOf(
                    Vilkårsgrunnlag.Arbeidsforhold(a1, EPOCH, type = Arbeidsforholdtype.ORDINÆRT),
                    Vilkårsgrunnlag.Arbeidsforhold(a2, EPOCH, type = Arbeidsforholdtype.ORDINÆRT)
                )
            )
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertUtbetalingsbeløp(
                1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 997,
                forventetArbeidsgiverRefusjonsbeløp = 1385,
                subset = 17.januar til 31.januar
            )
        }

        assertEquals(1, inspektør(a1).arbeidsgiverOppdrag.size)
        assertEquals(0, inspektør(a2).arbeidsgiverOppdrag.size)

        nullstillTilstandsendringer()

        a2 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 30000.månedlig)
        }

        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }

        a1 {
            håndterYtelser(1.vedtaksperiode)
            val speil = serializeForSpeil()
            val ag2Periode = speil.arbeidsgivere
                .single { it.organisasjonsnummer == a2 }
                .generasjoner
                .single()
                .perioder
                .single()
            assertTrue(ag2Periode.sammenslåttTidslinje.none { it.utbetalingstidslinjedagtype == UtbetalingstidslinjedagType.UkjentDag })
        }
        a1 {
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK) }

        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }

        a1 {
            assertUtbetalingsbeløp(
                1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1081,
                forventetArbeidsgiverRefusjonsbeløp = 1385,
                subset = 17.januar til 31.januar
            )
        }
        a2 {
            assertUtbetalingsbeløp(
                1.vedtaksperiode,
                forventetArbeidsgiverbeløp = 1080,
                forventetArbeidsgiverRefusjonsbeløp = 1385,
                subset = 17.januar til 31.januar
            )
        }
        assertEquals(2, inspektør(a1).utbetalinger.size)
        assertEquals(2, inspektør(a2).utbetalinger.size)
    }
}