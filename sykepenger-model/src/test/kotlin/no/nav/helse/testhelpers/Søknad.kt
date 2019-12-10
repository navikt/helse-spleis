package no.nav.helse.testhelpers

import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.hendelser.søknad.Sykepengesøknad
import no.nav.helse.toJsonNode
import no.nav.syfo.kafka.sykepengesoknad.dto.PeriodeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsperiodeDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import no.nav.syfo.kafka.sykepengesoknad.dto.SykepengesoknadDTO
import java.time.LocalDate

/* Dette er en hjelpeklasse for å generere testdata. Det er viktig at defaults her holdes til absolutt minimum, slik at
 * en ikke ender opp med tester som er avhengig av sære defaults i søknaden
 */
internal fun <Type, Builder> søknad(søknadtype: Søknadtype<Type, Builder>, block: Builder.() -> Unit) =
    søknadtype.søknad(block)

internal interface Søknadtype<Type, Builder> {
    fun søknad(block: Builder.() -> Unit): Type
}

internal object NySøknadHendelseWrapper : Søknadtype<NySøknadHendelse, SykepengesoknadDTOBuilder> {
    override fun søknad(block: SykepengesoknadDTOBuilder.() -> Unit) =
        NySøknadHendelseBuilder.søknad {
            sykepengesøknad {
                søknad {
                    status = SoknadsstatusDTO.NY
                    apply(block)
                }
            }
        }
}

internal object SendtSøknadHendelseWrapper : Søknadtype<SendtSøknadHendelse, SykepengesoknadDTOBuilder> {
    override fun søknad(block: SykepengesoknadDTOBuilder.() -> Unit) =
        SendtSøknadHendelseBuilder.søknad {
            sykepengesøknad {
                søknad {
                    status = SoknadsstatusDTO.SENDT
                    apply(block)
                }
            }
        }

}

internal object SykepengesøknadWrapper : Søknadtype<Sykepengesøknad, SykepengesoknadDTOBuilder> {
    override fun søknad(block: SykepengesoknadDTOBuilder.() -> Unit) =
        SykepengesøknadBuilder.søknad { søknad(block) }
}

internal class NySøknadHendelseBuilder {
    private lateinit var sykepengesøknad: Sykepengesøknad

    internal fun sykepengesøknad(block: SykepengesøknadBuilder.() -> Unit) {
        sykepengesøknad = SykepengesøknadBuilder.søknad(block)
    }

    private fun build() = NySøknadHendelse(sykepengesøknad)

    internal companion object Type : Søknadtype<NySøknadHendelse, NySøknadHendelseBuilder> {
        override fun søknad(block: NySøknadHendelseBuilder.() -> Unit) =
            NySøknadHendelseBuilder().apply(block).build()
    }
}

internal class SendtSøknadHendelseBuilder {
    private lateinit var sykepengesøknad: Sykepengesøknad

    internal fun sykepengesøknad(block: SykepengesøknadBuilder.() -> Unit) {
        sykepengesøknad = SykepengesøknadBuilder.søknad(block)
    }

    private fun build() = SendtSøknadHendelse(sykepengesøknad)

    internal companion object Type : Søknadtype<SendtSøknadHendelse, SendtSøknadHendelseBuilder> {
        override fun søknad(block: SendtSøknadHendelseBuilder.() -> Unit) =
            SendtSøknadHendelseBuilder().apply(block).build()
    }
}

internal class SykepengesøknadBuilder {
    private lateinit var søknad: SykepengesoknadDTO

    internal fun søknad(block: SykepengesoknadDTOBuilder.() -> Unit) {
        søknad = SykepengesoknadDTOBuilder.søknad(block)
    }

    private fun build() = Sykepengesøknad(søknad.toJsonNode())

    internal companion object Type : Søknadtype<Sykepengesøknad, SykepengesøknadBuilder> {
        override fun søknad(block: SykepengesøknadBuilder.() -> Unit) =
            SykepengesøknadBuilder().apply(block).build()
    }
}

internal class SykepengesoknadDTOBuilder {
    internal lateinit var fom: LocalDate
    internal lateinit var tom: LocalDate
    internal lateinit var status: SoknadsstatusDTO
    private val perioder = mutableListOf<SoknadsperiodeDTO>()
    private val egenmeldinger = mutableListOf<PeriodeDTO>()

    internal fun søknadsperiode(block: SøknadsperiodeBuilder.() -> Unit) {
        perioder.add(SøknadsperiodeBuilder.søknadsperiode(block))
    }

    internal fun egenmelding(block: PeriodeBuilder.() -> Unit) {
        egenmeldinger.add(PeriodeBuilder.periode(block))
    }

    private fun build(): SykepengesoknadDTO {
        perioder.takeIf { it.isEmpty() }?.add(SoknadsperiodeDTO(fom = fom, tom = tom))
        return SykepengesoknadDTO(
            fom = fom,
            tom = tom,
            soknadsperioder = perioder,
            egenmeldinger = egenmeldinger,
            status = status
        )
    }

    internal companion object Type : Søknadtype<SykepengesoknadDTO, SykepengesoknadDTOBuilder> {
        override fun søknad(block: SykepengesoknadDTOBuilder.() -> Unit) =
            SykepengesoknadDTOBuilder().apply(block).build()
    }
}

internal class SøknadsperiodeBuilder {
    internal lateinit var fom: LocalDate
    internal lateinit var tom: LocalDate
    private fun build() = SoknadsperiodeDTO(fom, tom)

    internal companion object {
        internal fun søknadsperiode(block: SøknadsperiodeBuilder.() -> Unit) =
            SøknadsperiodeBuilder().apply(block).build()
    }
}

internal class PeriodeBuilder {
    internal lateinit var fom: LocalDate
    internal lateinit var tom: LocalDate
    private fun build() = PeriodeDTO(fom, tom)

    internal companion object {
        fun periode(block: PeriodeBuilder.() -> Unit) = PeriodeBuilder().apply(block).build()
    }
}
