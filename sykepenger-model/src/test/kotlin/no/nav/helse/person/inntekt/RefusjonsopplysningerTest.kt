package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.Companion.gjennopprett
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.Companion.refusjonsopplysninger
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.RefusjonsopplysningerBuilder
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode.Companion.harNødvendigeRefusjonsopplysninger
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class RefusjonsopplysningerTest {

    @Test
    fun `ny refusjonsopplysning i midten av eksisterende`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(
            Refusjonsopplysning(
                meldingsreferanseId1,
                1.januar,
                31.januar,
                2000.daglig
            )
        ).refusjonsopplysninger()
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(
            Refusjonsopplysning(
                meldingsreferanseId2,
                15.januar,
                20.januar,
                1000.daglig
            )
        ).refusjonsopplysninger()

        assertEquals(15.januar, nyeRefusjonsopplysninger.finnFørsteDatoForEndring(refusjonsopplysninger))
        assertEquals(
            listOf(
                Refusjonsopplysning(meldingsreferanseId1, 1.januar, 14.januar, 2000.daglig),
                Refusjonsopplysning(meldingsreferanseId2, 15.januar, 20.januar, 1000.daglig),
                Refusjonsopplysning(meldingsreferanseId1, 21.januar, 31.januar, 2000.daglig)
            ),
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `ny refusjonsopplysning uten tom`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId1, 1.januar, 31.januar, 2000.daglig)).refusjonsopplysninger()
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId2, 15.januar, null, 1000.daglig)).refusjonsopplysninger()

        assertEquals(15.januar, nyeRefusjonsopplysninger.finnFørsteDatoForEndring(refusjonsopplysninger))
        assertEquals(
            listOf(
                Refusjonsopplysning(meldingsreferanseId1, 1.januar, 14.januar, 2000.daglig),
                Refusjonsopplysning(meldingsreferanseId2, 15.januar, null, 1000.daglig)
            ),
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `ny refusjonsopplysning erstatter gamle`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId1, 2.januar, 30.januar, 2000.daglig)).refusjonsopplysninger()
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId2, 1.januar, 31.januar, 1000.daglig)).refusjonsopplysninger()

        val forventet = listOf(Refusjonsopplysning(meldingsreferanseId2, 2.januar, 31.januar, 1000.daglig)).refusjonsopplysninger()

        assertEquals(2.januar, nyeRefusjonsopplysninger.finnFørsteDatoForEndring(refusjonsopplysninger))
        assertEquals(
            forventet.inspektør.refusjonsopplysninger,
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `ny refusjonsopplysning uten tom erstatter gammel uten tom`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId1, 1.januar, null, 2000.daglig)).refusjonsopplysninger()
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId2, 1.januar, null, 1000.daglig)).refusjonsopplysninger()

        assertEquals(1.januar, nyeRefusjonsopplysninger.finnFørsteDatoForEndring(refusjonsopplysninger))
        assertEquals(
            nyeRefusjonsopplysninger.inspektør.refusjonsopplysninger,
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `ny refusjonsopplysning uten tom legges på eksisterende uten tom`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId1, 1.januar, null, 2000.daglig)).refusjonsopplysninger()
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId2, 1.mars, null, 1000.daglig)).refusjonsopplysninger()

        assertEquals(1.mars, nyeRefusjonsopplysninger.finnFørsteDatoForEndring(refusjonsopplysninger))
        assertEquals(
            listOf(Refusjonsopplysning(meldingsreferanseId1, 1.januar, 28.februar, 2000.daglig), Refusjonsopplysning(meldingsreferanseId2, 1.mars, null, 1000.daglig)),
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `ny refusjonsopplysning uten tom som starter tidligere enn forrige`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId1, 1.mars, null, 2000.daglig)).refusjonsopplysninger()
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId2, 1.januar, null, 1000.daglig)).refusjonsopplysninger()

        val forventet = listOf(Refusjonsopplysning(meldingsreferanseId2, 1.mars, null, 1000.daglig)).refusjonsopplysninger()

        assertEquals(
            forventet.inspektør.refusjonsopplysninger,
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `perfekt overlapp - bruker nye opplysninger`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId1, 1.januar, 1.mars, 2000.daglig)).refusjonsopplysninger()
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(Refusjonsopplysning(meldingsreferanseId2, 1.januar, 1.mars, 1000.daglig)).refusjonsopplysninger()

        assertEquals(
            nyeRefusjonsopplysninger.inspektør.refusjonsopplysninger,
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `nye opplysninger erstatter deler av eksisterende opplysninger`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val meldingsreferanseId2 = UUID.randomUUID()
        val meldingsreferanseId3 = UUID.randomUUID()
        val meldingsreferanseId4 = UUID.randomUUID()
        val meldingsreferanseId5 = UUID.randomUUID()
        val meldingsreferanseId6 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(
            Refusjonsopplysning(meldingsreferanseId1, 1.januar, 5.januar, 2000.daglig),
            Refusjonsopplysning(meldingsreferanseId2, 6.januar, 11.januar, 3000.daglig),
            Refusjonsopplysning(meldingsreferanseId3, 12.januar, 17.januar, 4000.daglig),
        ).refusjonsopplysninger()
        val nyeRefusjonsopplysninger = listOf(
            Refusjonsopplysning(meldingsreferanseId4, 2.januar, 4.januar, 5000.daglig),
            Refusjonsopplysning(meldingsreferanseId5, 7.januar, 10.januar, 6000.daglig),
            Refusjonsopplysning(meldingsreferanseId6, 13.januar, 16.januar, 7000.daglig),
        ).refusjonsopplysninger()

        assertEquals(2.januar, nyeRefusjonsopplysninger.finnFørsteDatoForEndring(refusjonsopplysninger))
        assertEquals(
            listOf(
                Refusjonsopplysning(meldingsreferanseId1, 1.januar, 1.januar, 2000.daglig),
                Refusjonsopplysning(meldingsreferanseId4, 2.januar, 4.januar, 5000.daglig),
                Refusjonsopplysning(meldingsreferanseId1, 5.januar, 5.januar, 2000.daglig),
                Refusjonsopplysning(meldingsreferanseId2, 6.januar, 6.januar, 3000.daglig),
                Refusjonsopplysning(meldingsreferanseId5, 7.januar, 10.januar, 6000.daglig),
                Refusjonsopplysning(meldingsreferanseId2, 11.januar, 11.januar, 3000.daglig),
                Refusjonsopplysning(meldingsreferanseId3, 12.januar, 12.januar, 4000.daglig),
                Refusjonsopplysning(meldingsreferanseId6, 13.januar, 16.januar, 7000.daglig),
                Refusjonsopplysning(meldingsreferanseId3, 17.januar, 17.januar, 4000.daglig)
            ),
            refusjonsopplysninger.merge(nyeRefusjonsopplysninger).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `merging av to tomme refusjonsopplysninger blir en tom refusjonsopplysning`() {
        val gammelRefusjonsopplysninger = Refusjonsopplysninger()
        val nyeRefusjonsopplysninger = Refusjonsopplysninger()
        val resultat = nyeRefusjonsopplysninger.merge(gammelRefusjonsopplysninger)
        assertNull(resultat.finnFørsteDatoForEndring(nyeRefusjonsopplysninger))
        assertEquals(Refusjonsopplysninger().inspektør.refusjonsopplysninger, resultat.inspektør.refusjonsopplysninger)
    }

    @Test
    fun `perioder kant i kant hvor siste periode har tom null`() {
        val meldingsreferanseId = UUID.randomUUID()
        val originaleRefusjonsopplysninger = listOf(
            Refusjonsopplysning(meldingsreferanseId, 1.januar, 19.januar, 1000.månedlig),
            Refusjonsopplysning(meldingsreferanseId, 20.januar, 24.januar, 500.månedlig),
            Refusjonsopplysning(meldingsreferanseId, 25.januar, 28.februar, 2000.månedlig),
            Refusjonsopplysning(meldingsreferanseId, 1.mars, 19.mars, 999.månedlig),
            Refusjonsopplysning(meldingsreferanseId, 20.mars, 24.mars, 99.månedlig),
            Refusjonsopplysning(meldingsreferanseId, 25.mars, null, 9.månedlig)
        )

        assertEquals(
            originaleRefusjonsopplysninger,
            Refusjonsopplysninger(originaleRefusjonsopplysninger, LocalDateTime.now()).inspektør.refusjonsopplysninger
        )
    }

    @Test
    fun `senere periode uten tom`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val meldingsreferanseId2 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(
            Refusjonsopplysning(meldingsreferanseId1, 1.januar, 31.januar, 2000.daglig),
            Refusjonsopplysning(meldingsreferanseId2, 1.mars, null, 2000.daglig)
        )

        assertEquals(refusjonsopplysninger, Refusjonsopplysninger(refusjonsopplysninger, LocalDateTime.now()).inspektør.refusjonsopplysninger)
    }

    @Test
    fun `ny opplysning før oss`() {
        val eksisterende = Refusjonsopplysning(UUID.randomUUID(), 1.mars, 31.mars, 2000.daglig)
        val ny = Refusjonsopplysning(UUID.randomUUID(), 1.januar, 15.februar, 2000.daglig)
        assertEquals(listOf(ny, eksisterende), Refusjonsopplysninger(listOf(eksisterende, ny), LocalDateTime.now()).inspektør.refusjonsopplysninger)
    }

    @Test
    fun `ny opplysning før med overlapp`() {
        val eksisterendeId = UUID.randomUUID()
        val eksisterendeTidspunkt = LocalDateTime.now()
        val nyttTidspunkt = eksisterendeTidspunkt.plusSeconds(1)
        val eksisterende = Refusjonsopplysning(eksisterendeId, 1.mars, 31.mars, 2000.daglig)
        val meldingsreferanseId = UUID.randomUUID()
        val ny = Refusjonsopplysning(meldingsreferanseId, 1.mars, 1.mars, 1000.daglig)
        val refusjonsopplysning = RefusjonsopplysningerBuilder()
            .leggTil(eksisterende, eksisterendeTidspunkt).leggTil(ny, nyttTidspunkt).build()
        assertEquals(1.mars, refusjonsopplysning.finnFørsteDatoForEndring(RefusjonsopplysningerBuilder().leggTil(eksisterende).build()))
        assertEquals(listOf(ny, Refusjonsopplysning(eksisterendeId, 2.mars, 31.mars, 2000.daglig)), refusjonsopplysning.inspektør.refusjonsopplysninger)

    }

    @Test
    fun `håndterer å ta inn refusjonsopplysninger hulter til bulter`() {
        val eksisterendeTidspunkt = LocalDateTime.now()
        val nyttTidspunkt = eksisterendeTidspunkt.plusSeconds(1)
        val ny = Refusjonsopplysning(UUID.randomUUID(), 1.januar, 1.mars, 2000.daglig)
        val eksisterende = Refusjonsopplysning(UUID.randomUUID(), 1.mars, 31.mars, 2000.daglig)
        val eksisterendeFørst = RefusjonsopplysningerBuilder().leggTil(eksisterende, eksisterendeTidspunkt).leggTil(ny, nyttTidspunkt).build()
        val nyFørst = RefusjonsopplysningerBuilder().leggTil(ny, nyttTidspunkt).leggTil(eksisterende, eksisterendeTidspunkt).build()
        assertEquals(eksisterendeFørst, nyFørst)
    }

    @Test
    fun `refusjonsopplysninger med samme tidspunkt sorteres på fom`() {
        val tidspunkt = LocalDateTime.now()
        val januar = Refusjonsopplysning(UUID.randomUUID(), 1.januar, 1.mars, 2000.daglig)
        val mars = Refusjonsopplysning(UUID.randomUUID(), 1.mars, 31.mars, 2000.daglig)
        val marsFørst = RefusjonsopplysningerBuilder().leggTil(mars, tidspunkt).leggTil(januar,tidspunkt).build()
        val januarFørst = RefusjonsopplysningerBuilder().leggTil(januar, tidspunkt).leggTil(mars, tidspunkt).build()
        assertEquals(marsFørst, januarFørst)
    }

    @Test
    fun `har refusjonsopplysninger for forventede dager`() {
        val januar = Refusjonsopplysning(UUID.randomUUID(), 2.januar, 31.januar, 2000.daglig)
        val skjæringstidspunkt = 1.januar
        val refusjonsopplysninger = januar.refusjonsopplysninger

        assertFalse(refusjonsopplysninger.harNødvendigRefusjonsopplysninger(skjæringstidspunkt, listOf(skjæringstidspunkt.forrigeDag)))
        assertTrue(refusjonsopplysninger.harNødvendigRefusjonsopplysninger(skjæringstidspunkt, listOf(1.januar)))
        assertTrue(refusjonsopplysninger.harNødvendigRefusjonsopplysninger(skjæringstidspunkt, listOf(2.januar)))
        assertTrue(refusjonsopplysninger.harNødvendigRefusjonsopplysninger(skjæringstidspunkt, listOf(31.januar)))
        assertFalse(refusjonsopplysninger.harNødvendigRefusjonsopplysninger(skjæringstidspunkt, listOf(1.februar)))
        assertTrue(refusjonsopplysninger.harNødvendigRefusjonsopplysninger(skjæringstidspunkt, 2.januar til 31.januar))
        assertTrue(refusjonsopplysninger.harNødvendigRefusjonsopplysninger(skjæringstidspunkt, 3.januar til 30.januar))
        assertTrue(refusjonsopplysninger.harNødvendigRefusjonsopplysninger(skjæringstidspunkt, 1.januar til 31.januar))
        assertFalse(refusjonsopplysninger.harNødvendigRefusjonsopplysninger(skjæringstidspunkt, 2.januar til 1.februar))
        assertFalse(refusjonsopplysninger.harNødvendigRefusjonsopplysninger(skjæringstidspunkt, 31.januar til 28.februar))
    }

    @Test
    fun `kan gjenopprette refusjonsopplysninger som ikke overlapper`() {
        assertGjenopprettetRefusjonsopplysninger(emptyList())
        assertGjenopprettetRefusjonsopplysninger(listOf(Refusjonsopplysning(UUID.randomUUID(), 1.januar, 31.januar, 1000.daglig)))
        assertGjenopprettetRefusjonsopplysninger(listOf(Refusjonsopplysning(UUID.randomUUID(), 1.januar, null, 1000.daglig)))
        assertGjenopprettetRefusjonsopplysninger(listOf(
            Refusjonsopplysning(UUID.randomUUID(), 1.januar, 31.januar, 1000.daglig),
            Refusjonsopplysning(UUID.randomUUID(), 1.mars, 31.mars, 2000.daglig),
            Refusjonsopplysning(UUID.randomUUID(), 1.april, null, 3000.daglig),
        ))
    }

    @Test
    fun `kan ikke gjenopprette refusjonsopplysningr som overlapper`() {
        assertThrows<IllegalStateException> { listOf(
            Refusjonsopplysning(UUID.randomUUID(), 1.januar, 31.januar, 1000.daglig),
            Refusjonsopplysning(UUID.randomUUID(), 1.mars, null, 2000.daglig),
            Refusjonsopplysning(UUID.randomUUID(), 1.april, 30.april, 3000.daglig),
        ).gjennopprett() }

        assertThrows<IllegalStateException> { listOf(
            Refusjonsopplysning(UUID.randomUUID(), 1.januar, 31.januar, 1000.daglig),
            Refusjonsopplysning(UUID.randomUUID(), 1.januar, 28.februar, 2000.daglig)
        ).gjennopprett() }
    }

    @Test
    fun `Har ikke nødvendig refusjonsopplysninger etter oppholdsdager`() {
        val arbeidsgiverperiode = Arbeidsgiverperiode(listOf(1.januar til 16.januar)).apply {
            (17.januar til 25.januar).forEach { utbetalingsdag(it) }
            (26.januar til 31.januar).forEach { oppholdsdag(it) }
            (1.februar til 28.februar).forEach { utbetalingsdag(it) }
        }
        val refusjonsopplysninger = Refusjonsopplysning(UUID.randomUUID(), 1.januar, null, 20000.månedlig).refusjonsopplysninger
        assertTrue(harNødvendigeRefusjonsopplysninger(1.januar, 1.januar til 31.januar, refusjonsopplysninger, arbeidsgiverperiode))
        assertFalse(harNødvendigeRefusjonsopplysninger(1.januar, 1.februar til 28.februar, refusjonsopplysninger, arbeidsgiverperiode))
    }

    @Test
    fun `Har nødvendige refusjonsopplysninger når vi får nye refusjonsopplysninger for perioden etter oppholdet`() {
        val arbeidsgiverperiode = Arbeidsgiverperiode(listOf(1.januar til 16.januar)).apply {
            (17.januar til 25.januar).forEach { utbetalingsdag(it) }
            (26.januar til 31.januar).forEach { oppholdsdag(it) }
            (1.februar til 28.februar).forEach { utbetalingsdag(it) }
        }
        // IM: FF = 1.januar, AGP = 1.januar - 16.januar
        val refusjonsopplysningerJanuar = Refusjonsopplysning(UUID.randomUUID(), 1.januar, null, 20000.månedlig).refusjonsopplysninger
        assertTrue(harNødvendigeRefusjonsopplysninger(1.januar, 1.januar til 31.januar, refusjonsopplysningerJanuar, arbeidsgiverperiode))
        assertFalse(harNødvendigeRefusjonsopplysninger(1.januar, 1.februar til 28.februar, refusjonsopplysningerJanuar, arbeidsgiverperiode))

        // IM: FF = 1.februar, AGP = 1.januar - 16.januar
        val refusjonsopplysningerFebruar = Refusjonsopplysning(UUID.randomUUID(), 1.februar, null, 25000.månedlig).refusjonsopplysninger

        val oppdaterteRefusjonsopplysninger = refusjonsopplysningerJanuar.merge(refusjonsopplysningerFebruar)
        assertTrue(harNødvendigeRefusjonsopplysninger(1.januar, 1.januar til 31.januar, oppdaterteRefusjonsopplysninger, arbeidsgiverperiode))
        assertTrue(harNødvendigeRefusjonsopplysninger(1.januar, 1.februar til 28.februar, oppdaterteRefusjonsopplysninger, arbeidsgiverperiode))
    }

    @Test
    fun `finner riktige beløp`() {
        val meldingsreferanseIdJanuar = UUID.randomUUID()
        val meldingsreferanseIdFebruar = UUID.randomUUID()
        val skjæringstidspunkt = 1.januar

        val refusjonsopplysninger = RefusjonsopplysningerBuilder()
            .leggTil(Refusjonsopplysning(meldingsreferanseIdJanuar, skjæringstidspunkt, 31.januar, 1000.daglig), LocalDateTime.now())
            .leggTil(Refusjonsopplysning(meldingsreferanseIdFebruar, 1.februar, null, 1500.daglig), LocalDateTime.now())
            .build()

        (skjæringstidspunkt til 31.januar).forEach { dag ->
            assertEquals(1000.daglig, refusjonsopplysninger.refusjonsbeløpOrNull(dag))
        }
    }

    @Test
    fun `Spør om refusjonsopplysninger utelukkende i gråsonen`() {
        val skjæringstidspunkt = 1.januar
        val førsteDagMedRefusjonsopplysning = 20.januar
        val refusjonsopplysninger = Refusjonsopplysning(UUID.randomUUID(), førsteDagMedRefusjonsopplysning, null, 1000.daglig).refusjonsopplysninger
        val gråsonen = skjæringstidspunkt til førsteDagMedRefusjonsopplysning.forrigeDag
        assertTrue(refusjonsopplysninger.harNødvendigRefusjonsopplysninger(skjæringstidspunkt = skjæringstidspunkt, periode = gråsonen))
    }

    @Test
    fun `hashCode skal fungere også for en åpen periode`() {
        val refusjonsopplysning = Refusjonsopplysning(UUID.randomUUID(), 1.januar, null, 1000.daglig)
        assertDoesNotThrow { refusjonsopplysning.hashCode() }
        assertDoesNotThrow { refusjonsopplysning.refusjonsopplysninger.hashCode() }
    }

    @Test
    fun `Trekke to funksjonelt like refusjonsopplysninger fra hverandre gir tomme Refusjonsopplysninger`() {
        val eksisterendeRefusjonsopplysninger = Refusjonsopplysning(UUID.randomUUID(), 1.januar, null, 1000.daglig).refusjonsopplysninger
        val ønskedeRefusjonsopplysninger = Refusjonsopplysning(UUID.randomUUID(), 1.januar, null, 1000.daglig).refusjonsopplysninger
        assertEquals(Refusjonsopplysninger(), eksisterendeRefusjonsopplysninger - ønskedeRefusjonsopplysninger)
    }

    @Test
    fun `Identifisere nye refusjonsopplysninger`() {
        val eksisterendeRefusjonsopplysninger = RefusjonsopplysningerBuilder()
            .leggTil(Refusjonsopplysning(UUID.randomUUID(), 1.januar, 31.januar, 1500.daglig))
            .leggTil(Refusjonsopplysning(UUID.randomUUID(), 1.februar, 28.februar, 1200.daglig))
            .leggTil(Refusjonsopplysning(UUID.randomUUID(), 1.mars, 31.mars, 0.daglig))
            .leggTil(Refusjonsopplysning(UUID.randomUUID(), 1.april, 30.april, 1600.daglig))
            .leggTil(Refusjonsopplysning(UUID.randomUUID(), 1.mai, null, 1700.daglig))
            .build()

        val overstyringId = UUID.randomUUID()
        val ønskedeRefusjonsopplysninger = RefusjonsopplysningerBuilder()
            .leggTil(Refusjonsopplysning(overstyringId, 1.januar, 31.januar, 1500.daglig))
            .leggTil(Refusjonsopplysning(overstyringId, 1.februar, 27.februar, 1200.daglig)) // 28.februar -> 27.februar
            .leggTil(Refusjonsopplysning(overstyringId, 28.februar, 31.mars, 10.daglig)) // 1.mars -> 28.februar
            .leggTil(Refusjonsopplysning(overstyringId, 1.april, 30.april, 1599.daglig)) // 1600.daglig -> 1599.dalig
            .leggTil(Refusjonsopplysning(overstyringId, 1.mai, null, 1700.daglig))
            .build()

        assertEquals(listOf(
            Refusjonsopplysning(overstyringId, 1.februar, 27.februar, 1200.daglig),
            Refusjonsopplysning(overstyringId, 28.februar, 31.mars, 10.daglig),
            Refusjonsopplysning(overstyringId, 1.april, 30.april, 1599.daglig)
        ), (eksisterendeRefusjonsopplysninger - ønskedeRefusjonsopplysninger).inspektør.refusjonsopplysninger)
    }

    @Test
    fun `Merger nye refusjonsopplysninger`() {
        val inntektsmeldingId = UUID.randomUUID()
        val eksisterendeRefusjonsopplysninger = RefusjonsopplysningerBuilder()
            .leggTil(Refusjonsopplysning(inntektsmeldingId, 1.januar, 31.januar, 1500.daglig))
            .leggTil(Refusjonsopplysning(inntektsmeldingId, 1.februar, 28.februar, 1200.daglig))
            .leggTil(Refusjonsopplysning(inntektsmeldingId, 1.mars, 31.mars, 0.daglig))
            .leggTil(Refusjonsopplysning(inntektsmeldingId, 1.april, 30.april, 1600.daglig))
            .leggTil(Refusjonsopplysning(inntektsmeldingId, 1.mai, null, 1700.daglig))
            .build()

        val overstyringId = UUID.randomUUID()
        val ønskedeRefusjonsopplysninger = RefusjonsopplysningerBuilder()
            .leggTil(Refusjonsopplysning(overstyringId, 1.januar, 31.januar, 1500.daglig))
            .leggTil(Refusjonsopplysning(overstyringId, 1.februar, 27.februar, 1200.daglig)) // 28.februar -> 27.februar
            .leggTil(Refusjonsopplysning(overstyringId, 28.februar, 31.mars, 10.daglig)) // 1.mars -> 28.februar
            .leggTil(Refusjonsopplysning(overstyringId, 1.april, 30.april, 1599.daglig)) // 1600.daglig -> 1599.dalig
            .leggTil(Refusjonsopplysning(overstyringId, 1.mai, null, 1700.daglig))
            .build()

        assertEquals(listOf(
            Refusjonsopplysning(inntektsmeldingId, 1.januar, 31.januar, 1500.daglig),
            Refusjonsopplysning(overstyringId, 1.februar, 27.februar, 1200.daglig),
            Refusjonsopplysning(overstyringId, 28.februar, 31.mars, 10.daglig),
            Refusjonsopplysning(overstyringId, 1.april, 30.april, 1599.daglig),
            Refusjonsopplysning(inntektsmeldingId, 1.mai, null, 1700.daglig)
        ), eksisterendeRefusjonsopplysninger.merge(ønskedeRefusjonsopplysninger).inspektør.refusjonsopplysninger)
    }

    @Test
    fun `saksbehandler forkorter refusjonsopplysningens snute`() {
        val inntektsmeldingId = UUID.randomUUID()
        val eksisterendeRefusjonsopplysninger = Refusjonsopplysning(inntektsmeldingId, 1.januar, null, 1000.daglig).refusjonsopplysninger

        val overstyringId = UUID.randomUUID()
        val ønskedeRefusjonsopplysninger = Refusjonsopplysning(overstyringId, 10.januar, null, 1000.daglig).refusjonsopplysninger

        assertEquals(listOf(
            Refusjonsopplysning(inntektsmeldingId, 1.januar, 9.januar, 1000.daglig),
            Refusjonsopplysning(overstyringId, 10.januar, null, 1000.daglig)
        ), eksisterendeRefusjonsopplysninger.merge(ønskedeRefusjonsopplysninger).inspektør.refusjonsopplysninger)
    }

    @Test
    fun `saksbehandler forkorter refusjonsopplysningens hale`() {
        val inntektsmeldingId = UUID.randomUUID()
        val eksisterendeRefusjonsopplysninger = Refusjonsopplysning(inntektsmeldingId, 1.januar, 31.januar, 1000.daglig).refusjonsopplysninger

        val overstyringId = UUID.randomUUID()
        val ønskedeRefusjonsopplysninger = Refusjonsopplysning(overstyringId, 1.januar, 20.januar, 1000.daglig).refusjonsopplysninger

        assertEquals(listOf(
            Refusjonsopplysning(overstyringId, 1.januar, 20.januar, 1000.daglig),
            Refusjonsopplysning(inntektsmeldingId, 21.januar, 31.januar, 1000.daglig)
        ), eksisterendeRefusjonsopplysninger.merge(ønskedeRefusjonsopplysninger).inspektør.refusjonsopplysninger)
    }

    @Test
    fun `saksbehandler forkorter refusjonsopplysningens åpne hale`() {
        val inntektsmeldingId = UUID.randomUUID()
        val eksisterendeRefusjonsopplysninger = Refusjonsopplysning(inntektsmeldingId, 1.januar, null, 1000.daglig).refusjonsopplysninger

        val overstyringId = UUID.randomUUID()
        val ønskedeRefusjonsopplysninger = Refusjonsopplysning(overstyringId, 1.januar, 20.januar, 1000.daglig).refusjonsopplysninger

        assertEquals(listOf(
            Refusjonsopplysning(overstyringId, 1.januar, 20.januar, 1000.daglig),
            Refusjonsopplysning(inntektsmeldingId, 21.januar, null, 1000.daglig)
        ), eksisterendeRefusjonsopplysninger.merge(ønskedeRefusjonsopplysninger).inspektør.refusjonsopplysninger)
    }

    @Test
    fun `saksbehandler forkorter refusjonsopplysningens snute og hale`() {
        val inntektsmeldingId = UUID.randomUUID()
        val eksisterendeRefusjonsopplysninger = Refusjonsopplysning(inntektsmeldingId, 1.januar, 31.januar, 1000.daglig).refusjonsopplysninger

        val overstyringId = UUID.randomUUID()
        val ønskedeRefusjonsopplysninger = Refusjonsopplysning(overstyringId, 2.januar, 30.januar, 1000.daglig).refusjonsopplysninger

        assertEquals(listOf(
            Refusjonsopplysning(inntektsmeldingId, 1.januar, 1.januar, 1000.daglig),
            Refusjonsopplysning(overstyringId, 2.januar, 30.januar, 1000.daglig),
            Refusjonsopplysning(inntektsmeldingId, 31.januar, 31.januar, 1000.daglig)
        ), eksisterendeRefusjonsopplysninger.merge(ønskedeRefusjonsopplysninger).inspektør.refusjonsopplysninger)
    }

    @Test
    fun `saksbehandler lager hull i refusjonsopplysningene`() {
        val inntektsmeldingId = UUID.randomUUID()
        val eksisterendeRefusjonsopplysninger = RefusjonsopplysningerBuilder()
            .leggTil(Refusjonsopplysning(inntektsmeldingId, 1.januar, 31.januar, 1500.daglig))
            .leggTil(Refusjonsopplysning(inntektsmeldingId, 1.februar, 28.februar, 1200.daglig))
            .build()

        val overstyringId = UUID.randomUUID()
        val ønskedeRefusjonsopplysninger = RefusjonsopplysningerBuilder()
            .leggTil(Refusjonsopplysning(overstyringId, 1.januar, 31.januar, 1500.daglig))
            .leggTil(Refusjonsopplysning(overstyringId, 10.februar, 28.februar, 1200.daglig)) // Hull 1-9.februar
            .build()

        assertEquals(listOf(
            Refusjonsopplysning(inntektsmeldingId, 1.januar, 31.januar, 1500.daglig),
            Refusjonsopplysning(inntektsmeldingId, 1.februar, 9.februar, 1200.daglig),
            Refusjonsopplysning(overstyringId, 10.februar, 28.februar, 1200.daglig)
        ), eksisterendeRefusjonsopplysninger.merge(ønskedeRefusjonsopplysninger).inspektør.refusjonsopplysninger)
    }

    @Test
    fun `saksbehandler legger til ny opplysning midt i en eksisterende`() {
        val inntektsmeldingId = UUID.randomUUID()
        val eksisterendeRefusjonsopplysninger =
            Refusjonsopplysning(inntektsmeldingId, 1.januar, null, 1200.daglig).refusjonsopplysninger

        val overstyringId = UUID.randomUUID()
        val ønskedeRefusjonsopplysninger =
            Refusjonsopplysning(overstyringId, 13.januar, 1.desember, 1600.daglig).refusjonsopplysninger

        assertEquals(listOf(
            Refusjonsopplysning(inntektsmeldingId, 1.januar, 12.januar, 1200.daglig),
            Refusjonsopplysning(overstyringId, 13.januar, 1.desember, 1600.daglig),
            Refusjonsopplysning(inntektsmeldingId, 2.desember, null, 1200.daglig)
        ), eksisterendeRefusjonsopplysninger.merge(ønskedeRefusjonsopplysninger).inspektør.refusjonsopplysninger)
    }

    @Test
    fun `saksbehandler rydder opp i rotete refusjonsopplysninger`() {
        val beløp = 1500.daglig
        val eksisterendeRefusjonsopplysninger = RefusjonsopplysningerBuilder()
            .leggTil(Refusjonsopplysning(UUID.randomUUID(), 1.januar, 31.januar, beløp))
            .leggTil(Refusjonsopplysning(UUID.randomUUID(), 1.februar, 28.februar, beløp))
            .leggTil(Refusjonsopplysning(UUID.randomUUID(), 1.mars, 31.mars, beløp))
            .leggTil(Refusjonsopplysning(UUID.randomUUID(), 1.april, 30.april, beløp))
            .leggTil(Refusjonsopplysning(UUID.randomUUID(), 1.mai, null, beløp))
            .build()

        val overstyringId = UUID.randomUUID()
        val ønskedeRefusjonsopplysninger = Refusjonsopplysning(overstyringId, 1.januar, null, beløp).refusjonsopplysninger

        assertEquals(ønskedeRefusjonsopplysninger, eksisterendeRefusjonsopplysninger.merge(ønskedeRefusjonsopplysninger))
    }
    @Test
    fun `saksbehandler forsøker å fjerne alle refusjonsopplysninger`() {
        val eksisterendeRefusjonsopplysninger = Refusjonsopplysning(UUID.randomUUID(), 1.januar, null, 1000.daglig).refusjonsopplysninger
        val ønskedeRefusjonsopplysninger = Refusjonsopplysninger()
        assertEquals(eksisterendeRefusjonsopplysninger, eksisterendeRefusjonsopplysninger.merge(ønskedeRefusjonsopplysninger))
    }

    @Test
    fun `ny refusjonsopplysning midt i forrige med samme beløp`() {
        val meldingsreferanseId1 = UUID.randomUUID()
        val refusjonsopplysninger = listOf(
            Refusjonsopplysning(
                meldingsreferanseId1,
                1.januar,
                null,
                2000.daglig
            )
        ).refusjonsopplysninger()
        val meldingsreferanseId2 = UUID.randomUUID()
        val nyeRefusjonsopplysninger = listOf(
            Refusjonsopplysning(
                meldingsreferanseId2,
                1.januar,
                10.januar,
                2000.daglig
            ),
            Refusjonsopplysning(
                meldingsreferanseId2,
                11.januar,
                null,
                2000.daglig)

        ).refusjonsopplysninger()

        assertEquals(11.januar, nyeRefusjonsopplysninger.finnFørsteDatoForEndring(refusjonsopplysninger))
    }

    internal companion object {
        private fun harNødvendigeRefusjonsopplysninger(skjæringstidspunkt: LocalDate, periode: Periode, refusjonsopplysninger: Refusjonsopplysninger, arbeidsgiverperiode: Arbeidsgiverperiode) =
            harNødvendigeRefusjonsopplysninger(skjæringstidspunkt, periode, refusjonsopplysninger, arbeidsgiverperiode, Aktivitetslogg(), "")
        private fun assertGjenopprettetRefusjonsopplysninger(refusjonsopplysninger: List<Refusjonsopplysning>) {
            assertEquals(refusjonsopplysninger, refusjonsopplysninger.gjennopprett().inspektør.refusjonsopplysninger)
        }
        private fun Refusjonsopplysninger.harNødvendigRefusjonsopplysninger(skjæringstidspunkt: LocalDate, dager: List<LocalDate>) = harNødvendigRefusjonsopplysninger(skjæringstidspunkt, dager, null, Aktivitetslogg(), "")
        private fun Refusjonsopplysninger.harNødvendigRefusjonsopplysninger(skjæringstidspunkt: LocalDate, periode: Periode) = harNødvendigRefusjonsopplysninger(skjæringstidspunkt, periode.toList(), null, Aktivitetslogg(), "")
        private fun List<Refusjonsopplysning>.refusjonsopplysninger() = Refusjonsopplysninger(this, LocalDateTime.now())

        private fun Refusjonsopplysninger(refusjonsopplysninger: List<Refusjonsopplysning>, tidsstempel: LocalDateTime): Refusjonsopplysninger {
            val refusjonsopplysningerBuilder = RefusjonsopplysningerBuilder()
            refusjonsopplysninger.forEach { refusjonsopplysningerBuilder.leggTil(it, tidsstempel) }
            return refusjonsopplysningerBuilder.build()
        }

        private fun RefusjonsopplysningerBuilder.leggTil(refusjonsopplysning: Refusjonsopplysning) = leggTil(refusjonsopplysning, LocalDateTime.now())
    }
}