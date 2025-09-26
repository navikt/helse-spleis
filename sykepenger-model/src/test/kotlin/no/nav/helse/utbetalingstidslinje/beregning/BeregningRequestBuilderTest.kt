package no.nav.helse.utbetalingstidslinje.beregning

import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.assertInstanceOf
import no.nav.helse.utbetalingstidslinje.beregning.BeregningRequest.VedtaksperiodeForBeregning.DataForBeregning.Arbeidstaker
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertNull

internal class BeregningRequestBuilderTest {

    @Test
    fun `en arbeidsgiver - inngår i sykengegrunnlag`() {
        val request = BeregningRequest.Builder()
            .fastsattÅrsinntekt(Yrkesaktivitet.Arbeidstaker(a1), INNTEKT)
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), januar, Sykdomstidslinje(), arbeidstaker())
            .build()

        assertEquals(1, request.yrkesaktiviteter.size)
        assertEquals(Yrkesaktivitet.Arbeidstaker(a1), request.yrkesaktiviteter.single().yrkesaktivitet)
        assertEquals(1, request.yrkesaktiviteter.single().perioder.size)
        request.yrkesaktiviteter.single().perioder.single().also { beregningsperiode ->
            assertInstanceOf<BeregningRequest.VedtaksperiodeForBeregning>(beregningsperiode)
            assertEquals(januar, beregningsperiode.periode)
            assertEquals(INNTEKT, beregningsperiode.inntekt)
            assertEquals(Beløpstidslinje(), beregningsperiode.inntektsjusteringer)
        }
    }

    @Test
    fun `en arbeidsgiver - inngår ikke i sykepengegrunnag`() {
        val request = BeregningRequest.Builder()
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), januar, Sykdomstidslinje(), arbeidstaker())
            .build()

        assertEquals(1, request.yrkesaktiviteter.size)
        assertEquals(Yrkesaktivitet.Arbeidstaker(a1), request.yrkesaktiviteter.single().yrkesaktivitet)
        assertEquals(1, request.yrkesaktiviteter.single().perioder.size)
        request.yrkesaktiviteter.single().perioder.single().also { beregningsperiode ->
            assertInstanceOf<BeregningRequest.VedtaksperiodeForBeregning>(beregningsperiode)
            assertEquals(januar, beregningsperiode.periode)
            assertNull(beregningsperiode.inntekt)
            assertEquals(Beløpstidslinje(), beregningsperiode.inntektsjusteringer)
        }
    }

    @Test
    fun `flere arbeidsgiver - med ghostperioder og tilkommet inntekt`() {
        val request = BeregningRequest.Builder()
            .fastsattÅrsinntekt(Yrkesaktivitet.Arbeidstaker(a1), INNTEKT)
            .fastsattÅrsinntekt(Yrkesaktivitet.Arbeidstaker(a2), INNTEKT * 2)
            .inntektsjusteringer(Yrkesaktivitet.Arbeidstaker(a3), 10.januar, 14.januar, 1500.daglig)
            .inntektsjusteringer(Yrkesaktivitet.Arbeidstaker(a3), 25.januar, null, 1500.daglig)
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), 1.januar til 20.januar, Sykdomstidslinje(), arbeidstaker())
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), 21.januar til 25.januar, Sykdomstidslinje(), arbeidstaker())
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), 29.januar til 31.januar, Sykdomstidslinje(), arbeidstaker())
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a2), UUID.randomUUID(), 1.januar til 21.januar, Sykdomstidslinje(), arbeidstaker())
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a2), UUID.randomUUID(), 25.januar til 30.januar, Sykdomstidslinje(), arbeidstaker())
            .build()

        assertEquals(3, request.yrkesaktiviteter.size)
        request.yrkesaktiviteter[0].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a1), yrkesaktivitet.yrkesaktivitet)
            assertEquals(4, yrkesaktivitet.perioder.size)
            yrkesaktivitet.perioder[0].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.VedtaksperiodeForBeregning>(beregningsperiode)
                assertEquals(1.januar til 20.januar, beregningsperiode.periode)
            }
            yrkesaktivitet.perioder[1].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.VedtaksperiodeForBeregning>(beregningsperiode)
                assertEquals(21.januar til 25.januar, beregningsperiode.periode)
            }
            yrkesaktivitet.perioder[2].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.Ghostperiode>(beregningsperiode)
                assertEquals(26.januar til 28.januar, beregningsperiode.periode)
            }
            yrkesaktivitet.perioder[3].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.VedtaksperiodeForBeregning>(beregningsperiode)
                assertEquals(29.januar til 31.januar, beregningsperiode.periode)
            }
        }
        request.yrkesaktiviteter[1].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a2), yrkesaktivitet.yrkesaktivitet)
            assertEquals(4, yrkesaktivitet.perioder.size)
            yrkesaktivitet.perioder[0].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.VedtaksperiodeForBeregning>(beregningsperiode)
                assertEquals(1.januar til 21.januar, beregningsperiode.periode)
            }
            yrkesaktivitet.perioder[1].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.Ghostperiode>(beregningsperiode)
                assertEquals(22.januar til 24.januar, beregningsperiode.periode)
            }
            yrkesaktivitet.perioder[2].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.VedtaksperiodeForBeregning>(beregningsperiode)
                assertEquals(25.januar til 30.januar, beregningsperiode.periode)
            }

            yrkesaktivitet.perioder[3].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.Ghostperiode>(beregningsperiode)
                assertEquals(31.januar til 31.januar, beregningsperiode.periode)
            }
        }

        request.yrkesaktiviteter[2].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a3), yrkesaktivitet.yrkesaktivitet)
            assertEquals(2, yrkesaktivitet.perioder.size)
            yrkesaktivitet.perioder[0].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.AnnenInntektsperiode>(beregningsperiode)
                assertEquals(10.januar til 14.januar, beregningsperiode.periode)
            }
            yrkesaktivitet.perioder[1].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.AnnenInntektsperiode>(beregningsperiode)
                assertEquals(25.januar til 31.januar, beregningsperiode.periode)
            }
        }
    }

    @Test
    fun `tilkommet inntekt uten vedtaksperioder`() {
        val request = BeregningRequest.Builder()
            .fastsattÅrsinntekt(Yrkesaktivitet.Arbeidstaker(a1), INNTEKT)
            .inntektsjusteringer(Yrkesaktivitet.Arbeidstaker(a3), 1.januar, 10.januar, 1500.daglig)
            .inntektsjusteringer(Yrkesaktivitet.Arbeidstaker(a3), 15.januar, null, 1500.daglig)
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), 1.januar til 20.januar, Sykdomstidslinje(), arbeidstaker())
            .build()

        assertEquals(2, request.yrkesaktiviteter.size)
        request.yrkesaktiviteter[0].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a1), yrkesaktivitet.yrkesaktivitet)
            assertEquals(1, yrkesaktivitet.perioder.size)
            yrkesaktivitet.perioder[0].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.VedtaksperiodeForBeregning>(beregningsperiode)
                assertEquals(1.januar til 20.januar, beregningsperiode.periode)
            }
        }
        request.yrkesaktiviteter[1].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a3), yrkesaktivitet.yrkesaktivitet)
            assertEquals(2, yrkesaktivitet.perioder.size)
            yrkesaktivitet.perioder[0].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.AnnenInntektsperiode>(beregningsperiode)
                assertEquals(1.januar til 10.januar, beregningsperiode.periode)
            }
            yrkesaktivitet.perioder[1].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.AnnenInntektsperiode>(beregningsperiode)
                assertEquals(15.januar til 20.januar, beregningsperiode.periode)
            }
        }
    }

    @Test
    fun `tilkommet inntekt med vedtaksperioder`() {
        val request = BeregningRequest.Builder()
            .fastsattÅrsinntekt(Yrkesaktivitet.Arbeidstaker(a1), INNTEKT)
            .inntektsjusteringer(Yrkesaktivitet.Arbeidstaker(a1), 1.januar, null, 1500.daglig)
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), 1.januar til 20.januar, Sykdomstidslinje(), arbeidstaker())
            .build()

        assertEquals(1, request.yrkesaktiviteter.size)
        request.yrkesaktiviteter[0].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a1), yrkesaktivitet.yrkesaktivitet)
            assertEquals(1, yrkesaktivitet.perioder.size)
            yrkesaktivitet.perioder[0].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.VedtaksperiodeForBeregning>(beregningsperiode)
                assertEquals(1.januar til 20.januar, beregningsperiode.periode)
            }
        }
    }

    @Test
    fun `tilkommet inntekt med egne vedtaksperioder`() {
        val request = BeregningRequest.Builder()
            .fastsattÅrsinntekt(Yrkesaktivitet.Arbeidstaker(a1), INNTEKT)
            .inntektsjusteringer(Yrkesaktivitet.Arbeidstaker(a3), 1.januar, null, 1500.daglig)
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a1), UUID.randomUUID(), 1.januar til 20.januar, Sykdomstidslinje(), arbeidstaker())
            .vedtaksperiode(Yrkesaktivitet.Arbeidstaker(a3), UUID.randomUUID(), 1.januar til 10.januar, Sykdomstidslinje(), arbeidstaker())
            .build()

        assertEquals(2, request.yrkesaktiviteter.size)
        request.yrkesaktiviteter[0].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a1), yrkesaktivitet.yrkesaktivitet)
            assertEquals(1, yrkesaktivitet.perioder.size)
            yrkesaktivitet.perioder[0].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.VedtaksperiodeForBeregning>(beregningsperiode)
                assertEquals(1.januar til 20.januar, beregningsperiode.periode)
            }
        }
        request.yrkesaktiviteter[1].also { yrkesaktivitet ->
            assertEquals(Yrkesaktivitet.Arbeidstaker(a3), yrkesaktivitet.yrkesaktivitet)
            assertEquals(2, yrkesaktivitet.perioder.size)
            yrkesaktivitet.perioder[0].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.VedtaksperiodeForBeregning>(beregningsperiode)
                assertEquals(1.januar til 10.januar, beregningsperiode.periode)
            }
            yrkesaktivitet.perioder[1].also { beregningsperiode ->
                assertInstanceOf<BeregningRequest.AnnenInntektsperiode>(beregningsperiode)
                assertEquals(11.januar til 20.januar, beregningsperiode.periode)
            }
        }
    }

    private fun arbeidstaker(): Arbeidstaker {
        return Arbeidstaker(
            arbeidsgiverperiode = listOf(1.januar til 16.januar),
            dagerNavOvertarAnsvar = emptyList(),
            refusjonstidslinje = Beløpstidslinje.fra(
                januar, INNTEKT,
                Kilde(MeldingsreferanseId(UUID.randomUUID()), Avsender.ARBEIDSGIVER, LocalDateTime.now())
            )
        )
    }
}
