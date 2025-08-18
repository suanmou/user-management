package com.example.user.repository;

import com.example.user.entity.OperationHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationHistoryRepository extends MongoRepository<OperationHistory, String> {
    List<OperationHistory> findByUserIdOrderByOperationTimeDesc(String userId);
    List<OperationHistory> findByUserIdAndIpIdOrderByOperationTimeDesc(String userId, String ipId);
}
