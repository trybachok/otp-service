package com.example.otp.application.scheduler;

import com.example.otp.domain.model.OtpCode;
import com.example.otp.domain.model.OtpStatus;
import com.example.otp.infrastructure.dao.OtpCodeDao;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class OtpExpirationInMemoryTest {

    @Test
    void expireNowChangesOnlyExpiredActiveCodes() {
        InMemoryOtpCodeDao otpCodeDao = new InMemoryOtpCodeDao();

        UUID expiredActiveId = UUID.randomUUID();
        UUID validActiveId = UUID.randomUUID();
        UUID usedExpiredId = UUID.randomUUID();

        otpCodeDao.save(new OtpCode(
                expiredActiveId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "hash",
                OtpStatus.ACTIVE,
                Instant.now().minusSeconds(10),
                Instant.now().minusSeconds(100),
                null
        ));

        otpCodeDao.save(new OtpCode(
                validActiveId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "hash",
                OtpStatus.ACTIVE,
                Instant.now().plusSeconds(300),
                Instant.now(),
                null
        ));

        otpCodeDao.save(new OtpCode(
                usedExpiredId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "hash",
                OtpStatus.USED,
                Instant.now().minusSeconds(10),
                Instant.now().minusSeconds(100),
                Instant.now()
        ));

        OtpExpirationScheduler scheduler = new OtpExpirationScheduler(otpCodeDao, 60);

        int expiredCount = scheduler.expireNow();

        assertEquals(1, expiredCount);
        assertEquals(OtpStatus.EXPIRED, otpCodeDao.get(expiredActiveId).status());
        assertEquals(OtpStatus.ACTIVE, otpCodeDao.get(validActiveId).status());
        assertEquals(OtpStatus.USED, otpCodeDao.get(usedExpiredId).status());
    }

    private static final class InMemoryOtpCodeDao implements OtpCodeDao {

        private final Map<UUID, OtpCode> codes = new ConcurrentHashMap<>();

        @Override
        public OtpCode save(OtpCode otpCode) {
            codes.put(otpCode.id(), otpCode);
            return otpCode;
        }

        @Override
        public Optional<OtpCode> findActiveByUserIdAndOperationId(UUID userId, UUID operationId) {
            return codes.values()
                    .stream()
                    .filter(code -> code.userId().equals(userId))
                    .filter(code -> code.operationId().equals(operationId))
                    .filter(code -> code.status() == OtpStatus.ACTIVE)
                    .findFirst();
        }

        @Override
        public void markUsed(UUID id, Instant usedAt) {
            OtpCode oldCode = codes.get(id);

            codes.put(id, new OtpCode(
                    oldCode.id(),
                    oldCode.userId(),
                    oldCode.operationId(),
                    oldCode.codeHash(),
                    OtpStatus.USED,
                    oldCode.expiresAt(),
                    oldCode.createdAt(),
                    usedAt
            ));
        }

        @Override
        public void markExpired(UUID id) {
            OtpCode oldCode = codes.get(id);

            codes.put(id, new OtpCode(
                    oldCode.id(),
                    oldCode.userId(),
                    oldCode.operationId(),
                    oldCode.codeHash(),
                    OtpStatus.EXPIRED,
                    oldCode.expiresAt(),
                    oldCode.createdAt(),
                    oldCode.usedAt()
            ));
        }

        @Override
        public int markExpiredCodes(Instant now) {
            int expiredCount = 0;

            for (OtpCode code : codes.values()) {
                if (code.status() == OtpStatus.ACTIVE && code.expiresAt().isBefore(now)) {
                    markExpired(code.id());
                    expiredCount++;
                }
            }

            return expiredCount;
        }

        @Override
        public void deleteByUserId(UUID userId) {
            codes.values().removeIf(code -> code.userId().equals(userId));
        }

        OtpCode get(UUID id) {
            return codes.get(id);
        }
    }
}