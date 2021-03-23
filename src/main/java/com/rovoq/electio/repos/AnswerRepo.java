package com.rovoq.electio.repos;

import com.rovoq.electio.domain.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepo extends JpaRepository<Answer, Long> {
}
