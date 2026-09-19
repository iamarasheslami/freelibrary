package com.freelibrary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freelibrary.app.data.local.entity.Book
import com.freelibrary.app.data.local.entity.BookSubject
import com.freelibrary.app.data.local.entity.Subject

@Dao
interface BookSubjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookSubject: BookSubject): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookSubjects: List<BookSubject>)

    @Query(
        """
        SELECT subjects.* FROM subjects
        INNER JOIN book_subjects ON subjects.id = book_subjects.subjectId
        WHERE book_subjects.bookId = :bookId
        """,
    )
    suspend fun getSubjectsForBook(bookId: Long): List<Subject>

    @Query(
        """
        SELECT books.* FROM books
        INNER JOIN book_subjects ON books.id = book_subjects.bookId
        WHERE book_subjects.subjectId = :subjectId
        """,
    )
    suspend fun getBooksForSubject(subjectId: Long): List<Book>

    /**
     * Clears every subject association for a book, so the sync client can
     * rebuild them fresh when a book's subjects change. Safe to do
     * unconditionally since this table holds catalog associations, not user
     * data - unlike deleting the book row itself.
     */
    @Query("DELETE FROM book_subjects WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: Long)
}
