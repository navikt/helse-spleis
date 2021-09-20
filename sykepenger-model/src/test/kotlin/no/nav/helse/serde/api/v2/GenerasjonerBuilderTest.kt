package no.nav.helse.serde.api.v2

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.hendelser.til
import no.nav.helse.person.arbeidsgiver
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GenerasjonerBuilderTest : AbstractEndToEndTest() {

    private val generasjoner get() = generasjoner()

    private fun generasjoner(): List<Generasjon> {
        val generasjonerBuilder = GenerasjonerBuilder(emptyList())
        person.arbeidsgiver(ORGNUMMER).accept(generasjonerBuilder)
        return generasjonerBuilder.build()
    }

    @Test
    fun `happy case`() {
        nyttVedtak(1.januar, 31.januar)

        assertEquals(1, generasjoner.size)
        assertEquals(1, generasjoner[0].perioder.size)
        0.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `happy case med to perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        assertEquals(1, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        0.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `happy case med to perioder med gap`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(2.februar, 28.februar)

        0.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (2.februar til 28.februar) medAntallDager 27 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `periode blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyring((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)

        assertEquals(2, generasjoner.size)
        assertEquals(1, generasjoner[0].perioder.size)
        assertEquals(1, generasjoner[1].perioder.size)

        0.generasjon {
            tidslinjeperiode(0) er "Ubetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `forlengelse blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyring((27.februar til 28.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)

        assertEquals(2, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)

        0.generasjon {
            tidslinjeperiode(0) er "Ubetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `to perioder - første blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyring((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        assertEquals(2, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)

        0.generasjon {
            tidslinjeperiode(0) er "Ubetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(1) er "Ubetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `to perioder - første blir revurdert to ganger`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyring((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)
        håndterOverstyring((29.januar til 31.januar).map { manuellSykedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        assertEquals(3, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)
        assertEquals(2, generasjoner[2].perioder.size)

        0.generasjon {
            tidslinjeperiode(0) er "Ubetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(1) er "Ubetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        2.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `to perioder - første blir revurdert to ganger, deretter blir andre revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyring((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)
        håndterOverstyring((29.januar til 31.januar).map { manuellSykedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        håndterOverstyring((27.februar til 28.februar).map { manuellSykedag(it) })
        håndterYtelser(2.vedtaksperiode)

        assertEquals(4, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)
        assertEquals(2, generasjoner[2].perioder.size)
        assertEquals(2, generasjoner[3].perioder.size)

        0.generasjon {
            tidslinjeperiode(0) er "Ubetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        2.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        3.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `to perioder med gap - siste blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(2.februar, 28.februar)

        håndterOverstyring((27.februar til 28.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)

        assertEquals(2, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)

        0.generasjon {
            tidslinjeperiode(0) er "Ubetalt" avType "REVURDERING" fra (2.februar til 28.februar) medAntallDager 27 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (2.februar til 28.februar) medAntallDager 27 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `én periode som blir annullert`() {
        nyttVedtak(1.januar, 31.januar)
        håndterAnnullerUtbetaling()

        assertEquals(2, generasjoner.size)
        assertEquals(1, generasjoner[0].perioder.size)
        assertEquals(1, generasjoner[1].perioder.size)

        0.generasjon {
            tidslinjeperiode(0) er "Sendt" avType "ANNULLERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet true
        }

        1.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet true
        }
    }

    @Test
    fun `to perioder som blir annullert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterAnnullerUtbetaling()

        assertEquals(2, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)

        0.generasjon {
            tidslinjeperiode(0) er "Sendt" avType "ANNULLERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet true
            tidslinjeperiode(1) er "Sendt" avType "ANNULLERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet true
        }

        1.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet true
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet true
        }
    }

    @Test
    fun `to perioder som blir annullert - deretter nye perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterAnnullerUtbetaling()

        assertEquals(2, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)

        0.generasjon {
            tidslinjeperiode(0) er "Sendt" avType "ANNULLERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet true
            tidslinjeperiode(1) er "Sendt" avType "ANNULLERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet true
        }

        1.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet true
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet true
        }
    }

    @Test
    fun `to arbeidsgiverperioder - siste blir annullert`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(1.mars, 31.mars)
        håndterAnnullerUtbetaling()

        assertEquals(2, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)

        0.generasjon {
            tidslinjeperiode(0) er "Sendt" avType "ANNULLERING" fra (1.mars til 31.mars) medAntallDager 31 forkastet true
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet true
        }

        1.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.mars til 31.mars) medAntallDager 31 forkastet true
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet true
        }
    }

    @Test
    fun `to perioder som blir revurdert - deretter forlengelse`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyring((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        forlengVedtak(1.mars, 31.mars)

        assertEquals(2, generasjoner.size)
        assertEquals(3, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)

        0.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.mars til 31.mars) medAntallDager 31 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(2) er "Utbetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `to perioder som blir revurdert - deretter forlengelse som så blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyring((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        forlengVedtak(1.mars, 31.mars)
        håndterOverstyring((1.mars til 31.mars).map { manuellFeriedag(it) })
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt(3.vedtaksperiode)

        assertEquals(3, generasjoner.size)
        assertEquals(3, generasjoner[0].perioder.size)
        assertEquals(3, generasjoner[1].perioder.size)
        assertEquals(2, generasjoner[2].perioder.size)

        0.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "REVURDERING" fra (1.mars til 31.mars) medAntallDager 31 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(2) er "Utbetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.mars til 31.mars) medAntallDager 31 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(2) er "Utbetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        2.generasjon {
            tidslinjeperiode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            tidslinjeperiode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `korte perioder - arbeidsgiversøknader`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar, 15.januar, 100.prosent))

        0.generasjon {
            kortPeriode(0) fra (1.januar til 15.januar) medAntallDager 15 forkastet false
        }
    }

    @Test
    fun `kort periode med forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 15.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        assertEquals(1, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)

        0.generasjon {
            tidslinjeperiode(0) er "Ubetalt" avType "UTBETALING" fra (16.januar til 15.februar) medAntallDager 31 forkastet false
            kortPeriode(1) fra (1.januar til 15.januar) medAntallDager 15 forkastet false
        }
    }

    @Test
    fun `ventende perioder - ubehandlet`() {
        TODO()
    }

    @Test
    fun `periode uten utbetaling - kun ferie`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Søknad.Søknadsperiode.Ferie(17.januar, 31.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser()
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser()

        0.generasjon {
            tidslinjeperiode(0) er "GodkjentUtenUtbetaling" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    private fun Int.generasjon(assertBlock: Generasjon.() -> Unit) {
        require(this >= 0) { "Kan ikke være et negativt tall!" }
        generasjoner[this].run(assertBlock)
    }

    private infix fun <T : Periode> T.medAntallDager(antall: Int): T {
        assertEquals(antall, sammenslåttTidslinje.size)
        return this
    }

    private infix fun <T : Periode> T.forkastet(forkastet: Boolean): T {
        assertEquals(forkastet, this.erForkastet)
        return this
    }

    private infix fun Tidslinjeperiode.er(utbetalingstilstand: String): Tidslinjeperiode {
        assertEquals(utbetalingstilstand, this.utbetalingstilstand)
        return this
    }

    private infix fun Tidslinjeperiode.avType(type: String): Tidslinjeperiode {
        assertEquals(type, this.utbetalingstype)
        return this
    }

    private infix fun <T : Periode> T.fra(periode: no.nav.helse.hendelser.Periode): T {
        assertEquals(periode.start, this.fom)
        assertEquals(periode.endInclusive, this.tom)
        return this
    }

    private fun Generasjon.tidslinjeperiode(index: Int): Tidslinjeperiode {
        val periode = this.perioder[index]
        require(periode is Tidslinjeperiode) { "Perioden er ikke en tidslinjeperiode!" }
        return periode
    }

    private fun Generasjon.kortPeriode(index: Int): KortPeriode {
        val periode = this.perioder[index]
        require(periode is KortPeriode) { "Perioden er ikke en kort periode!" }
        return periode
    }
}
