package com.secureVault.redis;

import com.secureVault.password.VaultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    String CACHE_KEY_PREFIX="password:list:";
    private final RedisTemplate<String,Object> redisTemplate;
    public void cachePasswordList(String username, List<VaultDto>vaultDtos){
        String key=CACHE_KEY_PREFIX+username;
        redisTemplate.opsForValue().set(key,vaultDtos);
    }
    @SuppressWarnings("unchecked")
    public List<VaultDto> getCachedPasswordList(String username){
        String key = CACHE_KEY_PREFIX+username;
        return (List<VaultDto>) redisTemplate.opsForValue().get(key);
    }
    public void clearCachedPasswordList(String username){
        String key = CACHE_KEY_PREFIX+username;
        redisTemplate.delete(key);
    }
}
