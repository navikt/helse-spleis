package no.nav.helse.person.inntekt

import java.util.UUID
import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.EndringIRefusjon
import no.nav.helse.person.inntekt.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.refusjonsopplysninger
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RefusjonshistorikkTilRefusjonsopplysningerTest {

    @Test
    fun `tom refusjonshistorikk medfører ingen refusjonsopplysninger`() {
        val refusjonshistorikk = Refusjonshistorikk()
        assertEquals(Refusjonsopplysninger(), refusjonshistorikk.refusjonsopplysninger(1.januar))
    }

    @Test
    fun `happy case`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = emptyList()
        ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, null, 1000.daglig),
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `siste refusjonsdag satt på inntektsmeldingen`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = 31.januar,
                endringerIRefusjon = emptyList()
        ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 31.januar, 1000.daglig),
                Refusjonsopplysning(inntektsmeldingJanuar, 1.februar, null, INGEN)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `To inntektsmeldinger på ulike skjæringstidspunkt uten sisteRefusjonsdag`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = emptyList()
        ))

        val inntektsmeldingMars = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingMars,
                førsteFraværsdag = 1.mars,
                arbeidsgiverperioder = listOf(1.mars til 16.mars),
                beløp = 2000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = emptyList()
        ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 28.februar, 1000.daglig),
                Refusjonsopplysning(inntektsmeldingMars, 1.mars, null, 2000.daglig)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `IM uten første fraværsdag - refusjonsopplysning fra første dag i AGP`() {
        val refusjonshistorikk = Refusjonshistorikk()
        // mottar inntektsmelding for Januar
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = null,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = emptyList()
        ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, null, 1000.daglig)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `IM med ingen refusjon`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = null,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = null,
                sisteRefusjonsdag = null,
                endringerIRefusjon = emptyList()
        ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, null, INGEN)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `tar ikke med refusjonsopplysninger før skjæringstidspunkt`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = emptyList()
        ))

        val inntektsmeldingMars = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingMars,
                førsteFraværsdag = 1.mars,
                arbeidsgiverperioder = listOf(1.mars til 16.mars),
                beløp = 2000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = emptyList()
        ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmeldingMars, 1.mars, null, 2000.daglig)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.mars).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `IM med en endring i refusjon`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = listOf(EndringIRefusjon(500.daglig, 20.januar))
        ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 19.januar, 1000.daglig),
                Refusjonsopplysning(inntektsmeldingJanuar, 20.januar, null, 500.daglig)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar).inspektør.refusjonsopplysninger
        )
    }


    @Test
    fun `IM med flere endringer i refusjon`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = listOf(
                    EndringIRefusjon(2000.daglig, 25.januar),
                    EndringIRefusjon(500.daglig, 20.januar)
                )
        ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 19.januar, 1000.daglig),
                Refusjonsopplysning(inntektsmeldingJanuar, 20.januar, 24.januar, 500.daglig),
                Refusjonsopplysning(inntektsmeldingJanuar, 25.januar, null, 2000.daglig)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `Flere IM med flere endringer i refusjon`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = listOf(
                    EndringIRefusjon(2000.daglig, 25.januar),
                    EndringIRefusjon(500.daglig, 20.januar)
                )
        ))

        val inntektsmeldingMars = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingMars,
                førsteFraværsdag = 1.mars,
                arbeidsgiverperioder = listOf(1.mars til 16.mars),
                beløp = 999.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = listOf(
                    EndringIRefusjon(99.daglig, 25.mars),
                    EndringIRefusjon(9.daglig, 20.mars)
                )
        ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 19.januar, 1000.daglig),
                Refusjonsopplysning(inntektsmeldingJanuar, 20.januar, 24.januar, 500.daglig),
                Refusjonsopplysning(inntektsmeldingJanuar, 25.januar, 28.februar, 2000.daglig),

                Refusjonsopplysning(inntektsmeldingMars, 1.mars, 19.mars, 999.daglig),
                Refusjonsopplysning(inntektsmeldingMars, 20.mars, 24.mars, 9.daglig),
                Refusjonsopplysning(inntektsmeldingMars, 25.mars, null, 99.daglig)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `IM flere endringer i refusjon - og med siste refusjonsdag satt`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = 19.januar,
                endringerIRefusjon = listOf(
                    EndringIRefusjon(500.daglig, 20.januar)
                )
        ))
        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 19.januar, 1000.daglig),
                Refusjonsopplysning(inntektsmeldingJanuar, 20.januar, null, INGEN)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar).inspektør.refusjonsopplysninger
        )
    }
    @Test
    fun `IM flere endringer i refusjon - endring i refusjon samme dag som siste refusjonsdag`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = 19.januar,
                endringerIRefusjon = listOf(
                    EndringIRefusjon(500.daglig, 19.januar),
                    EndringIRefusjon(1000.daglig, 20.januar)
                )
        ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 18.januar, 1000.daglig),
                Refusjonsopplysning(inntektsmeldingJanuar, 19.januar, 19.januar, 500.daglig),
                Refusjonsopplysning(inntektsmeldingJanuar, 20.januar, null, INGEN),
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar).inspektør.refusjonsopplysninger
        )
    }
    @Test
    fun `IM flere endringer i refusjon - og med siste refusjonsdag satt - gap mellom siste refusjonsdag og første endring`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = 19.januar,
                endringerIRefusjon = listOf(
                    EndringIRefusjon(2000.daglig, 25.januar),
                    EndringIRefusjon(500.daglig, 21.januar)
                )
        ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 19.januar, 1000.daglig),
                Refusjonsopplysning(inntektsmeldingJanuar, 20.januar, null, INGEN)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `overgang til brukerutbetaling`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmelding = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmelding,
                førsteFraværsdag = 1.februar,
                arbeidsgiverperioder = listOf(1.februar til 16.februar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = emptyList()
            ))

        val korrigerendeInntektsmelding = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = korrigerendeInntektsmelding,
                førsteFraværsdag = 1.mai,
                arbeidsgiverperioder = listOf(1.februar til 16.februar),
                beløp = 0.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = emptyList()
            ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmelding, 1.februar, 30.april, 1000.daglig),
                Refusjonsopplysning(korrigerendeInntektsmelding, 1.mai, null, 0.daglig)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.februar).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `første fraværsdag satt til før første dag i siste del av agp`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmelding = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmelding,
                førsteFraværsdag = 1.februar,
                arbeidsgiverperioder = listOf(1.februar til 8.februar, 10.februar til 17.februar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = emptyList()
            ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmelding, 1.februar, 9.februar, 1000.daglig),
                Refusjonsopplysning(inntektsmelding, 10.februar, null, 1000.daglig)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.februar).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `første fraværsdag satt til etter første dag i siste del av agp`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmelding = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmelding,
                førsteFraværsdag = 11.februar,
                arbeidsgiverperioder = listOf(1.februar til 8.februar, 10.februar til 17.februar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = emptyList()
            ))


        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmelding, 1.februar, 10.februar, 1000.daglig),
                Refusjonsopplysning(inntektsmelding, 11.februar, null, 1000.daglig)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.februar).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `første fraværsdag satt til etter agp`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmelding = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmelding,
                førsteFraværsdag = 20.februar,
                arbeidsgiverperioder = listOf(1.februar til 8.februar, 10.februar til 17.februar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = emptyList()
            ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmelding, 1.februar, 19.februar, 1000.daglig),
                Refusjonsopplysning(inntektsmelding, 20.februar, null, 1000.daglig)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.februar).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `ignorerer endringer før startskuddet`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmelding = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmelding,
                førsteFraværsdag = 9.februar,
                arbeidsgiverperioder = listOf(1.februar til 8.februar, 10.februar til 17.februar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = listOf(
                    EndringIRefusjon(2000.daglig, 11.februar),
                    EndringIRefusjon(99.daglig, 9.februar)
                )
            ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmelding, 1.februar, 9.februar, 1000.daglig),
                Refusjonsopplysning(inntektsmelding, 10.februar, 10.februar, 1000.daglig),
                Refusjonsopplysning(inntektsmelding, 11.februar, null, 2000.daglig)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.februar).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `endringer i refusjon hulter til bulter`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmelding = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmelding,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = listOf(
                    EndringIRefusjon(4000.daglig, 15.januar),
                    EndringIRefusjon(2000.daglig, 10.januar),
                    EndringIRefusjon(3000.daglig, 12.januar)
                )
            ))

        assertEquals(
            listOf(
                Refusjonsopplysning(inntektsmelding, 1.januar, 9.januar, 1000.daglig),
                Refusjonsopplysning(inntektsmelding, 10.januar, 11.januar, 2000.daglig),
                Refusjonsopplysning(inntektsmelding, 12.januar, 14.januar, 3000.daglig),
                Refusjonsopplysning(inntektsmelding, 15.januar, null, 4000.daglig)
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar).inspektør.refusjonsopplysninger
        )
    }
}