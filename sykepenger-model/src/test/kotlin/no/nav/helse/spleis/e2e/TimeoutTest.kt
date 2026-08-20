package no.nav.helse.spleis.e2e

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SY_4
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TimeoutTest : AbstractDslTest() {

    @Test
    fun `avventer inntektsmelding går først videre etter 90 dager`() {
        a1 {
            håndterSøknad(januar)
            nullstillTilstandsendringer()
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt = LocalDateTime.now().minusDays(89))
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, tilstandsendringstidspunkt = LocalDateTime.now().minusDays(90))
            assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `bruker flagg i påminnelse til avventer inntektsmelding for å tvinge gjennom timeout oppførsel`() {
        a1 {
            håndterSøknad(januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, flagg = setOf("ønskerInntektFraAOrdningen"))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `avventer søknad for overlappende periode går først videre etter 3 måneder`() {
        a1 { håndterSykmelding(januar) }
        a2 { håndterSykmelding(januar) }
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            nullstillTilstandsendringer()
            assertTilstander(1.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE, tilstandsendringstidspunkt = LocalDateTime.now().minusMonths(2))
            assertTilstander(1.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE, tilstandsendringstidspunkt = LocalDateTime.now().minusMonths(3))
            assertTilstander(1.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
            assertVarsel(RV_SY_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `bruker flagg i påminnelse til avventer søknad for overlappende periode for å tvinge gjennom timeout oppførsel`() {
        a1 { håndterSykmelding(januar) }
        a2 { håndterSykmelding(januar) }
        a1 {
            håndterSøknad(januar)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE, flagg = setOf("forkastOverlappendeSykmeldingsperioderAndreArbeidsgivere"))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsel(RV_SY_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `To perioder med gap, den siste venter på at den første skal bli ferdig - dersom den første når timeout skal behandlingen startes`() {
        a1 {
            håndterSykmelding(januar)
            håndterSykmelding(Sykmeldingsperiode(1.mai, 31.mai))

            håndterSøknad(januar)
            håndterSøknad(mai)

            håndterArbeidsgiveropplysninger(
                listOf(1.mai til 16.mai),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            håndterPåminnelse(
                1.vedtaksperiode,
                tilstand = AVVENTER_INNTEKTSMELDING,
                tilstandsendringstidspunkt = 5.februar.atStartOfDay()
            )

            assertTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `Første periode får søknad, men ikke inntektsmelding og må nå timeout før de neste kan fortsette behandling`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.november(2017), 30.november(2017)))
            håndterSykmelding(Sykmeldingsperiode(3.januar, 3.januar))
            håndterSykmelding(Sykmeldingsperiode(5.januar, 22.januar))
            håndterSøknad(Sykdom(1.november(2017), 30.november(2017), 100.prosent))
            håndterSøknad(Sykdom(3.januar, 3.januar, 100.prosent))
            håndterSøknad(Sykdom(5.januar, 22.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(3.januar til 3.januar, 5.januar til 19.januar), vedtaksperiodeId = 3.vedtaksperiode)

            assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, LocalDateTime.now().minusDays(91))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `periode i avventer blokkerende som venter på inntektsmelding fra annen arbeidsgiver bør ha samme timeout som avventer inntektsmelding`() {
        a1 {
            håndterSøknad(januar)
        }
        a2 {
            håndterSøknad(januar)
        }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertTimeoutTidspunkt(1.vedtaksperiode) { venterTil, gikkInnITilstand ->
                assertEquals(gikkInnITilstand.plusDays(90), venterTil)
            }
        }
        val a2VenterTil = a2 { venterTil(1.vedtaksperiode) }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
            assertTimeoutTidspunkt(1.vedtaksperiode) { venterTil, _ ->
                assertEquals(a2VenterTil, venterTil)
            }
        }
    }

    @Test
    fun `periode i avventer blokkerende som venter på søknad fra annen arbeidsgiver venter i 3 måneder`() {
        a1 {
            håndterSykmelding(januar)
        }
        a2 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE)
            assertTimeoutTidspunkt(1.vedtaksperiode) { venterTil, gikkInnITilstand ->
                assertEquals(gikkInnITilstand.plusMonths(3), venterTil)
            }
        }
    }

    @Test
    fun `periode i avventer blokkerende venter på annen periode til godkjenning har evig timeout`() {
        a1 {
            tilGodkjenning(januar)
            håndterSøknad(mars)
            håndterInntektsmelding(listOf(1.mars til 16.mars))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTimeoutTidspunkt(2.vedtaksperiode) { venterTil, _ ->
                assertEquals(LocalDateTime.MAX, venterTil)
            }
        }
    }

    @Test
    fun `periode i avventer blokkerende som venter på inntektsmelding fra annen arbeidsgiver tross tidligere periode til godkjenning har samme timeout som avventer inntektsmelding`() {
        a1 {
            tilGodkjenning(januar)
            håndterSøknad(mars)
        }
        a2 {
            håndterSøknad(mars)
        }
        a1 {
            håndterInntektsmelding(listOf(1.mars til 16.mars))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertTimeoutTidspunkt(1.vedtaksperiode) { venterTil, gikkInnITilstand ->
                assertEquals(gikkInnITilstand.plusDays(90), venterTil)
            }
        }

        val a2VenterTil = a2 { venterTil(1.vedtaksperiode) }
        a1 {
            assertTimeoutTidspunkt(1.vedtaksperiode) { venterTil, _ ->
                assertEquals(LocalDateTime.MAX, venterTil)
            }
            assertTimeoutTidspunkt(2.vedtaksperiode) { venterTil, _ ->
                assertEquals(a2VenterTil, venterTil)
            }
        }
    }

    @Test
    fun `perioder i avventer blokkerende som kun venter på godkjenning har evig timeout`() {
        a1 {
            tilGodkjenning(januar)
            håndterSøknad(mars)
        }
        a2 {
            håndterSøknad(mars)
        }
        a1 {
            håndterInntektsmelding(listOf(1.mars til 16.mars))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
        a2 {
            håndterInntektsmelding(listOf(1.mars til 16.mars))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTimeoutTidspunkt(1.vedtaksperiode) { venterTil, _ ->
                assertEquals(LocalDateTime.MAX, venterTil)
            }
        }
        a1 {
            assertTimeoutTidspunkt(1.vedtaksperiode) { venterTil, _ ->
                assertEquals(LocalDateTime.MAX, venterTil)
            }
            assertTimeoutTidspunkt(2.vedtaksperiode) { venterTil, _ ->
                assertEquals(LocalDateTime.MAX, venterTil)
            }
        }
    }

    private fun venterTil(vedtaksperiodeId: UUID) =
        observatør.vedtaksperiodeVenter.last { it.vedtaksperiodeId == vedtaksperiodeId }.venterTil

    private fun assertTimeoutTidspunkt(vedtaksperiodeId: UUID, assertionBlock: (venterTil: LocalDateTime, gikkInnITilstand: LocalDateTime) -> Unit) {
        val venterTil = venterTil(vedtaksperiodeId)
        val gikkInnITilstand = inspektør(vedtaksperiodeId).oppdatert
        assertionBlock(venterTil, gikkInnITilstand)
    }
}
