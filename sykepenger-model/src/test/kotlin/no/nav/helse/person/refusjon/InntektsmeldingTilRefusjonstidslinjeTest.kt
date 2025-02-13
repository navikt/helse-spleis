package no.nav.helse.person.refusjon

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class InntektsmeldingTilRefusjonstidslinjeTest {

    @Test
    fun `En helt planke inntektsmelding`() {
        val refusjonstidslinje = refusjonstidslinjeFra(1.januar, 1000.daglig)

        assertEquals(Beløpstidslinje.fra(1.januar.somPeriode(), 1000.daglig, kilde), refusjonstidslinje)
    }

    @Test
    fun `Opphør av refusjon opplyst å være før første fraværsdag`() {
        val refusjonstidslinje = refusjonstidslinjeFra(1.februar, 1000.daglig, opphørsdato = 15.januar)

        assertEquals(Beløpstidslinje.fra(1.februar.somPeriode(), INGEN, kilde), refusjonstidslinje)
    }

    @Test
    fun `Endring av refusjon oppgitt etter opphørsdato`() {
        val refusjonstidslinje = refusjonstidslinjeFra(1.januar, 1000.daglig, opphørsdato = 15.februar, endringerIRefusjon = mapOf(28.februar to 500.daglig))

        assertEquals(16.februar, refusjonstidslinje.last().dato)
        assertEquals(Beløpstidslinje.fra(1.januar til 15.februar, 1000.daglig, kilde) + Beløpstidslinje.fra(16.februar.somPeriode(), INGEN, kilde), refusjonstidslinje)
    }


    @Test
    fun `Endring av refusjon oppgitt før utløpet av en oppstykket agp`() {
        val refusjonstidslinje = refusjonstidslinjeFra(15.januar, 1000.daglig, opphørsdato = null, endringerIRefusjon = mapOf(15.januar to 500.daglig))
        assertEquals(15.januar, refusjonstidslinje.last().dato)
        assertEquals(Beløpstidslinje.fra(15.januar.somPeriode(), 1000.daglig, kilde), refusjonstidslinje)
    }

    @Test
    fun `Arbeidsgiver utnytter inntektsmeldingens potensiale for å opplyse om refusjonsopplysninger`() {
        val refusjonstidslinje = refusjonstidslinjeFra(
            førsteFraværsdag = 1.januar,
            refusjonsbeløp = 1000.daglig,
            opphørsdato = 1.mars,
            endringerIRefusjon = mapOf(
                31.januar to 500.daglig,
                15.februar to 0.daglig,
                20.februar to 400.daglig
            )
        )

        val forventet =
            Beløpstidslinje.fra(1.januar til 30.januar, 1000.daglig, kilde) +
                Beløpstidslinje.fra(31.januar til 14.februar, 500.daglig, kilde) +
                Beløpstidslinje.fra(15.februar til 19.februar, 0.daglig, kilde) +
                Beløpstidslinje.fra(20.februar til 1.mars, 400.daglig, kilde) +
                Beløpstidslinje.fra(2.mars.somPeriode(), 0.daglig, kilde)

        assertEquals(forventet, refusjonstidslinje)
    }

    private companion object {
        private val meldingsreferanseId = MeldingsreferanseId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
        private val mottatt = 1.januar.atStartOfDay()
        private val kilde = Kilde(meldingsreferanseId, Avsender.ARBEIDSGIVER, mottatt)

        fun refusjonstidslinjeFra(
            førsteFraværsdag: LocalDate,
            refusjonsbeløp: Inntekt? = null,
            opphørsdato: LocalDate? = null,
            endringerIRefusjon: Map<LocalDate, Inntekt> = emptyMap()
        ) = Inntektsmelding.Refusjon(
            beløp = refusjonsbeløp,
            opphørsdato = opphørsdato,
            endringerIRefusjon = endringerIRefusjon.map { Inntektsmelding.Refusjon.EndringIRefusjon(it.value, it.key) }
        ).refusjonstidslinje(førsteFraværsdag, meldingsreferanseId, mottatt)
    }
}
