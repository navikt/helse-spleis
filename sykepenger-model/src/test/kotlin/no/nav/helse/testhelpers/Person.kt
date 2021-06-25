package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal fun fangeSkjæringstidspunkt(person: Person): LocalDate {
    var dato: LocalDate? = null
    person.accept(object : PersonVisitor {
        override fun preVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            opprinneligPeriode: Periode,
            skjæringstidspunkt: LocalDate,
            periodetype: Periodetype,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            hendelseIder: Set<UUID>,
            inntektsmeldingInfo: InntektsmeldingInfo?,
            inntektskilde: Inntektskilde
        ) {
            dato = skjæringstidspunkt
        }
    })
    return requireNotNull(dato)
}

internal fun fangeSkjæringstidspunkt(targetVedtaksperiode: Vedtaksperiode): LocalDate {
    var dato: LocalDate? = null
    targetVedtaksperiode.accept(object : PersonVisitor {
        override fun preVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            periode: Periode,
            opprinneligPeriode: Periode,
            skjæringstidspunkt: LocalDate,
            periodetype: Periodetype,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            hendelseIder: Set<UUID>,
            inntektsmeldingInfo: InntektsmeldingInfo?,
            inntektskilde: Inntektskilde
        ) {
            dato = skjæringstidspunkt
        }
    })
    return requireNotNull(dato)
}
