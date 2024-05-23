package net.jitsi.sdktest

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EndPageActivity : AppCompatActivity() {

    private lateinit var dbHelper: FeedbackDatabaseHelper
    private lateinit var userNameEditText: EditText
    private lateinit var ratingBar: RatingBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_end_page)

        // Initialize views
        userNameEditText = findViewById(R.id.etUserName)
        ratingBar = findViewById(R.id.ratingBar)
        val btnSendFeedback: Button = findViewById(R.id.btnSendFeedback)

        // Initialize database helper
        dbHelper = FeedbackDatabaseHelper(this)

        // Set listener for send feedback button
        btnSendFeedback.setOnClickListener {
            sendFeedbackViaEmail()
        }
    }

    private fun sendFeedbackViaEmail() {
        val userName = userNameEditText.text.toString()
        val rating = ratingBar.rating.toInt()

        // Map the rating to a descriptive feedback message
        val feedback = when (rating) {
            1 -> "Terrible"
            2 -> "Poor"
            3 -> "Average"
            4 -> "Good"
            5 -> "Excellent"
            else -> "Unknown"
        }

        // Insert feedback entry into the database
        dbHelper.addFeedbackEntry(userName, rating, feedback)

        // Send feedback data via email
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.type = "text/plain"
        emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("utkarsh21107@iiitd.ac.in"))
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback from $userName")
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Rating: $rating\nFeedback: $feedback")
        try {
            startActivity(Intent.createChooser(emailIntent, "Send feedback via..."))
        } catch (e: Exception) {
            Toast.makeText(this, "Error sending feedback", Toast.LENGTH_SHORT).show()
        }
    }

    // Database helper class
    private class FeedbackDatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        companion object {
            private const val DATABASE_VERSION = 1
            private const val DATABASE_NAME = "FeedbackDatabase"
            private const val TABLE_FEEDBACK = "feedback"

            // Table columns
            private const val KEY_ID = "id"
            private const val KEY_NAME = "name"
            private const val KEY_RATING = "rating"
            private const val KEY_FEEDBACK = "feedback"
            private const val KEY_TIMESTAMP = "timestamp"
        }

        override fun onCreate(db: SQLiteDatabase) {
            // Create the feedback table
            val createTableQuery = ("CREATE TABLE $TABLE_FEEDBACK ("
                    + "$KEY_ID INTEGER PRIMARY KEY,"
                    + "$KEY_NAME TEXT,"
                    + "$KEY_RATING INTEGER,"
                    + "$KEY_FEEDBACK TEXT,"
                    + "$KEY_TIMESTAMP INTEGER)")
            db.execSQL(createTableQuery)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        }

        // Insert feedback entry into the database
        fun addFeedbackEntry(name: String, rating: Int, feedback: String) {
            val db = this.writableDatabase
            val values = ContentValues()
            values.put(KEY_NAME, name)
            values.put(KEY_RATING, rating)
            values.put(KEY_FEEDBACK, feedback)
            values.put(KEY_TIMESTAMP, System.currentTimeMillis())
            db.insert(TABLE_FEEDBACK, null, values)
            db.close()
        }
    }
}
