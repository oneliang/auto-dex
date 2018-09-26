package com.oneliang.tools.autodex.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import com.oneliang.Constants;
import com.oneliang.thirdparty.asm.util.AsmUtil;
import com.oneliang.thirdparty.asm.util.ClassDescription;
import com.oneliang.tools.autodex.AutoDexUtil;
import com.oneliang.tools.autodex.AutoDexUtil.Cache;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.logging.AbstractLogger;
import com.oneliang.util.logging.BaseLogger;
import com.oneliang.util.logging.ComplexLogger;
import com.oneliang.util.logging.FileLogger;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;
import com.oneliang.util.proguard.Retrace;
import com.oneliang.util.proguard.Retrace.Processor;

public class TestAutoDexUtil {

    private static final List<String> combinedClassList = Arrays.asList("/D:/autodex/main.jar");
    private static final String androidManifestFullFilename = "/D:/autodex/AndroidManifest.xml";
    private static final String cacheFullFilename = "/D:/autodex/cache";
    // private static final String
    // mainDexOtherClasses=".app.FirstCrashCatcher,.app.MMApplicationWrapper,.ui.NoRomSpaceDexUI,.svg.SVGPreload,.svg.SVGBuildConfig,.plugin.exdevice,.jni.C2JavaExDevice,.svg.graphics.SVGCodeDrawable,com.tenpay.cert.CertUtil,.svg.WeChatSVGRenderC2Java,com.tencent.kingkong.database.SQLiteDoneException,.crash.CrashUploaderService,.app.WorkerProfile,.app.PusherProfile,.app.ToolsProfile,.app.SandBoxProfile,.app.ExDeviceProfile,.app.PatchProfile,.app.TMAssistantProfile,.app.NoSpaceProfile,.plugin.sandbox.SubCoreSandBox,.sdk.platformtools.CrashMonitorForJni,.jni.utils.UtilsJni,.plugin.accountsync.Plugin,.plugin.sandbox.Plugin,.ui.base.preference.PreferenceScreen,.lan_cs.Client,.lan_cs.Server,.svg.SVGResourceRegister,.jni.platformcomm.PlatformCommBridge,.app.MMApplicationLike,.app.Application,com.tencent.tinker.loader.**";
    private static final String outputDirectory = "/D:/autodex/output";
    private static final String mainDexList = "/D:/autodex/main-dex-list.txt";
    private static final String mappingFile = "/D:/autodex/mapping.txt";
    private static final boolean debug = false;

    public static List<String> readMainDexClassList(String mainDexList) {
        List<String> mainDexClassList = new ArrayList<String>();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(mainDexList), Constants.Encoding.UTF8));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                String mainDexClass = line.trim();
                if (StringUtil.isNotBlank(mainDexClass)) {
                    mainDexClassList.add(mainDexClass);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mainDexClassList;
    }

    public static void main2(String[] args) throws Exception {
        String mappingFile = "/D:/autodex/mapping_2660.txt";
        String outputDirectory = "/D:/autodex";
        ClassProcessor classProcessor = new ClassProcessor();
        Retrace.readMapping(mappingFile, classProcessor);
        Properties properties = FileUtil.getProperties(outputDirectory + "/0_2660.txt");
        Iterator<Entry<Object, Object>> iterator = properties.entrySet().iterator();
        StringBuilder stringBuilder = new StringBuilder();
        while (iterator.hasNext()) {
            Object className = iterator.next().getKey();
            stringBuilder.append(classProcessor.classNameMap.get(className.toString()));
            stringBuilder.append(StringUtil.LF_STRING);
        }
        FileUtil.writeFile(outputDirectory + "/0_2660_retrace.txt", stringBuilder.toString().getBytes(Constants.Encoding.UTF8));
    }

    public static void main(String[] args) throws Exception {
//         traceClassDepend(Arrays.asList("com/tencent/mm/app/MMApplicationWrapper.class"));
//         System.exit(0);
        // String
        // classFullFilename="E:/Dandelion/java/githubWorkspace/auto-dex/bin/com/oneliang/tools/autodex/test/InnerClass$InnerInterface.class";
        // AsmUtil.traceClass(classFullFilename, new PrintWriter(System.out));
        // AsmUtil.findClassDescription(classFullFilename);
        List<AbstractLogger> loggerList = new ArrayList<AbstractLogger>();
        // loggerList.add(new BaseLogger(Logger.Level.INFO));
        loggerList.add(new FileLogger(Logger.Level.VERBOSE, new File("/D:/autodex/log.txt")));
        Logger logger = new ComplexLogger(Logger.Level.INFO, loggerList);
        LoggerManager.registerLogger(AutoDexUtil.class, logger);
        LoggerManager.registerLogger(AsmUtil.class, logger);
        System.setOut(new PrintStream(new FileOutputStream("/D:/autodex/log.txt")));
        FileUtil.deleteAllFile(outputDirectory);
        AutoDexUtil.Option option = new AutoDexUtil.Option(combinedClassList, androidManifestFullFilename, outputDirectory, debug);
        // option.minMainDex = true;
        // option.cacheFullFilename = cacheFullFilename;
        option.casual = true;
        // option.methodLimit = 0xE000;
        option.mainDexOtherClassList = readMainDexClassList(mainDexList);// Arrays.asList(mainDexOtherClasses.split(Constants.Symbol.COMMA));
        option.oldDexIdClassNameMap = getOldDexIdClassNameMap();
        AutoDexUtil.Result result = new AutoDexUtil.Result();
        try {
            AutoDexUtil.autoDex(option, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ClassProcessor classProcessor = new ClassProcessor();
        Retrace.readMapping(mappingFile, classProcessor);
        if (result.dexIdClassNameMap != null && result.classDescriptionMap != null && result.referencedClassDescriptionListMap != null) {
            // String referencedClassName =
            // "com/tencent/mm/ui/conversation/AppBrandServiceConversationUI.class";
            // List<ClassDescription> classDescriptionList =
            // result.referencedClassDescriptionListMap.get(referencedClassName);
            // if (classDescriptionList != null) {
            // for (ClassDescription classDescription : classDescriptionList) {
            // String callClassName = classDescription.className +
            // Constants.Symbol.DOT + Constants.File.CLASS;
            // System.out.println("call:" +
            // classProcessor.classNameMap.get(callClassName));
            // }
            // }
            // System.out.println("--------------------");
            Map<String, String> dexId0ClassNameMap = result.dexIdClassNameMap.get(0);
            Iterator<Entry<String, String>> dexId0ClassNameIterator = result.dexIdClassNameMap.get(0).entrySet().iterator();
            final String classPrefix = "com/tencent/mm/app/MMApplicationWrapper.class";
            while (dexId0ClassNameIterator.hasNext()) {
                Entry<String, String> classNameEntry = dexId0ClassNameIterator.next();
                String className = classNameEntry.getKey();
                List<ClassDescription> referencedClassDescriptionList = result.referencedClassDescriptionListMap.get(className);
                if (referencedClassDescriptionList != null) {
                    String tracedClassName = classProcessor.classNameMap.get(className);
                    if (tracedClassName != null && tracedClassName.startsWith(classPrefix)) {
                        // System.out.println("c:" +
                        // classProcessor.classNameMap.get(className));
                        for (ClassDescription classDescription : referencedClassDescriptionList) {
                            String callClassName = classDescription.className + Constants.Symbol.DOT + Constants.File.CLASS;
                            if (!dexId0ClassNameMap.containsKey(callClassName)) {// call
                                                                                 // class
                                                                                 // not
                                                                                 // in
                                                                                 // main
                                                                                 // dex
                                continue;
                            }
                            if (!callClassName.startsWith(classPrefix)) {
                                // System.out.println("\treal called(A be called
                                // by B):" +
                                // classProcessor.classNameMap.get(className) +
                                // "," +
                                // classProcessor.classNameMap.get(callClassName));
                            }
                        }
                    }
                    // System.out.println("c:" +
                    // classProcessor.classNameMap.get(className));
                    for (ClassDescription classDescription : referencedClassDescriptionList) {
                        String callClassName = classDescription.className + Constants.Symbol.DOT + Constants.File.CLASS;
                        if (classProcessor.classNameMap.get(callClassName) != null && classProcessor.classNameMap.get(callClassName).startsWith("com/tencent/mm")) {
                            // System.out.println("\treal called(A be called by
                            // B):" + classProcessor.classNameMap.get(className)
                            // + "," +
                            // classProcessor.classNameMap.get(callClassName));
                        }
                    }
                }
                ClassDescription classDescription = result.classDescriptionMap.get(className);
                // System.out.println("c:" +
                // classProcessor.classNameMap.get(className));
                for (String dependClassName : classDescription.dependClassNameList) {
                    dependClassName = dependClassName + Constants.Symbol.DOT + Constants.File.CLASS;
                    // System.out.println("\tdepend:" +
                    // classProcessor.classNameMap.get(dependClassName));
                }
            }
        }
        Properties properties = FileUtil.getProperties(outputDirectory + "/0.txt");
        Iterator<Entry<Object, Object>> iterator = properties.entrySet().iterator();
        StringBuilder stringBuilder = new StringBuilder();
        while (iterator.hasNext()) {
            Object className = iterator.next().getKey();
            stringBuilder.append(classProcessor.classNameMap.get(className.toString()));
            stringBuilder.append(StringUtil.LF_STRING);
        }
        FileUtil.writeFile(outputDirectory + "/0_retrace.txt", stringBuilder.toString().getBytes(Constants.Encoding.UTF8));
    }

    public static class ClassProcessor implements Processor {
        public Map<String, String> classNameMap = new HashMap<String, String>();

        public void processMethodMapping(String className, int firstLineNumber, int lastLineNumber, String methodReturnType, String methodName, String methodArguments, String newMethodName) {
        }

        public void processFieldMapping(String className, String fieldType, String fieldName, String newFieldName) {
        }

        public void processClassMapping(String className, String newClassName) {
            classNameMap.put(newClassName.replace(Constants.Symbol.DOT, Constants.Symbol.SLASH_LEFT) + Constants.Symbol.DOT + Constants.File.CLASS, className.replace(Constants.Symbol.DOT, Constants.Symbol.SLASH_LEFT) + Constants.Symbol.DOT + Constants.File.CLASS);
        }
    };

    private static void traceClassDepend(List<String> classList) throws Exception {
        for (String classFile : classList) {
            List<AbstractLogger> loggerList = new ArrayList<AbstractLogger>();
            System.setOut(new PrintStream(new File("/D:/autodex/" + new File(classFile).getName() + ".txt")));
            // loggerList.add(new FileLogger(Logger.Level.VERBOSE,new
            // File("/D:/autodex/"+new File(classFile).getName()+".txt")));
            // Logger logger=new ComplexLogger(Logger.Level.VERBOSE,
            // loggerList);
            Logger logger = new BaseLogger(Logger.Level.VERBOSE);
            LoggerManager.registerLogger(AutoDexUtil.class, logger);
            LoggerManager.registerLogger(AsmUtil.class, logger);
            AutoDexUtil.Option option = new AutoDexUtil.Option(combinedClassList, androidManifestFullFilename, outputDirectory, debug);
            option.minMainDex = false;
            option.mainDexOtherClassList = readMainDexClassList(mainDexList);
            // option.mainDexOtherClassList=Arrays.asList(mainDexOtherClasses.split(Constants.Symbol.COMMA));
            String cacheFullFilename = outputDirectory + Constants.Symbol.SLASH_LEFT + "cache.txt";
            Cache cache = AutoDexUtil.readAllCombinedClassWithCacheFile(option.combinedClassList, cacheFullFilename);
            Map<String, List<ClassDescription>> referencedClassDescriptionListMap = new HashMap<String, List<ClassDescription>>();
            Map<String, ClassDescription> classDescriptionMap = new HashMap<String, ClassDescription>();
            if (cache.classNameByteArrayMap != null) {
                classDescriptionMap.putAll(AsmUtil.findClassDescriptionMap(cache.classNameByteArrayMap, referencedClassDescriptionListMap));
            }
            Map<String, String> allClassNameMap = new HashMap<String, String>();
            Set<String> classNameKeySet = classDescriptionMap.keySet();
            for (String className : classNameKeySet) {
                allClassNameMap.put(className, className);
            }
            Set<String> rootClassNameSet = new HashSet<String>();
            // rootClassNameSet.add("com/tencent/mm/plugin/gcm/modelgcm/GcmBroadcastReceiver.class");
            rootClassNameSet.add(classFile);
            AsmUtil.findAllDependClassNameMap(rootClassNameSet, classDescriptionMap, referencedClassDescriptionListMap, allClassNameMap, true);
        }
    }

    private static Map<Integer, Map<String, String>> getOldDexIdClassNameMap() {
        Map<Integer, Map<String, String>> oldDexIdClassNameMap = new HashMap<Integer, Map<String, String>>();
        Map<String, String> dexId0ClassNameMap = new HashMap<String, String>();
        dexId0ClassNameMap.put("com/tencent/mm/plugin/wallet_core/model/h.class", "com/tencent/mm/plugin/wallet_core/model/h.class");
        oldDexIdClassNameMap.put(0, dexId0ClassNameMap);
        return oldDexIdClassNameMap;
    }
}
