package org.apache.lucene.analysis.jate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * see MWEMetadata for the types of shapes that will be recognised
 */
class WordShapeTagger {
    private static final Pattern acronym = Pattern.compile("\\b[A-Z0-9]+\\b");
    private static final Pattern number = Pattern.compile("\\b[0-9\\.,/]+\\b");
    private static final Pattern uppercase = Pattern.compile("[A-Z]");
    private static final Pattern digit = Pattern.compile("[0-9]");
    private static final Pattern symbol = Pattern.compile("\\W");

    public boolean hasNumber(String input){
        return number.matcher(input).find();
    }

    public boolean hasAcronym(String input){
        Matcher m= acronym.matcher(input);
        while(m.find()){
            String matched =input.substring(m.start(), m.end());
            if(uppercase.matcher(matched).find())
                return true;
        }
        return false;
    }

    public boolean hasUppercase(String input){
        return uppercase.matcher(input).find();
    }

    public boolean hasDigit(String input){
        return digit.matcher(input).find();
    }

    public boolean hasSymbol(String input){
        return symbol.matcher(input).find();
    }

    /*public static void main(String[] args) {
        WordShapeTagger test = new WordShapeTagger();
        String input = "*ER";
        System.out.println(test.hasNumber(input));
        System.out.println(test.hasAcronym(input));
        System.out.println(test.hasUppercase(input));
        System.out.println(test.hasDigit(input));
        System.out.println(test.hasSymbol(input));
    }*/

}
