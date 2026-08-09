package com.example.secdsp.modules.notification.repository;

import com.example.secdsp.modules.notification.entity.CustomerNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerNotificationRepository extends JpaRepository<CustomerNotification, Long> {

    List<CustomerNotification> findByUser_IdAndIsReadFalseOrderByCreatedAtAsc(Long userId);

    @Query("""
        SELECT n FROM CustomerNotification n
        WHERE n.user.id = :userId
          AND n.isRead = false
          AND (:afterId IS NULL OR n.id > :afterId)
        ORDER BY n.createdAt ASC
        """)
    List<CustomerNotification> findUnreadAfter(
        @Param("userId") Long userId,
        @Param("afterId") Long afterId
    );

    long countByUser_IdAndIsReadFalse(Long userId);

    @Modifying
    @Query("""
        UPDATE CustomerNotification n
        SET n.isRead = true
        WHERE n.user.id = :userId AND n.isRead = false
        """)
    int markAllReadForUser(@Param("userId") Long userId);
}
