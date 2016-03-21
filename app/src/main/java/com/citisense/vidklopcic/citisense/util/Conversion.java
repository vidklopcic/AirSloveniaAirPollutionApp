package com.citisense.vidklopcic.citisense.util;

import android.content.res.Resources;
import android.util.TypedValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class Conversion {
    public static class IO {
        public static String inputStreamToString(InputStream is) {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            StringBuilder total = new StringBuilder();
            String line;
            try {
                while ((line = r.readLine()) != null) {
                    total.append(line);
                }
            } catch (IOException e) {
                return "";
            }
            return total.toString();
        }
    }
}
