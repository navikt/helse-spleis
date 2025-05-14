package no.nav.helse.spleis.e2e

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.a1
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VarselVedNegativtBeløpE2ETest : AbstractDslTest() {

    @Test
    fun `skal ikke få varsel når utbetaling flyttes fra arbeidsgiver til person`() {
        a1 {
            nyttVedtak(januar)
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT, listOf(Triple(1.januar, null, Inntekt.INGEN)))))
            håndterYtelser(1.vedtaksperiode)
            assertEquals(-15741, inspektør.sisteUtbetaling().arbeidsgiverOppdrag.inspektør.nettoBeløp)
            assertEquals(15741, inspektør.sisteUtbetaling().personOppdrag.inspektør.nettoBeløp)
        }
    }

    @Test
    fun `skal få varsel når utbetaling flyttes fra person til arbeidsgiver`() {
        a1 {
            nyttVedtak(januar, refusjon = Inntektsmelding.Refusjon(Inntekt.INGEN, null, emptyList()))
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT, listOf(Triple(1.januar, null, INNTEKT)))))
            håndterYtelser(1.vedtaksperiode)
            assertEquals(15741, inspektør.sisteUtbetaling().arbeidsgiverOppdrag.inspektør.nettoBeløp)
            assertEquals(-15741, inspektør.sisteUtbetaling().personOppdrag.inspektør.nettoBeløp)
            assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `skal få varsel når utbetaling reduseres pga lavere inntekt`() {
        a1 {
            nyttVedtak(januar)

            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 0.8, emptyList())))
            håndterYtelser(1.vedtaksperiode)
            assertEquals(-3146, inspektør.sisteUtbetaling().arbeidsgiverOppdrag.inspektør.nettoBeløp)
            assertEquals(0, inspektør.sisteUtbetaling().personOppdrag.inspektør.nettoBeløp)
            assertVarsel(RV_UT_23, 1.vedtaksperiode.filter())
        }
    }
}
