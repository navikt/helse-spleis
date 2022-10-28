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
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, null, 1000.daglig)
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar)
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
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, null, 1000.daglig),
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar)
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
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, null, INGEN),
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar)
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
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingMars, 1.mars, null, 2000.daglig)
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.mars)
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
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 19.januar, 1000.daglig),
                    Refusjonsopplysning(inntektsmeldingJanuar, 20.januar, null, 500.daglig)
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar)
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
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 19.januar, 1000.daglig),
                    Refusjonsopplysning(inntektsmeldingJanuar, 20.januar, 24.januar, 500.daglig),
                    Refusjonsopplysning(inntektsmeldingJanuar, 25.januar, null, 2000.daglig)
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar)
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
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingJanuar, 1.januar, 19.januar, 1000.daglig),
                    Refusjonsopplysning(inntektsmeldingJanuar, 20.januar, 24.januar, 500.daglig),
                    Refusjonsopplysning(inntektsmeldingJanuar, 25.januar, 28.februar, 2000.daglig),

                    Refusjonsopplysning(inntektsmeldingMars, 1.mars, 19.mars, 999.daglig),
                    Refusjonsopplysning(inntektsmeldingMars, 20.mars, 24.mars, 9.daglig),
                    Refusjonsopplysning(inntektsmeldingMars, 25.mars, null, 99.daglig)
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar)
        )
    }

    @Test
    fun `IM med endring i refusjon før første dag i inntektsmeldingen`() {
        val refusjonshistorikk = Refusjonshistorikk()
        val inntektsmeldingJanuar = UUID.randomUUID()
        refusjonshistorikk.leggTilRefusjon(
            Refusjonshistorikk.Refusjon(
                meldingsreferanseId = inntektsmeldingJanuar,
                førsteFraværsdag = 1.februar,
                arbeidsgiverperioder = listOf(1.februar til 16.februar),
                beløp = 1000.daglig,
                sisteRefusjonsdag = null,
                endringerIRefusjon = listOf(
                    EndringIRefusjon(2000.daglig, 25.januar),
                )
            ))

        assertEquals(
            Refusjonsopplysninger(
                listOf(
                    Refusjonsopplysning(inntektsmeldingJanuar, 25.januar, 31.januar, 2000.daglig),
                    Refusjonsopplysning(inntektsmeldingJanuar, 1.februar, null, 1000.daglig),
                )
            ),
            refusjonshistorikk.refusjonsopplysninger(1.januar)
        )
    }


}