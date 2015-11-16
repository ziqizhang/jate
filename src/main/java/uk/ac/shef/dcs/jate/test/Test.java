package uk.ac.shef.dcs.jate.test;

import org.apache.commons.lang.StringEscapeUtils;
import uk.ac.shef.dcs.jate.feature.ContextWindow;

import java.text.BreakIterator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
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

        //todo Test object equality in hashset
        ContextWindow c = new ContextWindow();
        c.setDocId(1);

        ContextWindow c2 = new ContextWindow();
        c2.setDocId(1);

        Set<ContextWindow> all = new HashSet<>();
        all.add(c);
        System.out.println(all.contains(c2));
        all.add(c2);
        System.exit(1);

        String str="and this";
        String pstr = "(?<!\\w)"+Pattern.quote(str)+"(?!\\w)";
        Pattern pat=Pattern.compile(pstr);
        Matcher mm = pat.matcher("oh and this is");
        System.out.println(mm.find());
        System.exit(1);

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
