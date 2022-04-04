package no.nav.helse.serde.api.v2.buildere

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.arbeidsgiver
import no.nav.helse.serde.api.builders.InntektshistorikkForAOrdningenBuilder
import no.nav.helse.serde.api.v2.BeregnetPeriode
import no.nav.helse.serde.api.v2.Generasjon
import no.nav.helse.serde.api.v2.Tidslinjeperiode
import no.nav.helse.serde.api.v2.UberegnetPeriode
import no.nav.helse.serde.api.v2.Utbetalingstatus
import no.nav.helse.serde.api.v2.Utbetalingtype
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterAnnullerUtbetaling
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.manuellFeriedag
import no.nav.helse.spleis.e2e.manuellSykedag
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.søknadDTOer
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GenerasjonerBuilderTest : AbstractEndToEndTest() {

    private val generasjoner get() = generasjoner(ORGNUMMER)

    private fun generasjoner(organisasjonsnummer: String): List<Generasjon> {
        val sammenligningsgrunnlagBuilder = OppsamletSammenligningsgrunnlagBuilder(person)
        val inntektshistorikkForAordningenBuilder = InntektshistorikkForAOrdningenBuilder(person)
        val vilkårsgrunnlagHistorikk =
            VilkårsgrunnlagBuilder(person, sammenligningsgrunnlagBuilder, inntektshistorikkForAordningenBuilder).build()
        val generasjonerBuilder = GenerasjonerBuilder(
            søknadDTOer,
            UNG_PERSON_FNR_2018,
            vilkårsgrunnlagHistorikk,
            person.arbeidsgiver(organisasjonsnummer)
        )
        return generasjonerBuilder.build()
    }

    @Test
    fun `happy case`() {
        nyttVedtak(1.januar, 31.januar)

        assertEquals(1, generasjoner.size)
        assertEquals(1, generasjoner[0].perioder.size)
        0.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `happy case med periode til godkjenning`() {
        tilGodkjenning(1.januar, 31.januar, ORGNUMMER)

        assertEquals(1, generasjoner.size)
        assertEquals(1, generasjoner[0].perioder.size)
        0.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Ubetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `happy case med to perioder`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        assertEquals(1, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        0.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `happy case med to perioder med gap`() {
        nyttVedtak(1.januar, 31.januar)
        nyttVedtak(2.februar, 28.februar)

        0.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (2.februar til 28.februar) medAntallDager 27 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
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
            beregnetPeriode(0) er Utbetalingstatus.Ubetalt avType Utbetalingtype.REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
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
            beregnetPeriode(0) er Utbetalingstatus.Ubetalt avType Utbetalingtype.REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
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
            beregnetPeriode(0) er Utbetalingstatus.Ubetalt avType Utbetalingtype.REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Ubetalt avType Utbetalingtype.REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
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
        håndterUtbetalt()
        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellSykedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        assertEquals(3, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)
        assertEquals(2, generasjoner[2].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Ubetalt avType Utbetalingtype.REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Ubetalt avType Utbetalingtype.REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        2.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
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
        håndterUtbetalt()
        håndterOverstyrTidslinje((29.januar til 31.januar).map { manuellSykedag(it) })
        håndterYtelser(1.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrTidslinje((27.februar til 28.februar).map { manuellSykedag(it) })
        håndterYtelser(2.vedtaksperiode)

        assertEquals(4, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)
        assertEquals(2, generasjoner[2].perioder.size)
        assertEquals(2, generasjoner[3].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Ubetalt avType Utbetalingtype.REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        2.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        3.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
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
            beregnetPeriode(0) er Utbetalingstatus.Ubetalt avType Utbetalingtype.REVURDERING fra (2.februar til 28.februar) medAntallDager 27 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (2.februar til 28.februar) medAntallDager 27 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
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
            beregnetPeriode(0) er Utbetalingstatus.Sendt avType Utbetalingtype.ANNULLERING fra (1.januar til 31.januar) medAntallDager 31 forkastet true
        }

        1.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet true
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
            beregnetPeriode(0) er Utbetalingstatus.Sendt avType Utbetalingtype.ANNULLERING fra (1.februar til 28.februar) medAntallDager 28 forkastet true
            beregnetPeriode(1) er Utbetalingstatus.Sendt avType Utbetalingtype.ANNULLERING fra (1.januar til 31.januar) medAntallDager 31 forkastet true
        }

        1.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet true
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet true
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
            beregnetPeriode(0) er Utbetalingstatus.Sendt avType Utbetalingtype.ANNULLERING fra (1.februar til 28.februar) medAntallDager 28 forkastet true
            beregnetPeriode(1) er Utbetalingstatus.Sendt avType Utbetalingtype.ANNULLERING fra (1.januar til 31.januar) medAntallDager 31 forkastet true
        }

        1.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet true
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet true
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
            beregnetPeriode(0) er Utbetalingstatus.Sendt avType Utbetalingtype.ANNULLERING fra (1.mars til 31.mars) medAntallDager 31 forkastet true
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.mars til 31.mars) medAntallDager 31 forkastet true
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
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
        håndterUtbetalt()

        forlengVedtak(1.mars, 31.mars)

        assertEquals(2, generasjoner.size)
        assertEquals(3, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.mars til 31.mars) medAntallDager 31 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(2) er Utbetalingstatus.Utbetalt avType Utbetalingtype.REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
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
        håndterUtbetalt()

        forlengVedtak(1.mars, 31.mars)
        håndterOverstyrTidslinje((1.mars til 31.mars).map { manuellFeriedag(it) })
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        assertEquals(3, generasjoner.size)
        assertEquals(3, generasjoner[0].perioder.size)
        assertEquals(3, generasjoner[1].perioder.size)
        assertEquals(2, generasjoner[2].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.REVURDERING fra (1.mars til 31.mars) medAntallDager 31 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(2) er Utbetalingstatus.Utbetalt avType Utbetalingtype.REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.mars til 31.mars) medAntallDager 31 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.REVURDERING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(2) er Utbetalingstatus.Utbetalt avType Utbetalingtype.REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        2.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.februar til 28.februar) medAntallDager 28 forkastet false
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `korte perioder - arbeidsgiversøknader`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 15.januar, 100.prosent))

        0.generasjon {
            uberegnetPeriode(0) fra (1.januar til 15.januar) medAntallDager 15 forkastet false
        }
    }

    @Test
    fun `kort periode med forlengelse`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 15.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(16.januar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 15.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        assertEquals(1, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Ubetalt avType Utbetalingtype.UTBETALING fra (16.januar til 15.februar) medAntallDager 31 forkastet false
            uberegnetPeriode(1) fra (1.januar til 15.januar) medAntallDager 15 forkastet false
        }
    }

    @Test
    fun `kort periode med forlengelse og revurdering av siste periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 15.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 15.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(16.januar, 15.februar, 100.prosent))
        håndterSøknad(Sykdom(16.januar, 15.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterYtelser(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrTidslinje((13.februar til 14.februar).map { manuellFeriedag(it) })
        håndterYtelser(2.vedtaksperiode)

        assertEquals(2, generasjoner.size)
        assertEquals(2, generasjoner[0].perioder.size)
        assertEquals(2, generasjoner[1].perioder.size)

        0.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Ubetalt avType Utbetalingtype.REVURDERING fra (16.januar til 15.februar) medAntallDager 31 forkastet false
            uberegnetPeriode(1) fra (1.januar til 15.januar) medAntallDager 15 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (16.januar til 15.februar) medAntallDager 31 forkastet false
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
            beregnetPeriode(1) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
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
            beregnetPeriode(1) er Utbetalingstatus.Ubetalt avType Utbetalingtype.REVURDERING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }

        1.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.Utbetalt avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `periode uten utbetaling - kun ferie`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(17.januar, 31.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertEquals(1, generasjoner.size)
        assertEquals(1, generasjoner[0].perioder.size)
        0.generasjon {
            uberegnetPeriode(0) fra (1.januar til 31.januar) medAntallDager 31 forkastet false
        }
    }

    @Test
    fun `får riktig aldersvilkår per periode`() {
        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        0.generasjon {
            beregnetPeriode(0).assertAldersvilkår(true, 26)
            beregnetPeriode(1).assertAldersvilkår(true, 25)
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
            beregnetPeriode(0).assertAldersvilkår(true, 26)
            beregnetPeriode(1).assertAldersvilkår(true, 25)
            beregnetPeriode(0).assertSykepengedagerVilkår(29, 219, 1.januar(2019), 1.januar, true)
            beregnetPeriode(1).assertSykepengedagerVilkår(11, 237, 28.desember, 1.januar, true)
            beregnetPeriode(0).assertSøknadsfristVilkår(1.februar, 28.februar, 28.februar.atStartOfDay(), true)
            beregnetPeriode(1).assertSøknadsfristVilkår(1.januar, 31.januar, 31.januar.atStartOfDay(), true)
        }
        1.generasjon {
            beregnetPeriode(0).assertSøknadsfristVilkår(1.februar, 28.februar, 28.februar.atStartOfDay(), true)
            beregnetPeriode(1).assertSøknadsfristVilkår(1.januar, 31.januar, 31.januar.atStartOfDay(), true)
            beregnetPeriode(0).assertSykepengedagerVilkår(31, 217, 28.desember, 1.januar, true)
            beregnetPeriode(1).assertSykepengedagerVilkår(11, 237, 28.desember, 1.januar, true)
            beregnetPeriode(0).assertSøknadsfristVilkår(1.februar, 28.februar, 28.februar.atStartOfDay(), true)
            beregnetPeriode(1).assertSøknadsfristVilkår(1.januar, 31.januar, 31.januar.atStartOfDay(), true)
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
            beregnetPeriode(0).assertAldersvilkår(true, 26)
            beregnetPeriode(1).assertAldersvilkår(true, 25)
            // Revurdering av tidligere periode medfører at alle perioder berørt av revurderingen deler den samme utbetalingen, og derfor ender opp med samme
            // gjenstående dager, forbrukte dager og maksdato. Kan muligens skrives om i modellen slik at disse tallene kan fiskes ut fra utbetalingen gitt en
            // periode
            beregnetPeriode(0).assertSykepengedagerVilkår(29, 219, 1.januar(2019), 1.januar, true)
            beregnetPeriode(1).assertSykepengedagerVilkår(29, 219, 1.januar(2019), 1.januar, true)
            beregnetPeriode(0).assertSøknadsfristVilkår(1.februar, 28.februar, 28.februar.atStartOfDay(), true)
            beregnetPeriode(1).assertSøknadsfristVilkår(1.januar, 31.januar, 31.januar.atStartOfDay(), true)
        }
        1.generasjon {
            beregnetPeriode(0).assertSøknadsfristVilkår(1.februar, 28.februar, 28.februar.atStartOfDay(), true)
            beregnetPeriode(1).assertSøknadsfristVilkår(1.januar, 31.januar, 31.januar.atStartOfDay(), true)
            beregnetPeriode(0).assertSykepengedagerVilkår(31, 217, 28.desember, 1.januar, true)
            beregnetPeriode(1).assertSykepengedagerVilkår(11, 237, 28.desember, 1.januar, true)
            beregnetPeriode(0).assertSøknadsfristVilkår(1.februar, 28.februar, 28.februar.atStartOfDay(), true)
            beregnetPeriode(1).assertSøknadsfristVilkår(1.januar, 31.januar, 31.januar.atStartOfDay(), true)
        }
    }

    @Test
    fun `ta med personoppdrag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(
            refusjon = Inntektsmelding.Refusjon(0.månedlig, null),
            førsteFraværsdag = 1.januar,
            arbeidsgiverperioder = listOf(1.januar til 16.januar)
        )
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

    @Test
    fun `kun førstegangsbehandling har warnings fra vilkårsprøving`() {
        val fom = 1.januar
        val tom = 31.januar
        val forlengelseFom = 1.februar
        val forlengelseTom = 28.februar

        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent))
        håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = fom.plusDays(1))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(fom til fom.plusDays(15)),
            refusjon = Inntektsmelding.Refusjon(
                beløp = 1000.månedlig,
                opphørsdato = null,
                endringerIRefusjon = emptyList()
            ),
            beregnetInntekt = 1000.månedlig
        )
        håndterYtelser(1.vedtaksperiode)
        håndterVilkårsgrunnlag(
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt 1000.månedlig
                }
            })
        )
        håndterYtelser(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, automatiskBehandling = false)

        håndterSykmelding(Sykmeldingsperiode(forlengelseFom, forlengelseTom, 100.prosent))
        håndterSøknad(
            Sykdom(forlengelseFom, forlengelseTom, 100.prosent),
            sendtTilNAVEllerArbeidsgiver = forlengelseTom
        )
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, automatiskBehandling = false)

        assertEquals(1, generasjoner.size)

        0.generasjon {
            beregnetPeriode(0) er Utbetalingstatus.GodkjentUtenUtbetaling avType Utbetalingtype.UTBETALING fra (1.februar til 28.februar) utenWarning "Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag"
            beregnetPeriode(1) er Utbetalingstatus.GodkjentUtenUtbetaling avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medWarning "Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag"
        }
    }

    @Test
    fun `kun første arbeidsgiver har warnings fra vilkårsprøving`() {
        val fom = 1.januar
        val tom = 31.januar

        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(fom, tom, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(fom, tom, 100.prosent), sendtTilNAVEllerArbeidsgiver = fom.plusDays(1), orgnummer = a1)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(fom til fom.plusDays(15)),
            refusjon = Inntektsmelding.Refusjon(
                beløp = 1000.månedlig,
                opphørsdato = null,
                endringerIRefusjon = emptyList()
            ),
            beregnetInntekt = 1000.månedlig,
            orgnummer = a1
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(fom til fom.plusDays(15)),
            refusjon = Inntektsmelding.Refusjon(
                beløp = 1000.månedlig,
                opphørsdato = null,
                endringerIRefusjon = emptyList()
            ),
            beregnetInntekt = 1000.månedlig,
            orgnummer = a2
        )

        håndterSøknad(Sykdom(fom, tom, 100.prosent), orgnummer = a2)

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    a1 inntekt 1000.månedlig
                    a2 inntekt 1000.månedlig
                }
            }),
            orgnummer = a1
        )

        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, automatiskBehandling = false, orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, automatiskBehandling = false, orgnummer = a2)

        assertEquals(1, generasjoner(a1).size)
        assertEquals(1, generasjoner(a2).size)

        0.generasjon(a1) {
            beregnetPeriode(0) er Utbetalingstatus.GodkjentUtenUtbetaling avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) medWarning "Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag"
        }
        0.generasjon(a2) {
            beregnetPeriode(0) er Utbetalingstatus.GodkjentUtenUtbetaling avType Utbetalingtype.UTBETALING fra (1.januar til 31.januar) utenWarning "Perioden er avslått på grunn av at inntekt er under krav til minste sykepengegrunnlag"
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

    private fun Int.generasjon(organisasjonsnummer: String = ORGNUMMER, assertBlock: Generasjon.() -> Unit) {
        require(this >= 0) { "Kan ikke være et negativt tall!" }
        generasjoner(organisasjonsnummer)[this].run(assertBlock)
    }

    private infix fun <T : Tidslinjeperiode> T.medAntallDager(antall: Int): T {
        assertEquals(antall, sammenslåttTidslinje.size)
        return this
    }

    private infix fun <T : Tidslinjeperiode> T.forkastet(forkastet: Boolean): T {
        assertEquals(forkastet, this.erForkastet)
        return this
    }

    private infix fun BeregnetPeriode.er(utbetalingstilstand: Utbetalingstatus): BeregnetPeriode {
        assertEquals(utbetalingstilstand, this.utbetaling.status)
        return this
    }

    private infix fun BeregnetPeriode.avType(type: Utbetalingtype): BeregnetPeriode {
        assertEquals(type, this.utbetaling.type)
        return this
    }

    private infix fun <T : Tidslinjeperiode> T.fra(periode: Periode): T {
        assertEquals(periode.start, this.fom)
        assertEquals(periode.endInclusive, this.tom)
        return this
    }

    private infix fun BeregnetPeriode.medWarning(warning: String): BeregnetPeriode {
        assertTrue(this.aktivitetslogg.filter { it.alvorlighetsgrad == "W" }.any { it.melding == warning })
        return this
    }

    private infix fun BeregnetPeriode.utenWarning(warning: String): BeregnetPeriode {
        assertFalse(this.aktivitetslogg.filter { it.alvorlighetsgrad == "W" }.any { it.melding == warning })
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
