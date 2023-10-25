package no.nav.helse.spleis.e2e

import no.nav.helse.Grunnbeløp.Companion.`2G`
import no.nav.helse.Grunnbeløp.Companion.halvG
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.erHelg
import no.nav.helse.februar
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MinimumInntektE2E : AbstractDslTest() {
    private companion object {
        private val fødseldato67år =  31.januar(1951)
    }

    @Test
    fun `avslår dager tom 67 år`() {
        val inntektskrav = halvG.beløp(1.januar)
        val skjæringstidspunkt = 1.januar
        val inntekt = inntektskrav - 1.daglig

        medFødselsdato(fødseldato67år)

        a1 {
            nyPeriode(1.januar til 31.januar, a1)
            håndterInntektsmelding(listOf(1.januar til 16.januar), inntekt)
            håndterVilkårsgrunnlag(1.vedtaksperiode,
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, skjæringstidspunkt, inntekt.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, skjæringstidspunkt, inntekt.repeat(3))
                    ),
                    arbeidsforhold = emptyList()
                )
            )
            håndterYtelser(1.vedtaksperiode)
            inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { utbetalingstidslinjeInspektør ->
                assertEquals(11, utbetalingstidslinjeInspektør.avvistDagTeller)
                assertEquals((17.januar til 31.januar).filterNot { it.erHelg() }, utbetalingstidslinjeInspektør.avvistedatoer)
                assertTrue(utbetalingstidslinjeInspektør.avvistedatoer.all { utbetalingstidslinjeInspektør.begrunnelse(it).single() == Begrunnelse.MinimumInntekt })
            }
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_GODKJENNING)
        }
    }

    @Test
    fun `avslår dager etter fylte 67 år selv om skjæringstidspunktet var før fylte 67 år`() {
        val inntektskrav = `2G`.beløp(1.januar)
        val skjæringstidspunkt = 22.januar
        val inntekt = inntektskrav - 1.daglig

        medFødselsdato(fødseldato67år)

        a1 {
            nyPeriode(22.januar til 28.februar, a1)
            håndterInntektsmelding(listOf(22.januar til 6.februar), inntekt)
            håndterVilkårsgrunnlag(1.vedtaksperiode,
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, skjæringstidspunkt, inntekt.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, skjæringstidspunkt, inntekt.repeat(3))
                    ),
                    arbeidsforhold = emptyList()
                )
            )
            håndterYtelser(1.vedtaksperiode)
            inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { utbetalingstidslinjeInspektør ->
                assertEquals(16, utbetalingstidslinjeInspektør.avvistDagTeller)
                assertEquals((7.februar til 28.februar).filterNot { it.erHelg() }, utbetalingstidslinjeInspektør.avvistedatoer)
                assertTrue(utbetalingstidslinjeInspektør.avvistedatoer.all { utbetalingstidslinjeInspektør.begrunnelse(it).single() == Begrunnelse.MinimumInntektOver67 })
            }
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            assertTag(1.vedtaksperiode, PersonObserver.VedtakFattetEvent.Tag.SykepengegrunnlagUnder2G)
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
        }
    }

    @Test
    fun `tagger ikke normal inntekt`() {
        nyttVedtak(1.januar, 31.januar)
        assertIkkeTag(1.vedtaksperiode, PersonObserver.VedtakFattetEvent.Tag.SykepengegrunnlagUnder2G)
    }

    @Test
    fun `avslår dager etter fylte 67 år`() {
        val inntektskrav = `2G`.beløp(1.januar)
        val skjæringstidspunkt = 1.februar
        val inntekt = inntektskrav - 1.daglig

        medFødselsdato(fødseldato67år)

        a1 {
            nyPeriode(1.februar til 28.februar, a1)
            håndterInntektsmelding(listOf(1.februar til 16.februar), inntekt)
            håndterVilkårsgrunnlag(1.vedtaksperiode,
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, skjæringstidspunkt, inntekt.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, skjæringstidspunkt, inntekt.repeat(3))
                    ),
                    arbeidsforhold = emptyList()
                )
            )
            håndterYtelser(1.vedtaksperiode)
            inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { utbetalingstidslinjeInspektør ->
                assertEquals(8, utbetalingstidslinjeInspektør.avvistDagTeller)
                assertEquals((17.februar til 28.februar).filterNot { it.erHelg() }, utbetalingstidslinjeInspektør.avvistedatoer)
                assertTrue(utbetalingstidslinjeInspektør.avvistedatoer.all { utbetalingstidslinjeInspektør.begrunnelse(it).single() == Begrunnelse.MinimumInntektOver67 })
            }
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_GODKJENNING)
        }
    }

    @Test
    fun `avslår perioden med ny begrunnelse etter fylte 67 år`() {
        val inntektskrav = halvG.beløp(1.januar)
        val skjæringstidspunkt = 15.januar
        val inntekt = inntektskrav - 1.daglig

        medFødselsdato(fødseldato67år)

        a1 {
            nyPeriode(15.januar til 28.februar, a1)
            håndterInntektsmelding(listOf(15.januar til 30.januar), inntekt)
            håndterVilkårsgrunnlag(1.vedtaksperiode,
                inntektsvurdering = Inntektsvurdering(
                    listOf(
                        sammenligningsgrunnlag(a1, skjæringstidspunkt, inntekt.repeat(12))
                    )
                ),
                inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                    inntekter = listOf(
                        grunnlag(a1, skjæringstidspunkt, inntekt.repeat(3))
                    ),
                    arbeidsforhold = emptyList()
                )
            )
            håndterYtelser(1.vedtaksperiode)
            inspektør.utbetalingstidslinjer(1.vedtaksperiode).inspektør.also { utbetalingstidslinjeInspektør ->
                assertEquals(21, utbetalingstidslinjeInspektør.avvistDagTeller)
                assertEquals((31.januar til 28.februar).filterNot { it.erHelg() }, utbetalingstidslinjeInspektør.avvistedatoer)
                assertEquals(Begrunnelse.MinimumInntekt, utbetalingstidslinjeInspektør.begrunnelse(31.januar).single())
                assertTrue((1.februar til 28.februar).filterNot { it.erHelg() }.all { utbetalingstidslinjeInspektør.begrunnelse(it).single() == Begrunnelse.MinimumInntektOver67 })
            }
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_GODKJENNING)
        }
    }
}