package com.netbanking.auth.repository;

import com.netbanking.auth.entity.EmailOtp;
import com.netbanking.auth.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {
    Optional<EmailOtp> findTopByUserUserIdAndOtpPurposeAndVerifiedAtIsNullOrderByCreatedAtDesc(Long userId, OtpPurpose purpose);
}
