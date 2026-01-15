package com.lucance.boot.backend.controller;

import com.lucance.boot.backend.config.MarketProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * 市场元数据控制器
 */
@RestController
@RequestMapping("/api/markets")
@RequiredArgsConstructor
public class MarketController {

    private final MarketProperties marketProperties;

    @GetMapping("/symbols")
    public ResponseEntity<List<MarketSymbol>> getSymbols() {
        List<MarketProperties.SymbolConfig> symbols = marketProperties.getSymbols();
        if (symbols == null || symbols.isEmpty()) {
            return ResponseEntity.ok(defaultSymbols());
        }

        List<MarketSymbol> result = symbols.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getSymbol() != null && !item.getSymbol().isBlank())
                .map(item -> new MarketSymbol(item.getSymbol(), resolveType(item.getType())))
                .toList();
        return ResponseEntity.ok(result.isEmpty() ? defaultSymbols() : result);
    }

    private List<MarketSymbol> defaultSymbols() {
        return List.of(
                new MarketSymbol("BTC/USDT", "spot"),
                new MarketSymbol("ETH/USDT", "spot"),
                new MarketSymbol("SOL/USDT", "spot"),
                new MarketSymbol("BNB/USDT", "spot"),
                new MarketSymbol("XRP/USDT", "spot"),
                new MarketSymbol("DOGE/USDT", "spot"));
    }

    private String resolveType(String type) {
        if (type == null || type.isBlank()) {
            return "spot";
        }
        return type.toLowerCase();
    }

    public record MarketSymbol(String symbol, String type) {
    }
}
