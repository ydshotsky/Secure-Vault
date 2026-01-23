package com.secureVault.password;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VaultService {
    private final VaultRepository vaultRepository;

    public Optional<VaultPassword> findPasswordById(long id) {
        return vaultRepository.findById(id);
    }
    @Transactional
    public void savePassword(VaultPassword vaultPassword) {
        vaultRepository.save(vaultPassword);
    }
    public List<VaultPassword> findPasswordByUserUsername(String username) {
        return vaultRepository.findByUserUsername(username);
    }
    public List<VaultPassword> findPasswordByKeywordAndUserUsername(String keyword,String username) {
        return vaultRepository.findByKeywordAndUserUsername(keyword,username);
    }
    public void deletePasswordById(long id) {
        vaultRepository.deleteById(id);
    }
}
