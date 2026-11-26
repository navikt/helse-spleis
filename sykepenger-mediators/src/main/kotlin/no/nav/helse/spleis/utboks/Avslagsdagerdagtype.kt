package no.nav.helse.spleis.utboks

import no.nav.helse.dto.SykdomstidslinjeDagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto

/**
 * Dagtypene i sykdomstidslinja i behovet `Avslagsdager` til sp-avslagsdager. Kontrakten har sin egen enum, slik at
 * lagringsformatet i Spleis kan endres uten at behovet endres. Helgedager har samme dagtype som hverdagene.
 */
internal enum class Avslagsdagerdagtype {
    ARBEIDSDAG,
    ARBEIDSGIVERDAG,
    MELDING_TIL_NAV_DAG,
    FERIEDAG,
    ARBEID_IKKE_GJENOPPTATT_DAG,
    FRISK_HELGEDAG,
    FORELDET_SYKEDAG,
    PERMISJONSDAG,
    PROBLEMDAG,
    SYKEDAG,
    ANDRE_YTELSER_FORELDREPENGER,
    ANDRE_YTELSER_AAP,
    ANDRE_YTELSER_OMSORGSPENGER,
    ANDRE_YTELSER_PLEIEPENGER,
    ANDRE_YTELSER_SVANGERSKAPSPENGER,
    ANDRE_YTELSER_OPPLÆRINGSPENGER,
    ANDRE_YTELSER_DAGPENGER,
    UKJENT_DAG,
}

internal fun SykdomstidslinjeDagDto.avslagsdagerdagtype(): Avslagsdagerdagtype =
    when (this) {
        is SykdomstidslinjeDagDto.SykedagDto,
        is SykdomstidslinjeDagDto.SykHelgedagDto,
        // sp-avslagsdager finner utenlandsoppholdet i søknaden, så dagen er en sykedag i behovet
        is SykdomstidslinjeDagDto.SykUtenforEøsDto,
        -> Avslagsdagerdagtype.SYKEDAG
        is SykdomstidslinjeDagDto.ForeldetSykedagDto -> Avslagsdagerdagtype.FORELDET_SYKEDAG
        is SykdomstidslinjeDagDto.ArbeidsgiverdagDto,
        is SykdomstidslinjeDagDto.ArbeidsgiverHelgedagDto,
        -> Avslagsdagerdagtype.ARBEIDSGIVERDAG
        is SykdomstidslinjeDagDto.MeldingTilNavDagDto,
        is SykdomstidslinjeDagDto.MeldingTilNavHelgedagDto,
        -> Avslagsdagerdagtype.MELDING_TIL_NAV_DAG
        is SykdomstidslinjeDagDto.ArbeidsdagDto -> Avslagsdagerdagtype.ARBEIDSDAG
        is SykdomstidslinjeDagDto.FeriedagDto -> Avslagsdagerdagtype.FERIEDAG
        is SykdomstidslinjeDagDto.ArbeidIkkeGjenopptattDagDto -> Avslagsdagerdagtype.ARBEID_IKKE_GJENOPPTATT_DAG
        is SykdomstidslinjeDagDto.FriskHelgedagDto -> Avslagsdagerdagtype.FRISK_HELGEDAG
        is SykdomstidslinjeDagDto.PermisjonsdagDto -> Avslagsdagerdagtype.PERMISJONSDAG
        is SykdomstidslinjeDagDto.ProblemDagDto -> Avslagsdagerdagtype.PROBLEMDAG
        is SykdomstidslinjeDagDto.UkjentDagDto -> Avslagsdagerdagtype.UKJENT_DAG
        is SykdomstidslinjeDagDto.AndreYtelserDto ->
            when (ytelse) {
                YtelseDto.Foreldrepenger -> Avslagsdagerdagtype.ANDRE_YTELSER_FORELDREPENGER
                YtelseDto.AAP -> Avslagsdagerdagtype.ANDRE_YTELSER_AAP
                YtelseDto.Omsorgspenger -> Avslagsdagerdagtype.ANDRE_YTELSER_OMSORGSPENGER
                YtelseDto.Pleiepenger -> Avslagsdagerdagtype.ANDRE_YTELSER_PLEIEPENGER
                YtelseDto.Svangerskapspenger -> Avslagsdagerdagtype.ANDRE_YTELSER_SVANGERSKAPSPENGER
                YtelseDto.Opplæringspenger -> Avslagsdagerdagtype.ANDRE_YTELSER_OPPLÆRINGSPENGER
                YtelseDto.Dagpenger -> Avslagsdagerdagtype.ANDRE_YTELSER_DAGPENGER
            }
    }
