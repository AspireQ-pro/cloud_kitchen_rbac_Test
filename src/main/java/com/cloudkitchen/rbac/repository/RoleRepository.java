package com.cloudkitchen.rbac.repository;

import com.cloudkitchen.rbac.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
