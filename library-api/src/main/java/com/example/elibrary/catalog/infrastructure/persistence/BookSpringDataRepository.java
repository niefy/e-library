package com.example.elibrary.catalog.infrastructure.persistence;

import com.example.elibrary.catalog.domain.Book;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookSpringDataRepository extends JpaRepository<Book, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") UUID id);

    /**
     * locate() treats the query literally, unlike LIKE with user-supplied wildcards.
     */
    @Query("""
            select b from Book b
            where locate(:query, lower(b.title)) > 0
               or locate(:query, lower(b.author)) > 0
               or locate(:query, lower(b.isbn)) > 0
            """)
    Page<Book> search(@Param("query") String query, Pageable pageable);
}
