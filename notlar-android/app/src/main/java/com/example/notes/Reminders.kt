package com.example.notes

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.notes.data.Note
import com.example.notes.data.NoteStore

/** Schedules note reminders with AlarmManager and shows them as notifications. */
object Reminders {
    private const val CHANNEL = "reminders"
    const val EXTRA_NOTE_ID = "note_id"
    private const val ACTION_FIRE = "com.example.notes.REMINDER"

    fun schedule(context: Context, note: Note) {
        val time = note.reminder ?: return
        if (note.inTrash || time <= System.currentTimeMillis()) return
        val am = context.getSystemService(AlarmManager::class.java)
        // Inexact alarms need no special permission; they still fire within a few minutes.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending(context, note.id))
    }

    fun cancel(context: Context, id: String) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context, id))
        NotificationManagerCompat.from(context).cancel(id.hashCode())
    }

    private fun pending(context: Context, id: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        id.hashCode(),
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_NOTE_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun notify(context: Context, note: Note) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Hatırlatıcılar", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(
            context,
            note.id.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_NOTE_ID, note.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = note.plainText().removePrefix(note.title).trim()
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_shortcut_note)
            .setContentTitle(note.title.ifBlank { "Hatırlatıcı" })
            .setContentText(text.ifBlank { "Notunuza göz atın" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(note.id.hashCode(), notification)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notes = NoteStore(context).load()
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            notes.forEach { Reminders.schedule(context, it) }
            return
        }
        val id = intent.getStringExtra(Reminders.EXTRA_NOTE_ID) ?: return
        notes.firstOrNull { it.id == id && !it.inTrash }?.let { Reminders.notify(context, it) }
    }
}
