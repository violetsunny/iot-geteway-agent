package top.iot.protocol.godson.utils;

import java.util.regex.Pattern;

/**
 * @Author: alec
 * Description:
 * @date: 14:46 2020-10-20
 */
public class ToBinBitUtils {

    public static int[] toBit(int number, int len) {
        //2.需要一个长度为32的int数组来存储结果二进制
        int[] bit = new int[len];
        //3.循环，把原始数除以2取得余数，这个余数就是二进制数，原始的数等于商。
        //商如果不能再除以二，结束循环。
        for(int i = 0; number >= 1; i++)
        {
            //取得除以2的余数
            int b = number % 2;
            //数字赋值为除以2的商
            number = number / 2;
            bit[i] = b;
            if( number < 2 )
            {
                //已经不能再把数除以2，就把上直接放到数组的下一位
                bit[i + 1] = number;
            }
        }
        //4.翻转数组
        for(int i = 0; i < bit.length / 2;i++)
        {
            int temp = bit[i];
            //第一个数的值设置为最后一个数的值
            //第二次的时候，i是1，把第二个数的值，赋值为倒数第二个
            bit[i] = bit[ bit.length - 1 - i ];
            bit[ bit.length - 1 - i ] = temp;
        }
        return bit;
    }

    /**
     * 将字节数组转换成整数
     * */
    public static int toInt(int [] bites, int start, int end) {
        if (end >= bites.length || start >= bites.length) {
            return -1;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = start; i <= end; i++) {
            stringBuilder.append(bites[i]);
        }
        return Integer.parseInt(stringBuilder.toString(), 2);
    }

    public static boolean isBase64(String str) {
        String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
        return Pattern.matches(base64Pattern, str);
    }
}