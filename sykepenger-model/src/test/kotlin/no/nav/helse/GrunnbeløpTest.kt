package no.nav.helse

import java.time.LocalDate
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

        // person er under 67 ved skjæringstidspunkt
        assertMinsteinntektOk(
            skjæringstidspunkt = 26.mai(2019),
            inntekt = halvG2018,
            alder = under67(),
        )
        assertMinimumInntektAvslag(
            skjæringstidspunkt = 27.mai(2019),
            inntekt = halvG2018,
            alder = under67(),
        )
        assertMinsteinntektOk(
            skjæringstidspunkt = 27.mai(2019),
            inntekt = halvG2019,
            alder = under67(),
        )

        // 67-åring blir behandlet som under 67 år
        assertMinsteinntektOk(
            skjæringstidspunkt = 26.mai(2019),
            inntekt =  halvG2018,
            alder = akkurat67(26.mai(2019)),
        )
        assertMinimumInntektAvslag(
            skjæringstidspunkt = 27.mai(2019),
            inntekt =  halvG2018,
            alder = akkurat67(27.mai(2019)),
        )
        assertMinsteinntektOk(
            skjæringstidspunkt = 27.mai(2019),
            inntekt = halvG2019,
            alder = akkurat67(27.mai(2019)),
        )


        // person er over 67 ved skjæringstidspunkt
        assertMinsteinntektOk(
            skjæringstidspunkt = 26.mai(2019),
            inntekt = `2G2018`,
            alder = over67(30.april(2019)),
        )
        assertMinimumInntektOver67Avslag(
            skjæringstidspunkt = 27.mai(2019),
            inntekt = `2G2018`,
            alder = over67(27.mai(2019)),
        )
        assertMinsteinntektOk(
            skjæringstidspunkt = 27.mai(2019),
            inntekt = `2G2019`,
            alder = over67(30.april(2019)),
        )
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
        assertThrows<IllegalArgumentException> { Grunnbeløp.virkningstidspunktFor(ikkeGyldigGrunnbeløp)}
    }

    private fun assertMinsteinntektOk(
        skjæringstidspunkt: LocalDate,
        inntekt: Inntekt,
        alder: Alder
    ) = assertNull(
        Grunnbeløp.validerMinsteInntekt(
            skjæringstidspunkt = skjæringstidspunkt,
            inntekt = inntekt,
            alder = alder,
            subsumsjonObserver = MaskinellJurist()
        )
    )

    private fun assertMinimumInntektOver67Avslag(
        skjæringstidspunkt: LocalDate,
        inntekt: Inntekt,
        alder: Alder
    ) = assertEquals(
        Begrunnelse.MinimumInntektOver67,
        Grunnbeløp.validerMinsteInntekt(
            skjæringstidspunkt = skjæringstidspunkt,
            inntekt = inntekt,
            alder = alder,
            subsumsjonObserver = MaskinellJurist()
        )
    )

    private fun assertMinimumInntektAvslag(
        skjæringstidspunkt: LocalDate,
        inntekt: Inntekt,
        alder: Alder
    ) = assertEquals(
        Begrunnelse.MinimumInntekt, Grunnbeløp.validerMinsteInntekt(
            skjæringstidspunkt = skjæringstidspunkt,
            inntekt = inntekt,
            alder = alder,
            subsumsjonObserver = MaskinellJurist()
        )
    )

    private fun under67() = Alder(LocalDate.now().minusYears(66))
    private fun over67(skjæringstidspunkt: LocalDate) =
        Alder(skjæringstidspunkt.minusYears(67).minusDays(1))
    private fun akkurat67(skjæringstidspunkt: LocalDate) =
        Alder(skjæringstidspunkt.minusYears(67))
}
