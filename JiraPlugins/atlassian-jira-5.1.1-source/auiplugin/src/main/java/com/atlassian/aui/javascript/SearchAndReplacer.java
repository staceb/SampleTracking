package com.atlassian.aui.javascript;

import com.google.common.base.Function;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs a regex based search-and-replace over a string.
 *
 * @since v3.5
 */
public class SearchAndReplacer
{
    private final Pattern pattern;
    private final Function<Matcher, String> replacer;

    /**
     * @param pattern the pattern to find in the input
     * @param replacer a function that gives replacement text for the given match
     */
    public SearchAndReplacer(Pattern pattern, Function<Matcher, String> replacer) {
        this.pattern = pattern;
        this.replacer = replacer;
    }

    /**
     * Replace all occurences of the pattern in the input, using the given function
     */
    public CharSequence replaceAll(CharSequence input)
    {
        Matcher matcher = pattern.matcher(input);
        StringBuffer output = new StringBuffer();
        while (matcher.find())
        {
            String r = replacer.apply(matcher);
            matcher.appendReplacement(output, "");
            output.append(r);
        }
        matcher.appendTail(output);
        return output;

    }
}
