package top.iot.gateway.component.common.utils;

import java.util.regex.Pattern;

/**
 * 正则工具类
 *
 * @author kll
 * @since 2021-07-12
 */
public class RegexUtil {

    /**
     * 过滤非法字符正则
     * 1.过滤掉定界符
     * 2.过滤掉&符号
     * 3.过滤掉表情符号
     */
    private static Pattern XML_REPLACE_PATTERN = Pattern.compile("((<!\\[CDATA\\[[\\s\\S]*\\]\\]>)|(&)|([\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]))");

    /**
     * 替换替换非法干扰字符，例如：<list xmlns="http://www.yundaex.com/schema/2013">
     */
    private static Pattern XMLNS_REPLACE_PATTERN = Pattern.compile(" xmlns[^>]*\\>");

    /**
     * 清理非法字符
     */
    private static Pattern CLEAR_ADDRESS_PATTERN = Pattern.compile("[，|,|;|；]");

    /**
     * 清理非法字符
     */
    private static Pattern CLEAR_XML_PATTERN = Pattern.compile("<\\?.*\\?>\\n");

    /**
     * 日期格式 yyyy-MM-dd
     */
    private static Pattern DATE_PATTERN_1 = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    /**
     * 日期格式yyyy/MM/dd
     */
    private static Pattern DATE_PATTERN_2 = Pattern.compile("^\\d{4}/\\d{2}/\\d{2}$");

    /**
     * 日期时间格式 yyyy-MM-dd HH:mm:ss
     */
    private static Pattern DATE_TIME_PATTERN_1 = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$");

    /**
     * 日期时间格式 yyyy/MM/dd HH:mm:ss
     */
    private static Pattern DATE_TIME_PATTERN_2 = Pattern.compile("^\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}$");

    /**
     * 日期时间格式 yyyy:MM:dd HH:mm:ss.SSS
     */
    private static Pattern DATETIME_WITH_MILLISECOND_PATTERN_1 = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");

    /**
     * 日期时间格式 yyyy/MM/dd HH:mm:ss.SSS
     */
    private static Pattern DATETIME_WITH_MILLISECOND_PATTERN_2 = Pattern.compile("^\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}$");

    /**
     * 小数的正则表达式
     */
    private static Pattern PATTERN_FLOAT = Pattern.compile("[+-]?[0-9]+(\\.[0-9]+)?");

    /**
     * 数字的正则表达式
     */
    private static Pattern NUMBER = Pattern.compile("[0-9]*");

    /**
     * 数字、英文字母或英文符号的正则表达式
     */
    private static Pattern NUMBER_OR_ENGLISH_CHARACTER = Pattern.compile("^[a-zA-Z0-9\\_\\~\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\-\\+\\<\\>\\?\\.\\/\\:\\']+$");

    /**
     * 判断是否为小数
     *
     * @param str
     * @return
     */
    public static boolean validateFloatNumber(String str) {
        return PATTERN_FLOAT.matcher(str).matches();
    }

    /**
     * 判断是否为数字
     *
     * @param str
     * @return
     */
    public static boolean validateNumber(String str) {
        return NUMBER.matcher(str).matches();
    }

    /**
     * 判断是否为合规id
     *ID规则，只能英文、数字、字符~!@#$%^&*()-_+<>?
     * @param str
     * @return
     */
    public static boolean validateId(String str){
        return NUMBER_OR_ENGLISH_CHARACTER.matcher(str).matches();
    }
}
