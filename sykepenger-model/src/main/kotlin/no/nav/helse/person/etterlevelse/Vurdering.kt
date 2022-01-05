package no.nav.helse.person.etterlevelse

import no.nav.helse.person.Bokstav
import no.nav.helse.person.Ledd
import no.nav.helse.person.Paragraf
import no.nav.helse.person.Punktum
import java.time.LocalDate

class Vurdering internal constructor(
    private val oppfylt: Boolean,
    private val versjon: LocalDate,
    private val paragraf: Paragraf,
    private val ledd: Ledd,
    private val punktum: List<Punktum>,
    private val bokstaver: List<Bokstav> = emptyList(),
    private val inputdata: Map<Any, Any?>,
    private val outputdata: Map<Any, Any?>
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vurdering || javaClass != other.javaClass) return false

        return oppfylt == other.oppfylt
            && versjon == other.versjon
            && paragraf == other.paragraf
            && ledd == other.ledd
            && punktum == other.punktum
            && bokstaver == other.bokstaver
            && inputdata == other.inputdata
            && outputdata == other.outputdata
    }

    override fun hashCode(): Int {
        var result = oppfylt.hashCode()
        result = 31 * result + versjon.hashCode()
        result = 31 * result + paragraf.hashCode()
        result = 31 * result + ledd.hashCode()
        result = 31 * result + punktum.hashCode()
        result = 31 * result + bokstaver.hashCode()
        result = 31 * result + inputdata.hashCode()
        result = 31 * result + outputdata.hashCode()
        return result
    }

}
