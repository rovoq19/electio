package com.rovoq.electio.repos;

import com.rovoq.electio.domain.Meeting;
import com.rovoq.electio.domain.Voting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VotingRepo extends JpaRepository<Voting, Long> {
    List<Voting> findByMeeting(Meeting meeting);
}
