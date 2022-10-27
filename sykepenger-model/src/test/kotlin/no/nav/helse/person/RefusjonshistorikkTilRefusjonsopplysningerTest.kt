package no.nav.helse.person

import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Refusjonshistorikk.Refusjon.EndringIRefusjon
import no.nav.helse.person.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.refusjonsopplysninger
import no.nav.helse.person.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class RefusjonshistorikkTilRefusjonsopplysningerTest {

    @Test
    fun `tom refusjonshistorikk er tom refusjonsopplysninger`() {
        val refusjonshistorikk = Refusjonshistorikk()
        assertEquals(Refusjonsopplysninger(), refusjonshistorikk.refusjonsopplysninger(1.januar))
    }

    @Test
    fun `søknad og IM for januar`() {
        val refusjonshistorikk = Refusjonshistorikk()
        // mottar inntektsmelding for Januar
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
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, null, 1000.daglig)
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar)
        )
    }

    @Test
    fun `søknad og IM for januar og kun IM for mars`() {
        val refusjonshistorikk = Refusjonshistorikk()
        // mottar inntektsmelding for Januar
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
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 28.februar, 1000.daglig),
                    Refusjonsopplysning(inntektsmeldingMars, 1.mars, null, 2000.daglig)
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar)
        )
    }

    @Test
    fun `søknad og IM uten første fraværsdag`() {
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
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, null, 1000.daglig),
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar)
        )
    }

    @Test
    fun `IM med beløp = null`() {
        val refusjonshistorikk = Refusjonshistorikk()
        // mottar inntektsmelding for Januar
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
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, null, INGEN),
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar)
        )
    }

    @Test
    fun `ikke januar i resultatet - kun mars`() {
        val refusjonshistorikk = Refusjonshistorikk()
        // mottar inntektsmelding for Januar
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.månedlig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = emptyList()
        ))

        val inntektsmeldingMars = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingMars,
                førsteFraværsdag = 1.mars,
                arbeidsgiverperioder = listOf(1.mars til 16.mars),
                beløp = 2000.månedlig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = emptyList()
        ))

        assertEquals(
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingMars, 1.mars, null, 2000.månedlig)
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.mars)
        )
    }

    @Test
    fun `kun periode i januar med en endring i refusjon`() {
        val refusjonshistorikk = Refusjonshistorikk()
        // mottar inntektsmelding for Januar
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.månedlig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = listOf(EndringIRefusjon(500.månedlig, 20.januar))
        ))

        assertEquals(
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 19.januar, 1000.månedlig),
                    Refusjonsopplysning(inntektsmeldingJanuar, 20.januar, null, 500.månedlig)
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar)
        )
    }


    @Test
    fun `kun periode i januar med flere endringer i refusjon`() {
        val refusjonshistorikk = Refusjonshistorikk()
        // mottar inntektsmelding for Januar
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.månedlig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = listOf(
                    EndringIRefusjon(2000.månedlig, 25.januar),
                    EndringIRefusjon(500.månedlig, 20.januar)
                )
        ))

        assertEquals(
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 19.januar, 1000.månedlig),
                    Refusjonsopplysning(inntektsmeldingJanuar, 20.januar, 24.januar, 500.månedlig),
                    Refusjonsopplysning(inntektsmeldingJanuar, 25.januar, null, 2000.månedlig)
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar)
        )
    }

    @Disabled("funker ikke")
    @Test
    fun `periode i januar og mars med flere endringer i refusjon`() {
        val refusjonshistorikk = Refusjonshistorikk()
        // mottar inntektsmelding for Januar
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.januar,
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beløp = 1000.månedlig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = listOf(
                    EndringIRefusjon(2000.månedlig, 25.januar),
                    EndringIRefusjon(500.månedlig, 20.januar)
                )
        ))

        val inntektsmeldingMars = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingMars,
                førsteFraværsdag = 1.mars,
                arbeidsgiverperioder = listOf(1.mars til 16.mars),
                beløp = 999.månedlig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = listOf(
                    EndringIRefusjon(99.månedlig, 25.mars),
                    EndringIRefusjon(9.månedlig, 20.mars)
                )
        ))

        assertEquals(
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 19.januar, 1000.månedlig),
                    Refusjonsopplysning(inntektsmeldingJanuar, 20.januar, 24.januar, 500.månedlig),
                    Refusjonsopplysning(inntektsmeldingJanuar, 25.januar, 28.februar, 2000.månedlig),

                    Refusjonsopplysning(inntektsmeldingMars, 1.mars, 19.mars, 999.månedlig),
                    Refusjonsopplysning(inntektsmeldingMars, 20.mars, 24.mars, 99.månedlig),
                    Refusjonsopplysning(inntektsmeldingMars, 25.mars, null, 9.månedlig)
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar)
        )

        assertEquals(
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingMars, 1.mars, 19.mars, 999.månedlig),
                    Refusjonsopplysning(inntektsmeldingMars, 20.mars, 24.mars, 99.månedlig),
                    Refusjonsopplysning(inntektsmeldingMars, 25.mars, null, 9.månedlig)
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.mars)
        )
    }


}