package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.hendelser.DagerFraInntektsmelding.BegrunnelseForReduksjonEllerIkkeUtbetalt.Companion.FunksjonellBetydningAvBegrunnelseForReduksjonEllerIkkeUtbetalt
import no.nav.helse.hendelser.DagerFraInntektsmelding.BegrunnelseForReduksjonEllerIkkeUtbetalt.Companion.FunksjonellBetydningAvBegrunnelseForReduksjonEllerIkkeUtbetalt.ARBBEIDSGIVER_VIL_AT_NAV_SKAL_DEKKE_AGP_FRA_FØRSTE_DAG
import no.nav.helse.hendelser.DagerFraInntektsmelding.BegrunnelseForReduksjonEllerIkkeUtbetalt.Companion.FunksjonellBetydningAvBegrunnelseForReduksjonEllerIkkeUtbetalt.ARBEIDSGIVER_SIER_AT_DET_IKKE_ER_NOE_AGP_Å_SNAKKE_OM_I_DET_HELE_TATT
import no.nav.helse.hendelser.DagerFraInntektsmelding.BegrunnelseForReduksjonEllerIkkeUtbetalt.Companion.FunksjonellBetydningAvBegrunnelseForReduksjonEllerIkkeUtbetalt.ARBEIDSGIVER_VIL_BARE_DEKKE_DELVIS_AGP
import no.nav.helse.hendelser.DagerFraInntektsmelding.BegrunnelseForReduksjonEllerIkkeUtbetalt.Companion.FunksjonellBetydningAvBegrunnelseForReduksjonEllerIkkeUtbetalt.ARBEIDSGIVER_VIL_IKKE_DEKKE_NY_AGP_TROSS_GAP
import no.nav.helse.hendelser.DagerFraInntektsmelding.BegrunnelseForReduksjonEllerIkkeUtbetalt.Companion.funksjonellBetydningAvBegrunnelseForReduksjonEllerIkkeUtbetalt
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.januar
import no.nav.helse.mars
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FunksjonellBetydningAvBegrunnelseForReduksjonEllerIkkeUtbetaltTest {

    // Nyttig? = https://github.com/navikt/spinntektsmelding-frontend/commit/a99eaab06bbef83280715528f109118bc9511da5

    @Test
    fun `arbeidsgiver vil at nav skal dekke ny agp fra første dag hvis det er et veldig nytt arbeidsforhold uten fire ukers opptjeningstid`() {
        val førsteFraværsdag = 1.januar
        val arbeidsgiverperiode = listOf(1.januar til 16.januar)
        val begrunnelse = "ManglerOpptjening"
        val funksjonellBetydning = funksjonellBetydning(førsteFraværsdag, arbeidsgiverperiode, begrunnelse)
        assertEquals(ARBBEIDSGIVER_VIL_AT_NAV_SKAL_DEKKE_AGP_FRA_FØRSTE_DAG, funksjonellBetydning)
    }

    @Test
    fun `arbeidsgiver vil at nav skal dekke ny agp fra første dag hvis agp er gjennomført hos tidligere virksomhet`() {
        val førsteFraværsdag = 1.januar
        val arbeidsgiverperiode = listOf(1.januar til 16.januar)
        val begrunnelse = "TidligereVirksomhet"
        val funksjonellBetydning = funksjonellBetydning(førsteFraværsdag, arbeidsgiverperiode, begrunnelse)
        assertEquals(ARBBEIDSGIVER_VIL_AT_NAV_SKAL_DEKKE_AGP_FRA_FØRSTE_DAG, funksjonellBetydning)
    }

    @Test
    fun `arbeidsgiver sier det ikke er noe arbeidsgiverperiode å snakke om fordi sykmeldte ikke har hatt fravær`() {
        val førsteFraværsdag = 1.januar
        val arbeidsgiverperiode = emptyList<Periode>()
        val begrunnelse = "IkkeFravaer"
        val funksjonellBetydning = funksjonellBetydning(førsteFraværsdag, arbeidsgiverperiode, begrunnelse)
        assertEquals(ARBEIDSGIVER_SIER_AT_DET_IKKE_ER_NOE_AGP_Å_SNAKKE_OM_I_DET_HELE_TATT, funksjonellBetydning)
    }

    @Test
    fun `arbeidsgiver vil at nav skal dekke resten av agp hvis bruker har opphørt arbeidet midt i agp`() {
        val førsteFraværsdag = 1.januar
        val arbeidsgiverperiode = listOf(1.januar til 8.januar)
        val begrunnelse = "ArbeidOpphørt"
        val funksjonellBetydning = funksjonellBetydning(førsteFraværsdag, arbeidsgiverperiode, begrunnelse)
        assertEquals(ARBEIDSGIVER_VIL_BARE_DEKKE_DELVIS_AGP, funksjonellBetydning)
    }

    @Test
    fun `arbeidsgiver vil at nav skal dekke resten av agp hvis bruker er permitert midt i agp`() {
        val førsteFraværsdag = 1.januar
        val arbeidsgiverperiode = listOf(1.januar til 8.januar)
        val begrunnelse = "Permittering"
        val funksjonellBetydning = funksjonellBetydning(førsteFraværsdag, arbeidsgiverperiode, begrunnelse)
        assertEquals(ARBEIDSGIVER_VIL_BARE_DEKKE_DELVIS_AGP, funksjonellBetydning)
    }

    @Test
    fun `arbeidsgiver vil ikke dekke ny agp fordi arbeidet ikke har blitt gjenopptatt`() {
        val førsteFraværsdag = 1.mars
        val arbeidsgiverperiode = listOf(1.januar til 16.januar)
        val begrunnelse = "FerieEllerAvspasering"
        val funksjonellBetydning = funksjonellBetydning(førsteFraværsdag, arbeidsgiverperiode, begrunnelse)
        assertEquals(ARBEIDSGIVER_VIL_IKKE_DEKKE_NY_AGP_TROSS_GAP, funksjonellBetydning)
    }

    @Test
    fun `arbeidsgiver vil ikke dekke ny agp fordi arbeidet ikke har blitt _tilstrekkelig_ gjenopptatt`() {
        val førsteFraværsdag = 1.mars
        val arbeidsgiverperiode = listOf(1.januar til 16.januar)
        val begrunnelse = "IkkeFullStillingsandel"
        val funksjonellBetydning = funksjonellBetydning(førsteFraværsdag, arbeidsgiverperiode, begrunnelse)
        assertEquals(ARBEIDSGIVER_VIL_IKKE_DEKKE_NY_AGP_TROSS_GAP, funksjonellBetydning)
    }

    private fun funksjonellBetydning(
        førsteFraværsdag: LocalDate,
        arbeidsgiverperioder: List<Periode>,
        begrunnelse: String
    ): FunksjonellBetydningAvBegrunnelseForReduksjonEllerIkkeUtbetalt {
        val antallDagerIOpplystArbeidsgiverperiode = arbeidsgiverperioder.periode()?.count() ?: 0
        val periodeMellom = arbeidsgiverperioder.periode()?.periodeMellom(førsteFraværsdag)
        val førsteFraværsdagStarterMerEnn16DagerEtterEtterSisteDagIAGP = periodeMellom != null && periodeMellom.count() > 16
        return funksjonellBetydningAvBegrunnelseForReduksjonEllerIkkeUtbetalt(
            antallDagerIOpplystArbeidsgiverperiode,
            førsteFraværsdagStarterMerEnn16DagerEtterEtterSisteDagIAGP,
            begrunnelse
        )
    }
}