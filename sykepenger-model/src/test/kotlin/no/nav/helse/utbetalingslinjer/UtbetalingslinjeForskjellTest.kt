package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Endringskode.*
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class UtbetalingslinjeForskjellTest {

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNUMMER = "987654321"
        private const val FAGSYSTEMID = "FAGSYSTEMID"
    }

    private lateinit var aktivitetslogg: Aktivitetslogg
    private operator fun Oppdrag.minus(other: Oppdrag) = this.minus(other, aktivitetslogg)

    @BeforeEach
    fun setup(){
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `helt separate utbetalingslinjer`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(5.februar to 9.februar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(5.februar to 9.februar), actual)
        assertNotEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(NY, actual.endringskode)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `tomme utbetalingslinjer fungerer som Null Object Utbetalingslinjer`() {
        val original = tomtOppdrag()
        val recalculated = linjer(5.februar to 9.februar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(5.februar to 9.februar), actual)
        assertNotEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(NY, actual.endringskode)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `fullstendig overskriv`() {
        val original = linjer(8.januar to 13.januar)
        val recalculated = linjer(1.januar to 9.februar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 9.februar), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(original.fagsystemId, actual[0].refFagsystemId)
        assertEquals(original[0].id, actual[0].refId)
        assertEquals(original[0].id + 1, actual[0].id)
        assertEquals(NY, actual[0].endringskode)

        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `ny tom`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 13.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 13.januar), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(ENDR, actual[0].endringskode)
        assertEquals(original[0].id, actual[0].id)
        assertNull(actual[0].refId)
        assertNull(actual[0].refFagsystemId)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `trekke periode tilbake`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 13.januar)
        val actual = recalculated - original
        val revised = original - actual

        assertEquals(1, revised.size)
        assertEquals(1.januar, revised.last().fom)
        assertEquals(5.januar, revised.last().tom)
        assertEquals(ENDR, revised.last().endringskode)
        assertNull(revised.last().datoStatusFom)
    }

    @Test
    fun `trekke periode tilbake med opphold`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 5.januar, 8.januar to 13.januar)
        val actual = recalculated - original
        val revised = original - actual

        assertEquals(1, revised.size)
        assertEquals(1.januar, revised.first().fom)
        assertEquals(5.januar, revised.first().tom)
        assertEquals(NY, revised.first().endringskode)
        assertNull(revised.first().datoStatusFom)
        assertEquals(actual.last().id + 1, revised.first().id)
        assertEquals(actual.last().id, revised.first().refId)
    }

    @Test
    fun `trekke siste periode tilbake`() {
        val original = linjer(
            1.januar to 5.januar,
            8.januar to 13.januar,
            15.januar to 25.januar
        )
        val recalculated = linjer(
            1.januar to 5.januar,
            8.januar to 13.januar
        )
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            1.januar to 5.januar,
            8.januar to 13.januar,
            15.januar to 25.januar
        ), actual)
        assertEquals(3, actual.size)
        assertEquals(UEND, actual[0].endringskode)
        assertEquals(1, actual[0].id)

        assertEquals(UEND, actual[1].endringskode)
        assertEquals(actual[0].id + 1, actual[1].id)
        assertEquals(ENDR, actual[2].endringskode)
        assertEquals(actual[1].id + 1, actual[2].id)
        assertEquals(14.januar, actual[2].datoStatusFom)
    }

    @Test
    fun `trekke siste periode tilbake, så frem igjen`() {
        val original = linjer(
            1.januar to 5.januar,
            8.januar to 13.januar,
            15.januar to 25.januar
        )
        val recalculated = linjer(
            1.januar to 5.januar,
            8.januar to 13.januar
        )
        val revised = recalculated - original
        val actual = original - revised
        assertUtbetalinger(linjer(
            1.januar to 5.januar,
            8.januar to 13.januar,
            15.januar to 25.januar
        ), actual)
        assertEquals(3, actual.size)
        assertEquals(UEND, actual[0].endringskode)
        assertEquals(1, actual[0].id)

        assertEquals(UEND, actual[1].endringskode)
        assertEquals(actual[0].id + 1, actual[1].id)
        assertEquals(NY, actual[2].endringskode)
        assertEquals(revised[2].id + 1, actual[2].id)
        assertEquals(revised[2].id, actual[2].refId)
    }

    @Test
    fun `trekke siste periode tilbake, så forlenge siste periode`() {
        val original = linjer(
            1.januar to 5.januar,
            8.januar to 13.januar,
            15.januar to 25.januar
        )
        val recalculated = linjer(
            1.januar to 5.januar,
            8.januar to 13.januar
        )
        val revised = recalculated - original
        val extended = linjer(
            1.januar to 5.januar,
            8.januar to 25.januar
        )
        val actual = extended - revised
        assertUtbetalinger(linjer(
            1.januar to 5.januar,
            15.januar to 25.januar,
            8.januar to 25.januar
        ), actual)
        assertEquals(3, actual.size)
        assertEquals(UEND, actual[0].endringskode)
        assertEquals(1, actual[0].id)

        assertEquals(ENDR, actual[1].endringskode)
        assertEquals(revised[1].id + 1, actual[1].id)
        assertEquals(8.januar, actual[1].datoStatusFom)

        assertEquals(NY, actual[2].endringskode)
        assertEquals(actual[1].id + 1, actual[2].id)
        assertEquals(actual[1].id, actual[2].refId)
    }

    @Test
    fun `trekke periode tilbake potpourri 1`() {
        val original = linjer(1.januar to 5.januar)
        val intermediate = linjer(1.januar to 5.januar, 8.januar to 13.januar)
        val extended = intermediate - original
        val recalculated = linjer(1.januar to 5.januar, 8.januar to 20.januar, 28.januar to 5.februar)
        val actual = recalculated - extended
        val revised = original - actual

        assertEquals(1, revised.size)

        assertEquals(1.januar, revised.first().fom)
        assertEquals(5.januar, revised.first().tom)
        assertEquals(NY, revised.first().endringskode)
        assertNull(revised.first().datoStatusFom)
        assertEquals(actual.last().id, revised.first().refId)
        assertEquals(actual.last().id + 1, revised.first().id)
    }

    @Test
    fun `trekke periode tilbake potpourri 2`() {
        val original = linjer(1.januar to 5.januar)
        val intermediate = linjer(1.januar to 5.januar, 8.januar to 13.januar)
        val extended = intermediate - original

        val recalculated = linjer(1.januar to 5.januar, 8.januar to 20.januar, 23.januar to 26.januar, 28.januar to 5.februar)
        val actual = recalculated - extended

        val tilbakeført = linjer(1.januar to 5.januar, 8.januar to 13.januar)
        val revised = tilbakeført - actual

        assertEquals(3, revised.size)

        assertEquals(1.januar, revised[0].fom)
        assertEquals(5.januar, revised[0].tom)
        assertEquals(UEND, revised[0].endringskode)
        assertNull(revised[0].datoStatusFom)
        assertEquals(1, revised[0].id)
        assertNull(revised[0].refId)

        assertEquals(28.januar, revised[1].fom)
        assertEquals(5.februar, revised[1].tom)
        assertEquals(ENDR, revised[1].endringskode)
        assertEquals(8.januar, revised[1].datoStatusFom)
        assertEquals(4, revised[1].id)
        assertNull(revised[1].refId)

        assertEquals(8.januar, revised[2].fom)
        assertEquals(13.januar, revised[2].tom)
        assertEquals(NY, revised[2].endringskode)
        assertNull(revised[2].datoStatusFom)
        assertEquals(revised[1].id + 1, revised[2].id)
        assertEquals(revised[1].id, revised[2].refId)
    }

    @Test
    fun `trekke periode frem potpourri 1`() {
        val original = linjer(1.januar to 5.januar, 8.januar to 20.januar, 23.januar to 26.januar, 28.januar to 5.februar)
        val tilbakeført = linjer(1.januar to 6.januar, 8.januar to 13.januar)
        val revised = tilbakeført - original

        assertEquals(3, revised.size)

        assertEquals(28.januar, revised[0].fom)
        assertEquals(5.februar, revised[0].tom)
        assertEquals(ENDR, revised[0].endringskode)
        assertEquals(1.januar, revised[0].datoStatusFom)
        assertEquals(4, revised[0].id)
        assertNull(revised[0].refId)

        assertEquals(1.januar, revised[1].fom)
        assertEquals(6.januar, revised[1].tom)
        assertEquals(NY, revised[1].endringskode)
        assertNull(revised[1].datoStatusFom)
        assertEquals(revised[0].id + 1, revised[1].id)
        assertEquals(revised[0].id, revised[1].refId)

        assertEquals(8.januar, revised[2].fom)
        assertEquals(13.januar, revised[2].tom)
        assertEquals(NY, revised[2].endringskode)
        assertNull(revised[2].datoStatusFom)
        assertEquals(revised[1].id + 1, revised[2].id)
        assertEquals(revised[1].id, revised[2].refId)
    }

    @Test
    fun `trekke periode tilbake potpourri 3`() {
        val original = linjer(1.januar to 5.januar)
        val intermediate = linjer(1.januar to 5.januar, 8.januar to 13.januar)
        val extended = intermediate - original

        val recalculated = linjer(1.januar to 5.januar, 8.januar to 13.januar, 23.januar to 5.februar)
        val actual = recalculated - extended

        val tilbakeført = linjer(1.januar to 5.januar, 8.januar to 13.januar, 23.januar to 26.januar, 1.februar to 5.februar)
        val revised = tilbakeført - actual

        /*
            mulige utfall, alle OK:

            Sende opphør på siste linje
            - Linje 3, ENDR, OPPH, datoStatusFom=27.januar
            - Linje 4, NY, 1.februar - 5.februar
         */
        assertEquals(4, revised.size)

        assertEquals(1.januar, revised[0].fom)
        assertEquals(5.januar, revised[0].tom)
        assertEquals(UEND, revised[0].endringskode)
        assertNull(revised[0].datoStatusFom)
        assertEquals(1, revised[0].id)
        assertNull(revised[0].refId)

        assertEquals(8.januar, revised[1].fom)
        assertEquals(13.januar, revised[1].tom)
        assertEquals(UEND, revised[1].endringskode)
        assertNull(revised[1].datoStatusFom)
        assertEquals(revised[0].id + 1, revised[1].id)
        assertEquals(revised[0].id, revised[1].refId)

        assertEquals(23.januar, revised[2].fom)
        assertEquals(26.januar, revised[2].tom)
        assertEquals(ENDR, revised[2].endringskode)
        assertNull(revised[2].datoStatusFom)
        assertEquals(revised[1].id + 1, revised[2].id)
        assertNull(revised[2].refId)

        assertEquals(1.februar, revised[3].fom)
        assertEquals(5.februar, revised[3].tom)
        assertNull(revised[3].datoStatusFom)
        assertEquals(NY, revised[3].endringskode)
        assertEquals(revised[2].id + 1, revised[3].id)
        assertEquals(revised[2].id, revised[3].refId)
    }

    @Test
    fun `bare flere utbetalingslinjer`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 5.januar, 15.januar to 19.januar)
        val actual = recalculated - original
        assertUtbetalinger(recalculated, actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(UEND, actual[0].endringskode)
        assertEquals(NY, actual[1].endringskode)
        assertEquals(original[0].id, actual[1].refId)
        assertEquals(original[0].id + 1, actual[1].id)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `grad endres`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 5.januar grad 80, 15.januar to 19.januar)
        val actual = recalculated - original
        assertUtbetalinger(recalculated, actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(NY, actual[0].endringskode)
        assertEquals(NY, actual[1].endringskode)
        assertEquals(original[0].id + 1, actual[0].id)  // chained off of last of original
        assertEquals(actual[0].id + 1, actual[1].id)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `dagsats endres`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 5.januar dagsats 1000, 15.januar to 19.januar)
        val actual = recalculated - original
        assertUtbetalinger(recalculated, actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(NY, actual[0].endringskode)
        assertEquals(NY, actual[1].endringskode)
        assertEquals(original[0].id + 1, actual[0].id)  // chained off of last of original
        assertEquals(actual[0].id + 1, actual[1].id)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Tre perioder hvor grad endres i siste periode`() {
        val original = linjer(17.juni(2020) to 30.juni(2020))
        val new = linjer(17.juni(2020) to 31.juli(2020))
        val intermediate = new - original
        assertEquals(original.fagsystemId, intermediate.fagsystemId)

        val new2 = linjer(
            17.juni(2020) to 31.juli(2020),
            1.august(2020) to 31.august(2020) grad 50
        )

        val actual = new2 - intermediate

        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(intermediate.fagsystemId, actual.fagsystemId)

        assertEquals(original[0].id, actual[0].id)
        assertEquals(NY, original[0].endringskode)
        assertEquals(ENDR, intermediate[0].endringskode)

        assertEquals(original[0].id + 1, actual[1].id)
        assertEquals(UEND, actual[0].endringskode)
        assertEquals(NY, actual[1].endringskode)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `potpourri`() {
        val original = linjer(
            1.januar to 5.januar,
            6.januar to 12.januar grad 50,
            13.januar to 19.januar grad 80
        )
        val recalculated = linjer(
            1.januar to 5.januar,
            6.januar to 17.januar grad 50,  // extended tom
            18.januar to 19.januar grad 80,
            1.februar to 9.februar
        )
        val actual = recalculated - original
        assertUtbetalinger(recalculated, actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)

        assertEquals(UEND, actual[0].endringskode)
        assertNull(actual[0].refId)

        assertEquals(ENDR, actual[1].endringskode)
        assertNull(actual[1].refId)
        assertNull(actual[1].refFagsystemId)

        assertEquals(NY, actual[2].endringskode)
        assertEquals(original[2].id + 1, actual[2].id)  // chained off of last of original
        assertEquals(original[2].id, actual[2].refId)

        assertEquals(NY, actual[3].endringskode)
        assertEquals(actual[2].id + 1, actual[3].id)
        assertEquals(actual[2].id, actual[3].refId)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `potpourri 2`() {
        val original = linjer(
            1.januar to 5.januar,
            6.januar to 12.januar grad 50,
            13.januar to 19.januar grad 80,
            1.februar to 3.februar,
            4.februar to 6.februar,
            7.februar to 8.februar
        )
        val recalculated = linjer(
            1.januar to 5.januar,
            6.januar to 17.januar grad 50,  // extended tom
            18.januar to 19.januar grad 80,
            1.februar to 9.februar
        )
        val actual = recalculated - original
        assertUtbetalinger(recalculated, actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(UEND, actual[0].endringskode)
        assertEquals(ENDR, actual[1].endringskode)
        assertEquals(NY, actual[2].endringskode)
        assertEquals(NY, actual[3].endringskode)
        assertEquals(original[5].id + 1, actual[2].id)  // chained off of last of original
        assertEquals(actual[2].id + 1, actual[3].id)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `fom endres`() {
        val original = linjer(5.januar to 10.januar)
        val recalculated = linjer(1.januar to 10.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 10.januar), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(NY, actual[0].endringskode)
        assertEquals(original[0].id + 1, actual[0].id)
        assertEquals(original[0].id, actual[0].refId)

        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `fom og tom endres medfører implisit annullering av linjer etter tom hos Oppdrag`() {
        val original = linjer(5.januar to 10.januar)
        val recalculated = linjer(1.januar to 3.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 3.januar), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(NY, actual[0].endringskode)
        assertEquals(original[0].id + 1, actual[0].id)
        assertEquals(original[0].id, actual[0].refId)

        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `potpourri 3`() {
        val original = linjer(
            1.januar to 5.januar,
            6.januar to 12.januar grad 50,
            13.januar to 19.januar
        )
        val new = linjer(
            1.januar to 5.januar,
            6.januar to 19.januar grad 50,
            20.januar to 26.januar
        )
        val actual = new - original
        assertEquals(original.fagsystemId, actual.fagsystemId)

        assertEquals(original[0].id, actual[0].id)
        assertEquals(original[2].id, actual[1].id)
        assertEquals(original[2].id + 1, actual[2].id)

        assertEquals(NY, original[0].endringskode)
        assertEquals(NY, original[1].endringskode)
        assertEquals(NY, original[2].endringskode)

        assertEquals(UEND, actual[0].endringskode)
        assertEquals(ENDR, actual[1].endringskode)
        assertEquals(NY, actual[2].endringskode)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `slett nøyaktig en periode`() {
        val original = linjer(1.januar to 5.januar, 6.januar to 12.januar grad 50)
        val recalculated = linjer(6.januar to 12.januar grad 50)
        val actual = recalculated - original
        assertUtbetalinger(
            linjer(
                6.januar to 12.januar grad 50,
                6.januar to 12.januar grad 50)
            , actual
        )
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(ENDR, actual[0].endringskode)
        assertEquals(1.januar, actual[0].datoStatusFom)
        assertEquals(original[1].id, actual[0].id)
        assertEquals(null, actual[0].refId)
        assertEquals(null, actual[0].refFagsystemId)
        assertEquals(NY, actual[1].endringskode)
        assertEquals(original[1].id + 1, actual[1].id)
        assertEquals(original[1].id, actual[1].refId)

        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `original andre periode samsvarer delvis med ny`() {
        val original = linjer(1.januar to 5.januar, 6.januar to 12.januar grad 50)
        val recalculated = linjer(6.januar to 19.januar grad 50)
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            6.januar to 12.januar grad 50,
            6.januar to 19.januar grad 50
        ), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(ENDR, actual[0].endringskode)
        assertEquals(original[1].id, actual[0].id)
        assertEquals(null, actual[0].refId)
        assertEquals(1.januar, actual[0].datoStatusFom)
        assertEquals(NY, actual[1].endringskode)
        assertEquals(original[1].id + 1, actual[1].id)
        assertEquals(original[1].id, actual[1].refId)

        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `perioden min starter midt i en original periode`() {
        val original = linjer(1.januar to 3.januar, 4.januar to 12.januar grad 50)
        val recalculated = linjer(6.januar to 19.januar grad 50)
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            4.januar to 12.januar grad 50,
            6.januar to 19.januar grad 50
        ), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(ENDR, actual[0].endringskode)
        assertEquals(original[1].id, actual[0].id)
        assertEquals(null, actual[0].refId)
        assertEquals(1.januar, actual[0].datoStatusFom)
        assertEquals(NY, actual[1].endringskode)
        assertEquals(original[1].id + 1, actual[1].id)
        assertEquals(original[1].id, actual[1].refId)

        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `ny er tom`() {
        val original = linjer(1.januar to 3.januar, 4.januar to 12.januar grad 50)
        val recalculated = tomtOppdrag()
        val actual = recalculated - original
        assertUtbetalinger(
            linjer(4.januar to 12.januar grad 50),
            actual
        )
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(ENDR, actual[0].endringskode)
        assertEquals(original[1].id, actual[0].id)
        assertEquals(null, actual[0].refId)
        assertEquals(1.januar, actual[0].datoStatusFom)
        assertEquals(null, actual[0].refFagsystemId)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `ny er tom uten sisteArbeidsgiverdag`() {
        val original = linjer(1.januar to 3.januar, 4.januar to 12.januar grad 50)
        val recalculated = tomtOppdrag(null)
        val actual = recalculated - original
        assertUtbetalinger(
            linjer(4.januar to 12.januar grad 50),
            actual
        )
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(ENDR, actual[0].endringskode)
        assertEquals(original[1].id, actual[0].id)
        assertEquals(null, actual[0].refId)
        assertEquals(1.januar, actual[0].datoStatusFom)
        assertEquals(null, actual[0].refFagsystemId)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `ny er tom og sisteArbeidsgiverdag er etter tidligere`() {
        val original = linjer(1.januar to 3.januar, 4.januar to 12.januar grad 50)
        val recalculated = tomtOppdrag(1.februar)
        val actual = recalculated - original
        assertUtbetalinger(recalculated, actual)
        assertNotEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(NY, actual.endringskode)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `originalen har hale å slette`() {
        val original = linjer(1.januar to 12.januar)
        val recalculated = linjer(1.januar to 5.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 5.januar), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(ENDR, actual[0].endringskode)
        assertEquals(original[0].id, actual[0].id)
        assertNull(actual[0].refId)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `Sletting hvor alt blir sendt på nytt`() {
        val original = linjer(1.januar to 12.januar)
        val recalculated = linjer(1.januar to 5.januar, 7.januar to 10.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            1.januar to 5.januar,
            7.januar to 10.januar
        ), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(ENDR, actual[0].endringskode)
        assertEquals(1.januar, actual[0].fom)
        assertEquals(5.januar, actual[0].tom)
        assertEquals(original[0].id, actual[0].id)
        assertNull(actual[0].refId)
        assertEquals(NY, actual[1].endringskode)
        assertEquals(7.januar, actual[1].fom)
        assertEquals(10.januar, actual[1].tom)
        assertLink(actual[1], original[0])

        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `Sletting med UEND`() {
        val original = linjer(
            1.januar to 5.januar,
            8.januar to 12.januar
        )
        val recalculated = linjer(
            1.januar to 5.januar,
            8.januar to 10.januar
        )
        val actual = recalculated - original

        assertUtbetalinger(linjer(
            1.januar to 5.januar,
            8.januar to 10.januar
        ), actual)

        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(UEND, actual[0].endringskode)
        assertEquals(original[0].id, actual[0].id)
        assertEquals(null, actual[0].refId)
        assertNull(actual[0].datoStatusFom)
        assertEquals(1.januar, actual[0].fom)
        assertEquals(5.januar, actual[0].tom)
        assertEquals(original[1].id, actual[1].id)
        assertNull(actual[1].refId)
        assertEquals(ENDR, actual[1].endringskode)
        assertEquals(8.januar, actual[1].fom)
        assertEquals(10.januar, actual[1].tom)
        assertEquals(actual[0].id + 1, actual[1].id)
        assertNull(actual[1].refId)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `slette fra hode og hale av originalen`() {
        val original = linjer(1.januar to 12.januar)
        val recalculated = linjer(3.januar to 9.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            1.januar to 12.januar,
            3.januar to 9.januar
        ), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)

        assertEquals(ENDR, actual[0].endringskode)
        assertEquals(1.januar, actual[0].datoStatusFom)
        assertEquals(original[0].id, actual[0].id)
        assertNull(actual[0].refId)

        assertEquals(NY, actual[1].endringskode)
        assertEquals(original[0].id + 1, actual[1].id)
        assertEquals(original[0].id, actual[1].refId)

        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `deletion potpourri`() {
        val original = linjer(
            1.januar to 5.januar,
            6.januar to 12.januar grad 50,
            13.januar to 19.januar,
            20.januar to 31.januar
        )
        val new = linjer(
            6.januar to 19.januar grad 50,
            20.januar to 26.januar
        )
        val actual = new - original
        assertUtbetalinger(linjer(
            20.januar to 31.januar,
            6.januar to 19.januar grad 50,
            20.januar to 26.januar
        ), actual)

        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)

        assertEquals(ENDR, actual[0].endringskode)
        assertEquals(1.januar, actual[0].datoStatusFom)
        assertEquals(original[3].id, actual[0].id)
        assertEquals(null, actual[0].refId)

        assertEquals(NY, actual[1].endringskode)
        assertEquals(original[3].id + 1, actual[1].id)
        assertEquals(original[3].id, actual[1].refId)

        assertEquals(NY, actual[2].endringskode)
        assertEquals(actual[1].id + 1, actual[2].id)
        assertEquals(actual[1].id, actual[2].refId)

        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `deletion all`() {
        val original = linjer(
            1.januar to 5.januar,
            6.januar to 12.januar grad 50,
            13.januar to 19.januar,
            20.januar to 31.januar
        )
        val new = tomtOppdrag()
        val actual = new - original
        assertUtbetalinger(
            linjer(20.januar to 31.januar),
            actual
        )

        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)

        assertEquals(ENDR, actual[0].endringskode)
        assertEquals(1.januar, actual[0].datoStatusFom)
        assertEquals(original[3].id, actual[0].id)
        assertEquals(null, actual[0].refId)

        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `hashcode equality`() {
        val first = linjer(1.januar to 5.januar)
        val second = linjer(1.januar to 5.januar)

        assertEquals(first[0].hashCode(), second[0].hashCode())
        val intermediate = second - first
        assertNotEquals(first[0].hashCode(), second[0].hashCode())

        val third = linjer(1.januar to 5.januar)

        val final = third - intermediate

        assertNotEquals(first[0].hashCode(), intermediate[0].hashCode())
        assertEquals(intermediate[0].hashCode(), final[0].hashCode())
    }

    @Test
    fun `hashcode på siste linje endres når et oppdrag opphører`() {
        val original = linjer(
            1.januar to 5.januar
        )
        val new = tomtOppdrag()

        val actual = new - original

        assertNotNull(actual[0].datoStatusFom)
        assertNotEquals(original[0].hashCode(), actual[0].hashCode())
    }

    private fun tomtOppdrag(sisteArbeidsgiverdag: LocalDate? = 31.desember(2017)) =
        Oppdrag(ORGNUMMER, SykepengerRefusjon, sisteArbeidsgiverdag = sisteArbeidsgiverdag)

    private val Oppdrag.endringskode get() = this.get<Endringskode>("endringskode")

    private val Utbetalingslinje.endringskode get() = this.get<Endringskode>("endringskode")

    private val Utbetalingslinje.id get() = this.get<Int>("delytelseId")

    private val Utbetalingslinje.refId get() = this.get<Int?>("refDelytelseId")

    private val Oppdrag.fagsystemId get() = this.get<String>("fagsystemId")

    private val Utbetalingslinje.datoStatusFom get() = this.get<LocalDate?>("datoStatusFom")

    private fun assertUtbetalinger(expected: Oppdrag, actual: Oppdrag) {
        assertEquals(expected.size, actual.size, "Utbetalingslinjer er i forskjellige størrelser")
        (expected zip actual).forEach { (a, b) ->
            assertEquals(a.fom, b.fom, "fom stemmer ikke overens")
            assertEquals(a.tom, b.tom, "tom stemmer ikke overens")
            assertEquals(a.beløp, b.beløp, "dagsats stemmer ikke overens")
            assertEquals(a.grad, b.grad, "grad stemmer ikke overens")
        }
    }

    private fun linjer(vararg linjer: TestUtbetalingslinje) =
        Oppdrag(ORGNUMMER, SykepengerRefusjon, linjer.map { it.asUtbetalingslinje() }, sisteArbeidsgiverdag = 31.desember(2017)).also { oppdrag ->
            oppdrag.zipWithNext { a, b -> b.linkTo(a) }
            oppdrag.forEach { if(it.refId != null) it.refFagsystemId = oppdrag.fagsystemId() }
        }

    private fun linjer(vararg linjer: Utbetalingslinje) =
        Oppdrag(ORGNUMMER, SykepengerRefusjon, linjer.toList(), sisteArbeidsgiverdag = 31.desember(2017)).also { oppdrag ->
            oppdrag.zipWithNext { a, b -> b.linkTo(a) }
            oppdrag.forEach { if(it.refId != null) it.refFagsystemId = oppdrag.fagsystemId() }
        }

    private fun assertLink(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje) {
        assertEquals(tidligere.id + 1, nåværende.id)
        assertEquals(tidligere.id, nåværende.refId)
    }

    private inner class TestUtbetalingslinje(
        private val fom: LocalDate,
        private val tom: LocalDate
    ) {
        private var grad: Double = 100.0
        private var dagsats = 1200

        internal infix fun grad(percentage: Number): TestUtbetalingslinje {
            grad = percentage.toDouble()
            return this
        }

        internal infix fun dagsats(amount: Int): TestUtbetalingslinje {
            dagsats = amount
            return this
        }

        internal infix fun forskjell(other: Oppdrag) = this.asUtbetalingslinjer() - other

        internal fun asUtbetalingslinje() = Utbetalingslinje(fom, tom, dagsats, dagsats, grad)

        private fun asUtbetalingslinjer() = linjer(asUtbetalingslinje())
    }

    private infix fun LocalDate.to(other: LocalDate) = TestUtbetalingslinje(this, other)
}

