package com.oneliang.tools.autodex.test;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import com.oneliang.Constant;
import com.oneliang.util.file.FileUtil;

public class TestCountPackageClass {
    private Map<String, Counter> map = new TreeMap<String, Counter>();

    public void countPackageClass(String className) {
        int lastIndex = className.lastIndexOf(Constant.Symbol.SLASH_LEFT);
        if (lastIndex > 0) {
            while (lastIndex > 0) {
                String packageName = className.substring(0, lastIndex);
                if (map.containsKey(packageName)) {
                    map.get(packageName).count();
                } else {
                    map.put(packageName, new Counter());
                }
                lastIndex = packageName.lastIndexOf(Constant.Symbol.SLASH_LEFT);
            }
        } else {
            final String defaultPackageName = "default package";
            if (map.containsKey(defaultPackageName)) {
                map.get(defaultPackageName).count();
            } else {
                map.put(defaultPackageName, new Counter());
            }
        }
    }

    public void print() {
        Iterator<Entry<String, Counter>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, Counter> entry = iterator.next();
            System.out.println(entry.getKey() + "," + entry.getValue().getCount());
        }
    }

    public static void main(String[] args) throws Exception {
        String outputFullFilename = "/D:/packageClassCount.csv";
        System.setOut(new PrintStream(new FileOutputStream(outputFullFilename)));
        String fullFilename = "/D:/autodex/output/0_retrace.txt";
        TestCountPackageClass classCount = new TestCountPackageClass();

        Properties properties = FileUtil.getProperties(fullFilename);
        Iterator<Entry<Object, Object>> iterator = properties.entrySet().iterator();
        while (iterator.hasNext()) {
            Object className = iterator.next().getKey();
            classCount.countPackageClass(className.toString());
        }
        classCount.print();
    }

    private static final class Counter {
        private int count = 1;

        private void count() {
            count++;
        }

        public int getCount() {
            return this.count;
        }
    }
}
