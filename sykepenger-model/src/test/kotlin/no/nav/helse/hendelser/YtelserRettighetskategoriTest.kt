package no.nav.helse.hendelser

import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidstaker
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Selvstendig
import no.nav.helse.hendelser.Rettighetskategori.Hovedkategori
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RK_2
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import org.junit.jupiter.api.Test
import java.util.UUID

internal class YtelserRettighetskategoriTest {
    private val arbeidstaker = Arbeidstaker("a1")

    @Test
    fun `arbeidstaker stemmer med hovedkategorien arbeidstaker`() {
        valider(Hovedkategori.ARBEIDSTAKER, arbeidstaker).assertIngenFunksjonelleFeil()
    }

    @Test
    fun `selvstendig stemmer med hovedkategorien selvstendig næringsdrivende`() {
        valider(Hovedkategori.SELVSTENDIG_NÆRINGSDRIVENDE, Selvstendig).assertIngenFunksjonelleFeil()
    }

    @Test
    fun `selvstendig stemmer ikke med hovedkategorien arbeidstaker`() {
        valider(Hovedkategori.ARBEIDSTAKER, Selvstendig).assertFunksjonellFeil(RV_RK_2)
    }

    @Test
    fun `arbeidstaker stemmer ikke med hovedkategorien selvstendig næringsdrivende`() {
        valider(Hovedkategori.SELVSTENDIG_NÆRINGSDRIVENDE, arbeidstaker).assertFunksjonellFeil(RV_RK_2)
    }

    @Test
    fun `ingen yrkesaktivitetstype stemmer med hovedkategorien arbeidstaker og selvstendig næringsdrivende`() {
        valider(Hovedkategori.ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE, arbeidstaker).assertFunksjonellFeil(RV_RK_2)
        valider(Hovedkategori.ARBEIDSTAKER_OG_SELVSTENDIG_NÆRINGSDRIVENDE, Selvstendig).assertFunksjonellFeil(RV_RK_2)
    }

    @Test
    fun `uten hovedkategori hoppes sjekken over`() {
        valider(null, Selvstendig).assertIngenFunksjonelleFeil()
    }

    private fun valider(
        hovedkategori: Hovedkategori?,
        yrkesaktivitetstype: Behandlingsporing.Yrkesaktivitet,
    ): Aktivitetslogg {
        val ytelser =
            ArbeidsgiverHendelsefabrikk("a1", arbeidstaker)
                .lagYtelser(UUID.randomUUID(), opptjeningsvurderingResultatOk = true, rettighetskategoriResultat = hovedkategori)
        return Aktivitetslogg().also { ytelser.validerRettighetskategori(it, yrkesaktivitetstype) }
    }
}
