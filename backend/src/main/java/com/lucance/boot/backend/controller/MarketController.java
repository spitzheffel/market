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

    @GetMapping("/symbol-info")
    public ResponseEntity<MarketSymbolInfo> getSymbolInfo(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "binance") String exchange) {
        if (symbol == null || symbol.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String type = resolveSymbolType(symbol);
        SymbolParts parts = parseSymbolParts(symbol);
        return ResponseEntity.ok(new MarketSymbolInfo(symbol, type, parts.base(), parts.quote(), exchange));
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

    private String resolveSymbolType(String symbol) {
        String normalized = symbol.trim().toUpperCase();
        List<MarketProperties.SymbolConfig> symbols = marketProperties.getSymbols();
        if (symbols != null) {
            for (MarketProperties.SymbolConfig item : symbols) {
                if (item == null || item.getSymbol() == null) {
                    continue;
                }
                if (item.getSymbol().trim().toUpperCase().equals(normalized)) {
                    return resolveType(item.getType());
                }
            }
        }

        for (MarketSymbol item : defaultSymbols()) {
            if (item.symbol().trim().toUpperCase().equals(normalized)) {
                return resolveType(item.type());
            }
        }

        return "spot";
    }

    private SymbolParts parseSymbolParts(String symbol) {
        String trimmed = symbol.trim();
        String[] parts = trimmed.split("[/_-]", 2);
        if (parts.length == 2) {
            return new SymbolParts(parts[0], parts[1]);
        }
        return new SymbolParts(trimmed, "");
    }

    public record MarketSymbol(String symbol, String type) {
    }

    public record MarketSymbolInfo(String symbol, String type, String baseAsset, String quoteAsset, String exchange) {
    }

    private record SymbolParts(String base, String quote) {
    }
}
