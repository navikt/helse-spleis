package no.nav.helse.spleis.e2e.refusjon

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RefusjonsopplysningerPåBehandlingE2ETest : AbstractDslTest() {

    @Test
    fun `ny vedtaksperiode`() {
        håndterSøknad(januar)

        assertEquals(Beløpstidslinje(), inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
    }

    @Test
    fun `IM før vedtaksperiode`() {
        val tidsstempel = LocalDateTime.now()
        val im = håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, mottatt = tidsstempel)
        håndterSøknad(januar)

        val kilde = Kilde(im, Avsender.ARBEIDSGIVER, tidsstempel)
        assertEquals(Beløpstidslinje.fra(januar, INNTEKT, kilde), inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerVilkårsprøving`() {
        håndterSøknad(januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT)

        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Inntektsmelding.Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy)

        val kildeNy = Kilde(imNy, Avsender.ARBEIDSGIVER, tidsstempelNy)

        val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
        assertEquals(forventetTidslinje, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
        assertTilstander(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerHistorikk`() {
        håndterSøknad(januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT)
        håndterVilkårsgrunnlag(1.vedtaksperiode)

        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Inntektsmelding.Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy)

        val kildeNy = Kilde(imNy, Avsender.ARBEIDSGIVER, tidsstempelNy)

        val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
        assertEquals(forventetTidslinje, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerSimulering`() {
        håndterSøknad(januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)

        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Inntektsmelding.Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy)

        val kildeNy = Kilde(imNy, Avsender.ARBEIDSGIVER, tidsstempelNy)

        val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
        assertEquals(forventetTidslinje, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
        assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerGodkjenning`() {
        håndterSøknad(januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        val tidsstempelNy = LocalDateTime.now()
        val imNy = håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Inntektsmelding.Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy)

        val kildeNy = Kilde(imNy, Avsender.ARBEIDSGIVER, tidsstempelNy)

        val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
        assertEquals(forventetTidslinje, inspektør.vedtaksperioder(1.vedtaksperiode).refusjonstidslinje)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i TilUtbetaling`() {
        håndterSøknad(januar)
        val tidsstempelGammel = LocalDateTime.now()
        val imGammel = håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, mottatt = tidsstempelGammel)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)

        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Inntektsmelding.Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy)

        val kildeGammel = Kilde(imGammel, Avsender.ARBEIDSGIVER, tidsstempelGammel)
        val kildeNy = Kilde(imNy, Avsender.ARBEIDSGIVER, tidsstempelNy)

        val inspektør = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør
        assertEquals(2, inspektør.behandlinger.size)

        inspektør.behandlinger[0].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 31.januar, INNTEKT, kildeGammel)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        inspektør.behandlinger[1].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }

        assertTilstander(1.vedtaksperiode, TIL_UTBETALING, AVVENTER_REVURDERING)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i Avsluttet`() {
        val tidsstempelGammel = LocalDateTime.now()
        val imGammel = nyttVedtak(januar, tidsstempelGammel)
        val kildeGammel = Kilde(imGammel, Avsender.ARBEIDSGIVER, tidsstempelGammel)
        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Inntektsmelding.Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy)
        val kildeNy = Kilde(imNy, Avsender.ARBEIDSGIVER, tidsstempelNy)

        val inspektør = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør
        assertEquals(2, inspektør.behandlinger.size)

        inspektør.behandlinger[0].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 31.januar, INNTEKT, kildeGammel)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        inspektør.behandlinger[1].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `uendret refusjonsopplysninger i Avsluttet - arbeidsgiverperiode utført tidligere`() {
        val tidsstempelEldst = LocalDateTime.now().minusDays(10)
        val eldstId = nyttVedtak(januar, tidsstempelEldst)
        val kildeEldst = Kilde(eldstId, Avsender.ARBEIDSGIVER, tidsstempelEldst)

        val tidsstempelGammel = LocalDateTime.now().minusDays(1)
        val gammelId = nyttVedtak(10.februar til 28.februar, tidsstempelGammel, vedtaksperiode = 2, arbeidsgiverperiode = listOf(1.januar til 16.januar))
        val kildeGammel = Kilde(gammelId, Avsender.ARBEIDSGIVER, tidsstempelGammel)

        nullstillTilstandsendringer()

        inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.also { inspektør ->
            assertEquals(1, inspektør.behandlinger.size)
            inspektør.behandlinger[0].also {
                val forventetTidslinje = Beløpstidslinje.fra(januar, INNTEKT, kildeEldst)
                assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
            }
        }
        inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.also { inspektør ->
            assertEquals(1, inspektør.behandlinger.size)

            inspektør.behandlinger[0].also {
                val forventetTidslinje = Beløpstidslinje.fra(10.februar til 28.februar, INNTEKT, kildeGammel)
                assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
            }
        }
        assertTilstander(2.vedtaksperiode, AVSLUTTET)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerHistorikkRevurdering`() {
        val tidsstempelGammel = LocalDateTime.now()
        val imGammel = nyttVedtak(januar, tidsstempelGammel)
        val kildeGammel = Kilde(imGammel, Avsender.ARBEIDSGIVER, tidsstempelGammel)
        // Trigg en revurdering
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, mottatt = LocalDateTime.now())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Inntektsmelding.Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy)
        val kildeNy = Kilde(imNy, Avsender.ARBEIDSGIVER, tidsstempelNy)

        val inspektør = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør
        assertEquals(2, inspektør.behandlinger.size)

        inspektør.behandlinger[0].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 31.januar, INNTEKT, kildeGammel)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        inspektør.behandlinger[1].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerSimuleringRevurdering`() {
        val tidsstempelGammel = LocalDateTime.now()
        val imGammel = nyttVedtak(januar, tidsstempelGammel)
        val kildeGammel = Kilde(imGammel, Avsender.ARBEIDSGIVER, tidsstempelGammel)
        // Trigg en revurdering
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT*1.1, mottatt = LocalDateTime.now())
        håndterYtelser(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Inntektsmelding.Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy)
        val kildeNy = Kilde(imNy, Avsender.ARBEIDSGIVER, tidsstempelNy)

        val inspektør = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør
        assertEquals(2, inspektør.behandlinger.size)

        inspektør.behandlinger[0].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 31.januar, INNTEKT, kildeGammel)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        inspektør.behandlinger[1].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerGodkjenningRevurdering`() {
        val tidsstempelGammel = LocalDateTime.now()
        val imGammel = nyttVedtak(januar, tidsstempelGammel)
        val kildeGammel = Kilde(imGammel, Avsender.ARBEIDSGIVER, tidsstempelGammel)
        // Trigg en revurdering
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, mottatt = LocalDateTime.now())
        håndterYtelser(1.vedtaksperiode)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, refusjon = Inntektsmelding.Refusjon(500.daglig, 27.januar), mottatt = tidsstempelNy)
        val kildeNy = Kilde(imNy, Avsender.ARBEIDSGIVER, tidsstempelNy)

        val inspektør = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør
        assertEquals(2, inspektør.behandlinger.size)

        inspektør.behandlinger[0].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 31.januar, INNTEKT, kildeGammel)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        inspektør.behandlinger[1].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.januar til 27.januar, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.januar til 31.januar, INGEN, kildeNy)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING)
    }

    @Test
    fun `korrigerte refusjonsopplysninger i AvventerRevurdering`() {
        nyttVedtak(januar, tidsstempel = LocalDateTime.now())

        val tidsstempelGammel = LocalDateTime.now()
        val imGammel = nyttVedtak(mars, tidsstempel = tidsstempelGammel, vedtaksperiode = 2)
        val kildeGammel = Kilde(imGammel, Avsender.ARBEIDSGIVER, tidsstempelGammel)

        // Trigger en revurdering
        håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, mottatt = LocalDateTime.now())
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_REVURDERING)
        nullstillTilstandsendringer()

        val tidsstempelNy = LocalDateTime.now()
        val imNy = håndterInntektsmelding(listOf(1.mars til 16.mars), INNTEKT, refusjon = Inntektsmelding.Refusjon(500.daglig, 27.mars), mottatt = tidsstempelNy)
        val kildeNy = Kilde(imNy, Avsender.ARBEIDSGIVER, tidsstempelNy)

        val inspektør = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør
        assertEquals(2, inspektør.behandlinger.size)

        inspektør.behandlinger[0].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.mars til 31.mars, INNTEKT, kildeGammel)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        inspektør.behandlinger[1].also {
            val forventetTidslinje = Beløpstidslinje.fra(1.mars til 27.mars, 500.daglig, kildeNy) + Beløpstidslinje.fra(28.mars til 31.mars, INGEN, kildeNy)
            assertEquals(forventetTidslinje, it.endringer.last().refusjonstidslinje)
        }
        assertTilstander(2.vedtaksperiode, AVVENTER_REVURDERING)
    }

    @Test
    fun `Forlengelser bruker refusjonsopplysninger fra perioden før`() {
        a1 {
            val tidsstempel = LocalDateTime.now()
            val im = nyttVedtak(januar, tidsstempel)
            val kilde = Kilde(im, Avsender.ARBEIDSGIVER, tidsstempel)
            forlengVedtak(februar)

            val refusjonstidslinjeVedtaksperiode1 = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.single().endringer.last().refusjonstidslinje
            val refusjonstidslinjeVedtaksperiode2 = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.single().endringer.last().refusjonstidslinje

            assertEquals(Beløpstidslinje.fra(januar, INNTEKT, kilde), refusjonstidslinjeVedtaksperiode1)
            assertEquals(Beløpstidslinje.fra(februar, INNTEKT, kilde), refusjonstidslinjeVedtaksperiode2)
        }
    }


    @Test
    fun `Må kunne videreføre refusjonsopplysninger når det kommer søknad som bridger gap'et som før var der`() {
        a1 {
            nyttVedtak(mars)
            nyPeriode(januar)
            nyPeriode(februar)

            assertTrue(inspektør.vedtaksperioder(3.vedtaksperiode).refusjonstidslinje.isNotEmpty())
            assertTrue(inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje.isNotEmpty())
        }
    }

    @Test
    fun `Må kunne videreføre refusjonsopplysninger når flere perioder bridger gap'et som før var der`() {
        a1 {
            nyttVedtak(april)
            nyPeriode(januar)
            nyPeriode(februar)
            nyPeriode(mars)

            val vedtaksperiodeJanuar = 2.vedtaksperiode
            val vedtaksperiodeFebruar = 3.vedtaksperiode
            val vedtaksperiodeMars = 4.vedtaksperiode
            val vedtaksperiodeApril = 1.vedtaksperiode

            // Vi har egentlig refusjonsopplysninger for alle periodene, men for out-of-order-perioder får de ikke
            // refusjonsopplysninger før det er dens tur til å gå videre
            assertTrue(inspektør.vedtaksperioder(vedtaksperiodeJanuar).refusjonstidslinje.isNotEmpty())
            assertTrue(inspektør.vedtaksperioder(vedtaksperiodeFebruar).refusjonstidslinje.isEmpty())
            assertTrue(inspektør.vedtaksperioder(vedtaksperiodeMars).refusjonstidslinje.isNotEmpty())
            assertTrue(inspektør.vedtaksperioder(vedtaksperiodeApril).refusjonstidslinje.isNotEmpty())

            håndterVilkårsgrunnlag(vedtaksperiodeJanuar)
            håndterYtelser(vedtaksperiodeJanuar)
            håndterSimulering(vedtaksperiodeJanuar)
            håndterUtbetalingsgodkjenning(vedtaksperiodeJanuar)
            håndterUtbetalt()

            assertSisteTilstand(vedtaksperiodeFebruar, AVVENTER_HISTORIKK)
            assertTrue(inspektør.vedtaksperioder(vedtaksperiodeFebruar).refusjonstidslinje.isNotEmpty())

        }
    }

    @Test
    fun `Må kunne tilbakeføre refusjonsopplysninger når perioden etter deg har refusjonsopplysninger`() {
        a1 {
            nyttVedtak(februar)
            nyPeriode(januar)
            assertTrue(inspektør.vedtaksperioder(2.vedtaksperiode).refusjonstidslinje.isNotEmpty())
        }
    }

    @Test
    fun `Refusjonsopplysninger på en periode som kiler seg midt mellom to strekker opplysningene fra perioden før seg`() {
        a1 {
            nyttVedtak(januar, beregnetInntekt = INNTEKT)
            nyttVedtak(mars, beregnetInntekt = INNTEKT*1.1)
            nyPeriode(februar)

            assertTrue(inspektør.vedtaksperioder(3.vedtaksperiode).refusjonstidslinje.all { it.beløp == INNTEKT })
        }
    }

    @Test
    fun `Forlengelser bruker refusjonsopplysninger fra perioden før, men må hensynta eventuell opphør av refusjon i refusjonshistorikken`() {
        a1 {
            val tidsstempel = LocalDateTime.now()
            val im = nyttVedtak(januar, tidsstempel, opphørAvRefusjon = 31.januar)
            val kilde = Kilde(im, Avsender.ARBEIDSGIVER, tidsstempel)
            forlengVedtak(februar)

            val refusjonstidslinjeVedtaksperiode1 = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.single().endringer.last().refusjonstidslinje
            val refusjonstidslinjeVedtaksperiode2 = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.single().endringer.last().refusjonstidslinje

            assertEquals(Beløpstidslinje.fra(januar, INNTEKT, kilde), refusjonstidslinjeVedtaksperiode1)
            assertEquals(Beløpstidslinje.fra(februar, INGEN, kilde), refusjonstidslinjeVedtaksperiode2)

        }
    }

    @Test
    fun `Forlengelser bruker refusjonsopplysninger fra perioden før, men må hensynta eventuelle endringer i refusjonshistorikken`() {
        a1 {
            val tidsstempel = LocalDateTime.now()
            val im = nyttVedtak(januar, tidsstempel, endringerIRefusjon = listOf(
                Inntektsmelding.Refusjon.EndringIRefusjon(INNTEKT * 0.8, 1.februar),
                Inntektsmelding.Refusjon.EndringIRefusjon(INNTEKT * 0.5, 20.februar)
            ))
            val kilde = Kilde(im, Avsender.ARBEIDSGIVER, tidsstempel)
            forlengVedtak(februar)

            val refusjonstidslinjeVedtaksperiode1 = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.single().endringer.last().refusjonstidslinje
            val refusjonstidslinjeVedtaksperiode2 = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.single().endringer.last().refusjonstidslinje

            assertEquals(Beløpstidslinje.fra(januar, INNTEKT, kilde), refusjonstidslinjeVedtaksperiode1)
            assertEquals(Beløpstidslinje.fra(1.februar til 19.februar, INNTEKT * 0.8, kilde) + Beløpstidslinje.fra(20.februar til 28.februar, INNTEKT * 0.5, kilde), refusjonstidslinjeVedtaksperiode2)
        }
    }

    private fun nyttVedtak(
        periode: Periode,
        tidsstempel: LocalDateTime,
        vedtaksperiode: Int = 1,
        arbeidsgiverperiode: List<Periode> = listOf(periode.start til periode.start.plusDays(15)),
        opphørAvRefusjon: LocalDate? = null,
        endringerIRefusjon: List<Inntektsmelding.Refusjon.EndringIRefusjon> = emptyList()
    ): UUID {
        håndterSøknad(periode)
        val im = håndterInntektsmelding(
            arbeidsgiverperiode,
            INNTEKT,
            førsteFraværsdag = periode.start,
            mottatt = tidsstempel,
            refusjon = Inntektsmelding.Refusjon(INNTEKT, opphørsdato = opphørAvRefusjon, endringerIRefusjon = endringerIRefusjon)
        )
        håndterVilkårsgrunnlag(vedtaksperiode.vedtaksperiode)
        håndterYtelser(vedtaksperiode.vedtaksperiode)
        håndterSimulering(vedtaksperiode.vedtaksperiode)
        håndterUtbetalingsgodkjenning(vedtaksperiode.vedtaksperiode)
        håndterUtbetalt()
        return im
    }
}