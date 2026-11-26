package no.nav.helse.spleis.utboks

import no.nav.helse.dto.HendelseskildeDto
import no.nav.helse.dto.MeldingsreferanseDto
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.dto.SykdomstidslinjeDagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto
import no.nav.helse.januar
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.ANDRE_YTELSER_AAP
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.ANDRE_YTELSER_DAGPENGER
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.ANDRE_YTELSER_FORELDREPENGER
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.ANDRE_YTELSER_OMSORGSPENGER
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.ANDRE_YTELSER_OPPLÆRINGSPENGER
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.ANDRE_YTELSER_PLEIEPENGER
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.ANDRE_YTELSER_SVANGERSKAPSPENGER
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.ARBEIDSDAG
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.ARBEIDSGIVERDAG
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.ARBEID_IKKE_GJENOPPTATT_DAG
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.FERIEDAG
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.FORELDET_SYKEDAG
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.FRISK_HELGEDAG
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.MELDING_TIL_NAV_DAG
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.PERMISJONSDAG
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.PROBLEMDAG
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.SYKEDAG
import no.nav.helse.spleis.utboks.Avslagsdagerdagtype.UKJENT_DAG
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class AvslagsdagerdagtypeTest {
    @Test
    fun `sykedager og helgedager med sykdom`() {
        assertEquals(SYKEDAG, SykdomstidslinjeDagDto.SykedagDto(dato, kilde, grad).avslagsdagerdagtype())
        assertEquals(SYKEDAG, SykdomstidslinjeDagDto.SykHelgedagDto(dato, kilde, grad).avslagsdagerdagtype())
        assertEquals(SYKEDAG, SykdomstidslinjeDagDto.SykUtenforEøsDto(dato, kilde, grad).avslagsdagerdagtype())
        assertEquals(FORELDET_SYKEDAG, SykdomstidslinjeDagDto.ForeldetSykedagDto(dato, kilde, grad).avslagsdagerdagtype())
        assertEquals(ARBEIDSGIVERDAG, SykdomstidslinjeDagDto.ArbeidsgiverdagDto(dato, kilde, grad).avslagsdagerdagtype())
        assertEquals(ARBEIDSGIVERDAG, SykdomstidslinjeDagDto.ArbeidsgiverHelgedagDto(dato, kilde, grad).avslagsdagerdagtype())
        assertEquals(MELDING_TIL_NAV_DAG, SykdomstidslinjeDagDto.MeldingTilNavDagDto(dato, kilde, grad).avslagsdagerdagtype())
        assertEquals(MELDING_TIL_NAV_DAG, SykdomstidslinjeDagDto.MeldingTilNavHelgedagDto(dato, kilde, grad).avslagsdagerdagtype())
    }

    @Test
    fun `dager uten sykdom`() {
        assertEquals(ARBEIDSDAG, SykdomstidslinjeDagDto.ArbeidsdagDto(dato, kilde).avslagsdagerdagtype())
        assertEquals(FERIEDAG, SykdomstidslinjeDagDto.FeriedagDto(dato, kilde).avslagsdagerdagtype())
        assertEquals(ARBEID_IKKE_GJENOPPTATT_DAG, SykdomstidslinjeDagDto.ArbeidIkkeGjenopptattDagDto(dato, kilde).avslagsdagerdagtype())
        assertEquals(FRISK_HELGEDAG, SykdomstidslinjeDagDto.FriskHelgedagDto(dato, kilde).avslagsdagerdagtype())
        assertEquals(PERMISJONSDAG, SykdomstidslinjeDagDto.PermisjonsdagDto(dato, kilde).avslagsdagerdagtype())
        assertEquals(PROBLEMDAG, SykdomstidslinjeDagDto.ProblemDagDto(dato, kilde, kilde, "konflikt").avslagsdagerdagtype())
        assertEquals(UKJENT_DAG, SykdomstidslinjeDagDto.UkjentDagDto(dato, kilde).avslagsdagerdagtype())
    }

    @Test
    fun `andre ytelser`() {
        assertEquals(ANDRE_YTELSER_FORELDREPENGER, andreYtelser(YtelseDto.Foreldrepenger))
        assertEquals(ANDRE_YTELSER_AAP, andreYtelser(YtelseDto.AAP))
        assertEquals(ANDRE_YTELSER_OMSORGSPENGER, andreYtelser(YtelseDto.Omsorgspenger))
        assertEquals(ANDRE_YTELSER_PLEIEPENGER, andreYtelser(YtelseDto.Pleiepenger))
        assertEquals(ANDRE_YTELSER_SVANGERSKAPSPENGER, andreYtelser(YtelseDto.Svangerskapspenger))
        assertEquals(ANDRE_YTELSER_OPPLÆRINGSPENGER, andreYtelser(YtelseDto.Opplæringspenger))
        assertEquals(ANDRE_YTELSER_DAGPENGER, andreYtelser(YtelseDto.Dagpenger))
    }

    private val dato = 1.januar
    private val kilde = HendelseskildeDto("Søknad", MeldingsreferanseDto(UUID.randomUUID()), LocalDateTime.now())
    private val grad = ProsentdelDto(1.0)

    private fun andreYtelser(ytelse: YtelseDto) = SykdomstidslinjeDagDto.AndreYtelserDto(dato, kilde, ytelse).avslagsdagerdagtype()
}
