package com.scut.filemanager.util;


import java.text.DecimalFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FMFormatter {
    //默认保留一位小数,用于转换单位

    /*
    @Description: 一个简单的long 到String的转换函数，日期格式固定为 "HH:mm:ss yyyy-MM-dd"
     */
    static SimpleDateFormat simpleDateFormat=null;

    public static String timeDescriptionConvert_LongStyle_l2s(long time_long){
        Date date=new Date(time_long);
        if(simpleDateFormat==null) {
            simpleDateFormat = new SimpleDateFormat();
        }
        simpleDateFormat.applyPattern("HH:mm:ss yyyy-MM-dd");
        return simpleDateFormat.format(date);

    }

    public static String timeDescriptionConvert_ShortStyle_l2s(long time){
        if(simpleDateFormat==null){
            simpleDateFormat=new SimpleDateFormat();
        }
        simpleDateFormat.applyPattern("HH:mm:ss");
        return simpleDateFormat.format(
                new Date(time)
        );
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

    public static String getSuitableFileSizeString(long size){
        String unit;
        if(size<1024){
            unit="B";
        }
        else if(size>=1024&&size<1024*1024){
            unit="KB";
        }
        else if(size>=1024*1024&&size<1024*1024*1024){
            unit="MB";
        }
        else{
            unit="GB";
        }
        String sizeStr=byteCountDescriptionConvert_longToString(unit,size,1);
        return sizeStr+unit;
    }

    public static  String ip4Address_i2s(int IP4Address){
            return (IP4Address & 0xFF ) + "." +
                    ((IP4Address >> 8 ) & 0xFF) + "." +
                    ((IP4Address >> 16 ) & 0xFF) + "." +
                    ( IP4Address >> 24 & 0xFF) ;
    }


    private static DecimalFormat decimalFormat_d2s=new DecimalFormat();
    public static String d2s(double d, int savePoint){
        decimalFormat_d2s.setMaximumFractionDigits(savePoint);
        return decimalFormat_d2s.format(d);
    }

}
