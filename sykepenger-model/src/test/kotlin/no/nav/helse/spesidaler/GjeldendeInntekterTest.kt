package no.nav.helse.spesidaler

import java.time.LocalDate.parse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GjeldendeInntekterTest {

    @Test
    fun `En person med mye snax`() {
        val personidentifikator = Personidentifikator("1")
        val org1 = Kilde("org1")
        val org2 = Kilde("org2")
        val dao = TøyseteInntektDao()
        listOf(
            NyInntekt(org1, Beløp(1000), parse("2018-01-01"), parse("2018-01-31")),
            NyInntekt(org1, Beløp(2000), parse("2018-01-02"), parse("2018-01-30")),
            NyInntekt(org1, Beløp(3000), parse("2018-01-15"), parse("2018-01-15")),
            NyInntekt(org2, Beløp(5000), parse("2018-01-10"), null),
            NyInntekt(org2, Beløp(0), parse("2018-01-15"), parse("2018-01-30"))
        ).forEach { dao.nyInntekt(personidentifikator, it) }

        val gjeldendeInntekter = GjeldendeInntekter(
            personidentifikator = personidentifikator,
            periode = parse("2018-01-01") til parse("2018-01-31"),
            dao = dao
        )
        assertEquals(Referanse(5), gjeldendeInntekter.referanse)
        assertEquals(setOf(org1, org2), gjeldendeInntekter.inntekter.keys)

        assertEquals(listOf(
            Beløpsperiode(parse("2018-01-15") til parse("2018-01-15"), Beløp(3000)),
            Beløpsperiode(parse("2018-01-02") til parse("2018-01-14"), Beløp(2000)),
            Beløpsperiode(parse("2018-01-16") til parse("2018-01-30"), Beløp(2000)),
            Beløpsperiode(parse("2018-01-01") til parse("2018-01-01"), Beløp(1000)),
            Beløpsperiode(parse("2018-01-31") til parse("2018-01-31"), Beløp(1000))
        ), gjeldendeInntekter.inntekter.getValue(org1))

        assertEquals(listOf(
            Beløpsperiode(parse("2018-01-10") til parse("2018-01-14"), Beløp(5000)),
            Beløpsperiode(parse("2018-01-31") til parse("2018-01-31"), Beløp(5000)),
        ), gjeldendeInntekter.inntekter.getValue(org2))
    }

    @Test
    fun `En person uten noen inntekter i det hele tatt`() {
        val personidentifikator = Personidentifikator("1")
        val gjeldendeInntekter = GjeldendeInntekter(
            personidentifikator = personidentifikator,
            periode = parse("2018-01-01") til parse("2018-01-31"),
            dao = TøyseteInntektDao()
        )
        assertEquals(Referanse(0), gjeldendeInntekter.referanse)
        assertTrue(gjeldendeInntekter.inntekter.isEmpty())
    }

    @Test
    fun `En person uten noen inntekter i den aktuelle perioden`() {
        val personidentifikator = Personidentifikator("1")
        val dao = TøyseteInntektDao()
        dao.nyInntekt(personidentifikator, NyInntekt(Kilde("noe"), Beløp(5000), parse("2018-01-01"), parse("2018-01-31")))

        val gjeldendeInntekter = GjeldendeInntekter(
            personidentifikator = personidentifikator,
            periode = parse("2018-02-01") til parse("2018-02-28"),
            dao = dao
        )
        assertEquals(Referanse(1), gjeldendeInntekter.referanse)
        assertTrue(gjeldendeInntekter.inntekter.isEmpty())
    }

    @Test
    fun `Får samme informasjon mange ganger`() {
        val personidentifikator = Personidentifikator("1")
        val kilde = Kilde("noe")
        val fom = parse("2018-01-01")
        val tom = parse("2018-01-31")
        val beløp = Beløp(5000)
        val dao = TøyseteInntektDao()
        (1..10).forEach { _ ->
            dao.nyInntekt(personidentifikator, NyInntekt(kilde, beløp, fom, tom))
        }
        val gjeldendeInntekter = GjeldendeInntekter(
            personidentifikator = personidentifikator,
            periode = fom til tom,
            dao = dao
        )
        assertEquals(Referanse(1), gjeldendeInntekter.referanse)
        assertEquals(setOf(kilde), gjeldendeInntekter.inntekter.keys)
        assertEquals(listOf(Beløpsperiode(fom til tom, beløp)), gjeldendeInntekter.inntekter.getValue(kilde))
    }
}
