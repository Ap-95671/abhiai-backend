package com.abhiai.abhiai_backend.ai.pipeline;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class IntentClassifier {
    // Deliberately requires a direct creation request: discussion/code about image generation stays text.
    private static final Pattern IMAGE = Pattern.compile(
            "(?is)^\\s*(?:(?:please|can you|could you|would you)\\s+)*(?:generate|create|draw|paint|render|make)\\s+(?:(?:me|an?|the)\\s+)*(?:(?:photorealistic|realistic|digital)\\s+)?(?:image|picture|photo|illustration|portrait|painting)\\b(?!\\s+(?:generation|generator|api|code|service|pipeline)\\b)");

    public Intent classify(String text) {
        text = text == null ? "" : text;
        if (IMAGE.matcher(text).find()) return Intent.IMAGE_GENERATION;
        if (requiresFreshInformation(text)) return Intent.CURRENT_INFORMATION;
        if (has(text, "pdf|document|spreadsheet|attached file")) return Intent.DOCUMENT_ANALYSIS;
        if (has(text, "debug|code|java|python|typescript|javascript|sql|program|programming|stack trace")) return Intent.CODING;
        if (has(text, "architecture|compare|prove|reasoning|analyse|analyze")) return Intent.REASONING;
        if (has(text, "summarize|summarise|summary|tl;dr")) return Intent.SUMMARIZATION;
        if (has(text, "dataset|statistics|regression|data analysis")) return Intent.DATA_ANALYSIS;
        if (has(text, "plan|planning|roadmap")) return Intent.PLANNING;
        if (has(text, "poem|story|creative|brainstorm")) return Intent.CREATIVE_WRITING;
        if (has(text, "explain|teach|learn|what is|what are")) return Intent.EDUCATION;
        return Intent.GENERAL_CHAT;
    }

    public boolean requiresFreshInformation(String text) {
        if (text == null || text.isBlank()) return false;
        if (has(text, "do not search|don't search|without (?:web|internet) search")) return false;
        if (has(text, "search (?:the )?web|search online|browse the web|look up")) return true;
        if (has(text, "latest|recent|breaking|new release|new update|this week|this month|up.to.date|live scores?")) return true;
        if (has(text, "current(?:\\s+[\\p{L}\\p{N}.'&-]+){0,4}\\s+(?:ceo|president|leader|price|prices|version|versions|release|exchange rate|news|events?|information|status|company)")) return true;
        if (has(text, "today|yesterday|tonight|right now")
                && has(text, "what|which|has|did|news|happened|developments|released|announced|results|weather|prices|updates")) return true;
        return has(text, "who (?:is|runs|leads)") && has(text, "ceo|president|company|minister")
                || has(text, "how much (?:does|is)") && has(text, "cost|stock|price")
                || has(text, "news|weather forecast|stock price|exchange rate|sports results");
    }

    public static boolean has(String text, String alternatives) {
        return Pattern.compile("(?i)\\b(?:" + alternatives + ")\\b").matcher(text).find();
    }
}
