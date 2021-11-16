package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.Toggle
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.person.arbeidsgiver
import no.nav.helse.serde.api.v2.BeregnetPeriode
import no.nav.helse.serde.api.v2.Generasjon
import no.nav.helse.serde.api.v2.Tidslinjeperiode
import no.nav.helse.serde.api.v2.UberegnetPeriode
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class GenerasjonerBuilderTest : AbstractEndToEndTest() {

    private val generasjoner get() = generasjoner()

    private fun generasjoner(): List<Generasjon> {
        val sammenligningsgrunnlagBuilder = OppsamletSammenligningsgrunnlagBuilder(person)
        val vilkårsgrunnlagHistorikk = VilkårsgrunnlagBuilder(person, sammenligningsgrunnlagBuilder).build()
        val generasjonerBuilder = GenerasjonerBuilder(søknadDTOer, UNG_PERSON_FNR_2018.somFødselsnummer(), vilkårsgrunnlagHistorikk, person.arbeidsgiver(ORGNUMMER))
        return generasjonerBuilder.build()
    }

    @Test
    fun `happy case`() {
        nyttVedtak(1.januar, 31.januar)

        assertEquals(1, generasjoner.size)
        assertEquals(1, generasjoner[0].perioder.size)
        0.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `happy case med periode til godkjenning`() {
        tilGodkjenning(1.januar, 31.januar, ORGNUMMER)

        assertEquals(1, generasjoner.size)
        assertEquals(1, generasjoner[0].perioder.size)
        0.generasjon {
            beregnetPeriode(0) er "Ubetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `happy case med to perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        assertEquals(1, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        0.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `happy case med to perioder med gap`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(2.februar, 28.februar)

        0.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (2.februar til 28.februar) medAntallDager 27 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `periode blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)

        assertEquals(2, generasjoner.size)
        assertEquals(1, generasjoner[0].perioder.size)
        assertEquals(1, generasjoner[1].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er "Ubetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `forlengelse blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((27.februar til 28.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)

        assertEquals(2, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er "Ubetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `to perioder - første blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        assertEquals(2, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er "Ubetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Ubetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `to perioder - første blir revurdert to ganger`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)
        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellSykedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        assertEquals(3, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)
        assertEquals(2, generasjoner[2].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er "Ubetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Ubetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        2.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `to perioder - første blir revurdert to ganger, deretter blir andre revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)
        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellSykedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        håndterOverstyrTidslinje((27.februar til 28.februar).map { manuellSykedag(it) })
        håndterYtelser(2.vedtaksperiode)

        assertEquals(4, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)
        assertEquals(2, generasjoner[2].perioder.size)
        assertEquals(2, generasjoner[3].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er "Ubetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        2.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        3.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `to perioder med gap - siste blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(2.februar, 28.februar)

        håndterOverstyrTidslinje((27.februar til 28.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)

        assertEquals(2, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er "Ubetalt" avType "REVURDERING" fra (2.februar til 28.februar) medAntallDager 27 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (2.februar til 28.februar) medAntallDager 27 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
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
            beregnetPeriode(0) er "Sendt" avType "ANNULLERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet true
        }

        1.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet true
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
            beregnetPeriode(0) er "Sendt" avType "ANNULLERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet true
            beregnetPeriode(1) er "Sendt" avType "ANNULLERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet true
        }

        1.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet true
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet true
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
            beregnetPeriode(0) er "Sendt" avType "ANNULLERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet true
            beregnetPeriode(1) er "Sendt" avType "ANNULLERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet true
        }

        1.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet true
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet true
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
            beregnetPeriode(0) er "Sendt" avType "ANNULLERING" fra (1.mars til 31.mars) medAntallDager 31 forkastet true
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.mars til 31.mars) medAntallDager 31 forkastet true
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `to perioder som blir revurdert - deretter forlengelse`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
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
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.mars til 31.mars) medAntallDager 31 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(2) er "Utbetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `to perioder som blir revurdert - deretter forlengelse som så blir revurdert`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        forlengVedtak(1.mars, 31.mars)
        håndterOverstyrTidslinje((1.mars til 31.mars).map { manuellFeriedag(it) })
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt(3.vedtaksperiode)

        assertEquals(3, generasjoner.size)
        assertEquals(3, generasjoner[0].perioder.size)
        assertEquals(3, generasjoner[1].perioder.size)
        assertEquals(2, generasjoner[2].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "REVURDERING" fra (1.mars til 31.mars) medAntallDager 31 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(2) er "Utbetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.mars til 31.mars) medAntallDager 31 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "REVURDERING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(2) er "Utbetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        2.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `korte perioder - arbeidsgiversøknader`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar, 15.januar, 100.prosent))

        0.generasjon {
            uberegnetPeriode(0) fra (1.januar til 15.januar) medAntallDager 15 forkastet false
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
            beregnetPeriode(0) er "Ubetalt" avType "UTBETALING" fra (16.januar til 15.februar) medAntallDager 31 forkastet false
            uberegnetPeriode(1) fra (1.januar til 15.januar) medAntallDager 15 forkastet false
        }
    }

    @Test
    fun `kort periode med forlengelse og revurdering av siste periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar, 100.prosent))
        håndterSøknadArbeidsgiver(SøknadArbeidsgiver.Sykdom(1.januar, 15.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(16.januar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 15.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt(2.vedtaksperiode)

        håndterOverstyrTidslinje((13.februar til 14.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)

        assertEquals(2, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er "Ubetalt" avType "REVURDERING" fra (16.januar til 15.februar) medAntallDager 31 forkastet false
            uberegnetPeriode(1) fra (1.januar til 15.januar) medAntallDager 15 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (16.januar til 15.februar) medAntallDager 31 forkastet false
            uberegnetPeriode(1) fra (1.januar til 15.januar) medAntallDager 15 forkastet false
        }
    }

    @Test
    fun `ventende periode`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))

        assertEquals(1, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)

        0.generasjon {
            uberegnetPeriode(0) fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `ventende perioder med revurdert tidligere periode`() {
        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100.prosent))

        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)

        assertEquals(2, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(1, generasjoner[1].perioder.size)

        0.generasjon {
            uberegnetPeriode(0) fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er "Ubetalt" avType "REVURDERING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er "Utbetalt" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `periode uten utbetaling - kun ferie`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Søknad.Søknadsperiode.Ferie(17.januar, 31.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser()
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser()

        assertEquals(1, generasjoner.size)
        assertEquals(1, generasjoner[0].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er "GodkjentUtenUtbetaling" avType "UTBETALING" fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `får riktig aldersvilkår per periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        0.generasjon {
            beregnetPeriode(0).assertAldersvilkår(true, 18)
            beregnetPeriode(1).assertAldersvilkår(true, 17)
        }
    }

    @Test
    fun `får riktig sykepengedager-vilkår per periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        0.generasjon {
            beregnetPeriode(0).assertSykepengedagerVilkår(31, 217, 28.desember, 1.januar, true)
            beregnetPeriode(1).assertSykepengedagerVilkår(11, 237, 28.desember, 1.januar, true)
        }
    }

    @Test
    fun `får riktig søknadsfrist-vilkår per periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        0.generasjon {
            beregnetPeriode(0).assertSøknadsfristVilkår(1.februar, 28.februar, 28.februar.atStartOfDay(), true)
            beregnetPeriode(1).assertSøknadsfristVilkår(1.januar, 31.januar, 31.januar.atStartOfDay(), true)
        }
    }

    @Test
    fun `får riktig vilkår per periode ved revurdering av siste periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje((27.februar til 28.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)

        0.generasjon {
            beregnetPeriode(0).assertAldersvilkår(true, 18)
            beregnetPeriode(1).assertAldersvilkår(true, 17)
            beregnetPeriode(0).assertSykepengedagerVilkår(29,219, 1.januar(2019), 1.januar,true)
            beregnetPeriode(1).assertSykepengedagerVilkår(11,237, 28.desember, 1.januar,true)
            beregnetPeriode(0).assertSøknadsfristVilkår(1.februar, 28.februar, 28.februar.atStartOfDay(),true)
            beregnetPeriode(1).assertSøknadsfristVilkår(1.januar, 31.januar, 31.januar.atStartOfDay(),true)
        }
        1.generasjon {
            beregnetPeriode(0).assertSøknadsfristVilkår(1.februar, 28.februar, 28.februar.atStartOfDay(), true)
            beregnetPeriode(1).assertSøknadsfristVilkår(1.januar, 31.januar, 31.januar.atStartOfDay(), true)
            beregnetPeriode(0).assertSykepengedagerVilkår(31, 217, 28.desember, 1.januar, true)
            beregnetPeriode(1).assertSykepengedagerVilkår(11, 237, 28.desember, 1.januar, true)
            beregnetPeriode(0).assertSøknadsfristVilkår(1.februar, 28.februar, 28.februar.atStartOfDay(),true)
            beregnetPeriode(1).assertSøknadsfristVilkår(1.januar, 31.januar, 31.januar.atStartOfDay(),true)
        }
    }

    @Test
    fun `får riktig vilkår per periode ved revurdering av første periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)
        håndterOverstyrTidslinje((30.januar til 31.januar).map { manuellFeriedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        0.generasjon {
            beregnetPeriode(0).assertAldersvilkår(true, 18)
            beregnetPeriode(1).assertAldersvilkår(true, 17)
            // Revurdering av tidligere periode medfører at alle perioder berørt av revurderingen deler den samme utbetalingen, og derfor ender opp med samme
            // gjenstående dager, forbrukte dager og maksdato. Kan muligens skrives om i modellen slik at disse tallene kan fiskes ut fra utbetalingen gitt en
            // periode
            beregnetPeriode(0).assertSykepengedagerVilkår(29,219, 1.januar(2019), 1.januar,true)
            beregnetPeriode(1).assertSykepengedagerVilkår(29,219, 1.januar(2019), 1.januar,true)
            beregnetPeriode(0).assertSøknadsfristVilkår(1.februar, 28.februar, 28.februar.atStartOfDay(),true)
            beregnetPeriode(1).assertSøknadsfristVilkår(1.januar, 31.januar, 31.januar.atStartOfDay(),true)
        }
        1.generasjon {
            beregnetPeriode(0).assertSøknadsfristVilkår(1.februar, 28.februar, 28.februar.atStartOfDay(), true)
            beregnetPeriode(1).assertSøknadsfristVilkår(1.januar, 31.januar, 31.januar.atStartOfDay(), true)
            beregnetPeriode(0).assertSykepengedagerVilkår(31, 217, 28.desember, 1.januar, true)
            beregnetPeriode(1).assertSykepengedagerVilkår(11, 237, 28.desember, 1.januar, true)
            beregnetPeriode(0).assertSøknadsfristVilkår(1.februar, 28.februar, 28.februar.atStartOfDay(),true)
            beregnetPeriode(1).assertSøknadsfristVilkår(1.januar, 31.januar, 31.januar.atStartOfDay(),true)
        }
    }

    @Test
    fun `ta med personoppdrag`() {
        Toggle.LageBrukerutbetaling.enable {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(refusjon = Inntektsmelding.Refusjon(0.månedlig, null), førsteFraværsdag = 1.januar, arbeidsgiverperioder = listOf(1.januar til 16.januar))
            håndterYtelser()
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser()
            håndterSimulering()
            håndterUtbetalingsgodkjenning()
            håndterUtbetalt()

            0.generasjon {
                assertEquals(0, this.perioder.first().sammenslåttTidslinje[16].utbetalingsinfo!!.utbetaling)
                assertEquals(0, this.perioder.first().sammenslåttTidslinje[16].utbetalingsinfo!!.arbeidsgiverbeløp)
                assertEquals(0, this.perioder.first().sammenslåttTidslinje[16].utbetalingsinfo!!.refusjonsbeløp)
                assertEquals(1431, this.perioder.first().sammenslåttTidslinje[16].utbetalingsinfo!!.personbeløp)
                assertEquals(0, beregnetPeriode(0).utbetaling.arbeidsgiverNettoBeløp)
                assertEquals(15741, beregnetPeriode(0).utbetaling.personNettoBeløp)
            }
        }
    }

    private fun BeregnetPeriode.assertAldersvilkår(expectedOppfylt: Boolean, expectedAlderSisteSykedag: Int) {
        assertEquals(expectedOppfylt, periodevilkår.alder.oppfylt)
        assertEquals(expectedAlderSisteSykedag, periodevilkår.alder.alderSisteSykedag)
    }

    private fun BeregnetPeriode.assertSykepengedagerVilkår(
        expectedForbrukteSykedager: Int,
        expectedGjenståendeSykedager: Int,
        expectedMaksdato: LocalDate,
        expectedSkjæringstidspunkt: LocalDate,
        expectedOppfylt: Boolean
    ) {
        assertEquals(expectedForbrukteSykedager, periodevilkår.sykepengedager.forbrukteSykedager)
        assertEquals(expectedGjenståendeSykedager, periodevilkår.sykepengedager.gjenståendeDager)
        assertEquals(expectedMaksdato, periodevilkår.sykepengedager.maksdato)
        assertEquals(expectedSkjæringstidspunkt, periodevilkår.sykepengedager.skjæringstidspunkt)
        assertEquals(expectedOppfylt, periodevilkår.sykepengedager.oppfylt)
    }

    private fun BeregnetPeriode.assertSøknadsfristVilkår(
        expectedSøknadFom: LocalDate,
        expectedSøknadTom: LocalDate,
        expectedSendtNav: LocalDateTime,
        expectedOppfylt: Boolean
    ) {
        assertEquals(expectedSøknadFom, periodevilkår.søknadsfrist?.søknadFom)
        assertEquals(expectedSøknadTom, periodevilkår.søknadsfrist?.søknadTom)
        assertEquals(expectedSendtNav, periodevilkår.søknadsfrist?.sendtNav)
        assertEquals(expectedOppfylt, periodevilkår.søknadsfrist?.oppfylt)
    }

    private fun Int.generasjon(assertBlock: Generasjon.() -> Unit) {
        require(this >= 0) { "Kan ikke være et negativt tall!" }
        generasjoner[this].run(assertBlock)
    }

    private infix fun <T : Tidslinjeperiode> T.medAntallDager(antall: Int): T {
        assertEquals(antall, sammenslåttTidslinje.size)
        return this
    }

    private infix fun <T : Tidslinjeperiode> T.forkastet(forkastet: Boolean): T {
        assertEquals(forkastet, this.erForkastet)
        return this
    }

    private infix fun BeregnetPeriode.er(utbetalingstilstand: String): BeregnetPeriode {
        assertEquals(utbetalingstilstand, this.utbetaling.status)
        return this
    }

    private infix fun BeregnetPeriode.avType(type: String): BeregnetPeriode {
        assertEquals(type, this.utbetaling.type)
        return this
    }

    private infix fun <T : Tidslinjeperiode> T.fra(periode: Periode): T {
        assertEquals(periode.start, this.fom)
        assertEquals(periode.endInclusive, this.tom)
        return this
    }

    private fun Generasjon.beregnetPeriode(index: Int): BeregnetPeriode {
        val periode = this.perioder[index]
        require(periode is BeregnetPeriode) { "Perioden er ikke en tidslinjeperiode!" }
        return periode
    }

    private fun Generasjon.uberegnetPeriode(index: Int): UberegnetPeriode {
        val periode = this.perioder[index]
        require(periode is UberegnetPeriode) { "Perioden er ikke en kort periode!" }
        return periode
    }
}
