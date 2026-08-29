package com.abhiai.abhiai_backend.news.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.abhiai.abhiai_backend.news.model.NewsArticle;
import com.abhiai.abhiai_backend.news.model.NewsSource;

@Component
public class NewsArticleRanker {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "as", "at", "by", "for", "from", "in", "is", "of", "on", "the", "to", "with");

    public List<NewsArticle> mergeAndRank(List<NewsArticle> current, List<NewsArticle> incoming) {
        List<NewsArticle> merged = new ArrayList<>(current);
        for (NewsArticle candidate : incoming) {
            int duplicateIndex = findDuplicate(merged, candidate);
            if (duplicateIndex < 0) merged.add(candidate);
            else merged.set(duplicateIndex, merge(merged.get(duplicateIndex), candidate));
        }
        merged.sort(Comparator
                .comparingInt(NewsArticle::relatedStoryCount).reversed()
                .thenComparing(NewsArticle::publishedAt, Comparator.reverseOrder()));
        return List.copyOf(merged);
    }

    private int findDuplicate(List<NewsArticle> articles, NewsArticle candidate) {
        for (int index = 0; index < articles.size(); index++) {
            NewsArticle existing = articles.get(index);
            if (existing.id().equals(candidate.id()) || existing.articleUrl().equals(candidate.articleUrl())) return index;
            if (titleSimilarity(existing.title(), candidate.title()) >= 0.68) return index;
        }
        return -1;
    }

    static double titleSimilarity(String first, String second) {
        Set<String> firstTokens = tokens(first);
        Set<String> secondTokens = tokens(second);
        if (firstTokens.isEmpty() || secondTokens.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(firstTokens);
        intersection.retainAll(secondTokens);
        Set<String> union = new HashSet<>(firstTokens);
        union.addAll(secondTokens);
        return (double) intersection.size() / union.size();
    }

    private static Set<String> tokens(String title) {
        Set<String> result = new HashSet<>();
        for (String token : title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+")) {
            if (token.length() > 2 && !STOP_WORDS.contains(token)) result.add(token);
        }
        return result;
    }

    private NewsArticle merge(NewsArticle primary, NewsArticle related) {
        Map<String, NewsSource> sources = new LinkedHashMap<>();
        for (NewsSource source : primary.sources()) sources.put(source.name().toLowerCase(Locale.ROOT), source);
        for (NewsSource source : related.sources()) sources.putIfAbsent(source.name().toLowerCase(Locale.ROOT), source);
        NewsArticle newest = related.publishedAt().isAfter(primary.publishedAt()) ? related : primary;
        return new NewsArticle(
                primary.id(),
                primary.title(),
                primary.description().isBlank() ? related.description() : primary.description(),
                primary.sourceName(),
                primary.sourceUrl(),
                primary.articleUrl(),
                primary.imageUrl() == null ? related.imageUrl() : primary.imageUrl(),
                newest.publishedAt(),
                primary.category(),
                primary.country().isBlank() ? related.country() : primary.country(),
                primary.region(),
                primary.language(),
                primary.author().isBlank() ? related.author() : primary.author(),
                primary.provider(),
                sources.size(),
                new ArrayList<>(sources.values()));
    }
}
