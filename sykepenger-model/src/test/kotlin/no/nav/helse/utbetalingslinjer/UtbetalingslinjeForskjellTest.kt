package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Endringskode.*
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

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
    fun periode() {
        val oppdrag1 = linjer(1.januar to 5.januar)
        val oppdrag2 = linjer(1.februar to 28.februar)
        assertEquals(1.januar til 28.februar, Oppdrag.periode(oppdrag1, oppdrag2))
    }

    @Test
    fun `periode med tomt oppdrag`() {
        val oppdrag1 = linjer(1.januar to 5.januar)
        val oppdrag2 = linjer()
        assertEquals(1.januar til 5.januar, Oppdrag.periode(oppdrag1, oppdrag2))
    }

    @Test
    fun `periode med bare tomme oppdrag`() {
        assertEquals(LocalDate.MIN til LocalDate.MAX, Oppdrag.periode())
        assertEquals(LocalDate.MIN til LocalDate.MAX, Oppdrag.periode(linjer()))
        assertEquals(LocalDate.MIN til LocalDate.MAX, Oppdrag.periode(linjer(), linjer()))
    }

    @Test
    fun stønadsdager() {
        (1.januar to 5.januar dagsats 500).asUtbetalingslinje().also {
            assertEquals(5, it.stønadsdager())
            assertEquals(2500, it.totalbeløp())
        }
        (1.januar to 7.januar dagsats 500).asUtbetalingslinje().also {
            assertEquals(5, it.stønadsdager())
            assertEquals(2500, it.totalbeløp())
        }
        (1.januar to 8.januar dagsats 500).asUtbetalingslinje().also {
            assertEquals(6, it.stønadsdager())
            assertEquals(3000, it.totalbeløp())
        }
        (1.januar to 5.januar dagsats 500).asUtbetalingslinje().opphørslinje(1.januar).also {
            assertEquals(0, it.stønadsdager())
            assertEquals(0, it.totalbeløp())
        }
    }

    @Test
    fun `helt separate utbetalingslinjer`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(5.februar to 9.februar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(5.februar to 9.februar), actual)
        assertEquals(5, actual.stønadsdager())
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
        assertUtbetalinger(linjer(1.januar to 9.februar endringskode NY pekerPå original.last()), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(30, actual.stønadsdager())
        assertEquals(ENDR, actual.endringskode)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `ny tom`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 13.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 13.januar endrer original.last()), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(10, actual.stønadsdager())
        assertEquals(ENDR, actual.endringskode)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `trekke periode tilbake`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 13.januar)
        val actual = recalculated - original
        val revised = original - actual

        assertUtbetalinger(linjer(1.januar to 5.januar endrer original.last()), revised)
    }

    @Test
    fun `ingen endringer`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 5.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 5.januar endringskode UEND), actual)
        assertEquals(5, actual.stønadsdager())
    }

    @Test
    fun `utvide tom`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 6.januar)
        val actual = recalculated - original

        assertUtbetalinger(linjer(1.januar to 6.januar endrer original.last()), actual)
    }

    @Test
    fun `utvide fom`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(2.januar to 5.januar)
        val actual = recalculated - original

        assertUtbetalinger(linjer(
            1.januar to 5.januar endrer original.last() opphører 1.januar,
            2.januar to 5.januar endringskode NY pekerPå actual[0]
        ), actual)
    }

    @Test
    fun `utvide fom og trekke tilbake`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(2.januar to 4.januar)
        val actual = recalculated - original

        assertUtbetalinger(linjer(
            1.januar to 5.januar endrer original.last() opphører 1.januar,
            2.januar to 4.januar endringskode NY pekerPå actual[0]
        ), actual)
    }

    @Test
    fun `slå sammen til én linje`() {
        val original = linjer(1.januar to 2.januar, 4.januar to 5.januar)
        val recalculated = linjer(1.januar to 5.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            1.januar to 5.januar endringskode NY pekerPå original.last()
        ), actual)
    }

    @Test
    fun `flytter FOM til en senere dag `() {
        val original = linjer(1.januar to 2.januar, 4.januar to 5.januar)
        val recalculated = linjer(2.januar to 5.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            4.januar to 5.januar endrer original.last() opphører 1.januar,
            2.januar to 5.januar endringskode NY pekerPå original.last()
        ), actual)
    }

    @Test
    fun `slå sammen til én linje - og trekke tilbake`() {
        val original = linjer(1.januar to 2.januar, 4.januar to 5.januar)
        val recalculated = linjer(1.januar to 4.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            1.januar to 4.januar endringskode NY pekerPå original.last()
        ), actual)
    }

    @Test
    fun `slå sammen til én linje - og trekke tilbake 2`() {
        val original = linjer(1.januar to 2.januar, 4.januar to 5.januar)
        val recalculated = linjer(1.januar to 1.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            1.januar to 1.januar endringskode NY pekerPå original.last()
        ), actual)
    }

    @Test
    fun `trekke tilbake siste linje`() {
        val original = linjer(1.januar to 2.januar, 4.januar to 5.januar)
        val recalculated = linjer(1.januar to 2.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            1.januar to 2.januar endringskode NY pekerPå original.last()
        ), actual)
    }

    @Test
    fun `bygg videre på opphørt`() {
        val original = linjer(1.januar to 2.januar)
        val recalculated = linjer(other = original)
        val deleted = recalculated - original
        val revised = linjer(2.januar to 5.januar, other = deleted)
        val actual = revised - deleted
        assertUtbetalinger(linjer(
            2.januar to 5.januar endringskode NY pekerPå original.last()
        ), actual)
    }

    @Test
    fun `har egentlig flyttet bakover`() {
        val original = linjer(1.januar to 5.januar, 14.januar to 20.januar)
        val recalculated = linjer(4.januar to 5.januar, 14.januar to 20.januar)
        val revised = recalculated - original
        val amended = linjer(1.januar to 2.januar, 14.januar to 20.januar)
        val actual = amended - revised
        assertUtbetalinger(linjer(
            14.januar to 20.januar endrer original.first() opphører 1.januar,
            4.januar to 5.januar endringskode NY pekerPå original.last(),
            14.januar to 20.januar endringskode NY pekerPå revised[1]
        ), revised)
        assertUtbetalinger(linjer(
            1.januar to 2.januar endringskode NY pekerPå revised.last(),
            14.januar to 20.januar endringskode NY pekerPå actual[0]
        ), actual)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
        assertTrue(aktivitetslogg.warn().map(Aktivitetslogg.Aktivitet::toString).any { it.contains(WARN_OPPDRAG_FOM_ENDRET) })
    }

    @Test
    fun `endre fom på siste linje`() {
        val original = linjer(24.januar to 29.januar, 30.januar to 3.februar)
        val recalculated =  linjer(24.januar to 29.januar, 1.februar to 3.februar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            24.januar to 29.januar endringskode UEND,
            30.januar to 3.februar endrer original.last() opphører 30.januar,
            1.februar to 3.februar endringskode NY pekerPå actual[1]
        ), actual)
    }

    @Test
    fun `endrer fom på siste linje når forrige oppdrag har endret linje med opphør`() {
        val oppdrag1 = linjer(1.januar to 18.januar)
        val originalUtvidet = linjer(1.januar to 18.januar, 20.januar to 26.januar)
        val oppdrag2 = originalUtvidet - oppdrag1

        val oppdrag2EndretSåUtvidet = linjer(1.januar to 18.januar, 24.januar to 29.januar, 30.januar to 3.februar)
        val oppdrag3 = oppdrag2EndretSåUtvidet - oppdrag2

        val oppdrag3EndretSisteFom = linjer(1.januar to 18.januar, 24.januar to 29.januar, 1.februar to 3.februar)
        val oppdrag4 = oppdrag3EndretSisteFom - oppdrag3

        assertUtbetalinger(linjer(
            1.januar to 18.januar endringskode UEND,
            20.januar to 26.januar endrer oppdrag2.last() opphører 20.januar,
            24.januar to 29.januar endringskode NY pekerPå oppdrag3[1],
            30.januar to 3.februar endringskode NY pekerPå oppdrag3[2]
        ), oppdrag3)

        assertUtbetalinger(linjer(
            1.januar to 18.januar endringskode UEND,
            24.januar to 29.januar endringskode UEND pekerPå oppdrag2.last(),
            30.januar to 3.februar endrer oppdrag3.last() opphører 30.januar,
            1.februar to 3.februar endringskode NY pekerPå oppdrag4[2]
        ), oppdrag4)
    }

    @Test
    fun `splitte opp linjer - ikke utvide oppdragets lengde`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 2.januar, 4.januar to 5.januar)
        val actual = recalculated - original

        assertUtbetalinger(linjer(
            1.januar to 2.januar endrer original.last(),
            4.januar to 5.januar endringskode NY pekerPå actual[0]
        ), actual)
    }

    @Test
    fun `gjeninnfører en opphørt periode`() {
        val original = linjer(1.januar to 3.januar, 4.januar to 12.januar grad 50)
        val recalculated = linjer(2.januar to 3.januar, 4.januar to 12.januar grad 50)
        val actual = recalculated - original
        val gjeninnført = linjer(4.januar to 12.januar grad 50)
        val revised = gjeninnført - actual

        assertUtbetalinger(linjer(
            4.januar to 12.januar grad 50 endrer original.last() opphører 1.januar,
            2.januar to 3.januar endringskode NY pekerPå original.last(),
            4.januar to 12.januar grad 50 endringskode NY pekerPå actual[1]
        ), actual)
        assertUtbetalinger(linjer(
            4.januar to 12.januar grad 50 endrer actual.last() opphører 2.januar,
            4.januar to 12.januar grad 50 endringskode NY pekerPå actual.last()
        ), revised)
        assertEquals(original.fagsystemId, revised.fagsystemId)
        assertEquals(ENDR, revised.endringskode)
    }

    @Test
    fun `byggere videre på en opphørt periode`() {
        val original = linjer(1.januar to 3.januar, 4.januar to 12.januar grad 50)
        val recalculated = linjer(2.januar to 3.januar, 4.januar to 12.januar grad 50)
        val actual = recalculated - original
        val gjeninnført = linjer(2.januar to 3.januar, 4.januar to 20.januar grad 50)
        val revised = gjeninnført - actual

        assertUtbetalinger(linjer(
            4.januar to 12.januar grad 50 endrer original.last() opphører 1.januar,
            2.januar to 3.januar endringskode NY pekerPå original.last(),
            4.januar to 12.januar grad 50 endringskode NY pekerPå actual[1]
        ), actual)
        assertUtbetalinger(linjer(
            2.januar to 3.januar endringskode UEND pekerPå original.last(),
            4.januar to 20.januar grad 50 endrer actual.last()
        ), revised)
        assertEquals(original.fagsystemId, revised.fagsystemId)
        assertEquals(ENDR, revised.endringskode)
    }

    @Test
    fun `trekke periode tilbake med opphold`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 5.januar, 8.januar to 13.januar)
        val actual = recalculated - original
        val revised = original - actual

        assertUtbetalinger(linjer(
            1.januar to 5.januar endringskode NY pekerPå actual.last()
        ), revised)
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
            1.januar to 5.januar endringskode UEND,
            8.januar to 13.januar endringskode NY pekerPå original.last()
        ), actual)
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
        val fremtrukket = linjer(
            1.januar to 5.januar,
            8.januar to 13.januar,
            15.januar to 25.januar
        )
        val actual = fremtrukket - revised
        assertUtbetalinger(linjer(
            1.januar to 5.januar endringskode UEND,
            8.januar to 13.januar endringskode UEND pekerPå original.last(),
            15.januar to 25.januar endringskode NY pekerPå actual[1]
        ), actual)
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
            1.januar to 5.januar endringskode UEND,
            8.januar to 25.januar endrer revised.last()
        ), actual)
    }

    @Test
    fun `trekke periode tilbake potpourri 1`() {
        val original = linjer(1.januar to 5.januar)
        val intermediate = linjer(1.januar to 5.januar, 8.januar to 13.januar)
        val extended = intermediate - original
        val recalculated = linjer(1.januar to 5.januar, 8.januar to 20.januar, 28.januar to 5.februar)
        val actual = recalculated - extended
        val revised = original - actual
        assertUtbetalinger(linjer(1.januar to 5.januar endringskode NY pekerPå actual.last()), revised)
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

        assertUtbetalinger(linjer(
            1.januar to 5.januar endringskode UEND,
            8.januar to 13.januar endringskode NY pekerPå actual.last()
        ), revised)
    }

    @Test
    fun `trekke periode frem potpourri 1`() {
        val original = linjer(1.januar to 5.januar, 8.januar to 20.januar, 23.januar to 26.januar, 28.januar to 5.februar)
        val tilbakeført = linjer(1.januar to 6.januar, 8.januar to 13.januar)
        val revised = tilbakeført - original
        assertUtbetalinger(linjer(
            1.januar to 6.januar endringskode NY pekerPå original.last(),
            8.januar to 13.januar endringskode NY pekerPå revised[0]
        ), revised)
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

        assertUtbetalinger(linjer(
            1.januar to 5.januar endringskode UEND,
            8.januar to 13.januar endringskode UEND pekerPå revised[0],
            23.januar to 26.januar endrer actual.last(),
            1.februar to 5.februar endringskode NY pekerPå revised[2]
        ), revised)

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
        val original = linjer(1.januar to 5.januar dagsats 500)
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

        assertUtbetalinger(linjer(
            1.januar to 5.januar endringskode UEND,
            6.januar to 17.januar grad 50 endringskode NY pekerPå original.last(),
            18.januar to 19.januar grad 80 endringskode NY pekerPå actual[1],
            1.februar to 9.februar endringskode NY pekerPå actual[2]
        ), actual)
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
        assertUtbetalinger(linjer(
            1.januar to 5.januar endringskode UEND,
            6.januar to 17.januar grad 50 endringskode NY pekerPå original.last(),
            18.januar to 19.januar grad 80 endringskode NY pekerPå actual[1],
            1.februar to 9.februar endringskode NY pekerPå actual[2]
        ), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `fom endres`() {
        val original = linjer(5.januar to 10.januar)
        val recalculated = linjer(1.januar to 10.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 10.januar endringskode NY pekerPå original.last()), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `ingen overlapp og ulik fagsystemId`() {
        val original = linjer(5.januar to 10.januar)
        val recalculated = linjer(1.januar to 3.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 3.januar endringskode NY), actual)
        assertNotEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(NY, actual.endringskode)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `fom og tom ulik og samme fagsystemId`() {
        val original = linjer(5.januar to 10.januar)
        val recalculated = linjer(1.januar to 3.januar, other = original)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 3.januar endringskode NY pekerPå original.last()), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
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
            6.januar to 19.januar grad 50, // extend tom
            20.januar to 26.januar
        )
        val actual = new - original
        assertUtbetalinger(linjer(
            1.januar to 5.januar endringskode UEND,
            6.januar to 19.januar grad 50 endringskode NY pekerPå original.last(), // extend tom
            20.januar to 26.januar endringskode NY pekerPå actual[1]
        ), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `slett nøyaktig en periode`() {
        val original = linjer(1.januar to 5.januar, 6.januar to 12.januar grad 50)
        val recalculated = linjer(6.januar to 12.januar grad 50)
        val actual = recalculated - original
        assertUtbetalinger(
            linjer(
                6.januar to 12.januar grad 50 endrer original.last() opphører 1.januar,
                6.januar to 12.januar grad 50 endringskode NY pekerPå actual[0]
            ), actual
        )
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `original andre periode samsvarer delvis med ny`() {
        val original = linjer(1.januar to 5.januar, 6.januar to 12.januar grad 50)
        val recalculated = linjer(6.januar to 19.januar grad 50)
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            6.januar to 12.januar grad 50 endrer original.last() opphører 1.januar,
            6.januar to 19.januar grad 50 endringskode NY pekerPå actual[0]
        ), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `perioden min starter midt i en original periode`() {
        val original = linjer(1.januar to 3.januar, 4.januar to 12.januar grad 50)
        val recalculated = linjer(6.januar to 19.januar grad 50)
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            4.januar to 12.januar grad 50 endrer original.last() opphører 1.januar,
            6.januar to 19.januar grad 50 endringskode NY pekerPå original.last()
        ), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `ny er tom`() {
        val original = linjer(1.januar to 3.januar, 4.januar to 12.januar grad 50)
        val recalculated = tomtOppdrag()
        val actual = recalculated - original
        assertUtbetalinger(tomtOppdrag(), actual)
        assertNotEquals(original.fagsystemId, actual.fagsystemId)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `originalen har hale å slette`() {
        val original = linjer(1.januar to 12.januar)
        val recalculated = linjer(1.januar to 5.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 5.januar endrer original.last()), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `Sletting hvor alt blir sendt på nytt`() {
        val original = linjer(1.januar to 12.januar)
        val recalculated = linjer(1.januar to 5.januar, 7.januar to 10.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            1.januar to 5.januar endrer original.last(),
            7.januar to 10.januar endringskode NY pekerPå actual[0]
        ), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
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
            1.januar to 5.januar endringskode UEND,
            8.januar to 10.januar endrer original.last()
        ), actual)

        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test fun `slette fra hode og hale av originalen`() {
        val original = linjer(1.januar to 12.januar)
        val recalculated = linjer(3.januar to 9.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(
            1.januar to 12.januar endrer original.last() opphører 1.januar,
            3.januar to 9.januar endringskode NY pekerPå actual[0]
        ), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
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
            20.januar to 31.januar endrer original.last() opphører 1.januar,
            6.januar to 19.januar grad 50 endringskode NY pekerPå actual[0],
            20.januar to 26.januar endringskode NY pekerPå actual[1]
        ), actual)

        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
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
        val new = tomtOppdrag(original.fagsystemId())
        val actual = new - original
        assertUtbetalinger(
            linjer(20.januar to 31.januar endrer original.last() opphører 1.januar),
            actual
        )

        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(0, actual.stønadsdager())
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
        val new = tomtOppdrag(original.fagsystemId())

        val actual = new - original

        assertNotNull(actual[0].datoStatusFom)
        assertNotEquals(original[0].hashCode(), actual[0].hashCode())
    }

    private fun tomtOppdrag(fagsystemId: String = genererUtbetalingsreferanse(UUID.randomUUID())) =
        Oppdrag(ORGNUMMER, SykepengerRefusjon, fagsystemId = fagsystemId, sisteArbeidsgiverdag = null)

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
            assertEquals(a.datoStatusFom, b.datoStatusFom)
            assertEquals(a.endringskode, b.endringskode) { "endringskode ${b.endringskode} matcher ikke forventet ${a.endringskode}" }
            assertEquals(a.id, b.id) { "delytelseid ${b.id} matcher ikke forventet ${a.id}"}
            assertEquals(a.refId, b.refId) { "refdelytelseid ${b.refId} matcher ikke forventet ${a.refId}"}
        }
    }

    private fun linjer(vararg linjer: TestUtbetalingslinje, other: Oppdrag? = null) =
        Oppdrag(ORGNUMMER, SykepengerRefusjon, linjer.map { it.asUtbetalingslinje() }, fagsystemId = other?.fagsystemId() ?: genererUtbetalingsreferanse(UUID.randomUUID()), sisteArbeidsgiverdag = 31.desember(2017)).also { oppdrag ->
            oppdrag.forEach { if(it.refId != null) it.refFagsystemId = oppdrag.fagsystemId() }
        }

    private fun linjer(vararg linjer: Utbetalingslinje) =
        Oppdrag(ORGNUMMER, SykepengerRefusjon, linjer.toList(), sisteArbeidsgiverdag = 31.desember(2017)).also { oppdrag ->
            oppdrag.zipWithNext { a, b -> b.kobleTil(a) }
            oppdrag.forEach { if(it.refId != null) it.refFagsystemId = oppdrag.fagsystemId() }
        }

    private inner class TestUtbetalingslinje(
        private val fom: LocalDate,
        private val tom: LocalDate
    ) {
        private var delytelseId = 1
        private var endringskode: Endringskode = NY
        private var grad: Double = 100.0
        private var dagsats = 1200
        private var datoStatusFom: LocalDate? = null
        private var refDelytelseId: Int? = null

        internal infix fun grad(percentage: Number): TestUtbetalingslinje {
            grad = percentage.toDouble()
            return this
        }

        internal infix fun dagsats(amount: Int): TestUtbetalingslinje {
            dagsats = amount
            return this
        }

        internal infix fun endringskode(kode: Endringskode): TestUtbetalingslinje {
            endringskode = kode
            return this
        }

        internal infix fun opphører(dato: LocalDate): TestUtbetalingslinje {
            datoStatusFom = dato
            return this
        }

        internal infix fun pekerPå(id: Int): TestUtbetalingslinje {
            refDelytelseId = id
            return this
        }

        internal infix fun pekerPå(other: Utbetalingslinje): TestUtbetalingslinje {
            delytelseId = other.id + 1
            refDelytelseId = other.id
            return this
        }

        internal infix fun endrer(other: Utbetalingslinje): TestUtbetalingslinje {
            endringskode(ENDR)
            delytelseId = other.id
            return this
        }

        internal infix fun forskjell(other: Oppdrag) = this.asUtbetalingslinjer() - other

        internal fun asUtbetalingslinje() = Utbetalingslinje(fom, tom, Satstype.DAG, dagsats, dagsats, grad, endringskode = endringskode, datoStatusFom = datoStatusFom, refDelytelseId = refDelytelseId, delytelseId = delytelseId)

        private fun asUtbetalingslinjer() = linjer(asUtbetalingslinje())
    }

    private infix fun LocalDate.to(other: LocalDate) = TestUtbetalingslinje(this, other)
}

