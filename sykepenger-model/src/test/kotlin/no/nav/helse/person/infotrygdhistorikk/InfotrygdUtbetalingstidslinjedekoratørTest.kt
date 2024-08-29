package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.faktaavklarteInntekter
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeBuilder
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiodeteller
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.UkjentDag
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilder
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InfotrygdUtbetalingstidslinjedekoratørTest {
    private val infotrygdkilde = SykdomshistorikkHendelse.Hendelseskilde("Infotrygdhistorikk", UUID.randomUUID(), LocalDateTime.now())

    @BeforeEach
    fun setup() {
        resetSeed()
    }

    @Test
    fun `ekskluderer dager før første dag`() {
        val builder = UtbetalingstidslinjeBuilder(
            hendelse = Aktivitetslogg(),
            faktaavklarteInntekter = listOf("a1" to 25000.månedlig).faktaavklarteInntekter(1.januar),
            organisasjonsnummer = "a1",
            regler = NormalArbeidstaker,
            subsumsjonslogg = MaskinellJurist(),
            beregningsperiode = februar
        )
        val dekoratør = InfotrygdUtbetalingstidslinjedekoratør(builder, februar, emptyList(), infotrygdkilde)
        val tidslinje = 31.S(infotrygdkilde) + 28.S + 31.S(infotrygdkilde)
        tidslinje.accept(ArbeidsgiverperiodeBuilder(Arbeidsgiverperiodeteller.NormalArbeidstaker, dekoratør, Subsumsjonslogg.NullObserver))
        assertEquals(februar, builder.result().periode())
    }

    @Test
    fun `ekskluderer infotrygd-snuter`() {
        val builder = UtbetalingstidslinjeBuilder(
            hendelse = Aktivitetslogg(),
            faktaavklarteInntekter = listOf("a1" to 25000.månedlig).faktaavklarteInntekter(1.januar),
            organisasjonsnummer = "a1",
            regler = NormalArbeidstaker,
            subsumsjonslogg = MaskinellJurist(),
            beregningsperiode = februar
        )
        val dekoratør = InfotrygdUtbetalingstidslinjedekoratør(builder, februar, listOf(1.januar til 10.februar), infotrygdkilde)
        val tidslinje = 31.S(infotrygdkilde) + 28.S
        tidslinje.accept(ArbeidsgiverperiodeBuilder(Arbeidsgiverperiodeteller.NormalArbeidstaker, dekoratør, Subsumsjonslogg.NullObserver))
        assertEquals(februar, builder.result().periode())
    }

    @Test
    fun `ekskluderer infotrygd-haler`() {
        val builder = UtbetalingstidslinjeBuilder(
            hendelse = Aktivitetslogg(),
            faktaavklarteInntekter = listOf("a1" to 25000.månedlig).faktaavklarteInntekter(1.januar),
            organisasjonsnummer = "a1",
            regler = NormalArbeidstaker,
            subsumsjonslogg = MaskinellJurist(),
            beregningsperiode = januar
        )
        val dekoratør = InfotrygdUtbetalingstidslinjedekoratør(builder, 1.januar til 31.januar, listOf(20.januar til 28.februar), infotrygdkilde)
        val tidslinje = 31.S + 28.S(infotrygdkilde)
        tidslinje.accept(ArbeidsgiverperiodeBuilder(Arbeidsgiverperiodeteller.NormalArbeidstaker, dekoratør, Subsumsjonslogg.NullObserver))
        assertEquals(januar, builder.result().periode())
    }

    @Test
    fun `legger ikke til samme ukjente dag flere ganger selv om det er utbetalt for flere arbeidsgivere`() {
        val builder = UtbetalingstidslinjeBuilder(
            hendelse = Aktivitetslogg(),
            faktaavklarteInntekter = listOf("a1" to 25000.månedlig).faktaavklarteInntekter(1.januar),
            organisasjonsnummer = "a1",
            regler = NormalArbeidstaker,
            subsumsjonslogg = MaskinellJurist(),
            beregningsperiode = 1.januar til 31.mars
        )
        val dekoratør = InfotrygdUtbetalingstidslinjedekoratør(builder, 1.januar til 31.mars, listOf(
            februar, // ag1 i IT
            februar // ag2 i IT
        ), infotrygdkilde)
        val tidslinje = 31.S + 28.S(infotrygdkilde) + 31.S
        tidslinje.accept(ArbeidsgiverperiodeBuilder(Arbeidsgiverperiodeteller.NormalArbeidstaker, dekoratør, Subsumsjonslogg.NullObserver))
        val utbetalingstidslinje = builder.result()
        assertEquals(1.januar til 31.mars, utbetalingstidslinje.periode())
        assertTrue((februar).all { utbetalingstidslinje[it] is UkjentDag })
    }
}
