package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Test

internal class KorrigerendeInntektsmeldingTest: AbstractEndToEndTest() {

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved helt lik korrigerende inntektsmelding`() {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar))

        assertIngenVarsel(RV_IM_4)
    }

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved korrigerende inntektsmelding med endring i agp`() {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(2.januar til 17.januar))

        assertVarsel(RV_IM_4)
    }

    @Test
    fun `Avsluttet vedtaksperiode skal ikke få varsel ved korrigerende inntektsmelding med endring i refusjonsbeløp`() {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(2000.månedlig, null, emptyList()))

        assertVarsel(RV_IM_4)
    }

    @Test
    fun `Korrigerende arbeidsgiverperiode i Inntektsmelding som treffer Avsluttet periode uten nye inntekts- eller refusjonsopplysninger`() {
        nyttVedtak(10.januar, 31.januar)

        håndterInntektsmelding(listOf(1.januar til 5.januar, 10.januar til 20.januar))
        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

    }

    @Test
    fun `Korrigerende arbedisgiverperiode i Inntektsmelding som treffer Avsluttet med `() {
        nyttVedtak(1.januar, 31.januar)

        håndterInntektsmelding(listOf(2.januar til 17.januar))
        nullstillTilstandsendringer()

        assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)

    }

    /*

    1. kun er korrigert inntektsopplysninger. samme datoer, men oppjuster inntekt eller refusjon: STØTTER
    1.5 endret start dato for refusjonsopplysninger, men med samme beløp - både agp og inntektsopplysninger endres. ønsker mulighet for pølsestrekk. noe med startskuddet: støtter delvis, ønsker å sende til manuell behandling.
    2. Ikke noen endrede inntektsopplysninger, men en endret agp. Der vil vi starte en agp revurdering -> ønsker mulighet for pølsestrekk: lager gosys oppgave på im - ønsker oppgave i Speil isteden
    3. IM uten endring
     */

}