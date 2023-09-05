package statusbar.finder.misc;

import java.util.regex.*;
public class checkStringLang {

//    public static boolean isJapenese(String text) {
//        Set<Character.UnicodeBlock> japaneseUnicodeBlocks = new HashSet<Character.UnicodeBlock>() {{
//            add(Character.UnicodeBlock.HIRAGANA);
//            add(Character.UnicodeBlock.KATAKANA);
//            add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
//        }};
//
//        for (char c : text.toCharArray()) {
//            if (japaneseUnicodeBlocks.contains(Character.UnicodeBlock.of(c))) {
//                return true;
//            } else
//                return false;
//        }
//        return false;
//    }

    public static boolean isJapanese(String text) {
        // 为了避免误判中文
        // 本方法只检测平假名与片假名
        // 没有平假名与片假名的日文仍会返回false

        Pattern pattern = Pattern.compile("[ぁ-んァ-ン]+");
        return pattern.matcher(text).find();
    }
}