package com.example.inframonitor.utils

import retrofit2.Response as RetrofitResponse
import okhttp3.Response as OkHttpResponse
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object NetworkErrorMapper {

    fun parseCode(code: Int): String {
        return when (code) {
            524 -> "El servidor está tardando, la operación sigue en curso (Timeout de Cloudflare 524)"
            in 520..527 -> "El servidor está tardando, la operación sigue en curso (Respuesta Cloudflare $code)"
            504 -> "El servidor tardó demasiado en responder (Gateway Timeout 504). La operación puede seguir en ejecución."
            in 500..503, in 505..511 -> "Error interno del servidor ($code). Intenta nuevamente en unos momentos."
            401, 403 -> "Error de autenticación. Verifica que la API Key sea correcta."
            404 -> "Recurso no encontrado (404)."
            else -> "Error en el servidor: HTTP $code"
        }
    }

    fun parseResponseError(response: RetrofitResponse<*>): String {
        return parseCode(response.code())
    }

    fun parseResponseError(response: OkHttpResponse): String {
        return parseCode(response.code)
    }

    fun parseThrowable(throwable: Throwable): String {
        return when (throwable) {
            is SocketTimeoutException, is InterruptedIOException -> {
                "El servidor está tardando, la operación sigue en curso"
            }
            is UnknownHostException, is ConnectException -> {
                "Error de conexión. Verifica tu red o la dirección del servidor."
            }
            is SSLException -> {
                "Error de conexión segura SSL/TLS. Verifica la URL de tu servidor."
            }
            else -> {
                throwable.localizedMessage ?: "Error de conexión o red desconocido"
            }
        }
    }
}
