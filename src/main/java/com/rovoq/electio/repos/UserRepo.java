package com.rovoq.electio.repos;

import com.rovoq.electio.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User, Long>{
    User findByUsername(String username);

    User findByActivation(String code);
}
