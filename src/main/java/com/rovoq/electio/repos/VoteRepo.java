package com.rovoq.electio.repos;

import com.rovoq.electio.domain.Answer;
import com.rovoq.electio.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteRepo extends JpaRepository<Vote, Long> {
}
