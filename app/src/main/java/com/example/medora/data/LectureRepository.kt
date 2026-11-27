package com.example.medora.data

import kotlinx.coroutines.flow.Flow

// The only door to lecture data. UI talks to this, never to Room directly.
class LectureRepository(private val dao: LectureDao) {
    val lectures: Flow<List<Lecture>> = dao.observeAll()

    fun observe(id: Long): Flow<Lecture?> = dao.observe(id)

    suspend fun add(title: String, subject: String, content: String = "") {
        dao.insert(Lecture(title = title.trim(), subject = subject.trim(), content = content))
    }

    suspend fun saveAi(id: Long, summary: String, slidesJson: String) = dao.saveAi(id, summary, slidesJson)

    suspend fun updateNote(id: Long, title: String, content: String) =
        dao.updateNote(id, title.trim(), content)

    suspend fun delete(lecture: Lecture) = dao.delete(lecture)
}
