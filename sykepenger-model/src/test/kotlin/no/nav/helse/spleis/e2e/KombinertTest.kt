package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.selvstendig
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import org.junit.jupiter.api.Test

internal class KombinertTest : AbstractDslTest() {

    @Test
    fun `en selvstendigsøknad er en forlengelse av en arbeidstakersøknad`() {
        a1 {
            nyttVedtak(januar)
        }
        selvstendig {
            håndterFørstegangssøknadSelvstendig(februar)
            assertFunksjonellFeil(Varselkode.`RV_SØ_53`, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `en selvstendigsøknad er en forlengelse av en arbeidstakersøknad og infaller på en måndag`() {
        a1 {
            håndterSøknad(1.januar til 5.januar)
        }
        selvstendig {
            håndterFørstegangssøknadSelvstendig(8.januar til 31.januar)
            assertFunksjonellFeil(Varselkode.`RV_SØ_53`, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `en arbeidstakersøknad er en forlengelse av en selvstendigsøknad`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(januar)
        }
        a1 {
            håndterSøknad(1.februar til 28.februar)
            assertFunksjonellFeil(Varselkode.`RV_SØ_53`, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_SØ_54, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `en arbeidstakersøknad er en forlengelse av en selvstendigsøknad og infaller på en måndag`() {
        selvstendig {
            håndterFørstegangssøknadSelvstendig(1.januar til 5.januar)
        }
        a1 {
            håndterSøknad(8.januar til 31.januar)
            assertFunksjonellFeil(Varselkode.`RV_SØ_53`, 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_SØ_54, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `selvstendig løper videre til vilkårsprøving selv om vi har en arbeidstaker-periode som ikke har refusjonsopplysninger`() {

        selvstendig {
            håndterFørstegangssøknadSelvstendig(14.januar til 31.januar)
        }

        a1 {
            håndterSøknad(1.januar til 14.januar)
            håndterSøknad(15.januar til 20.januar)
            håndterSøknad(25.januar til 31.januar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(3.vedtaksperiode, TIL_INFOTRYGD)
        }

        selvstendig {
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        }
    }
}
