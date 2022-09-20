package no.nav.helse.spleis.e2e

import no.nav.helse.desember
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.januar
import no.nav.helse.person.Varselkode.RV_SØ_1
import no.nav.helse.person.Varselkode.RV_SØ_2
import no.nav.helse.person.Varselkode.RV_SØ_3
import no.nav.helse.person.Varselkode.RV_SØ_4
import no.nav.helse.person.Varselkode.RV_SØ_5
import no.nav.helse.person.Varselkode.RV_SØ_8
import no.nav.helse.person.Varselkode.RV_SØ_9
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class VarslerE2ETest: AbstractEndToEndTest() {

    @Test
    fun `varsel - Søknaden inneholder permittering, Vurder om permittering har konsekvens for rett til sykepenger`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            permittert = true
        )
        assertVarsel(RV_SØ_1, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Minst én dag er avslått på grunn av foreldelse, Vurder å sende vedtaksbrev fra Infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), mottatt = 1.januar(2019).atStartOfDay())
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.januar(2019))
        assertVarsel(RV_SØ_2)
    }

    @Test
    fun `varsel - Sykmeldingen er tilbakedatert, vurder fra og med dato for utbetaling`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            merknaderFraSykmelding = listOf(Søknad.Merknad("UGYLDIG_TILBAKEDATERING"))
        )
        assertVarsel(RV_SØ_3, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Utdanning oppgitt i perioden i søknaden`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            Søknad.Søknadsperiode.Utdanning(20.januar, 31.januar)
        )
        assertVarsel(RV_SØ_4, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Søknaden inneholder Permisjonsdager utenfor sykdomsvindu`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            Søknad.Søknadsperiode.Permisjon(1.desember(2017), 31.desember(2017)),
        )
        assertVarsel(RV_SØ_5, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Søknad med utenlandsopphold og studieopphold gir warning`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(3.januar, 26.januar, 100.prosent),
            Søknad.Søknadsperiode.Utlandsopphold(11.januar, 15.januar)
        )
        assertVarsel(RV_SØ_8, 1.vedtaksperiode.filter())
    }

    @Test
    fun `varsel - Det er oppgitt annen inntektskilde i søknaden, Vurder inntekt`() {
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent),
            andreInntektskilder = listOf(Søknad.Inntektskilde(true, "ANNET")),
        )
        assertVarsel(RV_SØ_9, 1.vedtaksperiode.filter())
    }
}