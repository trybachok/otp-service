package com.example.otp.infrastructure.dao;

import com.example.otp.domain.model.Operation;

import java.util.Optional;
import java.util.UUID;

public interface OperationDao {

    Operation save(Operation operation);

    Optional<Operation> findByUserIdAndOperationId(UUID userId, String operationId);

    void deleteByUserId(UUID userId);
}