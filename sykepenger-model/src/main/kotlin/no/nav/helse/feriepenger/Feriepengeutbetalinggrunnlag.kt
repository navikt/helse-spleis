package no.nav.helse.feriepenger

import no.nav.helse.dto.serialisering.FeriepengeutbetalinggrunnlagUtDto
import no.nav.helse.dto.serialisering.UtbetaltDagUtDto
import no.nav.helse.dto.deserialisering.FeriepengeutbetalinggrunnlagInnDto
import java.time.LocalDate
import java.time.Year

internal data class Feriepengeutbetalinggrunnlag(
    val opptjeningsår: Year,
    val utbetalteDager: List<UtbetaltDag>,
    val feriepengedager: List<UtbetaltDag>
) {
    val datoer = feriepengedager.map { it.dato }.distinct()

    internal fun dto() = FeriepengeutbetalinggrunnlagUtDto(
        opptjeningsår = this.opptjeningsår,
        utbetalteDager = this.utbetalteDager.map { it.dto() },
        feriepengedager = this.feriepengedager.map { it.dto() }
    )

    internal sealed class UtbetaltDag(
        val orgnummer: String,
        val dato: LocalDate,
        val beløp: Int
    ) {
        internal companion object {
            internal fun gjenopprett(dto: FeriepengeutbetalinggrunnlagInnDto.UtbetaltDagInnDto) =
                when (dto) {
                    is FeriepengeutbetalinggrunnlagInnDto.UtbetaltDagInnDto.InfotrygdArbeidsgiver -> InfotrygdArbeidsgiver.gjenopprett(dto)
                    is FeriepengeutbetalinggrunnlagInnDto.UtbetaltDagInnDto.InfotrygdPerson -> InfotrygdPerson.gjenopprett(dto)
                    is FeriepengeutbetalinggrunnlagInnDto.UtbetaltDagInnDto.SpleisArbeidsgiver -> SpleisArbeidsgiver.gjenopprett(dto)
                    is FeriepengeutbetalinggrunnlagInnDto.UtbetaltDagInnDto.SpleisPerson -> SpleisPerson.gjenopprett(dto)
                }
        }

        internal class InfotrygdPerson(
            orgnummer: String,
            dato: LocalDate,
            beløp: Int
        ) : UtbetaltDag(orgnummer, dato, beløp) {
            override fun dto() = UtbetaltDagUtDto.InfotrygdPerson(orgnummer, dato, beløp)

            internal companion object {
                internal fun gjenopprett(dto: FeriepengeutbetalinggrunnlagInnDto.UtbetaltDagInnDto.InfotrygdPerson): InfotrygdPerson {
                    return InfotrygdPerson(
                        orgnummer = dto.orgnummer,
                        dato = dto.dato,
                        beløp = dto.beløp
                    )
                }
            }
        }

        internal class InfotrygdArbeidsgiver(
            orgnummer: String,
            dato: LocalDate,
            beløp: Int
        ) : UtbetaltDag(orgnummer, dato, beløp) {
            override fun dto() = UtbetaltDagUtDto.InfotrygdArbeidsgiver(orgnummer, dato, beløp)

            internal companion object {
                internal fun gjenopprett(dto: FeriepengeutbetalinggrunnlagInnDto.UtbetaltDagInnDto.InfotrygdArbeidsgiver): InfotrygdArbeidsgiver {
                    return InfotrygdArbeidsgiver(
                        orgnummer = dto.orgnummer,
                        dato = dto.dato,
                        beløp = dto.beløp
                    )
                }
            }
        }

        internal class SpleisArbeidsgiver(
            orgnummer: String,
            dato: LocalDate,
            beløp: Int
        ) : UtbetaltDag(orgnummer, dato, beløp) {
            override fun dto() = UtbetaltDagUtDto.SpleisArbeidsgiver(orgnummer, dato, beløp)

            internal companion object {
                internal fun gjenopprett(dto: FeriepengeutbetalinggrunnlagInnDto.UtbetaltDagInnDto.SpleisArbeidsgiver): SpleisArbeidsgiver {
                    return SpleisArbeidsgiver(
                        orgnummer = dto.orgnummer,
                        dato = dto.dato,
                        beløp = dto.beløp
                    )
                }
            }
        }

        internal class SpleisPerson(
            orgnummer: String,
            dato: LocalDate,
            beløp: Int
        ) : UtbetaltDag(orgnummer, dato, beløp) {
            override fun dto() = UtbetaltDagUtDto.SpleisPerson(orgnummer, dato, beløp)

            internal companion object {
                internal fun gjenopprett(dto: FeriepengeutbetalinggrunnlagInnDto.UtbetaltDagInnDto.SpleisPerson): SpleisPerson {
                    return SpleisPerson(
                        orgnummer = dto.orgnummer,
                        dato = dto.dato,
                        beløp = dto.beløp
                    )
                }
            }
        }

        abstract fun dto(): UtbetaltDagUtDto
    }
}
