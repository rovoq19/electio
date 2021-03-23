package com.rovoq.electio.repos;

import com.rovoq.electio.domain.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepo extends JpaRepository<Meeting, Long> {
}
