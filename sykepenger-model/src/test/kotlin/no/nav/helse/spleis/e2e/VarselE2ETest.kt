package no.nav.helse.spleis.e2e

import no.nav.helse.desember
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.dsl.tilGodkjenning
import no.nav.helse.februar
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_9
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_14
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_MV_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_5
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class VarselE2ETest : AbstractDslTest() {

    @Test
    fun `varsel - Sykmeldingen er tilbakedatert, vurder fra og med dato for utbetaling`() {
        a1 {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
                merknaderFraSykmelding = listOf(Søknad.Merknad("UGYLDIG_TILBAKEDATERING"))
            )
            assertVarsel(RV_SØ_3, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `varsel - Søknaden inneholder Permisjonsdager utenfor sykdomsvindu`() {
        a1 {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
                Søknad.Søknadsperiode.Permisjon(1.desember(2017), 31.desember(2017)),
            )
            assertVarsel(RV_SØ_5, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `varsel - Perioden er avslått på grunn av at den sykmeldte ikke er medlem av Folketrygden`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Nei)

            assertVarsel(RV_MV_2, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `varsel - Endrer tidligere oppdrag, Kontroller simuleringen`() {
        a1 {
            nyttVedtak(3.januar til 26.januar)
            nullstillTilstandsendringer()

            håndterOverstyrTidslinje((20.januar til 22.januar).map { manuellFeriedag(it) })
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()

            håndterOverstyrTidslinje((23.januar til 23.januar).map { manuellFeriedag(it) })
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `varsel - Det er institusjonsopphold i perioden - Vurder retten til sykepenger`() {
        a1 {
            nyttVedtak(januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
            håndterYtelser(1.vedtaksperiode, institusjonsoppholdsperioder = listOf(Institusjonsopphold.Institusjonsoppholdsperiode(1.januar, 31.januar)))

            assertVarsler(listOf(RV_AY_9, Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `varsel - Utbetalingen ble gjennomført, men med advarsel`() {
        a1 {
            tilGodkjenning(januar)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt(status = Oppdragstatus.AKSEPTERT_MED_FEIL)
            assertInfo("Utbetalingen ble gjennomført, men med advarsel", AktivitetsloggFilter.arbeidsgiver(a1))
        }
    }

    @Test
    fun `varsel revurdering - Støtter ikke overgang fra infotrygd for flere arbeidsgivere`() {
        a1 {
            nyttVedtak(januar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.januar, Dagtype.Feriedag)))
            håndterUtbetalingshistorikkEtterInfotrygdendring(
            )
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(Varselkode.RV_UT_23), 1.vedtaksperiode.filter())
            assertIngenFunksjonelleFeil(1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `varsel revurdering - Forlenger en Infotrygdperiode på tvers av arbeidsgivere`() {
        a1 {
            nyttVedtak(januar)
            forlengVedtak(februar)
            håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(17.februar, Dagtype.Feriedag)))
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 31.januar))
            )
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(RV_IT_3, 1.vedtaksperiode.filter())
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            assertVarsel(RV_IT_14, 2.vedtaksperiode.filter())
            assertIngenFunksjonelleFeil(2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `varsel - skjæringstidspunkt endres som følge av historikk fra IT`() {
        a1 {
            håndterSøknad(10.januar til 25.januar)
            håndterSøknad(26.januar til 31.januar)
            håndterInntektsmelding(listOf(10.januar til 25.januar))
            håndterVilkårsgrunnlag(2.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                utbetalinger = listOf(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 9.januar))
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsler(listOf(RV_IT_14), 1.vedtaksperiode.filter())
            assertVarsler(emptyList(), 2.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING)
            assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `varsel - skjæringstidspunkt endres som følge av historikk fra IT - forlengelse`() {
        a1 {
            nyttVedtak(10.februar til 28.februar)
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(mars)
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                listOf(ArbeidsgiverUtbetalingsperiode(a1, 5.februar, 11.februar))
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(RV_IT_3, 1.vedtaksperiode.filter())
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }
}
