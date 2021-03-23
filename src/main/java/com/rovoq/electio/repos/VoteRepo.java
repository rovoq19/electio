package com.rovoq.electio.repos;

import com.rovoq.electio.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface VoteRepo extends JpaRepository<Vote, Long> {
    Vote findByUsername(String username);
}
