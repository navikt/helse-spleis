package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.inntekt.Infotrygd
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
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
        assertEquals(inntektsopplysning(null).hashCode(), inntektsopplysning(null).hashCode())
        assertEquals(inntektsopplysning(1.januar).hashCode(), inntektsopplysning(1.januar).hashCode())
        assertNotEquals(inntektsopplysning().hashCode(), inntektsopplysning(1.januar).hashCode())
        assertNotEquals(inntektsopplysning(1.januar).hashCode(), inntektsopplysning(2.januar).hashCode())
        assertNotEquals(Inntektsopplysning(ORGNR, DATO, 1000.månedlig, true, null).hashCode(), Inntektsopplysning(ORGNR, DATO, 1000.månedlig, false, null).hashCode())
        assertNotEquals(Inntektsopplysning(ORGNR, DATO, 1000.månedlig, true, null).hashCode(), Inntektsopplysning(ORGNR, DATO, 2000.månedlig, true, null).hashCode())
        assertNotEquals(Inntektsopplysning(ORGNR, 1.januar, 1000.månedlig, true, null).hashCode(), Inntektsopplysning(ORGNR, 2.januar, 2100.månedlig, true, null).hashCode())
        assertNotEquals(Inntektsopplysning("ag1", DATO, 1000.månedlig, true, null).hashCode(), Inntektsopplysning("ag2", DATO, 2100.månedlig, true, null).hashCode())
        assertEquals(Inntektsopplysning("ag1", DATO, 123.6667.månedlig, true, null).hashCode(), Inntektsopplysning("ag1", DATO, 123.6667.månedlig, true, null).hashCode())
    }

    @Test
    fun `refusjon opphører før perioden`() {
        inntektsopplysning(1.januar).valider(aktivitetslogg, DATO, Nødnummer.Sykepenger)
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `refusjon opphører i perioden`() {
        inntektsopplysning(15.februar).valider(aktivitetslogg, DATO, Nødnummer.Sykepenger)
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `refusjon opphører etter perioden`() {
        inntektsopplysning(1.mars).valider(aktivitetslogg, DATO, Nødnummer.Sykepenger)
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
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
