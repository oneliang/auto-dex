package com.oneliang.tools.trace;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oneliang.Constants;
import com.oneliang.thirdparty.asm.util.AsmUtil;
import com.oneliang.thirdparty.asm.util.ClassDescription;
import com.oneliang.tools.autodex.AutoDexUtil;
import com.oneliang.tools.linearalloc.AllocClassVisitor.MethodReference;
import com.oneliang.tools.linearalloc.LinearAllocUtil;
import com.oneliang.tools.linearalloc.LinearAllocUtil.AllocStat;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.proguard.Retrace;

;

public class TraceClass {
    private static Map<String, AllocStat> cache = new HashMap<String, AllocStat>();

    public static void traceCanFixDependencyClass(List<String> combinedClassList, List<String> mainDexList, String androidManifestFullFilename, String mappingFile, int maxReferencedSize, String outputTraceFile) throws Exception {
//        String projectRealPath = new File(StringUtil.BLANK).getAbsolutePath();
//        String logFullFilename = projectRealPath + "/log/default.log";
//        FileUtil.createFile(logFullFilename);
//        System.setOut(new PrintStream(new FileOutputStream(logFullFilename)));

//        List<String> combinedClassList = Arrays.asList("/D:/autodex/main.jar");
//        String mainDexList = "/D:/autodex/main-dex-list.txt";
//        String androidManifestFullFilename = "/D:/autodex/AndroidManifest.xml";
//        String mappingFile = "/D:/autodex/mapping.txt";

        List<String> classNameList = new ArrayList<String>();
        classNameList.addAll(mainDexList);
        classNameList.addAll(AutoDexUtil.findMainDexClassListFromAndroidManifest(androidManifestFullFilename, false));
        Set<String> rootClassNameSet = new HashSet<String>();
        AutoDexUtil.Cache cache = AutoDexUtil.readAllCombinedClassWithCacheFile(combinedClassList, null);
        String packageName = AutoDexUtil.parsePackageName(androidManifestFullFilename);
        String slashPackageName = packageName.replace(Constants.Symbol.DOT, Constants.Symbol.SLASH_LEFT);
        rootClassNameSet.addAll(AutoDexUtil.findMainRootClassSet(cache.classNameByteArrayMap.keySet(), packageName, classNameList, null));

        ClassProcessor classProcessor = new ClassProcessor();
        Retrace.readMapping(mappingFile, classProcessor);

        Map<String, List<ClassDescription>> referencedClassDescriptionListMap = new HashMap<String, List<ClassDescription>>();
        Map<String, ClassDescription> classDescriptionMap = AsmUtil.findClassDescriptionMap(cache.classNameByteArrayMap, referencedClassDescriptionListMap);

        Map<String, String> allClassNameMap = new HashMap<String, String>();
        Set<String> classNameKeySet = classDescriptionMap.keySet();
        for (String className : classNameKeySet) {
            allClassNameMap.put(className, className);
        }

        Map<String, String> dependClassNameMap = AsmUtil.findAllDependClassNameMap(rootClassNameSet, classDescriptionMap, referencedClassDescriptionListMap, allClassNameMap, true);
        // System.out.println("begin");
        StringBuilder stringBuilder = new StringBuilder();
        for (String dependClassName : dependClassNameMap.keySet()) {
            List<ClassDescription> referencedClassDescriptionList = referencedClassDescriptionListMap.get(dependClassName);
            if (referencedClassDescriptionList == null || referencedClassDescriptionList.isEmpty()) {
                System.err.println("ReferencedClassDescriptionList is empty.class:" + dependClassName);
                continue;
            }
            int realReferencedSize = referencedClassDescriptionList.size() - 1;
            if (realReferencedSize <= maxReferencedSize) {
                ClassDescription classDescription = classDescriptionMap.get(dependClassName);
                if (!Modifier.isPublic(classDescription.access)) {
                    continue;
                }
                if (rootClassNameSet.contains(dependClassName)) {
                    continue;
                }
                if (!dependClassName.startsWith(slashPackageName)) {
                    continue;
                }
                StatWrapper statWrapper = caculate(dependClassName, classDescriptionMap, referencedClassDescriptionListMap, allClassNameMap, cache.classNameByteArrayMap, true);
                StatWrapper deepStatWrapper = caculate(dependClassName, classDescriptionMap, referencedClassDescriptionListMap, allClassNameMap, cache.classNameByteArrayMap, false);
                stringBuilder.append(classProcessor.classNameMap.get(dependClassName) + "," + statWrapper.size + "," + realReferencedSize + "," + statWrapper.allocStat.getFieldReferenceMap().size() + "," + statWrapper.allocStat.getMethodReferenceMap().size() + "," + deepStatWrapper.allocStat.getMethodReferenceMap().size());
                stringBuilder.append(StringUtil.LF_STRING);
            }
        }
        FileUtil.writeFile(outputTraceFile, stringBuilder.toString().getBytes(Constants.Encoding.UTF8));
    }

    private static StatWrapper caculate(String className, Map<String, ClassDescription> classDescriptionMap, Map<String, List<ClassDescription>> referencedClassDescriptionListMap, Map<String, String> allClassNameMap, Map<String, byte[]> classNameByteArrayMap, boolean onlyDirectDepend) throws Exception {
        Set<String> rootClassNameSet = new HashSet<String>();
        rootClassNameSet.add(className);
        Map<String, String> classNameMap = null;
        if (onlyDirectDepend) {
            ClassDescription classDescription = classDescriptionMap.get(className);
            classNameMap = new HashMap<String, String>();
            for (String key : classDescription.dependClassNameMap.keySet()) {
                String dependClassName = key + Constants.Symbol.DOT + Constants.File.CLASS;
                classNameMap.put(dependClassName, dependClassName);
            }
            // System.out.println("direct:");
        } else {
            classNameMap = AsmUtil.findAllDependClassNameMap(rootClassNameSet, classDescriptionMap, referencedClassDescriptionListMap, allClassNameMap, true);
            // System.out.println("deep:");
        }
        return caculate(className, classDescriptionMap, classNameMap, classNameByteArrayMap);
    }

    private static StatWrapper caculate(String className, Map<String, ClassDescription> classDescriptionMap, Map<String, String> dependClassNameMap, Map<String, byte[]> classNameByteArrayMap) throws Exception {
        AllocStat totalAllocStat = new AllocStat();
        totalAllocStat.setMethodReferenceMap(new HashMap<String, String>());
        totalAllocStat.setFieldReferenceMap(new HashMap<String, String>());
        for (String key : dependClassNameMap.keySet()) {
            AllocStat allocStat = null;
            // System.out.println("\tdepend:"+key);
            if (cache.containsKey(key)) {
                allocStat = cache.get(key);
            } else {
                if (classNameByteArrayMap.get(key) == null) {
                    continue;
                }
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
        statWrapper.size = dependClassNameMap.size();
        statWrapper.allocStat = totalAllocStat;
        return statWrapper;
    }

    static class StatWrapper {
        public int size;
        public AllocStat allocStat;
    }

    public static class ClassProcessor implements Retrace.Processor {
        public Map<String, String> classNameMap = new HashMap<String, String>();

        public void processMethodMapping(String className, int firstLineNumber, int lastLineNumber, String methodReturnType, String methodName, String methodArguments, String newMethodName) {
        }

        public void processFieldMapping(String className, String fieldType, String fieldName, String newFieldName) {
        }

        public void processClassMapping(String className, String newClassName) {
            classNameMap.put(newClassName.replace(Constants.Symbol.DOT, Constants.Symbol.SLASH_LEFT) + Constants.Symbol.DOT + Constants.File.CLASS, className.replace(Constants.Symbol.DOT, Constants.Symbol.SLASH_LEFT) + Constants.Symbol.DOT + Constants.File.CLASS);
        }
    }

    ;
}
