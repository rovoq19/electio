package com.rovoq.electio.repos;

import com.rovoq.electio.domain.Voting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotingRepo extends JpaRepository<Voting, Long> {
}
