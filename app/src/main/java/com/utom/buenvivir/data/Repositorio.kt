package com.utom.buenvivir.data

data class Toma(
    val id: Int,
    val medicina: String,
    val para: String,
    val hora: String,
    val cantidad: String,
    val indicacion: String
)

data class ProximaToma(val medicina: String, val cuando: String)

enum class EstadoDia { Completo, Falto, Hoy, Futuro }

data class DiaSemana(val letra: String, val nombre: String, val estado: EstadoDia)

data class Medicina(
    val nombre: String,
    val frecuencia: String,
    val para: String,
    val cantidad: String,
    val indicacion: String,
    val horarios: List<String>,
    val video: String,
    val id: Long = 0          // id en la base del celular, para abrir su detalle
)

/** Alguien que acompaña al paciente: un familiar o su médico. */
data class Contacto(val nombre: String, val relacion: String)

/** Lo que el paciente puede cambiar en Ajustes. */
data class Ajustes(
    val sonido: Boolean = true,
    val vibracion: Boolean = true,
    val tono: String = "Alarma",
    val repetirMinutos: Int = 10,
    val volumen: Int = 2,            // 0 Bajo, 1 Medio, 2 Alto
    val avisarEnReloj: Boolean = true,
    val vibrarEnReloj: Boolean = true,
    val sonarEnReloj: Boolean = false,
    val registrarDesdeReloj: Boolean = false,
    val temaOscuro: Boolean = false,
    val vozLenta: Boolean = true
)

/** Una toma pasada: si no la tomó, va el motivo. */
data class TomaPasada(
    val dia: String,
    val hora: String,               // la hora a la que tocaba
    val motivo: String? = null,
    val horaReal: String? = null    // la hora a la que de verdad se la tomó
)

/**
 * Datos fijos (nombre, motivos, familia, reloj) y datos de ejemplo para las vistas previas.
 * Las medicinas y las tomas reales vienen de la base de datos: ver RepositorioLocal.
 */
object Repositorio {
    val nombrePaciente = "Don José"
    val totalHoy = 2
    val tomadasAntes = 1

    val tomaActual = Toma(
        id = 2,
        medicina = "Metformina",
        para = "Para controlar el azúcar",
        hora = "8:00 p. m.",
        cantidad = "1 pastilla blanca",
        indicacion = "después de cenar"
    )

    /** La toma de la mañana, que ya se registró. */
    val primeraTomaHoy = Toma(
        id = 1,
        medicina = "Metformina",
        para = "Para controlar el azúcar",
        hora = "8:00 a. m.",
        cantidad = "1 pastilla blanca",
        indicacion = "después del desayuno"
    )

    val siguiente = ProximaToma(medicina = "Metformina", cuando = "Mañana · 8:00 a. m.")

    /** Semana de ejemplo: lunes 12 a domingo 18 (hoy). */
    val semana = listOf(
        DiaSemana("L", "Lunes", EstadoDia.Falto),
        DiaSemana("M", "Martes", EstadoDia.Completo),
        DiaSemana("M", "Miércoles", EstadoDia.Completo),
        DiaSemana("J", "Jueves", EstadoDia.Completo),
        DiaSemana("V", "Viernes", EstadoDia.Completo),
        DiaSemana("S", "Sábado", EstadoDia.Falto),
        DiaSemana("D", "Hoy", EstadoDia.Hoy)
    )

    /** Tomas de lunes a sábado (sin contar hoy). */
    val tomadasSemanaAntes = 10
    val totalSemanaAntes = 12

    val medicinas = listOf(
        Medicina(
            nombre = "Metformina",
            frecuencia = "2 veces al día",
            para = "Para controlar el azúcar",
            cantidad = "1 pastilla blanca",
            indicacion = "después del desayuno y después de la cena",
            horarios = listOf("8:00 a. m.", "8:00 p. m."),
            video = "¿Para qué sirve la metformina?"
        )
    )

    /** Últimas tomas, de la más nueva a la más vieja. */
    val historial = listOf(
        TomaPasada("Hoy", "8:00 a. m."),
        TomaPasada("Ayer", "8:00 p. m.", motivo = "Se me olvidó"),
        TomaPasada("Ayer", "8:00 a. m."),
        TomaPasada("Lunes 12", "8:00 p. m."),
        TomaPasada("Lunes 12", "8:00 a. m.", motivo = "No tenía la medicina")
    )

    val familiares = listOf(Contacto("María", "Tu hija"))
    val medico = Contacto("Dra. Laura Méndez", "Centro de Salud")

    val ajustesIniciales = Ajustes()

    /** Reloj de ejemplo; después vendrá de la conexión real con Wear OS. */
    val relojNombre = "Galaxy Watch4"
    val relojConectado = true

    val motivos = listOf(
        "Se me olvidó",
        "No estaba en casa",
        "No tenía la medicina",
        "No tenía dinero para comprarla",
        "Me da miedo",
        "No entendí cómo tomarla",
        "Otro motivo"
    )
}
