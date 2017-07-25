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

import com.oneliang.Constant;
import com.oneliang.thirdparty.asm.util.AsmUtil;
import com.oneliang.thirdparty.asm.util.ClassDescription;
import com.oneliang.tools.autodex.AutoDexUtil;
import com.oneliang.tools.autodex.AutoDexUtil.Cache;
import com.oneliang.tools.trace.TraceClass;
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
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(mainDexList), Constant.Encoding.UTF8));
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
        FileUtil.writeFile(outputDirectory + "/0_2660_retrace.txt", stringBuilder.toString().getBytes(Constant.Encoding.UTF8));
    }

    public static void main3(String[] args) throws Exception {
        // String
        // classFullFilename="E:/Dandelion/java/githubWorkspace/auto-dex/bin/com/oneliang/tools/autodex/test/InnerClass$InnerInterface.class";
        // AsmUtil.traceClass(classFullFilename, new PrintWriter(System.out));
        // AsmUtil.findClassDescription(classFullFilename);
        List<AbstractLogger> loggerList = new ArrayList<AbstractLogger>();
        // loggerList.add(new BaseLogger(Logger.Level.INFO));
        loggerList.add(new FileLogger(Logger.Level.VERBOSE, new File("/D:/autodex/log.txt")));
        Logger logger = new ComplexLogger(Logger.Level.VERBOSE, loggerList);
        LoggerManager.registerLogger(AutoDexUtil.class, logger);
        LoggerManager.registerLogger(AsmUtil.class, logger);
        System.setOut(new PrintStream(new FileOutputStream("/D:/autodex/log.txt")));
        FileUtil.deleteAllFile(outputDirectory);
        AutoDexUtil.Option option = new AutoDexUtil.Option(combinedClassList, androidManifestFullFilename, outputDirectory, debug);
        option.minMainDex = false;
        // option.methodLimit = 0xE000;
        option.mainDexOtherClassList = readMainDexClassList(mainDexList);// Arrays.asList(mainDexOtherClasses.split(Constant.Symbol.COMMA));
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
            // Constant.Symbol.DOT + Constant.File.CLASS;
            // System.out.println("call:" +
            // classProcessor.classNameMap.get(callClassName));
            // }
            // }
            // System.out.println("--------------------");
            Iterator<Entry<String, String>> dexId0ClassNameIterator = result.dexIdClassNameMap.get(0).entrySet().iterator();
            final String classPrefix = "com/tencent/mm/plugin/appbrand";
            while (dexId0ClassNameIterator.hasNext()) {
                Entry<String, String> classNameEntry = dexId0ClassNameIterator.next();
                String className = classNameEntry.getKey();
                List<ClassDescription> referencedClassDescriptionList = result.referencedClassDescriptionListMap.get(className);
                if (referencedClassDescriptionList != null) {
                    String tracedClassName = classProcessor.classNameMap.get(className);
                    if (tracedClassName != null && !tracedClassName.startsWith(classPrefix)) {
                        System.out.println("c:" + classProcessor.classNameMap.get(className));
                        ClassDescription classDescription = result.classDescriptionMap.get(className);
                        for (String dependClassName : classDescription.dependClassNameList) {
                            dependClassName = dependClassName + Constant.Symbol.DOT + Constant.File.CLASS;
                            if (dependClassName.startsWith(classPrefix)) {
                                System.out.println("\tdepend:" + classProcessor.classNameMap.get(dependClassName));
                            }
                        }
                        for (ClassDescription referencedClassDescription : referencedClassDescriptionList) {
                            String callClassName = referencedClassDescription.className + Constant.Symbol.DOT + Constant.File.CLASS;
                            // if (!callClassName.startsWith(classPrefix)) {
                            // System.out.println("\treal called:" +
                            // classProcessor.classNameMap.get(callClassName));
                            // }
                        }
                    }
                    // System.out.println("c:" +
                    // classProcessor.classNameMap.get(className));
                    for (ClassDescription classDescription : referencedClassDescriptionList) {
                        String callClassName = classDescription.className + Constant.Symbol.DOT + Constant.File.CLASS;
                        // System.out.println("\tread call:" +
                        // classProcessor.classNameMap.get(callClassName));
                    }
                }
                // ClassDescription classDescription =
                // result.classDescriptionMap.get(className);
                // System.out.println("c:" +
                // classProcessor.classNameMap.get(className));
                // for (String dependClassName :
                // classDescription.dependClassNameList) {
                // dependClassName = dependClassName + Constant.Symbol.DOT +
                // Constant.File.CLASS;
                // System.out.println("\tdepend:" +
                // classProcessor.classNameMap.get(dependClassName));
                // }
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
        FileUtil.writeFile(outputDirectory + "/0_retrace.txt", stringBuilder.toString().getBytes(Constant.Encoding.UTF8));
    }

    public static class ClassProcessor implements Processor {
        public Map<String, String> classNameMap = new HashMap<String, String>();

        public void processMethodMapping(String className, int firstLineNumber, int lastLineNumber, String methodReturnType, String methodName, String methodArguments, String newMethodName) {
        }

        public void processFieldMapping(String className, String fieldType, String fieldName, String newFieldName) {
        }

        public void processClassMapping(String className, String newClassName) {
            classNameMap.put(newClassName.replace(Constant.Symbol.DOT, Constant.Symbol.SLASH_LEFT) + Constant.Symbol.DOT + Constant.File.CLASS, className.replace(Constant.Symbol.DOT, Constant.Symbol.SLASH_LEFT) + Constant.Symbol.DOT + Constant.File.CLASS);
        }
    };

    public static class MappingClassProcessor implements Processor {
        public Map<String, String> classNameMap = new HashMap<String, String>();

        public void processMethodMapping(String className, int firstLineNumber, int lastLineNumber, String methodReturnType, String methodName, String methodArguments, String newMethodName) {
        }

        public void processFieldMapping(String className, String fieldType, String fieldName, String newFieldName) {
        }

        public void processClassMapping(String className, String newClassName) {
            classNameMap.put(className.replace(Constant.Symbol.DOT, Constant.Symbol.SLASH_LEFT) + Constant.Symbol.DOT + Constant.File.CLASS, newClassName.replace(Constant.Symbol.DOT, Constant.Symbol.SLASH_LEFT) + Constant.Symbol.DOT + Constant.File.CLASS);
        }
    };

    static List<String> array = new ArrayList<String>();

    static {
        array.add("com/tencent/recovery/model/RecoveryHandleResult.class");
        array.add("com/tencent/recovery/wx/service/WXRecoveryUploadService.class");
        array.add("com/tencent/recovery/option/OptionFactory.class");
        array.add("com/tencent/recovery/report/RecoveryReporter.class");
        array.add("com/tencent/recovery/service/RecoveryUploadService.class");
        array.add("com/tencent/recovery/service/RecoveryHandleService$InnerService.class");
        array.add("com/tencent/tinker/loader/BuildConfig.class");
        array.add("com/tencent/mm/plugin/ext/provider/ExtControlProviderMsg.class");
        array.add("com/tencent/recovery/ConstantsRecovery$ProcessStartFlag.class");
        array.add("com/tencent/recovery/wx/util/MyDES.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareConstants.class");
        array.add("com/tencent/recovery/wx/util/NetUtil.class");
        array.add("com/tencent/tinker/loader/TinkerParallelDexOptimizer$StreamConsumer$1.class");
        array.add("com/tencent/mm/app/SandBoxProfile.class");
        array.add("com/tencent/tinker/loader/shareutil/SharePatchInfo.class");
        array.add("com/tencent/mm/plugin/accountsync/model/AccountAuthenticatorService.class");
        array.add("com/tencent/wcdb/database/SQLiteConnection.class");
        array.add("com/tencent/gmtrace/GMTrace.class");
        array.add("com/tencent/wcdb/repair/RepairKit.class");
        array.add("com/tencent/recovery/util/Util.class");
        array.add("com/tencent/recovery/ConstantsRecovery$IntentAction.class");
        array.add("com/tencent/mm/ui/base/preference/PreferenceScreen.class");
        array.add("com/tencent/recovery/RecoveryLogic.class");
        array.add("com/tencent/mm/app/MMApplicationLike.class");
        array.add("com/tenpay/cert/CertUtil.class");
        array.add("com/tencent/mm/booter/BluetoothStateReceiver.class");
        array.add("com/tencent/tinker/loader/TinkerParallelDexOptimizer.class");
        array.add("com/tencent/recovery/handler/RecoveryMessageHandler.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareOatUtil$1.class");
        array.add("com/tencent/recovery/log/RecoveryFileLog.class");
        array.add("com/tencent/recovery/ConstantsRecovery$IntentKeys.class");
        array.add("com/tencent/mm/sdk/platformtools/CrashMonitorForJni.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareOatUtil.class");
        array.add("com/tencent/mm/plugin/sandbox/Plugin.class");
        array.add("com/tencent/recovery/wx/WXConstantsRecovery$HandleReportKeys.class");
        array.add("com/tencent/mm/app/WorkerProfile.class");
        array.add("com/tencent/mm/plugin/base/stub/WXPayEntryActivity.class");
        array.add("com/tencent/wcdb/support/Log.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareElfFile$ProgramHeader.class");
        array.add("com/tencent/recovery/wx/util/WXUtil.class");
        array.add("com/tencent/tinker/loader/app/ApplicationLike.class");
        array.add("com/tencent/tinker/loader/TinkerTestAndroidNClassLoader.class");
        array.add("com/tencent/recovery/wx/RecoveryTinkerManager.class");
        array.add("com/tencent/tinker/loader/SystemClassLoaderAdder$V23.class");
        array.add("com/tencent/wcdb/CursorWindow.class");
        array.add("com/tencent/mm/pluginsdk/model/downloader/FileDownloadReceiver.class");
        array.add("com/tencent/mm/plugin/sandbox/SubCoreSandBox.class");
        array.add("com/tencent/mm/plugin/risk_scanner/RiskScannerReqBufferProvider.class");
        array.add("com/tencent/recovery/model/RecoveryHandleItem$1.class");
        array.add("com/tencent/recovery/crash/RecoveryExceptionHandler.class");
        array.add("com/tencent/recovery/log/RecoveryLog.class");
        array.add("com/tencent/mm/app/MMApplicationWrapper.class");
        array.add("com/tencent/wcdb/database/ChunkedCursorWindow.class");
        array.add("com/tencent/mm/plugin/base/stub/WXCustomSchemeEntryActivity.class");
        array.add("com/tencent/mm/boot/svg/SVGPreload.class");
        array.add("com/tencent/recovery/wx/util/FileUtil.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareElfFile$ElfHeader.class");
        array.add("com/tencent/recovery/wx/R$layout.class");
        array.add("com/tencent/mm/plugin/auto/service/MMAutoMessageHeardReceiver.class");
        array.add("com/tencent/recovery/option/CommonOptions$1.class");
        array.add("com/tencent/recovery/log/RecoveryCacheLog$LogItem.class");
        array.add("com/tencent/gmtrace/GMTraceBitSet.class");
        array.add("com/tencent/recovery/storage/MMappedFileStorage.class");
        array.add("com/tencent/recovery/ConstantsRecovery$ProcessStage.class");
        array.add("com/tencent/recovery/RecoveryContext.class");
        array.add("com/tencent/mm/lan_cs/Server.class");
        array.add("com/tencent/mm/sdk/platformtools/MultiProcessSharedPreferences.class");
        array.add("com/tencent/recovery/ConstantsRecovery$ReportType.class");
        array.add("com/tencent/tinker/loader/TinkerResourcesKey$V19.class");
        array.add("com/tencent/recovery/model/RecoveryStatusItem.class");
        array.add("com/tencent/recovery/R$layout.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareBsDiffPatchInfo.class");
        array.add("com/tencent/tinker/loader/TinkerResourcesKey$V24.class");
        array.add("com/tencent/mm/ui/NoRomSpaceDexUI.class");
        array.add("com/tencent/mm/plugin/accountsync/Plugin.class");
        array.add("com/tencent/tinker/loader/AndroidNClassLoader.class");
        array.add("com/tencent/tinker/loader/TinkerParallelDexOptimizer$OptimizeWorker.class");
        array.add("com/tencent/tinker/loader/SystemClassLoaderAdder$V4.class");
        array.add("com/tencent/tinker/loader/shareutil/SharePatchFileUtil.class");
        array.add("com/tencent/tinker/loader/TinkerParallelDexOptimizer$1.class");
        array.add("com/tencent/gmtrace/GMTrace$1.class");
        array.add("com/tencent/tinker/loader/TinkerResourcesKey$V17.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareOatUtil$InstructionSet.class");
        array.add("com/tencent/recovery/Recovery$1.class");
        array.add("com/tencent/mm/svg/SVGResourceRegister.class");
        array.add("com/tencent/recovery/wx/BuildConfig.class");
        array.add("com/tencent/recovery/wx/util/MyByteArray.class");
        array.add("com/tencent/mm/svg/WeChatSVGRenderC2Java.class");
        array.add("com/tencent/mm/plugin/ext/provider/ExtControlProviderAccountSync.class");
        array.add("com/tencent/tinker/loader/app/ApplicationLifeCycle.class");
        array.add("com/tencent/tinker/loader/TinkerResourceLoader.class");
        array.add("com/tencent/recovery/log/RecoveryCacheLog.class");
        array.add("com/tencent/tinker/loader/TinkerLoader.class");
        array.add("com/tencent/wcdb/database/SQLiteDebug.class");
        array.add("com/tencent/tinker/loader/SystemClassLoaderAdder$V19.class");
        array.add("com/tencent/tinker/loader/app/DefaultApplicationLike.class");
        array.add("com/tencent/mm/plugin/auto/service/MMAutoMessageReplyReceiver.class");
        array.add("com/tencent/mm/app/NoSpaceProfile.class");
        array.add("com/tencent/mm/svg/graphics/SVGCodeDrawable.class");
        array.add("com/tencent/recovery/DefaultOptionsCreator.class");
        array.add("com/tencent/recovery/Recovery.class");
        array.add("com/tencent/recovery/wx/service/WXRecoveryHandleService.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareResPatchInfo.class");
        array.add("com/tencent/mm/plugin/gwallet/GWalletQueryProvider.class");
        array.add("com/tencent/mm/plugin/ext/provider/ExtControlProviderSearchContact.class");
        array.add("com/tencent/mm/booter/MMReceivers$ConnectionReceiver.class");
        array.add("com/tencent/recovery/util/AttributeUtil.class");
        array.add("com/tencent/mm/crash/CrashUploaderService.class");
        array.add("com/tencent/tinker/loader/SystemClassLoaderAdder.class");
        array.add("com/tencent/recovery/ConstantsRecovery.class");
        array.add("com/tencent/recovery/crash/JNICrashHandler.class");
        array.add("com/tencent/gmtrace/GMTrace$GMTraceWorker.class");
        array.add("com/tencent/recovery/ConstantsRecovery$ProcessStatus.class");
        array.add("com/tencent/recovery/ConstantsRecovery$DefaultProcessOptions.class");
        array.add("com/tencent/wcdb/database/SQLiteDirectQuery.class");
        array.add("com/tencent/recovery/wx/util/PByteArray.class");
        array.add("com/tencent/mm/plugin/ext/provider/ExtControlProviderNearBy.class");
        array.add("com/tencent/tinker/loader/R.class");
        array.add("com/tencent/recovery/config/ExpressItem.class");
        array.add("com/tencent/tinker/loader/TinkerTestDexLoad.class");
        array.add("com/tencent/recovery/R$string.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareTinkerInternals.class");
        array.add("com/tencent/mm/plugin/ext/provider/ExtContentProviderBase.class");
        array.add("com/tencent/recovery/model/RecoveryStatusItem$1.class");
        array.add("com/tencent/recovery/wx/util/EncryptUtil.class");
        array.add("com/tencent/recovery/BuildConfig.class");
        array.add("com/tencent/recovery/log/RecoveryCacheLog$1.class");
        array.add("com/tencent/mm/app/FirstCrashCatcher.class");
        array.add("com/tencent/mm/plugin/base/stub/WXCommProvider.class");
        array.add("com/tencent/recovery/model/RecoveryPersistentItem.class");
        array.add("com/tencent/tinker/loader/TinkerDexLoader.class");
        array.add("com/tencent/recovery/option/ProcessOptions.class");
        array.add("com/tencent/mm/plugin/ext/provider/ExtControlProviderSNS.class");
        array.add("com/tencent/mm/plugin/base/stub/WXEntryActivity.class");
        array.add("com/tencent/recovery/ConstantsRecovery$DefaultCommonOptions.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareIntentUtil.class");
        array.add("com/tencent/recovery/option/CommonOptions.class");
        array.add("com/tencent/mm/ui/LauncherUI.class");
        array.add("com/tencent/mm/booter/MountReceiver.class");
        array.add("com/tencent/wcdb/repair/RecoverKit.class");
        array.add("com/tencent/recovery/option/ProcessOptions$Builder.class");
        array.add("com/tencent/recovery/wx/R$string.class");
        array.add("com/tencent/recovery/model/RecoveryData$1.class");
        array.add("com/tencent/wcdb/FileUtils.class");
        array.add("com/tencent/mm/jni/utils/UtilsJni.class");
        array.add("com/tencent/recovery/service/RecoveryHandleService.class");
        array.add("com/tencent/mm/plugin/base/stub/WXEntryActivity$EntryReceiver.class");
        array.add("com/tencent/recovery/service/RecoveryReportService.class");
        array.add("com/tencent/tinker/loader/TinkerSoLoader.class");
        array.add("com/tencent/tinker/loader/app/TinkerApplication.class");
        array.add("com/tencent/gmtrace/GMTraceHandler.class");
        array.add("com/tencent/recovery/log/RecoveryLog$RecoveryLogImpl.class");
        array.add("com/tencent/gmtrace/Constants.class");
        array.add("com/tencent/tinker/loader/TinkerDexLoader$1.class");
        array.add("com/tencent/mm/plugin/gcm/modelgcm/GcmBroadcastReceiver.class");
        array.add("com/tencent/mm/plugin/ext/provider/ExtControlProviderEntry.class");
        array.add("com/tencent/recovery/model/RecoveryData.class");
        array.add("com/tencent/mm/plugin/photoedit/cache/ArtistCacheManager.class");
        array.add("com/tencent/recovery/crash/DefaultExceptionHandler.class");
        array.add("com/tencent/recovery/ConstantsRecovery$SpKeys.class");
        array.add("com/tencent/tinker/loader/TinkerResourcePatcher.class");
        array.add("com/tencent/recovery/option/IOptionsCreator.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareElfFile.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareElfFile$1.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareReflectUtil.class");
        array.add("com/tencent/wcdb/database/SQLiteGlobal.class");
        array.add("com/tencent/mm/app/PusherProfile.class");
        array.add("com/tencent/recovery/R.class");
        array.add("com/tencent/recovery/config/Express.class");
        array.add("com/tencent/tinker/loader/TinkerParallelDexOptimizer$StreamConsumer.class");
        array.add("com/tencent/mm/plugin/backup/bakoldlogic/bakoldmodel/BakOldUSBReceiver.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareFileLockHelper.class");
        array.add("com/tencent/mm/plugin/accountsync/model/ContactsSyncService.class");
        array.add("com/tencent/mm/booter/ClickFlowReceiver.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareResPatchInfo$LargeModeInfo.class");
        array.add("com/tencent/mm/plugin/base/stub/MMPluginProvider.class");
        array.add("com/tencent/recovery/model/RecoveryHandleItem.class");
        array.add("com/tencent/tinker/loader/TinkerUncaughtHandler.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareDexDiffPatchInfo.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareElfFile$SectionHeader.class");
        array.add("com/tencent/wcdb/repair/DBDumpUtil.class");
        array.add("com/tencent/tinker/loader/AbstractTinkerLoader.class");
        array.add("com/tencent/tinker/loader/TinkerRuntimeException.class");
        array.add("com/tencent/mm/booter/MMReceivers$BootReceiver.class");
        array.add("com/tencent/tinker/loader/SystemClassLoaderAdder$V14.class");
        array.add("com/tencent/mm/app/PatchProfile.class");
        array.add("com/tencent/mm/booter/BluetoothReceiver.class");
        array.add("com/tencent/recovery/wx/WXConstantsRecovery.class");
        array.add("com/tencent/tinker/loader/shareutil/ShareSecurityCheck.class");
        array.add("com/tencent/recovery/wx/WXConstantsRecovery$IntentAction.class");
        array.add("com/tencent/mm/plugin/base/stub/WXShortcutEntryActivity.class");
        array.add("com/tencent/mm/boot/svg/SVGBuildConfig.class");
        array.add("com/tencent/recovery/ConstantsRecovery$DefaultExpress.class");
        array.add("com/tencent/recovery/ConstantsRecovery$Message.class");
        array.add("com/tencent/mm/app/Application.class");
        array.add("com/tencent/recovery/option/CommonOptions$Builder.class");
        array.add("com/tencent/tinker/loader/TinkerResourcesKey.class");
        array.add("com/tencent/mm/booter/InstallReceiver.class");
        array.add("com/tencent/mm/jni/C2JavaExDevice.class");
        array.add("com/tencent/mm/plugin/wear/model/service/WearDataLayerService.class");
        array.add("com/tencent/tinker/loader/TinkerResourcesKey$V7.class");
        array.add("com/tencent/tinker/loader/TinkerParallelDexOptimizer$ResultCallback.class");
        array.add("com/tencent/mm/booter/MMReceivers$ExdeviceProcessReceiver.class");
        array.add("com/tencent/mm/lan_cs/Client.class");
        array.add("com/tencent/recovery/wx/R.class");
        array.add("com/tencent/wcdb/repair/BackupKit.class");
    }

    public static void main(String[] args) throws Exception {
        String outputTraceFile="/D:/canFix.csv";
        TraceClass.traceCanFixDependencyClass(combinedClassList, readMainDexClassList(mainDexList), androidManifestFullFilename, mappingFile, 5, outputTraceFile);
        System.exit(0);
        
        
        MappingClassProcessor mappingClassProcessor = new MappingClassProcessor();
        Retrace.readMapping(mappingFile, mappingClassProcessor);
        for (String classFile : array) {
            String newClassFile = mappingClassProcessor.classNameMap.get(classFile);
            if (newClassFile == null) {
                System.out.println(classFile);
                continue;
            }
            List<AbstractLogger> loggerList = new ArrayList<AbstractLogger>();
            System.setOut(new PrintStream(new File("/D:/autodex/classes/" + new File(newClassFile).getName() + ".txt")));
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
            // option.mainDexOtherClassList=Arrays.asList(mainDexOtherClasses.split(Constant.Symbol.COMMA));
            String cacheFullFilename = outputDirectory + Constant.Symbol.SLASH_LEFT + "cache.txt";
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
            rootClassNameSet.add(newClassFile);
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
