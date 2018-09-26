package com.oneliang.tools.linearalloc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;

import com.oneliang.Constants;
import com.oneliang.thirdparty.asm.util.AsmUtil;
import com.oneliang.thirdparty.asm.util.ClassDescription;
import com.oneliang.tools.linearalloc.AllocClassVisitor.MethodReference;

/**
 * Tool to get stats about dalvik classes.
 */
public final class LinearAllocUtil {

    /** Utility class: do not instantiate */
    private LinearAllocUtil() {
    }

    // Reasonable defaults based on dreiss's observations.
    private static final Map<Pattern, Integer> PENALTIES = new HashMap<Pattern, Integer>();
    static {
        PENALTIES.put(Pattern.compile("Layout$"), 1500);
        PENALTIES.put(Pattern.compile("View$"), 1500);
        PENALTIES.put(Pattern.compile("ViewGroup$"), 1800);
        PENALTIES.put(Pattern.compile("Activity$"), 1100);
    }

    public static void main(String[] args) throws Exception {
        // System.setOut(new PrintStream(new
        // FileOutputStream("/D:/method.txt")));
        // BuilderUtil.androidDx("/D:/a.dex",
        // Arrays.asList("/D:/android/classes/dexClasses1"), true);

        // String dex1="/D:/android/classes/dexClasses1";
        // AsmUtil.traceClass(dex1, new PrintWriter(System.out));
        // List<String> fileList=FileUtil.findMatchFile(dex1, ".class");
        AllocStat totalAllocStat = new AllocStat();
        totalAllocStat.setMethodReferenceMap(new HashMap<String, String>());
        List<String> fileList = Arrays.asList("/D:/android/classes/dexClasses2/com/tencent/mm/plugin/shake/ui/ShakeReportUI.class");
        for (String file : fileList) {
            AllocStat allocStat = LinearAllocUtil.estimateClass(new FileInputStream(file));
            List<MethodReference> methodReferenceList = allocStat.getMethodReferenceList();
            if (methodReferenceList != null) {
                for (MethodReference methodReference : methodReferenceList) {
                    totalAllocStat.getMethodReferenceMap().put(methodReference.toString(), methodReference.toString());
                }
            }
            totalAllocStat.setTotalAlloc(totalAllocStat.getTotalAlloc() + allocStat.getTotalAlloc());
        }
        System.out.println(totalAllocStat.getTotalAlloc() + "," + totalAllocStat.getMethodReferenceMap().size());
        Set<String> keySet = totalAllocStat.getMethodReferenceMap().keySet();
        for (String key : keySet) {
            System.out.println(key);
        }
        // String jarFullFilename="/D:/android1/optimized/proguard/sdk.jar";
        // AllocStat allocStat=estimateJar(jarFullFilename);
        // System.out.println(allocStat.getTotalAlloc());
        // String
        // classFullFilename="/D:/android1/classes/sdk/com/tencent/mm/algorithm/DES.class";
        // InputStream inputStream=new FileInputStream(classFullFilename);
        // AllocStat allocStat=estimateClass(inputStream);
        // inputStream.close();
        // System.out.println(allocStat.getTotalAlloc()+","+allocStat.getMethodCount());

    }

    /**
     * estimate jar
     * 
     * @param jarFullFilename
     * @return AllocStat
     */
    public static AllocStat estimateJar(String jarFullFilename) {
        Map<String, String> methodReferenceMap = new HashMap<String, String>();
        Map<String, String> fieldReferenceMap = new HashMap<String, String>();
        AllocStat totalAllocStat = new AllocStat();
        int totalAlloc = 0;
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(jarFullFilename);
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                if (zipEntry.getName().endsWith(Constants.Symbol.DOT + Constants.File.CLASS)) {
                    InputStream inputStream = null;
                    try {
                        inputStream = zipFile.getInputStream(zipEntry);
                        AllocStat allocStat = LinearAllocUtil.estimateClass(inputStream);
                        totalAlloc += allocStat.getTotalAlloc();
                        List<MethodReference> methodReferenceList = allocStat.getMethodReferenceList();
                        if (methodReferenceList != null) {
                            for (MethodReference methodReference : methodReferenceList) {
                                methodReferenceMap.put(methodReference.toString(), methodReference.toString());
                            }
                        }
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    }
                    // field reference
                    try {
                        inputStream = zipFile.getInputStream(zipEntry);
                        ClassDescription classDescription = AsmUtil.findClassDescription(inputStream);
                        Map<String, String> referenceFieldNameMap = classDescription.referenceFieldNameMap;
                        Iterator<Entry<String, String>> iterator = referenceFieldNameMap.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Entry<String, String> entry = iterator.next();
                            String value = entry.getValue();
                            fieldReferenceMap.put(value, value);
                        }
                        List<String> fieldNameList = classDescription.fieldNameList;
                        for (String fieldName : fieldNameList) {
                            fieldReferenceMap.put(fieldName, fieldName);
                        }
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new LinearAllocException(e);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    throw new LinearAllocException(e);
                }
            }
        }
        totalAllocStat.setTotalAlloc(totalAlloc);
        totalAllocStat.setMethodReferenceMap(methodReferenceMap);
        totalAllocStat.setFieldReferenceMap(fieldReferenceMap);
        return totalAllocStat;
    }

    /**
     * Estimates the footprint that a given class will have in the LinearAlloc
     * buffer of Android's Dalvik VM.
     * 
     * @param inputStream
     *            Raw bytes of the Java class to analyze.need to close manual
     * @return AllocStat
     */
    public static AllocStat estimateClass(InputStream inputStream) {
        AllocStat allocStat = null;
        try {
            ClassReader classReader = new ClassReader(inputStream);
            allocStat = estimateClass(classReader);
        } catch (Exception e) {
            new LinearAllocException(e);
        }
        return allocStat != null ? allocStat : AllocStat.ZERO;
    }

    /**
     * Estimates the footprint that a given class will have in the LinearAlloc
     * buffer of Android's Dalvik VM.
     * 
     * @param classReader
     *            reader containing the Java class to analyze.
     * @return AllocStat
     */
    static AllocStat estimateClass(ClassReader classReader) {
        // SKIP_FRAMES was required to avoid an exception in ClassReader when
        // running on proguard
        // output. We don't need to visit frames so this isn't an issue.
        AllocClassVisitor allocClassVisitor = new AllocClassVisitor(PENALTIES);
        classReader.accept(allocClassVisitor, ClassReader.SKIP_FRAMES);
        AllocStat allocStat = new AllocStat();
        allocStat.setTotalAlloc(allocClassVisitor.getTotalAlloc());
        allocStat.setMethodReferenceList(allocClassVisitor.getMethodReferenceList());
        return allocStat;
    }

    public static class AllocStat {
        static final AllocStat ZERO = new AllocStat();
        private int totalAlloc = 0;
        private List<MethodReference> methodReferenceList = null;
        private Map<String, String> methodReferenceMap = null;
        private Map<String, String> fieldReferenceMap = null;

        /**
         * @return the totalAlloc
         */
        public int getTotalAlloc() {
            return totalAlloc;
        }

        /**
         * @param totalAlloc
         *            the totalAlloc to set
         */
        public void setTotalAlloc(int totalAlloc) {
            this.totalAlloc = totalAlloc;
        }

        /**
         * @return the methodReferenceList
         */
        public List<MethodReference> getMethodReferenceList() {
            return methodReferenceList;
        }

        /**
         * @param methodReferenceList
         *            the methodReferenceList to set
         */
        public void setMethodReferenceList(List<MethodReference> methodReferenceList) {
            this.methodReferenceList = methodReferenceList;
        }

        /**
         * @return the methodReferenceMap
         */
        public Map<String, String> getMethodReferenceMap() {
            return methodReferenceMap;
        }

        /**
         * @param methodReferenceMap
         *            the methodReferenceMap to set
         */
        public void setMethodReferenceMap(Map<String, String> methodReferenceMap) {
            this.methodReferenceMap = methodReferenceMap;
        }

        /**
         * @return the fieldReferenceMap
         */
        public Map<String, String> getFieldReferenceMap() {
            return fieldReferenceMap;
        }

        /**
         * @param fieldReferenceMap
         *            the fieldReferenceMap to set
         */
        public void setFieldReferenceMap(Map<String, String> fieldReferenceMap) {
            this.fieldReferenceMap = fieldReferenceMap;
        }
    }

    public static class LinearAllocException extends RuntimeException {
        private static final long serialVersionUID = -6285932056376466775L;

        public LinearAllocException(Throwable cause) {
            super(cause);
        }
    }
}
