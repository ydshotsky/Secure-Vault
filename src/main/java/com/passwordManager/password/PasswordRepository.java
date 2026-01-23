package com.passwordManager.password;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PasswordRepository extends JpaRepository<VaultPassword,Long> {
   long countByUserUsername(String username);

    @Query("SELECT p FROM VaultPassword p WHERE " +
            "(LOWER(p.siteUsername) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.siteUrl) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.notes) LIKE LOWER(CONCAT('%', :keyword, '%')))"+
            "AND p.user.username = :name")
    List<VaultPassword> findByKeywordAndUserUsername(String keyword, String name);

    void deleteAllByUserUsername(String username);

    List<VaultPassword> findByUserUsername(String name);
}
