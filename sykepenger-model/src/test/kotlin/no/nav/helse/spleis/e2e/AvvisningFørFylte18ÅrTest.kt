package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Test

internal class AvvisningFørFylte18ÅrTest : AbstractDslTest() {
    private val FYLLER_18_ÅR_2_NOVEMBER = 2.november(2000)

    @Test
    fun `avviser søknader for person under 18 år ved søknadstidspunkt`() {
        medFødselsdato(FYLLER_18_ÅR_2_NOVEMBER)
        a1 {
            håndterSøknad(Sykdom(1.oktober, 31.oktober, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.november)
            assertFunksjonellFeil(Varselkode.RV_SØ_17, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `avviser korrigerte søknader for person under 18 år ved søknadstidspunkt`() {
        medFødselsdato(FYLLER_18_ÅR_2_NOVEMBER)
        a1 {
            håndterSøknad(Sykdom(1.oktober, 31.oktober, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.november)
            håndterSøknad(Sykdom(1.oktober, 31.oktober, 80.prosent), sendtTilNAVEllerArbeidsgiver = 5.november)
            assertFunksjonellFeil(Varselkode.RV_SØ_17, 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `avviser ikke søknader for person som er 18 år ved søknadstidspunkt`() {
        medFødselsdato(FYLLER_18_ÅR_2_NOVEMBER)
        a1 {
            håndterSøknad(Sykdom(1.oktober, 31.oktober, 100.prosent), sendtTilNAVEllerArbeidsgiver = 2.november)
            assertIngenFunksjonelleFeil(1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
        }
    }
}
