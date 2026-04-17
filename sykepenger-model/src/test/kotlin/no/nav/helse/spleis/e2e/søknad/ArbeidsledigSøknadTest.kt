package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Arbeidsledigsøknad er lagt til grunn`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Støtter ikke førstegangsbehandlinger for arbeidsledigsøknader`
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class ArbeidsledigSøknadTest : AbstractDslTest() {

    @Test
    fun `støtter arbeidsledigsøknad som forlengelse av tidligere vilkårsprøvd skjæringstidspunkt`() {
        a1 {
            nyttVedtak(januar)
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), arbeidssituasjon = Søknad.Arbeidssituasjon.ARBEIDSLEDIG)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
            assertVarsel(`Arbeidsledigsøknad er lagt til grunn`, 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `trenger ikke varsel ved forlengelse hvis det ikke er refusjon`() {
        a1 {
            nyttVedtak(januar, refusjon = Inntektsmelding.Refusjon(Inntekt.INGEN, null, emptyList()))
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), arbeidssituasjon = Søknad.Arbeidssituasjon.ARBEIDSLEDIG)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
            assertVarsler(emptyList(), 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `støtter ikke arbeidsledigsøknad som førstegangssøknad`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), arbeidssituasjon = Søknad.Arbeidssituasjon.ARBEIDSLEDIG)
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertFunksjonellFeil(`Støtter ikke førstegangsbehandlinger for arbeidsledigsøknader`, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `støtter arbeidsledigsøknad som forlengelse av tidligere vilkårsprøvd skjæringstidspunkt med fler arbeidsgivere i sykepengegrunnlaget`() {
        (a1 og a2).nyeVedtak(januar)
        a1 {
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), arbeidssituasjon = Søknad.Arbeidssituasjon.ARBEIDSLEDIG)
            assertVarsel(Varselkode.RV_SØ_43, 2.vedtaksperiode.filter())
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
        }
    }
}
