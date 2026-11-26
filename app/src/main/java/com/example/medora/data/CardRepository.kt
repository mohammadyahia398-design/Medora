package com.example.medora.data

import kotlinx.coroutines.flow.Flow

class CardRepository(private val dao: CardDao) {
    val cards: Flow<List<Card>> = dao.observeAll()

    fun observeByLecture(lectureId: Long): Flow<List<Card>> = dao.observeByLecture(lectureId)

    fun observeDueCount(): Flow<Int> = dao.observeDueCount(System.currentTimeMillis())

    fun observeLearnedCount(): Flow<Int> = dao.observeLearnedCount()

    suspend fun getAll(): List<Card> = dao.getAll()

    suspend fun getDue(limit: Int = 100): List<Card> = dao.getDue(System.currentTimeMillis(), limit)

    suspend fun add(lectureId: Long, front: String, back: String, source: String) {
        dao.insertAll(listOf(Card(lectureId = lectureId, front = front, back = back, source = source)))
    }

    suspend fun addAll(cards: List<Card>) = dao.insertAll(cards)

    suspend fun update(card: Card) = dao.update(card)

    suspend fun delete(card: Card) = dao.delete(card)
}
