package com.rovoq.electio.repos;

import com.rovoq.electio.domain.Answer;
import com.rovoq.electio.domain.Voting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerRepo extends JpaRepository<Answer, Long> {
    List<Answer> findByVoting(Voting voting);
}
