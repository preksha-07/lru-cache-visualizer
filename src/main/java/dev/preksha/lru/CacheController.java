package dev.preksha.lru;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private final CacheService cacheService;

    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/keys/{key}")
    public ResponseEntity<String> get(@PathVariable String key) {
        String value = cacheService.get(key);

        if (value == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(value);
    }

    @PutMapping("/keys/{key}")
    public ResponseEntity<String> put(
            @PathVariable String key,
            @RequestBody String value) {

        cacheService.put(key, value);
        return ResponseEntity.ok(value);
    }

    @DeleteMapping("/keys/{key}")
    public ResponseEntity<Void> remove(@PathVariable String key) {
        String removed = cacheService.remove(key);

        if (removed == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        cacheService.clear();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/state")
    public CacheState state() {
        return new CacheState(
                cacheService.snapshot(),
                cacheService.stats(),
                cacheService.size(),
                cacheService.capacity()
        );
    }

    public record CacheState(
            List<Map.Entry<String, String>> entries,
            CacheStats stats,
            int size,
            int capacity
    ) {
    }
}