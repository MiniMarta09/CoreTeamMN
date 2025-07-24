package com.example.coreteamproject

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// Classe che gestisce la ricezione dei messaggi
class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Metodo chiamato automaticamente quando arriva un messaggio da Firebase
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Se il messaggio contiene una notifica (titolo e corpo), la mostra
        remoteMessage.notification?.let {
            sendNotification(it.title, it.body)
        }
    }

    // Metodo che crea e mostra la notifica all'utente
    private fun sendNotification(title: String?, messageBody: String?) {
        // Intent che apre MainActivity quando l’utente clicca sulla notifica
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // PendingIntent serve a "prenotare" l’intento per essere eseguito in futuro
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,                      // Codice richiesta
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE // Solo una volta + sicurezza
        )

        // ID del canale di notifica
        val channelId = "default_channel"

        // Costruzione della notifica
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Icona visibile nella barra
            .setContentTitle(title ?: "Notifica")      // Titolo della notifica
            .setContentText(messageBody ?: "Hai un nuovo messaggio.") // Corpo
            .setAutoCancel(true)                       // La notifica scompare se cliccata
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) // Suono default
            .setContentIntent(pendingIntent)           // Azione al click

        // Ottiene il NotificationManager dal sistema per inviare la notifica
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Se Android è 8.0 o superiore, crea il canale notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel) // Registra il canale
        }

        // Mostra la notifica all’utente
        notificationManager.notify(0, notificationBuilder.build())
    }
}
