package no.nav.helse

import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class GrunnbeløpTest {

    @Test
    fun dagsats() {
        assertEquals(2304.daglig, Grunnbeløp.`6G`.dagsats(1.mai(2019)))
        assertEquals(2236.daglig, Grunnbeløp.`6G`.dagsats(30.april(2019)))
    }

    @Test
    fun grunnbeløp() {
        assertEquals(599148.årlig, Grunnbeløp.`6G`.beløp(1.mai(2019)))
        assertEquals(581298.årlig, Grunnbeløp.`6G`.beløp(30.april(2019)))
    }

    @Test
    fun snitt() {
        assertEquals(116239.årlig, Grunnbeløp.`1G`.snitt(2023))
        assertEquals(109784.årlig, Grunnbeløp.`1G`.snitt(2022))
        assertEquals(104716.årlig, Grunnbeløp.`1G`.snitt(2021))
        assertEquals(33575.årlig, Grunnbeløp.`1G`.snitt(1990))
    }

    @Test
    fun `grunnbeløp før virkingdato`() {
        assertEquals(99858.årlig, Grunnbeløp.`1G`.beløp(1.mai(2020)))
        assertEquals(99858.årlig, Grunnbeløp.`1G`.beløp(30.april(2020), 20.september(2020)))
        assertEquals(99858.årlig, Grunnbeløp.`1G`.beløp(1.mai(2020), 20.september(2020)))
    }

    @Test
    fun `grunnbeløp etter virkingdato`() {
        assertEquals(99858.årlig, Grunnbeløp.`1G`.beløp(30.april(2020), 21.september(2020)))
        assertEquals(101351.årlig, Grunnbeløp.`1G`.beløp(1.mai(2020), 21.september(2020)))
        assertEquals(101351.årlig, Grunnbeløp.`1G`.beløp(21.september(2020)))
    }

    @Test
    fun `skjæringstidspunkt brukes når virkningsdato er eldre`() {
        assertEquals(101351.årlig, Grunnbeløp.`1G`.beløp(10.oktober(2020), 21.september(2020)))
    }


    @Test
    fun `virkningstidspunktet for regulering av kravet til minsteinntekt`() {
        val halvG2018 = Grunnbeløp.halvG.beløp(30.april(2019))
        val halvG2019 = Grunnbeløp.halvG.beløp(1.mai(2019))
        val `2G2018` = Grunnbeløp.`2G`.beløp(30.april(2019))
        val `2G2019` = Grunnbeløp.`2G`.beløp(1.mai(2019))

        assertEquals(48441.5.årlig, halvG2018)
        assertEquals(49929.årlig, halvG2019)
        assertEquals(193766.årlig, `2G2018`)
        assertEquals(199716.årlig, `2G2019`)
    }

    @Test
    fun `virkningstidspunkt for Grunnbeløp`() {
        val beløp = Grunnbeløp.`1G`.beløp(1.mai(2019))
        val virkningstidspunkt = Grunnbeløp.virkningstidspunktFor(beløp)
        assertEquals(1.mai(2019), virkningstidspunkt)
    }

    @Test
    fun `virkningstidspunkt før ny G sitt virkningstidspunkt`() {
        val beløp = Grunnbeløp.`1G`.beløp(30.april(2020))
        val virkningstidspunkt = Grunnbeløp.virkningstidspunktFor(beløp)
        assertEquals(1.mai(2019), virkningstidspunkt)
    }


    @Test
    fun `virkningstidspunkt etter ny G sitt virkningstidspunkt`() {
        val beløp = Grunnbeløp.`1G`.beløp(2.mai(2021))
        val virkningstidspunkt = Grunnbeløp.virkningstidspunktFor(beløp)
        assertEquals(1.mai(2021), virkningstidspunkt)
    }

    @Test
    fun `kaster exception for beløp som ikke er gyldig Grunnbeløp`() {
        val ikkeGyldigGrunnbeløp = 123123.årlig
        assertThrows<IllegalArgumentException> { Grunnbeløp.virkningstidspunktFor(ikkeGyldigGrunnbeløp) }
    }
}
