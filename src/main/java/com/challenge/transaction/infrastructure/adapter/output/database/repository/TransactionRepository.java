package com.challenge.transaction.infrastructure.adapter.output.database.repository;

import com.challenge.transaction.infrastructure.adapter.output.database.entity.TransactionEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID>,
        JpaSpecificationExecutor<TransactionEntity> {

    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);
}
