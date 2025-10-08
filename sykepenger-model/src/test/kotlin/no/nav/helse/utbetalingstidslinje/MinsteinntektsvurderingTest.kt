package no.nav.helse.utbetalingstidslinje

import no.nav.helse.Grunnbeløp.Companion.`2G`
import no.nav.helse.Grunnbeløp.Companion.halvG
import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.utbetalingstidslinje.Minsteinntektsvurdering.Companion.lagMinsteinntektsvurdering
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MinsteinntektsvurderingTest {

    @Test
    fun `innenfor krav om minsteinntekt til fylte 67`() {
        val skjæringstidspunkt = 1.januar
        val minsteinntektkravTilFylte67 = halvG.minsteinntekt(skjæringstidspunkt)
        val vurdering = lagMinsteinntektsvurdering(
            skjæringstidspunkt = skjæringstidspunkt,
            vedtaksperiode = februar,
            sykepengegrunnlag = minsteinntektkravTilFylte67,
            redusertYtelseAlder = 1.april
        )
        assertNull(vurdering.periodeTilFylte67UnderMinsteinntekt)
        assertNull(vurdering.periodeEtterFylte67UnderMinsteinntekt)
        assertFalse(vurdering.erUnderMinsteinntektskravTilFylte67)
        assertTrue(vurdering.erUnderMinsteinntektEtterFylte67)
        assertFalse(vurdering.erUnderMinsteinntektskrav)
    }

    @Test
    fun `innenfor krav om minsteinntekt etter fylte 67`() {
        val skjæringstidspunkt = 1.januar
        val minsteinntektkravTilFylte67 = `2G`.minsteinntekt(skjæringstidspunkt)
        val vurdering = lagMinsteinntektsvurdering(
            skjæringstidspunkt = skjæringstidspunkt,
            vedtaksperiode = februar,
            sykepengegrunnlag = minsteinntektkravTilFylte67,
            redusertYtelseAlder = 1.januar
        )
        assertNull(vurdering.periodeTilFylte67UnderMinsteinntekt)
        assertNull(vurdering.periodeEtterFylte67UnderMinsteinntekt)
        assertFalse(vurdering.erUnderMinsteinntektskravTilFylte67)
        assertFalse(vurdering.erUnderMinsteinntektEtterFylte67)
        assertFalse(vurdering.erUnderMinsteinntektskrav)
    }

    @Test
    fun `innenfor krav om minsteinntekt til fylte 67 - på 67års dagen`() {
        val skjæringstidspunkt = 1.januar
        val minsteinntektkravTilFylte67 = halvG.minsteinntekt(skjæringstidspunkt)
        val vurdering = lagMinsteinntektsvurdering(
            skjæringstidspunkt = skjæringstidspunkt,
            vedtaksperiode = 1.februar.somPeriode(),
            sykepengegrunnlag = minsteinntektkravTilFylte67,
            redusertYtelseAlder = 1.februar
        )
        assertNull(vurdering.periodeTilFylte67UnderMinsteinntekt)
        assertNull(vurdering.periodeEtterFylte67UnderMinsteinntekt)
        assertFalse(vurdering.erUnderMinsteinntektskravTilFylte67)
        assertTrue(vurdering.erUnderMinsteinntektEtterFylte67)
        assertFalse(vurdering.erUnderMinsteinntektskrav)
    }

    @Test
    fun `under krav om minsteinntekt til fylte 67`() {
        val skjæringstidspunkt = 1.januar
        val minsteinntektkravTilFylte67 = halvG.minsteinntekt(skjæringstidspunkt)
        val vurdering = lagMinsteinntektsvurdering(
            skjæringstidspunkt = skjæringstidspunkt,
            vedtaksperiode = februar,
            sykepengegrunnlag = minsteinntektkravTilFylte67 - 1.daglig,
            redusertYtelseAlder = 1.april
        )
        assertEquals(februar, vurdering.periodeTilFylte67UnderMinsteinntekt)
        assertNull(vurdering.periodeEtterFylte67UnderMinsteinntekt)
        assertTrue(vurdering.erUnderMinsteinntektskravTilFylte67)
        assertTrue(vurdering.erUnderMinsteinntektEtterFylte67)
        assertTrue(vurdering.erUnderMinsteinntektskrav)
    }

    @Test
    fun `innenfor krav om minsteinntekt til fylte 67, men under etter fylte 67`() {
        val skjæringstidspunkt = 1.januar
        val minsteinntektkravTilFylte67 = `2G`.minsteinntekt(skjæringstidspunkt)
        val vurdering = lagMinsteinntektsvurdering(
            skjæringstidspunkt = skjæringstidspunkt,
            vedtaksperiode = februar,
            sykepengegrunnlag = minsteinntektkravTilFylte67 - 1.daglig,
            redusertYtelseAlder = 10.februar
        )
        assertNull(vurdering.periodeTilFylte67UnderMinsteinntekt)
        assertEquals(11.februar til 28.februar, vurdering.periodeEtterFylte67UnderMinsteinntekt)
        assertFalse(vurdering.erUnderMinsteinntektskravTilFylte67)
        assertTrue(vurdering.erUnderMinsteinntektEtterFylte67)
        assertTrue(vurdering.erUnderMinsteinntektskrav)
    }
}
