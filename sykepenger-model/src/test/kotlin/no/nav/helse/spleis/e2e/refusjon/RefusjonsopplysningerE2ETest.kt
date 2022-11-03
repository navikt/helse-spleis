package no.nav.helse.spleis.e2e.refusjon

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import org.junit.jupiter.api.Test

internal class RefusjonsopplysningerE2ETest : AbstractDslTest() {

    @Test
    fun `første fraværsdag oppgitt til dagen etter arbeidsgiverperioden`(){
        a1 {
            nyttVedtak(førsteFraværsdag = 17.januar, arbeidsgiverperiode = listOf(1.januar til 16.januar), fom = 1.januar, tom = 31.januar)
            assertIngenInfoSomInneholder("Mangler refusjonsopplysninger på orgnummer")
        }
    }

    @Test
    fun `første fraværsdag oppgitt til dagen etter arbeidsgiverperioden over helg`(){
        a1 {
            nyttVedtak(førsteFraværsdag = 22.januar, arbeidsgiverperiode = listOf(4.januar til 19.januar), fom = 4.januar, tom = 31.januar)
            assertIngenInfoSomInneholder("Mangler refusjonsopplysninger på orgnummer")
        }
    }

    @Test
    fun `første fraværsdag oppgitt til dagen etter arbeidsgiverperioden over helg med en dags gap`(){
        a1 {
            nyttVedtak(førsteFraværsdag = 23.januar, arbeidsgiverperiode = listOf(4.januar til 19.januar), fom = 4.januar, tom = 31.januar)
            assertInfo("Mangler refusjonsopplysninger på orgnummer $a1 for periodene [22-01-2018 til 22-01-2018]")
        }
    }

    @Test
    fun `første fraværsdag oppgitt med en dags gap til arbeidsgiverperioden`(){
        a1 {
            nyttVedtak(førsteFraværsdag = 18.januar, arbeidsgiverperiode = listOf(1.januar til 16.januar), fom = 1.januar, tom = 31.januar)
            assertInfo("Mangler refusjonsopplysninger på orgnummer $a1 for periodene [17-01-2018 til 17-01-2018]")
        }
    }
}