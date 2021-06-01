package com.rovoq.electio.repos;

import com.rovoq.electio.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepo extends JpaRepository<Group, Long> {
}
