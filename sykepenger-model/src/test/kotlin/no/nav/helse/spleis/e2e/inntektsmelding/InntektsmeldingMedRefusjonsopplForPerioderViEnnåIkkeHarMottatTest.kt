package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

internal class InntektsmeldingMedRefusjonsopplForPerioderViEnnåIkkeHarMottatTest : AbstractDslTest() {

    @Test
    fun `håndterer korrigerte refusjonsopplysinger frem i tid som sier at det er refusjon allikevel`() {
        a1 {
            nyttVedtak(
                periode = januar,
                refusjon = Inntektsmelding.Refusjon(
                    beløp = INNTEKT,
                    opphørsdato = 31.januar,
                )
            )
            val fremtidigeRefusjonsopplysninger = inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer.getValue(1.januar)
            assertBeløpstidslinje(fremtidigeRefusjonsopplysninger, 1.februar.somPeriode(), INGEN)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(
                    beløp = INNTEKT,
                    opphørsdato = null,
                )
            )
            val reviderteOpplysninger = inspektør.ubrukteRefusjonsopplysninger.refusjonstidslinjer[1.januar]
            assertNull(reviderteOpplysninger)
        }
    }
}
