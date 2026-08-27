package com.example.inframonitor.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.inframonitor.data.network.RetrofitClient
import com.example.inframonitor.utils.NotificationHelper
import java.util.Locale

class AlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val notificationHelper = NotificationHelper(context)

    override suspend fun doWork(): Result {
        try {
            val response = RetrofitClient.instance.getContainers()
            if (response.isSuccessful) {
                val containers = response.body() ?: emptyList()
                
                for (container in containers) {
                    // Check status
                    if (container.status == "exited") {
                        notificationHelper.showNotification(
                            "Alerta de Contenedor",
                            "El contenedor ${container.name} se ha detenido inesperadamente."
                        )
                    }

                    // Check resource usage
                    val cpu = container.cpu_usage ?: 0.0
                    val ram = container.ram_usage ?: 0.0
                    
                    if (cpu > 85.0) {
                        notificationHelper.showNotification(
                            "Uso Crítico de CPU",
                            "El contenedor ${container.name} está usando ${String.format(Locale.getDefault(), "%.1f", cpu)}% de CPU."
                        )
                    }
                    
                    if (ram > 85.0) {
                        notificationHelper.showNotification(
                            "Uso Crítico de RAM",
                            "El contenedor ${container.name} está usando ${String.format(Locale.getDefault(), "%.1f", ram)}% de RAM."
                        )
                    }
                }
            }

            // Also check for general alerts
            val alerts = try { RetrofitClient.instance.getAlerts() } catch (e: Exception) { emptyList() }
            if (alerts.isNotEmpty()) {
                val latest = alerts.last()
                notificationHelper.showNotification(
                    "Nueva Alerta de Sistema",
                    latest.message
                )
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
