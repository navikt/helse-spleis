package no.nav.helse.serde.api.v2.buildere

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.serde.api.dto.AnnullertPeriode
import no.nav.helse.serde.api.dto.BeregnetPeriode
import no.nav.helse.serde.api.dto.GenerasjonDTO
import no.nav.helse.serde.api.dto.Periodetilstand
import no.nav.helse.serde.api.dto.Tidslinjeperiode
import no.nav.helse.serde.api.dto.Tidslinjeperiode.Companion.sorterEtterHendelse
import no.nav.helse.serde.api.dto.Tidslinjeperiodetype
import no.nav.helse.serde.api.dto.UberegnetPeriode
import no.nav.helse.serde.api.dto.Utbetaling
import no.nav.helse.serde.api.dto.Utbetalingstatus
import no.nav.helse.serde.api.dto.Utbetalingtype
import no.nav.helse.serde.api.speil.Generasjoner
import no.nav.helse.serde.api.v2.buildere.GenerasjonerTest.Hva.beregnet
import no.nav.helse.serde.api.v2.buildere.GenerasjonerTest.Hva.uberegnet
import no.nav.helse.utbetalingslinjer.UtbetalingInntektskilde
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass


internal class GenerasjonerTest {
    @Test
    fun `bare førstegangsbehandlinger`() {
        val generasjoner = byggGenerasjoner(
            uberegnetPeriode(1.mars til 10.mars),
            beregnetPeriode(11.mars til 31.mars, utbetaling(Utbetalingtype.UTBETALING)),
            beregnetPeriode(10.april til 28.april, utbetaling(Utbetalingtype.UTBETALING)),
            uberegnetPeriode(1.januar til 10.januar)
        )
        generasjoner(1) {
            generasjon(0, 4) {
                periode(0) fra 10.april til 28.april er beregnet
                periode(1) fra 11.mars til 31.mars er beregnet
                periode(2) fra 1.mars til 10.mars er uberegnet
                periode(3) fra 1.januar til 10.januar er uberegnet
            }
        }
    }

    @Test
    fun `out of order - ny beregnet periode`() {
        val generasjoner = byggGenerasjoner(
            beregnetPeriode(11.mars til 31.mars, utbetaling(Utbetalingtype.UTBETALING)),
            beregnetPeriode(10.april til 28.april, utbetaling(Utbetalingtype.UTBETALING)),
            beregnetPeriode(1.januar til 31.januar, utbetaling(Utbetalingtype.UTBETALING))

        )
        generasjoner(1) {
            generasjon(0, 3) {
                periode(0) fra 10.april til 28.april er beregnet
                periode(1) fra 11.mars til 31.mars er beregnet
                periode(2) fra 1.januar til 31.januar er beregnet
            }
        }
    }

    @Test
    fun `out of order - revurdering etter ny beregnet periode`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val vedtaksperiode1 = beregnetPeriode(11.mars til 31.mars, utbetaling(Utbetalingtype.UTBETALING), vedtaksperiodeId1)
        val vedtaksperiode2 = beregnetPeriode(10.april til 28.april, utbetaling(Utbetalingtype.UTBETALING), vedtaksperiodeId2)
        val generasjoner = byggGenerasjoner(
            vedtaksperiode1,
            vedtaksperiode2,
            beregnetPeriode(1.januar til 31.januar, utbetaling(Utbetalingtype.UTBETALING)),
            beregnetPeriode(11.mars til 31.mars, utbetaling(Utbetalingtype.REVURDERING), vedtaksperiodeId1, forrigeGenerasjon = vedtaksperiode1),
            beregnetPeriode(10.april til 28.april, utbetaling(Utbetalingtype.REVURDERING), vedtaksperiodeId2, forrigeGenerasjon = vedtaksperiode2)
        )
        generasjoner(3) {
            generasjon(0, 3) {
                periode(0) fra 10.april til 28.april er beregnet
                periode(1) fra 11.mars til 31.mars er beregnet
                periode(2) fra 1.januar til 31.januar er beregnet
            }
            generasjon(1, 3) {
                periode(0) fra 10.april til 28.april er beregnet
                periode(1) fra 11.mars til 31.mars er beregnet
                periode(2) fra 1.januar til 31.januar er beregnet
            }
            generasjon(2, 2) {
                periode(0) fra 10.april til 28.april er beregnet
                periode(1) fra 11.mars til 31.mars er beregnet
            }
        }
    }

    @Test
    fun `annullering lager ny rad`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjoner = byggGenerasjoner(
            uberegnetPeriode(1.mars til 10.mars),
            beregnetPeriode(11.mars til 31.mars, utbetaling(Utbetalingtype.UTBETALING), vedtaksperiodeId),
            annullertPeriode(11.mars til 31.mars, utbetaling(Utbetalingtype.ANNULLERING), vedtaksperiodeId),
        )
        generasjoner(2) {
            generasjon(0, 2) {
                periode(0) fra 11.mars til 31.mars er beregnet
                periode(1) fra 1.mars til 10.mars er uberegnet
            }
            generasjon(1, 2) {
                periode(0) fra 11.mars til 31.mars er beregnet
                periode(1) fra 1.mars til 10.mars er uberegnet
            }
        }
    }

    @Test
    fun `annullering etter en annen annullering fortsetter på samme rad`() {
        val vedtaksperiodeId1 = UUID.randomUUID()

        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjoner = byggGenerasjoner(
            beregnetPeriode(1.mars til 31.mars, utbetaling(Utbetalingtype.UTBETALING), vedtaksperiodeId1),
            beregnetPeriode(1.mai til 31.mai, utbetaling(Utbetalingtype.UTBETALING), vedtaksperiodeId2),
            annullertPeriode(1.mai til 31.mai, utbetaling(Utbetalingtype.ANNULLERING), vedtaksperiodeId2),
            annullertPeriode(1.mars til 31.mars, utbetaling(Utbetalingtype.ANNULLERING), vedtaksperiodeId1),
        )
        generasjoner(2) {
            generasjon(0, 2) {
                periode(0) fra 1.mai til 31.mai er beregnet
                periode(1) fra 1.mars til 31.mars er beregnet
            }
            generasjon(1, 2) {
                periode(0) fra 1.mai til 31.mai er beregnet
                periode(1) fra 1.mars til 31.mars er beregnet
            }
        }
    }

    @Test
    fun `revurdering lager ny rad`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjoner = byggGenerasjoner(
            uberegnetPeriode(1.mars til 10.mars),
            beregnetPeriode(11.mars til 31.mars, utbetaling(Utbetalingtype.UTBETALING), vedtaksperiodeId),
            beregnetPeriode(11.mars til 31.mars, utbetaling(Utbetalingtype.REVURDERING), vedtaksperiodeId),

        )
        generasjoner(2) {
            generasjon(0, 2) {
                periode(0) fra 11.mars til 31.mars er beregnet
                periode(1) fra 1.mars til 10.mars er uberegnet
            }
            generasjon(1, 2) {
                periode(0) fra 11.mars til 31.mars er beregnet
                periode(1) fra 1.mars til 10.mars er uberegnet
            }
        }
    }

    @Test
    fun `samme revurdering fortsetter på samme rad`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val revurderingId1 = UUID.randomUUID()
        val revurderingId2 = UUID.randomUUID()
        val vilkårsgrunnlagId = UUID.randomUUID()
        val vedtaksperiode1 = beregnetPeriode(1.mars til 31.mars, utbetaling(Utbetalingtype.UTBETALING), vedtaksperiodeId1)
        val vedtaksperiode2 = beregnetPeriode(1.april til 30.april, utbetaling(Utbetalingtype.UTBETALING), vedtaksperiodeId2)
        val generasjoner = byggGenerasjoner(
            vedtaksperiode1,
            vedtaksperiode2,
            beregnetPeriode(1.mars til 31.mars, utbetaling(Utbetalingtype.REVURDERING, utbetalingId = revurderingId1), vedtaksperiodeId1, vilkårsgrunnlagId, vedtaksperiode1),
            beregnetPeriode(1.april til 30.april, utbetaling(Utbetalingtype.REVURDERING, utbetalingId = revurderingId2), vedtaksperiodeId2, vilkårsgrunnlagId, vedtaksperiode2),

        )
        generasjoner(2) {
            generasjon(0, 2) {
                periode(0) fra 1.april til 30.april er beregnet
                periode(1) fra 1.mars til 31.mars er beregnet
            }
            generasjon(1, 2) {
                periode(0) fra 1.april til 30.april er beregnet
                periode(1) fra 1.mars til 31.mars er beregnet
            }
        }
    }

    @Test
    fun `ny revurdering lager ny rad`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val revurderingId1 = UUID.randomUUID()
        val revurderingId2 = UUID.randomUUID()
        val vedtaksperiode1 = beregnetPeriode(1.mars til 31.mars, utbetaling(Utbetalingtype.UTBETALING), vedtaksperiodeId1)
        val vedtaksperiode2 = beregnetPeriode(1.april til 30.april, utbetaling(Utbetalingtype.UTBETALING), vedtaksperiodeId2)
        val generasjoner = byggGenerasjoner(
            vedtaksperiode1,
            vedtaksperiode2,
            beregnetPeriode(1.mars til 31.mars, utbetaling(Utbetalingtype.REVURDERING, utbetalingId = revurderingId1), vedtaksperiodeId1, forrigeGenerasjon = vedtaksperiode1),
            beregnetPeriode(1.april til 30.april, utbetaling(Utbetalingtype.REVURDERING, utbetalingId = revurderingId2), vedtaksperiodeId2, forrigeGenerasjon = vedtaksperiode2),

        )
        generasjoner(3) {
            generasjon(0, 2) {
                periode(0) fra 1.april til 30.april er beregnet
                periode(1) fra 1.mars til 31.mars er beregnet
            }
            generasjon(1, 2) {
                periode(0) fra 1.april til 30.april er beregnet
                periode(1) fra 1.mars til 31.mars er beregnet
            }
            generasjon(2, 2) {
                periode(0) fra 1.april til 30.april er beregnet
                periode(1) fra 1.mars til 31.mars er beregnet
            }
        }
    }

    @Test
    fun `revurdere etter annullering`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjoner = byggGenerasjoner(
            beregnetPeriode(1.mars til 31.mars, utbetaling(Utbetalingtype.UTBETALING), vedtaksperiodeId1),
            beregnetPeriode(1.april til 30.april, utbetaling(Utbetalingtype.UTBETALING), vedtaksperiodeId2),
            annullertPeriode(1.april til 30.april, utbetaling(Utbetalingtype.ANNULLERING), vedtaksperiodeId2),
            beregnetPeriode(1.mars til 31.mars, utbetaling(Utbetalingtype.REVURDERING), vedtaksperiodeId1),
        )
        generasjoner(3) {
            generasjon(0, 2) {
                periode(0) fra 1.april til 30.april er beregnet
                periode(1) fra 1.mars til 31.mars er beregnet
            }
            generasjon(1, 2) {
                periode(0) fra 1.april til 30.april er beregnet
                periode(1) fra 1.mars til 31.mars er beregnet
            }
            generasjon(2, 2) {
                periode(0) fra 1.april til 30.april er beregnet
                periode(1) fra 1.mars til 31.mars er beregnet
            }
        }
    }

    private fun byggGenerasjoner(vararg perioder: Tidslinjeperiode) = perioder.toList().generasjoner
    private val List<Tidslinjeperiode>.generasjoner get() = Generasjoner().apply {
        sorterEtterHendelse().forEach { it.tilGenerasjon(this) }
    }.build()

    private operator fun List<GenerasjonDTO>.invoke(forventetAntall: Int, assertBlock: List<GenerasjonDTO>.() -> Unit) {
        assertEquals(forventetAntall, this.size)
        assertBlock(this)
    }
    private fun List<GenerasjonDTO>.generasjon(index: Int, forventetAntall: Int, assertBlock: GenerasjonDTO.() -> Unit) {
        assertEquals(forventetAntall, this[index].perioder.size)
        assertBlock(this[index])
    }
    private fun GenerasjonDTO.periode(index: Int, assertBlock: Tidslinjeperiode.() -> Unit = { }): Tidslinjeperiode {
        assertBlock(this.perioder[index])
        return this.perioder[index]
    }
    private enum class Hva(val kClass: KClass<out Tidslinjeperiode>) { beregnet(BeregnetPeriode::class), uberegnet(UberegnetPeriode::class) }
    private infix fun Tidslinjeperiode.er(hva: Hva) = apply {
        assertEquals(hva.kClass, this::class)
    }
    private infix fun Tidslinjeperiode.fra(fom: LocalDate) = apply {
        assertEquals(fom, this.fom)
    }
    private infix fun Tidslinjeperiode.til(tom: LocalDate) = apply {
        assertEquals(tom, this.tom)
    }

    private fun utbetaling(type: Utbetalingtype, utbetalingId: UUID = UUID.randomUUID()) = Utbetaling(
        type = type,
        korrelasjonsId = UUID.randomUUID(),
        status = Utbetalingstatus.Utbetalt,
        arbeidsgiverNettoBeløp = 0,
        personNettoBeløp = 0,
        arbeidsgiverFagsystemId = "",
        personFagsystemId = "",
        oppdrag = emptyMap(),
        vurdering = null,
        id = utbetalingId
    )
    private fun uberegnetPeriode(periode: Periode) = UberegnetPeriode(
        vedtaksperiodeId = UUID.randomUUID(),
        fom = periode.start,
        tom = periode.endInclusive,
        sammenslåttTidslinje = emptyList(),
        periodetype = Tidslinjeperiodetype.FØRSTEGANGSBEHANDLING,
        inntektskilde = UtbetalingInntektskilde.EN_ARBEIDSGIVER,
        erForkastet = false,
        sorteringstidspunkt = LocalDateTime.now(),
        opprettet = LocalDateTime.now(),
        oppdatert = LocalDateTime.now(),
        periodetilstand = Periodetilstand.IngenUtbetaling,
        skjæringstidspunkt = periode.start,
        hendelser = emptyList()
    )
    private fun beregnetPeriode(periode: Periode, utbetaling: Utbetaling, vedtaksperiodeId: UUID = UUID.randomUUID(), vilkårsgrunnlagId: UUID = UUID.randomUUID(), forrigeGenerasjon: BeregnetPeriode? = null) = BeregnetPeriode(
        vedtaksperiodeId = vedtaksperiodeId,
        fom = periode.start,
        tom = periode.endInclusive,
        sammenslåttTidslinje = emptyList(),
        periodetype = Tidslinjeperiodetype.FØRSTEGANGSBEHANDLING,
        inntektskilde = UtbetalingInntektskilde.EN_ARBEIDSGIVER,
        erForkastet = false,
        opprettet = LocalDateTime.now(),
        beregnet = LocalDateTime.now(),
        oppdatert = LocalDateTime.now(),
        periodetilstand = Periodetilstand.IngenUtbetaling,
        skjæringstidspunkt = periode.start,
        beregningId = UUID.randomUUID(),
        gjenståendeSykedager = 0,
        forbrukteSykedager = 0,
        maksdato = 28.desember,
        utbetaling = utbetaling,
        hendelser = emptyList(),
        periodevilkår = BeregnetPeriode.Vilkår(
            BeregnetPeriode.Sykepengedager(periode.start, 28.desember, 0, 0, true),
            BeregnetPeriode.Alder(30, true)
        ),
        vilkårsgrunnlagId = vilkårsgrunnlagId,
        forrigeGenerasjon = forrigeGenerasjon
    )

    fun annullertPeriode(periode: Periode, utbetaling: Utbetaling, vedtaksperiodeId: UUID = UUID.randomUUID()) = AnnullertPeriode(
        vedtaksperiodeId = vedtaksperiodeId,
        fom = periode.start,
        tom = periode.endInclusive,
        opprettet = LocalDateTime.now(),
        vilkår = BeregnetPeriode.Vilkår(
            BeregnetPeriode.Sykepengedager(periode.start, 28.desember, null, null, true),
            BeregnetPeriode.Alder(30, true)
        ),
        beregnet = LocalDateTime.now(),
        oppdatert = LocalDateTime.now(),
        periodetilstand = Periodetilstand.Annullert,
        hendelser = emptyList(),
        beregningId = UUID.randomUUID(),
        utbetaling = utbetaling
    )
}