package com.passwordManager.password;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordService {
    private final PasswordRepository passwordRepository;

    public Optional<VaultPassword> findPasswordById(long id) {
        return passwordRepository.findById(id);
    }
    @Transactional
    public void savePassword(VaultPassword vaultPassword) {
        passwordRepository.save(vaultPassword);
    }
    public List<VaultPassword> findPasswordByUserUsername(String username) {
        return passwordRepository.findByUserUsername(username);
    }
    public List<VaultPassword> findPasswordByKeywordAndUserUsername(String keyword,String username) {
        return passwordRepository.findByKeywordAndUserUsername(keyword,username);
    }
    public void deletePasswordById(long id) {
        passwordRepository.deleteById(id);
    }
}
