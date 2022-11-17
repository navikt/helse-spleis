package no.nav.helse.person.infotrygdhistorikk

import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.testhelpers.somVilkårsgrunnlagHistorikk
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeBuilder
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiodeteller
import no.nav.helse.utbetalingstidslinje.Inntekter
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.UkjentDag
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilder
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InfotrygdUtbetalingstidslinjedekoratørTest {

    @BeforeEach
    fun setup() {
        resetSeed()
    }

    @Test
    fun `ekskluderer dager før første dag`() {
        val builder = UtbetalingstidslinjeBuilder(Inntekter(
            vilkårsgrunnlagHistorikk = mapOf(
                1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), 25000.månedlig)
            ).somVilkårsgrunnlagHistorikk("a1"),
            organisasjonsnummer = "a1",
            regler = NormalArbeidstaker,
            subsumsjonObserver = MaskinellJurist(),
        ), beregningsperiode = 1.februar til 28.februar)
        val dekoratør = InfotrygdUtbetalingstidslinjedekoratør(builder, 1.februar til 28.februar, emptyList())
        val tidslinje = 31.S + 28.S + 31.S
        tidslinje.accept(ArbeidsgiverperiodeBuilder(Arbeidsgiverperiodeteller.NormalArbeidstaker, dekoratør, SubsumsjonObserver.NullObserver))
        assertEquals(1.februar til 28.februar, builder.result().periode())
    }

    @Test
    fun `ekskluderer infotrygd-snuter`() {
        val builder = UtbetalingstidslinjeBuilder(Inntekter(
            vilkårsgrunnlagHistorikk = mapOf(
                1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), 25000.månedlig)
            ).somVilkårsgrunnlagHistorikk("a1"),
            organisasjonsnummer = "a1",
            regler = NormalArbeidstaker,
            subsumsjonObserver = MaskinellJurist()
        ), beregningsperiode = 1.februar til 28.februar)
        val dekoratør = InfotrygdUtbetalingstidslinjedekoratør(builder, 1.februar til 28.februar, listOf(1.januar til 10.februar))
        val tidslinje = 31.S + 28.S
        tidslinje.accept(ArbeidsgiverperiodeBuilder(Arbeidsgiverperiodeteller.NormalArbeidstaker, dekoratør, SubsumsjonObserver.NullObserver))
        assertEquals(11.februar til 28.februar, builder.result().periode())
    }

    @Test
    fun `ekskluderer infotrygd-haler`() {
        val builder = UtbetalingstidslinjeBuilder(Inntekter(
            vilkårsgrunnlagHistorikk = mapOf(
                1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), 25000.månedlig)
            ).somVilkårsgrunnlagHistorikk("a1"),
            organisasjonsnummer = "a1",
            regler = NormalArbeidstaker,
            subsumsjonObserver = MaskinellJurist()
        ), beregningsperiode = 1.januar til 31.januar)
        val dekoratør = InfotrygdUtbetalingstidslinjedekoratør(builder, 1.januar til 31.januar, listOf(20.januar til 28.februar))
        val tidslinje = 31.S + 28.S
        tidslinje.accept(ArbeidsgiverperiodeBuilder(Arbeidsgiverperiodeteller.NormalArbeidstaker, dekoratør, SubsumsjonObserver.NullObserver))
        assertEquals(1.januar til 19.januar, builder.result().periode())
    }

    @Test
    fun `legger ikke til samme ukjente dag flere ganger selv om det er utbetalt for flere arbeidsgivere`() {
        val builder = UtbetalingstidslinjeBuilder(Inntekter(
            vilkårsgrunnlagHistorikk = mapOf(
                1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), 25000.månedlig)
            ).somVilkårsgrunnlagHistorikk("a1"),
            organisasjonsnummer = "a1",
            regler = NormalArbeidstaker,
            subsumsjonObserver = MaskinellJurist()
        ), beregningsperiode = 1.januar til 31.mars)
        val dekoratør = InfotrygdUtbetalingstidslinjedekoratør(builder, 1.januar til 31.mars, listOf(
            1.februar til 28.februar, // ag1 i IT
            1.februar til 28.februar // ag2 i IT
        ))
        val tidslinje = 31.S + 28.S + 31.S
        tidslinje.accept(ArbeidsgiverperiodeBuilder(Arbeidsgiverperiodeteller.NormalArbeidstaker, dekoratør, SubsumsjonObserver.NullObserver))
        val utbetalingstidslinje = builder.result()
        assertEquals(1.januar til 31.mars, utbetalingstidslinje.periode())
        assertTrue((1.februar til 28.februar).all { utbetalingstidslinje[it] is UkjentDag })
    }
}
