package no.nav.helse

class SykdomManager(private val makskapasitet: Int) {
    private val personer = mutableListOf<String>()
    private val observers = mutableListOf<Observer>()

    fun registrer(fødselsnummer: String): Boolean {
        if (fødselsnummer in personer) return true
        if (personer.size >= makskapasitet) return false
        personer.add(fødselsnummer)
        observers.forEach { it.personRegistrert(fødselsnummer, makskapasitet - personer.size) }
        return true
    }

    fun addObserver(observer: Observer) {
        observers.add(observer)
    }

    companion object {
        fun gjennopprett(personer: List<String>, makskapasitet: Int): SykdomManager {
            return SykdomManager(makskapasitet).apply { this.personer.addAll(personer) }
        }
    }

    interface Observer {
        fun personRegistrert(fødselsnummer: String, kapasitet: Int)
    }

}
