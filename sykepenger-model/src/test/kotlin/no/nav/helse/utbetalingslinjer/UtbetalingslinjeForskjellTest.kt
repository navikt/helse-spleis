package no.nav.helse.utbetalingslinjer

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.roundToInt
import no.nav.helse.august
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.FagområdeDto
import no.nav.helse.dto.deserialisering.OppdragInnDto
import no.nav.helse.dto.deserialisering.UtbetalingslinjeInnDto
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.utbetalingslinjer.Endringskode.ENDR
import no.nav.helse.utbetalingslinjer.Endringskode.NY
import no.nav.helse.utbetalingslinjer.Endringskode.UEND
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingslinjeForskjellTest {

    private companion object {
        private const val ORGNUMMER = "987654321"
    }

    private lateinit var aktivitetslogg: Aktivitetslogg
    private operator fun Oppdrag.minus(other: Oppdrag) = this.minus(other, aktivitetslogg)

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `periode uten siste arbeidsgiverdag`() {
        val oppdrag1 = linjer(2.januar to 5.januar)
        val oppdrag2 = linjer(1.februar to 28.februar)
        assertEquals(2.januar til 28.februar, Oppdrag.periode(oppdrag1, oppdrag2))
    }

    @Test
    fun `periode med siste arbeidsgiverdag`() {
        val oppdrag1 = linjer(2.januar to 5.januar)
        val oppdrag2 = linjer(1.februar to 28.februar)
        assertEquals(2.januar til 28.februar, Oppdrag.periode(oppdrag1, oppdrag2))
    }

    @Test
    fun `periode med tomt oppdrag`() {
        val oppdrag1 = linjer(2.januar to 5.januar)
        val oppdrag2 = linjer()
        val oppdrag3 = linjer()
        assertEquals(2.januar til 5.januar, Oppdrag.periode(oppdrag1, oppdrag2))
        assertEquals(2.januar til 5.januar, Oppdrag.periode(oppdrag1, oppdrag3))
        assertNull(Oppdrag.periode(oppdrag2, oppdrag3))
    }

    @Test
    fun `periode med bare tomme oppdrag`() {
        assertNull(Oppdrag.periode())
        assertNull(Oppdrag.periode(linjer()))
        assertNull(Oppdrag.periode(linjer(), linjer()))
    }

    @Test
    fun hendelsemap() {
        (1.januar to 5.januar dagsats 500).asUtbetalingslinje().behovdetaljer().also { map ->
            assertEquals("${1.januar}", map.getValue("fom"))
            assertEquals("${5.januar}", map.getValue("tom"))
            assertEquals("DAG", map.getValue("satstype"))
            assertEquals(500, map.getValue("sats"))
            assertEquals(2500, map.getValue("totalbeløp"))
            assertEquals(100.0, map.getValue("grad"))
            assertEquals(5, map.getValue("stønadsdager"))
            assertEquals("NY", map.getValue("endringskode"))
            assertEquals(1, map.getValue("delytelseId"))
            assertNull(map.getValue("refDelytelseId"))
            assertNull(map.getValue("refFagsystemId"))
        }
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
    fun `stønadsdager teller ukedager`() {
        val oppdrag = linjer(1.januar to 7.januar)
        assertEquals(5, oppdrag.stønadsdager())
    }

    @Test
    fun `stønadsdager teller uendrede linjer`() {
        val original = linjer(1.januar to 7.januar)
        val recalculated = linjer(1.januar to 7.januar)
        val actual = recalculated - original
        assertEquals(5, actual.stønadsdager())
    }

    @Test
    fun `stønadsdager teller ikke opphørslinjer`() {
        val original = linjer(1.januar to 7.januar)
        val deleted = linjer()
        val actual = deleted - original
        assertEquals(0, actual.stønadsdager())
    }

    @Test
    fun `teller unike dager`() {
        val oppdrag1 = linjer(1.januar to 7.januar)
        val oppdrag2 = linjer(4.januar to 9.januar)
        assertEquals(7, Oppdrag.stønadsdager(oppdrag1, oppdrag2))
    }

    @Test
    fun `teller overlappende dager som opphører i det ene oppdraget`() {
        val arbeidsgiverOppdrag = linjer(1.januar to 7.januar)
        val personOppdrag = linjer(4.januar to 9.januar)
        val deleted = linjer()
        val opphørtPersonoppdrag = deleted - personOppdrag
        assertEquals(5, Oppdrag.stønadsdager(arbeidsgiverOppdrag, opphørtPersonoppdrag))
    }

    @Test
    fun `teller stønadsdager i et oppdrags om kjøres frem igjen`() {
        val arbeidsgiverOppdrag = linjer(1.januar to 7.januar)
        val personOppdrag = linjer(4.januar to 9.januar)
        val deleted = linjer()
        val opphørtPersonoppdrag = deleted - personOppdrag
        val personoppdragKjørtFremIgjen = linjer(4.januar to 9.januar)
        assertEquals(7, Oppdrag.stønadsdager(arbeidsgiverOppdrag, personoppdragKjørtFremIgjen - opphørtPersonoppdrag))
    }

    @Test
    fun `helt separate utbetalingslinjer`() {
        val original = linjer(2.januar to 5.januar)
        val recalculated = linjer(5.februar to 9.februar)
        val actual = recalculated - original
        val linje1 = 2.januar to 5.januar endrer original.last() opphører 2.januar
        val linje2 = 5.februar to 9.februar pekerPå linje1
        assertUtbetalinger(
            linjer(
                linje1,
                linje2
            ), actual
        )
        assertEquals(5, actual.stønadsdager())
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
    }

    @Test
    fun `fjerner en dag i starten av en linje i midten av et oppdrag`() {
        val linje1 = 1.januar to 7.januar grad 80
        val linje2 = 8.januar to 20.januar grad 100 pekerPå linje1
        val linje3 = 21.januar to 31.januar grad 90 pekerPå linje2
        val original = linjer(linje1, linje2, linje3)
        val fjernetEnDag = linjer(
            1.januar to 7.januar grad 80,
            9.januar to 20.januar grad 100,
            21.januar to 25.januar grad 90,
            26.januar to 31.januar grad 100
        )
        val actual = fjernetEnDag - original
        val linje4 = 21.januar to 31.januar grad 90 endrer original.last() opphører 8.januar
        val linje5 = 9.januar to 20.januar grad 100 pekerPå linje4
        val linje6 = 21.januar to 25.januar grad 90 pekerPå linje5
        val linje7 = 26.januar to 31.januar grad 100 pekerPå linje6
        val expected = linjer(linje1.endringskode(UEND), linje4, linje5, linje6, linje7)
        assertUtbetalinger(expected, actual)
    }

    @Test
    fun `fjerner en dag i midten av en linje i midten av et oppdrag`() {
        val linje1 = 1.januar to 7.januar grad 80
        val linje2 = 8.januar to 20.januar grad 100 pekerPå linje1
        val linje3 = 21.januar to 31.januar grad 90 pekerPå linje2
        val original = linjer(linje1, linje2, linje3)
        val fjernetEnDag = linjer(
            1.januar to 7.januar grad 80,
            8.januar to 9.januar grad 100,
            11.januar to 20.januar grad 100,
            21.januar to 31.januar grad 90
        )
        val actual = fjernetEnDag - original
        val linje4 = 8.januar to 9.januar grad 100 pekerPå linje3
        val linje5 = 11.januar to 20.januar grad 100 pekerPå linje4
        val linje6 = 21.januar to 31.januar grad 90 pekerPå linje5
        val expected = linjer(linje1.endringskode(UEND), linje4, linje5, linje6)
        assertUtbetalinger(expected, actual)
    }

    @Test
    fun `fjerner en dag i slutten av en linje i midten av et oppdrag`() {
        val linje1 = 1.januar to 7.januar grad 80
        val linje2 = 8.januar to 20.januar grad 100 pekerPå linje1
        val linje3 = 21.januar to 31.januar grad 90 pekerPå linje2
        val original = linjer(linje1, linje2, linje3)
        val fjernetEnDag = linjer(
            1.januar to 7.januar grad 80,
            8.januar to 19.januar grad 100,
            21.januar to 31.januar grad 90
        )
        val actual = fjernetEnDag - original
        val linje4 = 8.januar to 19.januar grad 100 pekerPå linje3
        val linje5 = 21.januar to 31.januar grad 90 pekerPå linje4
        val expected = linjer(linje1.endringskode(UEND), linje4, linje5)
        assertUtbetalinger(expected, actual)
    }

    @Test
    fun `kjeder seg på forrige oppdrag når sisteArbeidsgiverdag er lik`() {
        val original = linjer(2.januar to 5.januar)
        val recalculated = linjer(*emptyArray<TestUtbetalingslinje>())
        val actual = recalculated - original
        assertUtbetalinger(linjer(2.januar to 5.januar endringskode ENDR opphører 2.januar), actual)
        assertEquals(0, actual.stønadsdager())
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `overtar fagsystemId fra et tomt oppdrag`() {
        val original = tomtOppdrag(sisteArbeidsgiverdag = 4.februar)
        val recalculated = linjer(5.februar to 9.februar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(5.februar to 9.februar), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(NY, actual.endringskode)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `overtar fagsystemId fra et tomt oppdrag når siste arbeidsgiverdag er ulik`() {
        val original = tomtOppdrag(sisteArbeidsgiverdag = 1.mars)
        val recalculated = linjer(5.februar to 9.februar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(5.februar to 9.februar), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(NY, actual.endringskode)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
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
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `trekke periode tilbake`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 13.januar)
        val actual = recalculated - original
        val revised = original - actual
        assertEquals(ENDR, actual.endringskode)
        assertUtbetalinger(linjer(1.januar to 5.januar endrer original.last()), revised)
    }

    @Test
    fun `ingen endringer`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 5.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 5.januar endringskode UEND), actual)
        assertEquals(UEND, actual.endringskode)
        assertEquals(5, actual.stønadsdager())
    }

    @Test
    fun `utvide tom`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 6.januar)
        val actual = recalculated - original
        assertEquals(ENDR, actual.endringskode)
        assertUtbetalinger(linjer(1.januar to 6.januar endrer original.last()), actual)
    }

    @Test
    fun `utvide fom`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(2.januar to 5.januar)
        val actual = recalculated - original
        assertEquals(ENDR, actual.endringskode)
        assertUtbetalinger(
            linjer(
                1.januar to 5.januar endrer original.last() opphører 1.januar,
                2.januar to 5.januar endringskode NY pekerPå actual[0]
            ), actual
        )
    }

    @Test
    fun `utvide fom og trekke tilbake`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(2.januar to 4.januar)
        val actual = recalculated - original
        assertEquals(ENDR, actual.endringskode)
        assertUtbetalinger(
            linjer(
                1.januar to 5.januar endrer original.last() opphører 1.januar,
                2.januar to 4.januar endringskode NY pekerPå actual[0]
            ), actual
        )
    }

    @Test
    fun `slå sammen til én linje`() {
        val original = linjer(1.januar to 2.januar, 4.januar to 5.januar)
        val recalculated = linjer(1.januar to 5.januar)
        val actual = recalculated - original
        assertEquals(ENDR, actual.endringskode)
        assertUtbetalinger(
            linjer(
                1.januar to 5.januar endringskode NY pekerPå original.last()
            ), actual
        )
    }

    @Test
    fun `flytter FOM til en senere dag `() {
        val original = linjer(1.januar to 2.januar, 4.januar to 5.januar)
        val recalculated = linjer(2.januar to 5.januar)
        val actual = recalculated - original
        assertEquals(ENDR, actual.endringskode)
        assertUtbetalinger(
            linjer(
                4.januar to 5.januar endrer original.last() opphører 1.januar,
                2.januar to 5.januar endringskode NY pekerPå original.last()
            ), actual
        )
    }

    @Test
    fun `slå sammen til én linje - og trekke tilbake`() {
        val original = linjer(1.januar to 2.januar, 4.januar to 5.januar)
        val recalculated = linjer(1.januar to 4.januar)
        val actual = recalculated - original
        assertUtbetalinger(
            linjer(
                1.januar to 4.januar endringskode NY pekerPå original.last()
            ), actual
        )
    }

    @Test
    fun `slå sammen til én linje - og trekke tilbake 2`() {
        val original = linjer(1.januar to 2.januar, 4.januar to 5.januar)
        val recalculated = linjer(1.januar to 1.januar)
        val actual = recalculated - original
        assertUtbetalinger(
            linjer(
                1.januar to 1.januar endringskode NY pekerPå original.last()
            ), actual
        )
    }

    @Test
    fun `trekke tilbake siste linje`() {
        val original = linjer(1.januar to 2.januar, 4.januar to 5.januar)
        val recalculated = linjer(1.januar to 2.januar)
        val actual = recalculated - original
        assertUtbetalinger(
            linjer(
                1.januar to 2.januar endringskode NY pekerPå original.last()
            ), actual
        )
    }

    @Test
    fun `bygg videre på opphørt`() {
        val original = linjer(1.januar to 2.januar)
        val recalculated = linjer(other = original)
        val deleted = recalculated - original
        val revised = linjer(2.januar to 5.januar, other = deleted)
        val actual = revised - deleted
        assertUtbetalinger(
            linjer(
                2.januar to 5.januar endringskode NY pekerPå original.last()
            ), actual
        )
    }

    @Test
    fun `tomt oppdrag bygg videre på opphørt`() {
        val original = linjer(1.januar to 2.januar, 5.januar to 10.januar)
        val recalculated = linjer(other = original)
        val deleted = recalculated - original
        val revised = linjer(other = deleted)
        val actual = revised - deleted
        assertUtbetalinger(
            linjer(
                5.januar to 10.januar endrer original.first() opphører 1.januar endringskode UEND
            ), actual
        )
    }

    @Test
    fun `opphøre oppdrag som bygget videre på opphør`() {
        val original = linjer(1.januar to 10.januar)
        val recalculated = linjer(8.januar to 10.januar, other = original)
        val deleted = recalculated - original
        val revised = linjer()
        val actual = revised - deleted
        assertUtbetalinger(
            linjer(
                8.januar to 10.januar endrer actual.last() opphører 8.januar
            ), actual
        )
    }

    @Test
    fun `har egentlig flyttet bakover`() {
        val original = linjer(1.januar to 5.januar, 14.januar to 20.januar)
        val recalculated = linjer(4.januar to 5.januar, 14.januar to 20.januar)
        val revised = recalculated - original
        val amended = linjer(1.januar to 2.januar, 14.januar to 20.januar)
        val actual = amended - revised
        assertUtbetalinger(
            linjer(
                14.januar to 20.januar endrer original.first() opphører 1.januar,
                4.januar to 5.januar endringskode NY pekerPå original.last(),
                14.januar to 20.januar endringskode NY pekerPå revised[1]
            ), revised
        )
        assertUtbetalinger(
            linjer(
                1.januar to 2.januar endringskode NY pekerPå revised.last(),
                14.januar to 20.januar endringskode NY pekerPå actual[0]
            ), actual
        )
    }

    @Test
    fun `endre fom på siste linje`() {
        val original = linjer(24.januar to 29.januar, 30.januar to 3.februar)
        val recalculated = linjer(24.januar to 29.januar, 1.februar to 3.februar)
        val actual = recalculated - original
        assertUtbetalinger(
            linjer(
                24.januar to 29.januar endringskode UEND,
                30.januar to 3.februar endrer original.last() opphører 30.januar,
                1.februar to 3.februar endringskode NY pekerPå actual[1]
            ), actual
        )
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

        assertUtbetalinger(
            linjer(
                1.januar to 18.januar endringskode UEND,
                20.januar to 26.januar endrer oppdrag2.last() opphører 20.januar,
                24.januar to 29.januar endringskode NY pekerPå oppdrag3[1],
                30.januar to 3.februar endringskode NY pekerPå oppdrag3[2]
            ), oppdrag3
        )

        assertUtbetalinger(
            linjer(
                1.januar to 18.januar endringskode UEND,
                24.januar to 29.januar endringskode UEND pekerPå oppdrag2.last(),
                30.januar to 3.februar endrer oppdrag3.last() opphører 30.januar,
                1.februar to 3.februar endringskode NY pekerPå oppdrag4[2]
            ), oppdrag4
        )
    }

    @Test
    fun `splitte opp linjer - ikke utvide oppdragets lengde`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 2.januar, 4.januar to 5.januar)
        val actual = recalculated - original

        assertUtbetalinger(
            linjer(
                1.januar to 2.januar endrer original.last(),
                4.januar to 5.januar endringskode NY pekerPå actual[0]
            ), actual
        )
    }

    @Test
    fun `gjeninnfører en opphørt periode`() {
        val original = linjer(1.januar to 3.januar, 4.januar to 12.januar grad 50)
        val recalculated = linjer(2.januar to 3.januar, 4.januar to 12.januar grad 50)
        val actual = recalculated - original
        val gjeninnført = linjer(4.januar to 12.januar grad 50)
        val revised = gjeninnført - actual

        assertUtbetalinger(
            linjer(
                4.januar to 12.januar grad 50 endrer original.last() opphører 1.januar,
                2.januar to 3.januar endringskode NY pekerPå original.last(),
                4.januar to 12.januar grad 50 endringskode NY pekerPå actual[1]
            ), actual
        )
        assertUtbetalinger(
            linjer(
                4.januar to 12.januar grad 50 endrer actual.last() opphører 2.januar,
                4.januar to 12.januar grad 50 endringskode NY pekerPå actual.last()
            ), revised
        )
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

        assertUtbetalinger(
            linjer(
                4.januar to 12.januar grad 50 endrer original.last() opphører 1.januar,
                2.januar to 3.januar endringskode NY pekerPå original.last(),
                4.januar to 12.januar grad 50 endringskode NY pekerPå actual[1]
            ), actual
        )
        assertUtbetalinger(
            linjer(
                2.januar to 3.januar endringskode UEND pekerPå original.last(),
                4.januar to 20.januar grad 50 endrer actual.last()
            ), revised
        )
        assertEquals(original.fagsystemId, revised.fagsystemId)
        assertEquals(ENDR, revised.endringskode)
    }

    @Test
    fun `trekke periode tilbake med opphold`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 5.januar, 8.januar to 13.januar)
        val actual = recalculated - original
        val revised = original - actual

        assertUtbetalinger(
            linjer(
                1.januar to 5.januar endringskode NY pekerPå actual.last()
            ), revised
        )
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
        assertUtbetalinger(
            linjer(
                1.januar to 5.januar endringskode UEND,
                8.januar to 13.januar endringskode NY pekerPå original.last()
            ), actual
        )
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
        assertUtbetalinger(
            linjer(
                1.januar to 5.januar endringskode UEND,
                8.januar to 13.januar endringskode UEND pekerPå original.last(),
                15.januar to 25.januar endringskode NY pekerPå actual[1]
            ), actual
        )
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
        assertUtbetalinger(
            linjer(
                1.januar to 5.januar endringskode UEND,
                8.januar to 25.januar endrer revised.last()
            ), actual
        )
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

        val recalculated = linjer(
            1.januar to 5.januar,
            8.januar to 20.januar,
            23.januar to 26.januar,
            28.januar to 5.februar
        )
        val actual = recalculated - extended

        val tilbakeført = linjer(1.januar to 5.januar, 8.januar to 13.januar)
        val revised = tilbakeført - actual

        assertUtbetalinger(
            linjer(
                1.januar to 5.januar endringskode UEND,
                8.januar to 13.januar endringskode NY pekerPå actual.last()
            ), revised
        )
    }

    @Test
    fun `trekke periode frem potpourri 1`() {
        val original = linjer(
            1.januar to 5.januar,
            8.januar to 20.januar,
            23.januar to 26.januar,
            28.januar to 5.februar
        )
        val tilbakeført = linjer(1.januar to 6.januar, 8.januar to 13.januar)
        val revised = tilbakeført - original
        assertUtbetalinger(
            linjer(
                1.januar to 6.januar endringskode NY pekerPå original.last(),
                8.januar to 13.januar endringskode NY pekerPå revised[0]
            ), revised
        )
    }

    @Test
    fun `trekke periode tilbake potpourri 3`() {
        val original = linjer(1.januar to 5.januar)
        val intermediate = linjer(1.januar to 5.januar, 8.januar to 13.januar)
        val extended = intermediate - original

        val recalculated = linjer(1.januar to 5.januar, 8.januar to 13.januar, 23.januar to 5.februar)
        val actual = recalculated - extended

        val tilbakeført = linjer(
            1.januar to 5.januar,
            8.januar to 13.januar,
            23.januar to 26.januar,
            1.februar to 5.februar
        )
        val revised = tilbakeført - actual

        assertUtbetalinger(
            linjer(
                1.januar to 5.januar endringskode UEND,
                8.januar to 13.januar endringskode UEND pekerPå revised[0],
                23.januar to 26.januar endrer actual.last(),
                1.februar to 5.februar endringskode NY pekerPå revised[2]
            ), revised
        )

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
        val linje1 = 1.januar to 5.januar endringskode UEND
        val linje2 = 15.januar to 19.januar pekerPå linje1
        assertUtbetalinger(linjer(linje1, linje2), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(UEND, actual[0].endringskode)
        assertEquals(NY, actual[1].endringskode)
        assertEquals(original[0].id, actual[1].refId)
        assertEquals(original[0].id + 1, actual[1].id)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `grad endres`() {
        val original = linjer(1.januar to 5.januar)
        val recalculated = linjer(1.januar to 5.januar grad 80, 15.januar to 19.januar)
        val actual = recalculated - original
        val linje1 = 1.januar to 5.januar grad 80 endringskode NY pekerPå original.last()
        val linje2 = 15.januar to 19.januar endringskode NY pekerPå linje1
        assertUtbetalinger(linjer(linje1, linje2), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(NY, actual[0].endringskode)
        assertEquals(NY, actual[1].endringskode)
        assertEquals(original[0].id + 1, actual[0].id)  // chained off of last of original
        assertEquals(actual[0].id + 1, actual[1].id)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `dagsats endres`() {
        val original = linjer(1.januar to 5.januar dagsats 500)
        val recalculated = linjer(1.januar to 5.januar dagsats 1000, 15.januar to 19.januar)
        val actual = recalculated - original
        val linje1 = 1.januar to 5.januar dagsats 1000 pekerPå original.last()
        val linje2 = 15.januar to 19.januar pekerPå linje1
        assertUtbetalinger(linjer(linje1, linje2), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(NY, actual[0].endringskode)
        assertEquals(NY, actual[1].endringskode)
        assertEquals(original[0].id + 1, actual[0].id)  // chained off of last of original
        assertEquals(actual[0].id + 1, actual[1].id)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
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
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun potpourri() {
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

        val linje1 = 1.januar to 5.januar endringskode UEND
        val linje2 = 6.januar to 17.januar grad 50 endringskode NY pekerPå original.last()
        val linje3 = 18.januar to 19.januar grad 80 endringskode NY pekerPå linje2
        val linje4 = 1.februar to 9.februar grad 100 endringskode NY pekerPå linje3
        assertUtbetalinger(linjer(linje1, linje2, linje3, linje4), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)

        assertUtbetalinger(
            linjer(
                1.januar to 5.januar endringskode UEND,
                6.januar to 17.januar grad 50 endringskode NY pekerPå original.last(),
                18.januar to 19.januar grad 80 endringskode NY pekerPå actual[1],
                1.februar to 9.februar endringskode NY pekerPå actual[2]
            ), actual
        )
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
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
        assertUtbetalinger(
            linjer(
                1.januar to 5.januar endringskode UEND,
                6.januar to 17.januar grad 50 endringskode NY pekerPå original.last(),
                18.januar to 19.januar grad 80 endringskode NY pekerPå actual[1],
                1.februar to 9.februar endringskode NY pekerPå actual[2]
            ), actual
        )
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `fom endres`() {
        val original = linjer(5.januar to 10.januar)
        val recalculated = linjer(1.januar to 10.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 10.januar endringskode NY pekerPå original.last()), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
    }

    @Test
    fun `ingen overlapp og ulik fagsystemId`() {
        val original = linjer(5.januar to 10.januar)
        val recalculated = linjer(1.januar to 3.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 3.januar endringskode NY pekerPå original.last()), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
    }

    @Test
    fun `fom og tom ulik og samme fagsystemId`() {
        val original = linjer(5.januar to 10.januar)
        val recalculated = linjer(1.januar to 3.januar, other = original)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 3.januar endringskode NY pekerPå original.last()), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
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
        assertUtbetalinger(
            linjer(
                1.januar to 5.januar endringskode UEND,
                6.januar to 19.januar grad 50 endringskode NY pekerPå original.last(), // extend tom
                20.januar to 26.januar endringskode NY pekerPå actual[1]
            ), actual
        )
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `slett nøyaktig en periode`() {
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
    }

    @Test
    fun `original andre periode samsvarer delvis med ny`() {
        val original = linjer(1.januar to 5.januar, 6.januar to 12.januar grad 50)
        val recalculated = linjer(6.januar to 19.januar grad 50)
        val actual = recalculated - original
        assertUtbetalinger(
            linjer(
                6.januar to 12.januar grad 50 endrer original.last() opphører 1.januar,
                6.januar to 19.januar grad 50 endringskode NY pekerPå actual[0]
            ), actual
        )
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
    }

    @Test
    fun `perioden min starter midt i en original periode`() {
        val original = linjer(1.januar to 3.januar, 4.januar to 12.januar grad 50)
        val recalculated = linjer(6.januar to 19.januar grad 50)
        val actual = recalculated - original
        assertUtbetalinger(
            linjer(
                4.januar to 12.januar grad 50 endrer original.last() opphører 1.januar,
                6.januar to 19.januar grad 50 endringskode NY pekerPå original.last()
            ), actual
        )
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
    }

    @Test
    fun `ny er tom`() {
        val original = linjer(1.januar to 3.januar, 4.januar to 12.januar grad 50)
        val recalculated = tomtOppdrag()
        val actual = recalculated - original
        assertUtbetalinger(
            linjer(
                4.januar to 12.januar grad 50 endrer original.last() opphører 1.januar
            ), actual
        )
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `originalen har hale å slette`() {
        val original = linjer(1.januar to 12.januar)
        val recalculated = linjer(1.januar to 5.januar)
        val actual = recalculated - original
        assertUtbetalinger(linjer(1.januar to 5.januar endrer original.last()), actual)
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `Sletting hvor alt blir sendt på nytt`() {
        val original = linjer(1.januar to 12.januar)
        val recalculated = linjer(1.januar to 5.januar, 7.januar to 10.januar)
        val actual = recalculated - original
        assertUtbetalinger(
            linjer(
                1.januar to 5.januar endrer original.last(),
                7.januar to 10.januar endringskode NY pekerPå actual[0]
            ), actual
        )
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `Sletting med UEND`() {
        val original = linjer(
            1.januar to 5.januar,
            8.januar to 12.januar
        )
        val recalculated = linjer(
            1.januar to 5.januar,
            8.januar to 10.januar
        )
        val actual = recalculated - original

        assertUtbetalinger(
            linjer(
                1.januar to 5.januar endringskode UEND,
                8.januar to 10.januar endrer original.last()
            ), actual
        )

        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    @Test
    fun `slette fra hode og hale av originalen`() {
        val original = linjer(1.januar to 12.januar)
        val recalculated = linjer(3.januar to 9.januar)
        val actual = recalculated - original
        assertUtbetalinger(
            linjer(
                1.januar to 12.januar endrer original.last() opphører 1.januar,
                3.januar to 9.januar endringskode NY pekerPå actual[0]
            ), actual
        )
        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
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
        assertUtbetalinger(
            linjer(
                20.januar to 31.januar endrer original.last() opphører 1.januar,
                6.januar to 19.januar grad 50 endringskode NY pekerPå actual[0],
                20.januar to 26.januar endringskode NY pekerPå actual[1]
            ), actual
        )

        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
    }

    @Test
    fun `deletion all`() {
        val original = linjer(
            1.januar to 5.januar,
            6.januar to 12.januar grad 50,
            13.januar to 19.januar,
            20.januar to 31.januar
        )
        val new = tomtOppdrag(original.fagsystemId)
        val actual = new - original
        assertUtbetalinger(
            linjer(20.januar to 31.januar endrer original.last() opphører 1.januar),
            actual
        )

        assertEquals(original.fagsystemId, actual.fagsystemId)
        assertEquals(ENDR, actual.endringskode)
        assertEquals(0, actual.stønadsdager())
        assertFalse(aktivitetslogg.harVarslerEllerVerre())
    }

    private fun tomtOppdrag(fagsystemId: String = genererUtbetalingsreferanse(UUID.randomUUID()), sisteArbeidsgiverdag: LocalDate? = null) =
        Oppdrag(ORGNUMMER, SykepengerRefusjon, fagsystemId = fagsystemId)

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
            assertEquals(a.id, b.id) { "delytelseid ${b.id} matcher ikke forventet ${a.id} for ${a.fom} - ${a.tom}" }
            assertEquals(a.refId, b.refId) { "refdelytelseid ${b.refId} matcher ikke forventet ${a.refId}" }
        }
    }

    private fun linjer(vararg linjer: TestUtbetalingslinje, other: Oppdrag? = null): Oppdrag {
        val fagsystemId = other?.inspektør?.fagsystemId() ?: genererUtbetalingsreferanse(UUID.randomUUID())
        return Oppdrag.gjenopprett(
            OppdragInnDto(
                mottaker = ORGNUMMER,
                fagområde = FagområdeDto.SPREF,
                linjer = linjer.toList().tilUtbetalingslinjerDto(fagsystemId),
                fagsystemId = fagsystemId,
                endringskode = EndringskodeDto.NY,
                nettoBeløp = 0,
                overføringstidspunkt = null,
                avstemmingsnøkkel = null,
                status = null,
                tidsstempel = LocalDateTime.now(),
                erSimulert = false,
                simuleringsResultat = null
            )
        )
    }

    private fun linjer(vararg linjer: Utbetalingslinje): Oppdrag {
        val fagsystemId = genererUtbetalingsreferanse(UUID.randomUUID())
        return Oppdrag(ORGNUMMER, SykepengerRefusjon, linjer.toList(), fagsystemId = fagsystemId)
    }

    private fun List<TestUtbetalingslinje>.tilUtbetalingslinjerDto(fagsystemId: String): List<UtbetalingslinjeInnDto> {
        return (take(1).map { it.asUtbetalingslinje() } + drop(1).map { it.asUtbetalingslinje(fagsystemId) })
            .map { it.dto() }
            .map {
                UtbetalingslinjeInnDto(
                    fom = it.fom,
                    tom = it.tom,
                    beløp = it.beløp,
                    grad = it.grad,
                    refFagsystemId = it.refFagsystemId,
                    delytelseId = it.delytelseId,
                    refDelytelseId = it.refDelytelseId,
                    endringskode = it.endringskode,
                    klassekode = it.klassekode,
                    datoStatusFom = it.datoStatusFom,
                )
            }
    }

    private inner class TestUtbetalingslinje(
        private val fom: LocalDate,
        private val tom: LocalDate
    ) {
        private var delytelseId = 1
        private var endringskode: Endringskode = NY
        private var grad: Int = 100
        private var dagsats = 1200
        private var datoStatusFom: LocalDate? = null
        private var refDelytelseId: Int? = null

        infix fun grad(percentage: Number): TestUtbetalingslinje {
            grad = percentage.toDouble().roundToInt()
            return this
        }

        infix fun dagsats(amount: Int): TestUtbetalingslinje {
            dagsats = amount
            return this
        }

        infix fun endringskode(kode: Endringskode): TestUtbetalingslinje {
            endringskode = kode
            return this
        }

        infix fun opphører(dato: LocalDate): TestUtbetalingslinje {
            datoStatusFom = dato
            return this
        }

        infix fun pekerPå(other: Utbetalingslinje): TestUtbetalingslinje {
            delytelseId = other.id + 1
            refDelytelseId = other.id
            return this
        }

        infix fun pekerPå(other: TestUtbetalingslinje): TestUtbetalingslinje {
            delytelseId = other.delytelseId + 1
            refDelytelseId = other.delytelseId
            return this
        }

        infix fun endrer(other: Utbetalingslinje): TestUtbetalingslinje {
            endringskode(ENDR)
            delytelseId = other.id
            return this
        }

        fun asUtbetalingslinje(fagsystemId: String? = null) =
            Utbetalingslinje(
                fom = fom,
                tom = tom,
                beløp = dagsats,
                grad = grad,
                klassekode = Klassekode.RefusjonIkkeOpplysningspliktig,
                endringskode = endringskode,
                datoStatusFom = datoStatusFom,
                refDelytelseId = refDelytelseId,
                refFagsystemId = if (endringskode == NY) fagsystemId else null,
                delytelseId = delytelseId
            )

    }

    private infix fun LocalDate.to(other: LocalDate) = TestUtbetalingslinje(this, other)
}

