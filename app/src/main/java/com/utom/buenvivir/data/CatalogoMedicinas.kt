package com.utom.buenvivir.data

/**
 * Medicinas para la diabetes tipo 2 que el médico puede elegir al capturar una receta.
 *
 * Fuente: Compendio Nacional de Insumos para la Salud (CNIS), Grupo 5 "Endocrinología y Metabolismo",
 * Consejo de Salubridad General, edición 26/04/2021, consultado en vademecum.es/cnis (oct 2026).
 * Las clases de medicamentos coinciden con la NOM-015-SSA2-2010, punto 11.9.1.
 *
 * La dosis NUNCA se llena sola: [referencia] es solo la indicación del CNIS para que el médico la consulte.
 * Si la medicina no está aquí, el médico la puede escribir a mano.
 */
data class MedicinaCatalogo(
    val nombre: String,
    val para: String,
    val presentaciones: List<String>,
    val forma: Forma,
    val referencia: String
)

enum class Forma(val unidad: String) {
    TABLETA("tableta"),
    INYECCION("unidades")
}

object CatalogoMedicinas {
    private const val AZUCAR = "Para controlar el azúcar"

    val lista = listOf(
        MedicinaCatalogo(
            "Metformina", AZUCAR,
            listOf("850 mg", "500 mg liberación prolongada", "750 mg liberación prolongada"),
            Forma.TABLETA,
            "CNIS: 850 mg cada 12 horas con los alimentos. Dosis máxima 2550 mg al día."
        ),
        MedicinaCatalogo(
            "Glibenclamida", AZUCAR, listOf("5 mg"), Forma.TABLETA,
            "CNIS: 2.5 a 5 mg cada 24 horas, después de los alimentos. Dosis máxima 20 mg al día."
        ),
        MedicinaCatalogo(
            "Glimepirida", AZUCAR, listOf("2 mg"), Forma.TABLETA,
            "CNIS: inicial 1 mg una vez al día; habitual 1 a 4 mg al día."
        ),
        MedicinaCatalogo(
            "Sitagliptina", AZUCAR, listOf("50 mg", "100 mg"), Forma.TABLETA,
            "CNIS: 50 mg cada 12 horas o 100 mg cada 24 horas."
        ),
        MedicinaCatalogo(
            "Linagliptina", AZUCAR, listOf("5 mg"), Forma.TABLETA,
            "CNIS: 5 mg cada 24 horas."
        ),
        MedicinaCatalogo(
            "Dapagliflozina", AZUCAR, listOf("10 mg"), Forma.TABLETA,
            "CNIS: 10 mg cada 24 horas, en combinación con metformina."
        ),
        MedicinaCatalogo(
            "Empagliflozina", AZUCAR, listOf("10 mg", "25 mg"), Forma.TABLETA,
            "CNIS: 10 mg una vez al día; puede subir a 25 mg. En combinación con metformina."
        ),
        MedicinaCatalogo(
            "Pioglitazona", AZUCAR, listOf("15 mg"), Forma.TABLETA,
            "CNIS: 15 a 30 mg cada 24 horas."
        ),
        MedicinaCatalogo(
            "Insulina glargina", AZUCAR, listOf("3.64 mg/ml"), Forma.INYECCION,
            "CNIS: una vez al día, por la noche. La dosis la ajusta el médico."
        )
    )

    /** Busca sin importar mayúsculas ni acentos: "metf" encuentra Metformina. */
    fun buscar(texto: String): List<MedicinaCatalogo> {
        val t = sinAcentos(texto.trim())
        if (t.isEmpty()) return emptyList()
        return lista.filter { sinAcentos(it.nombre).contains(t) }
    }

    private fun sinAcentos(s: String) = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
}
