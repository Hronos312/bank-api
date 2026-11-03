package ru.bankapi.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bankapi.model.OutboxEvent;

import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}
