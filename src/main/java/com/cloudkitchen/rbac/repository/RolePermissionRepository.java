package com.cloudkitchen.rbac.repository;

import com.cloudkitchen.rbac.domain.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Integer> {
    
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.roleId = :roleId")
    List<RolePermission> findByRoleId(@Param("roleId") Integer roleId);
    
    @Query("SELECT rp FROM RolePermission rp WHERE rp.permission.permissionId = :permissionId")
    List<RolePermission> findByPermissionId(@Param("permissionId") Integer permissionId);
    
    @Query("SELECT COUNT(rp) > 0 FROM RolePermission rp WHERE rp.role.roleId = :roleId AND rp.permission.permissionId = :permissionId")
    boolean existsByRoleIdAndPermissionId(@Param("roleId") Integer roleId, @Param("permissionId") Integer permissionId);
    
    boolean existsByRoleAndPermission(com.cloudkitchen.rbac.domain.entity.Role role, com.cloudkitchen.rbac.domain.entity.Permission permission);
}
