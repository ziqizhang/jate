package uk.ac.shef.dcs.jate.v2.test;

import org.apache.commons.lang.StringEscapeUtils;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zqz on 23/09/2015.
 */
public class Test {
    public static void printEachBackward(BreakIterator boundary, String source) {
        int end = boundary.last();
        for (int start = boundary.previous();
             start != BreakIterator.DONE;
             end = start, start = boundary.previous()) {
            System.out.println(source.substring(start,end));
        }
    }

    public static void main(String[] args) {
        String test="This is sentence one. Sentence two! " +
                "Sentence three? " +
                "Sentence \"four\". " +
                "Sentence \"five\"! " +
                "Sentence \"six\"? " +
                "Sentence \"seven.\" " +
                "Sentence 'eight!' " +
                "Dr. Jones said: \"Mrs. Smith you have a lovely daughter!\" " +
                "The T.V.A. is a big project!";
        //if (args.length == 1) {
            String stringToExamine = test;
            //print each word in order
            BreakIterator boundary =  BreakIterator.getSentenceInstance(Locale.US);
            boundary.setText(stringToExamine);
            printEachBackward(boundary, stringToExamine);

       // }


        String pattern="(\\S.+?[.!?;]|[.!?;]['\"])(?<!Mr.|Mrs.|Ms.|Jr.|Dr.|Prof.|Sr.|[A-Z].)(?=|\\s+|$)";
        System.out.println(StringEscapeUtils.escapeXml(pattern));
        System.exit(1);
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(test);
        while(m.find()){
            System.out.println(test.substring(m.start(), m.end()));
        }
    }
}
