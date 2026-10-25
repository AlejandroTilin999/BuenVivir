package com.utom.buenvivir.data

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Cómo salió iniciar sesión, crear la cuenta o mandar el correo. */
sealed interface Resultado {
    data object Bien : Resultado
    data class Mal(val mensaje: String) : Resultado
}

/** Lo que escribió al crear su cuenta (médico o familiar). */
data class PerfilMedico(
    val nombre: String,
    val cedula: String,
    val lugar: String,
    val correo: String,
    val correoConfirmado: Boolean,
    val rol: String = SesionStore.MEDICO,
    val parentesco: String = ""      // solo familiar: "Hija", "Nieto"...
) {
    val esFamiliar get() = rol == SesionStore.FAMILIAR
}

/** Espera a que Firebase termine, sin usar otra librería. */
suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { c ->
    addOnSuccessListener { c.resume(it) }
    addOnFailureListener { c.resumeWithException(it) }
}

/**
 * Cuentas con correo y contraseña (médico; después también familiar).
 * El paciente no usa esto: él entra anónimo desde [Nube].
 *
 *   usuarios/{uid} -> rol, nombre, cedula, lugar, correo, creado
 */
object Cuenta {
    private const val MINIMO_CONTRASENA = 8
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    /** true si este celular ya tiene un paciente anónimo (para no volver a preguntar el rol). */
    fun esPacienteAnonimo() = auth.currentUser?.isAnonymous == true

    /** true si hay alguien con correo y contraseña dentro. */
    fun tieneSesion() = auth.currentUser?.isAnonymous == false

    suspend fun entrar(correo: String, contrasena: String, rolEsperado: String = SesionStore.MEDICO): Resultado {
        if (correo.isBlank() || contrasena.isBlank()) return Resultado.Mal("Escribe tu correo y tu contraseña.")
        return try {
            val uid = auth.signInWithEmailAndPassword(correo.trim(), contrasena).esperar().user!!.uid
            val rol = db.collection("usuarios").document(uid).get().esperar().getString("rol")
            if (rol != rolEsperado) {
                auth.signOut()
                Resultado.Mal(
                    if (rolEsperado == SesionStore.FAMILIAR) "Esta cuenta no es de cuidador. Regresa y elige \"Soy médico\"."
                    else "Esta cuenta no es de médico. Regresa y elige \"Soy cuidador\"."
                )
            } else Resultado.Bien
        } catch (e: Exception) {
            Resultado.Mal(mensajeDe(e))
        }
    }

    suspend fun crear(
        nombre: String,
        cedula: String,
        lugar: String,
        correo: String,
        contrasena: String,
        rol: String = SesionStore.MEDICO,
        parentesco: String = ""
    ): Resultado {
        when {
            nombre.isBlank() -> return Resultado.Mal("Escribe tu nombre.")
            correo.isBlank() -> return Resultado.Mal("Escribe tu correo.")
            contrasena.length < MINIMO_CONTRASENA ->
                return Resultado.Mal("La contraseña debe tener mínimo $MINIMO_CONTRASENA caracteres.")
        }
        return try {
            val usuario = auth.createUserWithEmailAndPassword(correo.trim(), contrasena).esperar().user!!
            db.collection("usuarios").document(usuario.uid).set(
                mapOf(
                    "rol" to rol,
                    "nombre" to nombre.trim(),
                    "parentesco" to parentesco.trim(),
                    "cedula" to cedula.trim(),
                    "lugar" to lugar.trim(),
                    "correo" to correo.trim(),
                    "creado" to Timestamp.now()
                )
            ).esperar()
            // Correo de "Confirma tu correo" (el texto se cambia en la consola de Firebase)
            usuario.sendEmailVerification()
            Resultado.Bien
        } catch (e: Exception) {
            Resultado.Mal(mensajeDe(e))
        }
    }

    suspend fun recuperar(correo: String): Resultado {
        if (correo.isBlank()) return Resultado.Mal("Escribe tu correo arriba y vuelve a tocar \"¿Olvidaste tu contraseña?\".")
        return try {
            auth.sendPasswordResetEmail(correo.trim()).esperar()
            Resultado.Bien
        } catch (e: Exception) {
            Resultado.Mal(mensajeDe(e))
        }
    }

    suspend fun miPerfil(): PerfilMedico? {
        val usuario = auth.currentUser ?: return null
        return try {
            val d = db.collection("usuarios").document(usuario.uid).get().esperar()
            PerfilMedico(
                nombre = d.getString("nombre") ?: "",
                cedula = d.getString("cedula") ?: "",
                lugar = d.getString("lugar") ?: "",
                correo = usuario.email ?: "",
                correoConfirmado = usuario.isEmailVerified,
                rol = d.getString("rol") ?: SesionStore.MEDICO,
                parentesco = d.getString("parentesco") ?: ""
            )
        } catch (_: Exception) {
            null
        }
    }

    fun salir() = auth.signOut()

    /**
     * Borra la cuenta para siempre: deja de ver a todos sus pacientes, se borran sus datos
     * y ya no puede entrar. Pide la contraseña otra vez para confirmar que es él.
     */
    suspend fun eliminar(contrasena: String): Resultado {
        val usuario = auth.currentUser ?: return Resultado.Mal("No hay ninguna sesión abierta.")
        val correo = usuario.email ?: return Resultado.Mal("Esta cuenta no tiene correo.")
        if (contrasena.isBlank()) return Resultado.Mal("Escribe tu contraseña para confirmar.")
        return try {
            usuario.reauthenticate(EmailAuthProvider.getCredential(correo, contrasena)).esperar()
            val uid = usuario.uid
            val lista = db.collection("usuarios").document(uid).collection("pacientes").get().esperar()
            for (doc in lista.documents) {
                val paciente = db.collection("pacientes").document(doc.id)
                // Si era su médico, se quita la marca para que su cuidador pueda agregar medicinas
                runCatching {
                    val marca = paciente.collection("marcas").document("medico")
                    if (marca.get().esperar().getString("uid") == uid) marca.delete().esperar()
                }
                runCatching { paciente.collection("equipo").document(uid).delete().esperar() }
                doc.reference.delete().esperar()
            }
            db.collection("usuarios").document(uid).delete().esperar()
            usuario.delete().esperar()
            Resultado.Bien
        } catch (e: Exception) {
            Resultado.Mal(mensajeDe(e))
        }
    }

    /** Convierte el error de Firebase en un mensaje que se entienda. */
    internal fun mensajeDe(e: Exception): String = when (e) {
        is FirebaseNetworkException -> "Revisa tu conexión a internet."
        is FirebaseTooManyRequestsException -> "Hiciste muchos intentos. Espera unos minutos e intenta otra vez."
        is FirebaseAuthException -> when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Ese correo no está bien escrito."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_USER_NOT_FOUND" ->
                "El correo o la contraseña no son correctos."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Ya hay una cuenta con ese correo. Mejor inicia sesión."
            "ERROR_WEAK_PASSWORD" -> "Esa contraseña es muy fácil de adivinar. Usa una más larga."
            "ERROR_USER_DISABLED" -> "Esta cuenta está desactivada."
            else -> "No se pudo. Intenta otra vez."
        }
        else -> "No se pudo. Intenta otra vez."
    }
}
