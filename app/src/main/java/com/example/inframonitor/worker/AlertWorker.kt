package com.example.inframonitor.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.inframonitor.data.network.RetrofitClient
import com.example.inframonitor.utils.NotificationHelper

class AlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val notificationHelper = NotificationHelper(context)

    override suspend fun doWork(): Result {
        try {
            val response = RetrofitClient.instance.getContainers()
            if (response.isSuccessful) {
                val containers = response.body() ?: emptyList()
                
                for (container in containers) {
                    if (container.isExited) {
                        notificationHelper.showNotification(
                            "Alerta de Contenedor",
                            "El contenedor '${container.name}' se ha apagado."
                        )
                    } else if (container.isPaused) {
                        notificationHelper.showNotification(
                            "Alerta de Contenedor",
                            "El contenedor '${container.name}' se pausó."
                        )
                    }
                }
            }

            val alertsRes = try { RetrofitClient.instance.getAlerts() } catch (e: Exception) { null }
            val alerts = if (alertsRes?.isSuccessful == true) alertsRes.body() ?: emptyList() else emptyList()
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
