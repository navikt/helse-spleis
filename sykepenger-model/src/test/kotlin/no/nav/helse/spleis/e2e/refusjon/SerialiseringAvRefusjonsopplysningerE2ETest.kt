package no.nav.helse.spleis.e2e.refusjon

import java.time.LocalDate
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.serde.serialize
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SerialiseringAvRefusjonsopplysningerE2ETest : AbstractEndToEndTest() {

    @Test
    fun `lagrer refusjonsopplysninger i vilkårsgrunnlag`() {
        nyttVedtak(1.januar, 31.januar, 100.prosent)
        inspektør.refusjonsopplysningerFraVilkårsgrunnlag(1.januar).assertRefusjonsbeløp(1.januar til 31.januar, INNTEKT)
        val json = person.serialize().json
        person = createTestPerson { jurist -> SerialisertPerson(json).deserialize(jurist, emptyList()) }
        inspektør.refusjonsopplysningerFraVilkårsgrunnlag(1.januar).assertRefusjonsbeløp(1.januar til 31.januar, INNTEKT)
    }

    @Test
    fun `lagrer refusjonsopplysninger i vilkårsgrunnlag for to arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, a1, a2)
        håndterInntektsmelding(orgnummer = a2, arbeidsgiverperioder = listOf(1.januar til 16.januar), refusjon = Inntektsmelding.Refusjon(15000.månedlig, opphørsdato = null))
        inspektør(a1).refusjonsopplysningerFraVilkårsgrunnlag(1.januar).assertRefusjonsbeløp(1.januar til 31.januar, 20000.månedlig)
        inspektør(a2).refusjonsopplysningerFraVilkårsgrunnlag(1.januar).assertRefusjonsbeløp(1.januar til 31.januar, 15000.månedlig)
        val json = person.serialize().json
        person = createTestPerson { jurist -> SerialisertPerson(json).deserialize(jurist, emptyList()) }
        inspektør(a1).refusjonsopplysningerFraVilkårsgrunnlag(1.januar).assertRefusjonsbeløp(1.januar til 31.januar, 20000.månedlig)
        inspektør(a2).refusjonsopplysningerFraVilkårsgrunnlag(1.januar).assertRefusjonsbeløp(1.januar til 31.januar, 15000.månedlig)
    }

    private fun Refusjonsopplysninger.assertRefusjonsbeløp(periode: Periode, beløp: Inntekt) {
        periode.forEach { dag ->
            assertEquals(beløp, refusjonsbeløp(skjæringstidspunkt = LocalDate.MAX, dag = dag, manglerRefusjonsopplysning = {_,_->}))
        }
    }
}