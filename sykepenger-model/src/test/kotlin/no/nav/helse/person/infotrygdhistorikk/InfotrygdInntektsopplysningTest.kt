package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.inntekt.Infotrygd
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InfotrygdInntektsopplysningTest {

    private companion object {
        private const val ORGNR = "123456789"
        private val DATO = 1.januar
    }

    private lateinit var historikk: Inntektshistorikk
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun setup() {
        historikk = Inntektshistorikk()
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun likhet() {
        assertEquals(inntektsopplysning(null), inntektsopplysning(null))
        assertEquals(inntektsopplysning(1.januar), inntektsopplysning(1.januar))
        assertNotEquals(inntektsopplysning(), inntektsopplysning(1.januar))
        assertNotEquals(inntektsopplysning(1.januar), inntektsopplysning(2.januar))
        assertNotEquals(Inntektsopplysning(ORGNR, DATO, 1000.månedlig, true, null), Inntektsopplysning(ORGNR, DATO, 1000.månedlig, false, null))
        assertNotEquals(Inntektsopplysning(ORGNR, DATO, 1000.månedlig, true, null), Inntektsopplysning(ORGNR, DATO, 2000.månedlig, true, null))
        assertNotEquals(Inntektsopplysning(ORGNR, 1.januar, 1000.månedlig, true, null), Inntektsopplysning(ORGNR, 2.januar, 2100.månedlig, true, null))
        assertNotEquals(Inntektsopplysning("ag1", DATO, 1000.månedlig, true, null), Inntektsopplysning("ag2", DATO, 2100.månedlig, true, null))
        assertEquals(Inntektsopplysning("ag1", DATO, 123.6667.månedlig, true, null), Inntektsopplysning("ag1", DATO, 123.6667.månedlig, true, null))
    }

    private fun assertEquals(one: Inntektsopplysning, two: Inntektsopplysning) {
        assertTrue(one.funksjoneltLik(two))
        assertTrue(two.funksjoneltLik(one))
    }
    private fun assertNotEquals(one: Inntektsopplysning, two: Inntektsopplysning) {
        assertFalse(one.funksjoneltLik(two))
        assertFalse(two.funksjoneltLik(one))
    }

    @Test
    fun equals() {
        val inntektID = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val tidsstempel = LocalDateTime.now()
        val inntektsopplysning1 = Infotrygd(
            id = inntektID,
            dato = 1.januar,
            hendelseId = hendelseId,
            beløp = 25000.månedlig,
            tidsstempel = tidsstempel
        )
        assertEquals(
            inntektsopplysning1,
            Infotrygd(
                id = inntektID,
                dato = 1.januar,
                hendelseId = hendelseId,
                beløp = 25000.månedlig,
                tidsstempel = tidsstempel
            )
        )
        assertNotEquals(
            inntektsopplysning1,
            Infotrygd(
                id = inntektID,
                dato = 5.januar,
                hendelseId = hendelseId,
                beløp = 25000.månedlig,
                tidsstempel = tidsstempel
            )
        )
        assertNotEquals(
            inntektsopplysning1,
            Infotrygd(
                id = inntektID,
                dato = 1.januar,
                hendelseId = hendelseId,
                beløp = 32000.månedlig,
                tidsstempel = tidsstempel
            )
        )
        assertEquals(
            inntektsopplysning1,
            Infotrygd(
                id = UUID.randomUUID(),
                dato = 1.januar,
                hendelseId = hendelseId,
                beløp = 25000.månedlig,
                tidsstempel = tidsstempel
            )
        )
        assertEquals(
            inntektsopplysning1,
            Infotrygd(
                id = inntektID,
                dato = 1.januar,
                hendelseId = UUID.randomUUID(),
                beløp = 25000.månedlig,
                tidsstempel = tidsstempel
            )
        )
        assertEquals(
            inntektsopplysning1,
            Infotrygd(
                id = inntektID,
                dato = 1.januar,
                hendelseId = hendelseId,
                beløp = 25000.månedlig,
                tidsstempel = LocalDate.EPOCH.atStartOfDay()
            )
        )
    }

    private fun inntektsopplysning(refusjonTom: LocalDate? = null) =
        Inntektsopplysning(ORGNR, DATO, 1000.månedlig, true, refusjonTom)
}
