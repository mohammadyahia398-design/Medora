package com.example.medora.data

import kotlinx.coroutines.flow.Flow

// The only door to lecture data. UI talks to this, never to Room directly.
class LectureRepository(private val dao: LectureDao) {
    val lectures: Flow<List<Lecture>> = dao.observeAll()

    fun observe(id: Long): Flow<Lecture?> = dao.observe(id)

    suspend fun add(title: String, subject: String, content: String = "", originalFile: String = "") {
        dao.insert(
            Lecture(
                title = title.trim(),
                subject = subject.trim(),
                content = content,
                originalFile = originalFile,
            )
        )
    }

    suspend fun getAll(): List<Lecture> = dao.getAll()

    suspend fun saveAi(id: Long, summary: String, slidesJson: String, insightsJson: String) =
        dao.saveAi(id, summary, slidesJson, insightsJson)

    suspend fun updateNote(id: Long, title: String, content: String) =
        dao.updateNote(id, title.trim(), content)

    suspend fun organize(id: Long, subject: String, folder: String, tags: List<String>) =
        dao.organize(id, subject.trim(), folder.trim(), tags.joinToString(", "))

    suspend fun delete(lecture: Lecture) = dao.delete(lecture)
}
