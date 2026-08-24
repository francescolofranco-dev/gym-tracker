package dev.francescolofranco.gymtracker.ui.screens.sessions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

private val whatsAppPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

/** Opens WhatsApp directly when available, with the system share sheet as a safe fallback. */
fun shareSessionOnWhatsApp(context: Context, text: String) {
    whatsAppPackages.forEach { packageName ->
        try {
            context.startActivity(textShareIntent(text).setPackage(packageName))
            return
        } catch (_: ActivityNotFoundException) {
            // Try WhatsApp Business next, then fall back to any compatible share target.
        }
    }

    context.startActivity(
        Intent.createChooser(textShareIntent(text), "Share workout"),
    )
}

private fun textShareIntent(text: String) = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, text)
}
