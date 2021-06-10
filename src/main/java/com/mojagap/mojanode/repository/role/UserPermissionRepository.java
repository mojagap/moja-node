package com.mojagap.mojanode.repository.role;


import com.mojagap.mojanode.model.role.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Integer> {

    UserPermission findOneByName(String name);
}
