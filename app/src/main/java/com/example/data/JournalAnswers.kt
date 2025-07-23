package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class QuestionAnswer(
    val text: String = "",
    val audioPath: String? = null,
    val photoPaths: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JournalAnswers(
    val answers: List<QuestionAnswer> = List(5) { QuestionAnswer() }
) {
    companion object {
        private val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        private val adapter = moshi.adapter(JournalAnswers::class.java)

        fun fromJson(json: String): JournalAnswers? {
            return try {
                adapter.fromJson(json)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun toJson(answers: JournalAnswers): String {
            return adapter.toJson(answers)
        }
    }
}
