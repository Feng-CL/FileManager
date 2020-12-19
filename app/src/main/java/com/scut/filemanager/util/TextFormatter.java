package com.scut.filemanager.util;


import java.text.DecimalFormat;
import android.os.Build;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TextFormatter {
    //默认保留一位小数,用于转换单位

    /*
    @Description: 一个简单的long 到String的转换函数，日期格式固定为 "HH:mm:ss yyyy-MM-dd"
     */
    public static String timeDescriptionConvert_simpleLongToString(long time_long){
        Date date=new Date(time_long);
        SimpleDateFormat Formatter=new SimpleDateFormat("HH:mm:ss yyyy-MM-dd");
        return Formatter.format(date);

    }

    /*
    * @Description: 描述日期的字符串必须是”HH:mm:ss yyyy/MM/dd" 类型的
    * 该函数暂时为descriptionConvert_simpleLongToString的逆向操作*/
    public static long timeDescriptionConvert_simpleStringToLong(String time_string) throws ParseException {
        SimpleDateFormat Formatter=new SimpleDateFormat("HH:mm:ss yyyy-MM-dd");
        Date date=Formatter.parse(time_string);
        return date.getTime();
    }

    public static String byteCountDescriptionConvert_longToString(String unit, long l, int savePoint){
        //
        double size_d;
        DecimalFormat decimalFormat=new DecimalFormat();
        decimalFormat.setMaximumFractionDigits(savePoint);
        StringBuffer varStr=new StringBuffer();
        varStr.setLength(0);
        switch(unit){
            case "MB":
                size_d=(double)l/Math.pow(1024,2);
                varStr.append(decimalFormat.format(size_d));
                break;
            case "KB":
                size_d=(double)l/(double)1024;
                varStr.append(decimalFormat.format(size_d));
                break;
            case "GB":
                size_d=(double)l/Math.pow(1024,3);
                varStr.append((decimalFormat.format(size_d)));
                break;
            default:
                varStr.append(Long.toString(l));
                break;
        }

        return varStr.toString();

    }

}
