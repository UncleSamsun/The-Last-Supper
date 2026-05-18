package com.goorm.thelastsupper.waiting.repository;

import com.goorm.thelastsupper.account.entity.Account;
import com.goorm.thelastsupper.waiting.entity.WaitingQueue;
import com.goorm.thelastsupper.waiting.entity.WaitingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitingQueueRepository extends JpaRepository<WaitingQueue,String> {
    boolean existsByAccountAndWaitingStatus(Account account, WaitingStatus waitingStatus);

    @Query("SELECT MAX(wq.number) FROM WaitingQueue wq WHERE wq.waitingStatus = :waitingStatus")
    Long findMaxNumberByWaitingStatus(@Param("waitingStatus") WaitingStatus waitingStatus);

    Optional<WaitingQueue> findByAccountAndWaitingStatus(Account account, WaitingStatus waitingStatus);

    List<WaitingQueue> findAllByWaitingStatus(WaitingStatus waitingStatus);

    // 특정 계정의 현재 대기행 조회
    Optional<WaitingQueue> findByAccount_IdAndWaitingStatus(String accountId, WaitingStatus status);

    // 번호(number)가 주어진 값보다 작은 대기중 행 개수(순위 계산용)
    int countByWaitingStatusAndNumberLessThan(WaitingStatus status, Long number);

    Optional<WaitingQueue> findFirstByWaitingStatusOrderByCreatedAtAsc(WaitingStatus waitingStatus);
}
