package com.scut.filemanager.util;


import java.text.DecimalFormat;
import android.os.Build;


public class textFormatter {
    //默认保留一位小数,用于转换单位

    public static String longToString(String unit, long l, int savePoint){
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
