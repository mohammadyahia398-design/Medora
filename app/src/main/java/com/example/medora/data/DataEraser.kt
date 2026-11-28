package com.example.medora.data

import android.content.Context
import com.example.medora.ai.AiSettings
import com.example.medora.reminder.ReminderScheduler

// "Delete all my data": removes the database (lectures, cards, reviews, quizzes),
// note images, original imported files, the saved AI keys and the daily reminder. Language and theme choices are kept.
object DataEraser {
    private const val DB_NAME = "medora.db"

    fun eraseAll(context: Context): Boolean {
        return try {
            ReminderScheduler.disable(context)
            val ai = AiSettings(context)
            ai.geminiKey = ""
            ai.openRouterKey = ""
            ai.geminiModel = ""
            ai.openRouterModel = ""
            MedoraDatabase.closeAndReset()
            context.deleteDatabase(DB_NAME)
            NoteImages.deleteAll(context)
            OriginalFiles.deleteAll(context)
            !context.getDatabasePath(DB_NAME).exists()
        } catch (e: Exception) {
            false
        }
    }
}
