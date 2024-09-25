package no.nav.helse.person.refusjon

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.desember
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.somPersonidentifikator
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RefusjonsfaktabøtteTest {

    @Test
    fun `en helt normal inntektsmelding går oppi bøtta kan avklare en Refusjonstidslinje`() {
        val bøtte = Refusjonsfaktabøtte()
        val kilde = bøtte.leggTil(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjonsbeløp = INNTEKT
        )
        val avklartRefusjonstidslinje = Refusjonstidslinje(
            1.januar til 31.desember, EtBeløpMedKildePåSeg(INNTEKT, kilde)
        )
        assertEquals(avklartRefusjonstidslinje, bøtte.avklar(1.januar til 31.desember))
    }

    @Test
    fun `en inntektsmelding uten første fraværsdag går oppi bøtta kan avklare en Refusjonstidslinje`() {
        val bøtte = Refusjonsfaktabøtte()
        val kilde = bøtte.leggTil(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            førsteFraværsdag = null,
            refusjonsbeløp = INNTEKT
        )
        val avklartRefusjonstidslinje = Refusjonstidslinje(
            1.januar til 31.desember, EtBeløpMedKildePåSeg(INNTEKT, kilde)
        )
        assertEquals(avklartRefusjonstidslinje, bøtte.avklar(1.januar til 31.desember))
    }

    @Test
    fun `en inntektsmelding med opphør på refusjon går oppi bøtta kan avklare en Refusjonstidslinje`() {
        val bøtte = Refusjonsfaktabøtte()
        val kilde = bøtte.leggTil(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjonsbeløp = INNTEKT,
            opphørsdato = 28.desember
        )
        val avklartRefusjonstidslinje = Refusjonstidslinje(
            1.januar til 28.desember to EtBeløpMedKildePåSeg(INNTEKT, kilde),
            29.desember til 31.desember to EtBeløpMedKildePåSeg(INGEN, kilde)
        )
        assertEquals(avklartRefusjonstidslinje, bøtte.avklar(1.januar til 31.desember))
    }

    @Test
    fun `en inntektsmelding uten refusjonsbeløp går oppi bøtta kan avklare en Refusjonstidslinje`() {
        val bøtte = Refusjonsfaktabøtte()
        val kilde = bøtte.leggTil(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            førsteFraværsdag = 1.januar,
            refusjonsbeløp = null
        )
        val avklartRefusjonstidslinje = Refusjonstidslinje(
            1.januar til 31.desember to EtBeløpMedKildePåSeg(INGEN, kilde)
        )
        assertEquals(avklartRefusjonstidslinje, bøtte.avklar(1.januar til 31.desember))
    }

    private companion object {
        val arbeidsgiverHendelsefabrikk = ArbeidsgiverHendelsefabrikk("1", "11111111111".somPersonidentifikator(), "a1")

        fun Refusjonsfaktabøtte.leggTil(
            førsteFraværsdag: LocalDate? = null,
            arbeidsgiverperioder: List<Periode> = emptyList(),
            refusjonsbeløp: Inntekt? = null,
            opphørsdato: LocalDate? = null,
            endringerIRefusjon: Map<LocalDate, Inntekt> = emptyMap(),
            meldingsreferanseId: UUID = UUID.randomUUID()
        ): Kilde {
            leggTil(arbeidsgiverHendelsefabrikk.lagInntektsmelding(
                id = meldingsreferanseId,
                arbeidsgiverperioder = arbeidsgiverperioder,
                beregnetInntekt = INNTEKT,
                førsteFraværsdag = førsteFraværsdag,
                refusjon = Inntektsmelding.Refusjon(
                    beløp = refusjonsbeløp,
                    opphørsdato = opphørsdato,
                    endringerIRefusjon = endringerIRefusjon.map { (endringsdato, beløp) ->
                        Inntektsmelding.Refusjon.EndringIRefusjon(beløp, endringsdato)
                    }
                )
            ))
            return Kilde(meldingsreferanseId, ARBEIDSGIVER)
        }
    }
}