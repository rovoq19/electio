package com.rovoq.electio.repos;

import com.rovoq.electio.domain.Result;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultRepo extends JpaRepository<Result, Long> {
}
