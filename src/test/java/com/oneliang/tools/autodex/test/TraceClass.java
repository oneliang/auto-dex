package com.oneliang.tools.autodex.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import com.oneliang.Constant;
import com.oneliang.thirdparty.asm.util.AsmUtil;
import com.oneliang.thirdparty.asm.util.ClassDescription;
import com.oneliang.tools.autodex.test.TestAutoDexUtil.ClassProcessor;
import com.oneliang.tools.linearalloc.AllocClassVisitor.MethodReference;
import com.oneliang.tools.linearalloc.LinearAllocUtil;
import com.oneliang.tools.linearalloc.LinearAllocUtil.AllocStat;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.proguard.Retrace;
import com.tencent.extraguard.smr.analyze.ClassHierarchyCollectVisitor;
import com.tencent.extraguard.smr.analyze.SyntheticMethodCollectVisitor;
import com.tencent.extraguard.smr.analyze.SyntheticMethodInlineVisitor;

public class TraceClass {
    public static void traceClass() throws Exception {
        String classFullFilename = "/D:/autodex/NearbyFriendShowSayHiUI.class";
        AsmUtil.traceClass(classFullFilename, new PrintWriter(System.out));
        AsmUtil.findClassDescription(classFullFilename);
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(classFullFilename);
            AllocStat allocStat = LinearAllocUtil.estimateClass(inputStream);
            System.out.println(allocStat.getMethodReferenceList().size());
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private static Map<String, AllocStat> cache = new HashMap<String, AllocStat>();

    private static StatWrapper caculate(String className, Map<String, ClassDescription> classDescriptionMap, Map<String, List<ClassDescription>> referencedClassDescriptionListMap, Map<String, String> allClassNameMap, Map<String, byte[]> classNameByteArrayMap) throws Exception {
        Set<String> rootClassNameSet = new HashSet<String>();
        rootClassNameSet.add(className);
        Map<String, String> classNameMap = AsmUtil.findAllDependClassNameMap(rootClassNameSet, classDescriptionMap, referencedClassDescriptionListMap, allClassNameMap, true);
        AllocStat totalAllocStat = new AllocStat();
        totalAllocStat.setMethodReferenceMap(new HashMap<String, String>());
        totalAllocStat.setFieldReferenceMap(new HashMap<String, String>());
        for (String key : classNameMap.keySet()) {
            AllocStat allocStat = null;
            if (cache.containsKey(key)) {
                allocStat = cache.get(key);
            } else {
                InputStream inputStream = new ByteArrayInputStream(classNameByteArrayMap.get(key));
                allocStat = LinearAllocUtil.estimateClass(inputStream);
                allocStat.setFieldReferenceMap(new HashMap<String, String>());
                allocStat.setMethodReferenceMap(new HashMap<String, String>());
                inputStream.close();
                List<MethodReference> methodReferenceList = allocStat.getMethodReferenceList();
                if (methodReferenceList != null) {
                    for (MethodReference methodReference : methodReferenceList) {
                        allocStat.getMethodReferenceMap().put(methodReference.toString(), methodReference.toString());
                    }
                }
                // field reference map
                ClassDescription classDescription = classDescriptionMap.get(key);
                allocStat.getFieldReferenceMap().putAll(classDescription.referenceFieldNameMap);
                for (String fieldName : classDescription.fieldNameList) {
                    allocStat.getFieldReferenceMap().put(fieldName, fieldName);
                }
                cache.put(key, allocStat);
            }
            totalAllocStat.getFieldReferenceMap().putAll(allocStat.getFieldReferenceMap());
            totalAllocStat.getMethodReferenceMap().putAll(allocStat.getMethodReferenceMap());
        }
        StatWrapper statWrapper = new StatWrapper();
        statWrapper.size = classNameMap.size();
        statWrapper.allocStat = totalAllocStat;
        return statWrapper;
    }

    public static void main1(String[] args) throws Exception {
        String allClassesJarFullFilename = "/D:/autodex/main.jar";
        Set<String> rootClassNameSet = new HashSet<String>();
        String rootClassName = "com/tencent/mm/ui/LauncherUI.class";
        rootClassNameSet.add(rootClassName);
        String mappingFile = "/D:/autodex/mapping.txt";
        ClassProcessor classProcessor = new ClassProcessor();
        Retrace.readMapping(mappingFile, classProcessor);

        Map<String, byte[]> classNameByteArrayMap = new HashMap<String, byte[]>();
        ZipFile zipFile = new ZipFile(allClassesJarFullFilename);
        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry zipEntry = enumeration.nextElement();
            String zipEntryName = zipEntry.getName();
            if (!zipEntryName.endsWith(Constant.Symbol.DOT + Constant.File.CLASS)) {
                continue;
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            FileUtil.copyStream(inputStream, byteArrayOutputStream);
            inputStream.close();
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            // logger.verbose(zipEntryName+","+byteArray.length);
            classNameByteArrayMap.put(zipEntryName, byteArray);
        }
        zipFile.close();
        Map<String, List<ClassDescription>> referencedClassDescriptionListMap = new HashMap<String, List<ClassDescription>>();
        Map<String, ClassDescription> classDescriptionMap = AsmUtil.findClassDescriptionMap(classNameByteArrayMap, referencedClassDescriptionListMap);

        Map<String, String> allClassNameMap = new HashMap<String, String>();
        Set<String> classNameKeySet = classDescriptionMap.keySet();
        for (String className : classNameKeySet) {
            allClassNameMap.put(className, className);
        }

        String projectRealPath = new File(StringUtil.BLANK).getAbsolutePath();
        String logFullFilename = projectRealPath + "/log/default.log";
        FileUtil.createFile(logFullFilename);
        System.setOut(new PrintStream(new FileOutputStream(logFullFilename)));
        Map<String, String> dependClassNameMap = AsmUtil.findAllDependClassNameMap(rootClassNameSet, classDescriptionMap, referencedClassDescriptionListMap, allClassNameMap, true);
        // for (String dependClassName : dependClassNameMap.keySet()) {
        String dependClassName = "com/tencent/mm/app/plugin/e.class";
        StatWrapper statWrapper = caculate(dependClassName, classDescriptionMap, referencedClassDescriptionListMap, allClassNameMap, classNameByteArrayMap);
        System.out.println(dependClassName + "," + classProcessor.classNameMap.get(dependClassName) + "," + statWrapper.size + "," + statWrapper.allocStat.getFieldReferenceMap().size() + "," + statWrapper.allocStat.getMethodReferenceMap().size());
        // }
    }

    static class StatWrapper {
        public int size;
        public AllocStat allocStat;
    }

    public static void main(String[] args) throws Exception {
        inlineJar();
    }

    static void inlineJar() throws Exception {
        ZipFile zipFile = new ZipFile("/D:/autodex/main.jar");
        File outputFile = new File("/D:/autodex/main_inline.jar");
        outputFile.createNewFile();
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile));
        Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry zipEntry = enumeration.nextElement();
            String zipEntryName = zipEntry.getName();
            if (!zipEntryName.endsWith(Constant.Symbol.DOT + Constant.File.CLASS)) {
                continue;
            }
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
            try {
                transformSingleClass(inputStream, zipOutputStream);
            } catch (ZipException e) {
                // do nothing
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                zipOutputStream.closeEntry();
            }

            inputStream.close();
        }
        zipFile.close();
        zipOutputStream.close();
    }

    public static void transformSingleClass(InputStream is, OutputStream os) throws IOException {
        final ClassReader cr = new ClassReader(is);

        final ClassHierarchyCollectVisitor chcv = new ClassHierarchyCollectVisitor();
        cr.accept(chcv, 0);

        final SyntheticMethodCollectVisitor smcv = new SyntheticMethodCollectVisitor(chcv.classInternalNameToInfoMap);
        cr.accept(smcv, ClassReader.EXPAND_FRAMES);

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            protected String getCommonSuperClass(String type1, String type2) {
                return "java/lang/Object";
            }
        };
        final SyntheticMethodInlineVisitor smiv = new SyntheticMethodInlineVisitor(smcv.syntheticMethodDescToNodeMap, smcv.targetDescsReferedBySyntheticMethod, cw);
        cr.accept(smiv, ClassReader.EXPAND_FRAMES);

        final byte[] newClassData = cw.toByteArray();
        os.write(newClassData);
        os.flush();
    }
}
