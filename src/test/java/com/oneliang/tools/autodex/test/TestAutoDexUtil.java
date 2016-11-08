package com.oneliang.tools.autodex.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oneliang.Constant;
import com.oneliang.thirdparty.asm.util.AsmUtil;
import com.oneliang.thirdparty.asm.util.ClassDescription;
import com.oneliang.tools.autodex.AutoDexUtil;
import com.oneliang.tools.autodex.AutoDexUtil.Cache;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.logging.AbstractLogger;
import com.oneliang.util.logging.BaseLogger;
import com.oneliang.util.logging.ComplexLogger;
import com.oneliang.util.logging.FileLogger;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public class TestAutoDexUtil {

	private static final List<String> combinedClassList = Arrays.asList("/D:/autodex/aop.jar");
	private static final String androidManifestFullFilename = "/D:/autodex/AndroidManifest.xml";
	// private static final String
	// mainDexOtherClasses=".app.FirstCrashCatcher,.app.MMApplicationWrapper,.ui.NoRomSpaceDexUI,.svg.SVGPreload,.svg.SVGBuildConfig,.plugin.exdevice,.jni.C2JavaExDevice,.svg.graphics.SVGCodeDrawable,com.tenpay.cert.CertUtil,.svg.WeChatSVGRenderC2Java,com.tencent.kingkong.database.SQLiteDoneException,.crash.CrashUploaderService,.app.WorkerProfile,.app.PusherProfile,.app.ToolsProfile,.app.SandBoxProfile,.app.ExDeviceProfile,.app.PatchProfile,.app.TMAssistantProfile,.app.NoSpaceProfile,.plugin.sandbox.SubCoreSandBox,.sdk.platformtools.CrashMonitorForJni,.jni.utils.UtilsJni,.plugin.accountsync.Plugin,.plugin.sandbox.Plugin,.ui.base.preference.PreferenceScreen,.lan_cs.Client,.lan_cs.Server,.svg.SVGResourceRegister,.jni.platformcomm.PlatformCommBridge,.app.MMApplicationLike,.app.Application,com.tencent.tinker.loader.**";
	private static final String outputDirectory = "/D:/autodex/output";
	private static final String mainDexList = "/D:/autodex/main-dex-list.txt";
	private static final boolean debug = false;

	private static List<String> readMainDexClassList(String mainDexList) {
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

	public static void main(String[] args) throws Exception {
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
		// System.setOut(new PrintStream(new
		// FileOutputStream("/D:/mainDex.txt")));
		// FileUtil.deleteAllFile(outputDirectory);
		AutoDexUtil.Option option = new AutoDexUtil.Option(combinedClassList, androidManifestFullFilename, outputDirectory, debug);
		option.minMainDex = false;
		option.mainDexOtherClassList = readMainDexClassList(mainDexList);// Arrays.asList(mainDexOtherClasses.split(Constant.Symbol.COMMA));
		AutoDexUtil.autoDex(option);
	}

	static List<String> array = new ArrayList<String>();

	static {
		array.add("com/tencent/mm/plugin/gcm/modelgcm/GcmBroadcastReceiver.class");
		array.add("com/tencent/mm/app/MMApplicationWrapper.class");
		array.add("com/tencent/tinker/loader/TinkerLoader.class");
		array.add("com/tencent/tinker/loader/SystemClassLoaderAdder$V19.class");
		array.add("com/tencent/mm/plugin/auto/service/MMAutoMessageReplyReceiver.class");
		array.add("com/tencent/mm/plugin/ext/provider/ExtControlProviderEntry.class");
		array.add("com/tencent/mm/app/NoSpaceProfile.class");
		array.add("com/tencent/mm/svg/graphics/SVGCodeDrawable.class");
		array.add("com/tencent/tinker/loader/a/d.class");
		array.add("com/tencent/mm/plugin/base/stub/WXCustomSchemeEntryActivity.class");
		array.add("com/tencent/tinker/loader/a/h.class");
		array.add("com/tencent/tinker/loader/BuildConfig.class");
		array.add("com/tencent/mm/plugin/ext/provider/ExtControlProviderMsg.class");
		array.add("com/tencent/mm/plugin/gwallet/GWalletQueryProvider.class");
		array.add("com/tencent/mm/plugin/backup/bakpcmodel/BakchatPcmgrNorify.class");
		array.add("com/tencent/mm/plugin/ext/provider/ExtControlProviderSearchContact.class");
		array.add("com/tencent/tinker/loader/app/DefaultApplicationLifeCycle.class");
		array.add("com/tencent/mm/plugin/auto/service/MMAutoMessageHeardReceiver.class");
		array.add("com/tencent/mm/app/TMAssistantProfile.class");
		array.add("com/tencent/tinker/loader/a/g.class");
		array.add("com/tencent/kingkong/database/SQLiteDoneException.class");
		array.add("com/tencent/mm/app/ExDeviceProfile.class");
		array.add("com/tencent/mm/app/SandBoxProfile.class");
		array.add("com/tencent/mm/booter/MMReceivers$ConnectionReceiver.class");
		array.add("com/tencent/mm/plugin/backup/bakpcmodel/BakchatPcUsbService.class");
		array.add("com/tencent/mm/crash/CrashUploaderService.class");
		array.add("com/tencent/mm/plugin/accountsync/model/AccountAuthenticatorService.class");
		array.add("com/tencent/mm/app/PusherProfile.class");
		array.add("com/tencent/mm/plugin/exdevice.class");
		array.add("com/tencent/tinker/loader/a/e.class");
		array.add("com/tencent/tinker/loader/SystemClassLoaderAdder.class");
		array.add("com/tencent/mm/svg/SVGBuildConfig.class");
		array.add("com/tencent/mm/lan_cs/Server.class");
		array.add("com/tencent/mm/sdk/platformtools/MultiProcessSharedPreferences.class");
		array.add("com/tencent/tinker/loader/IncrementalClassLoader$1.class");
		array.add("com/tencent/mm/plugin/accountsync/model/ContactsSyncService.class");
		array.add("com/tencent/mm/plugin/ext/provider/ExtControlProviderNearBy.class");
		array.add("com/tencent/mm/booter/ClickFlowReceiver.class");
		array.add("com/tencent/mm/plugin/base/stub/MMPluginProvider.class");
		array.add("com/tencent/tinker/loader/a/a.class");
		array.add("com/tencent/mm/ui/base/preference/PreferenceScreen.class");
		array.add("com/tenpay/cert/CertUtil.class");
		array.add("com/tencent/mm/booter/BluetoothStateReceiver.class");
		array.add("com/tencent/mm/plugin/ext/provider/ExtContentProviderBase.class");
		array.add("com/tencent/mm/ui/NoRomSpaceDexUI.class");
		array.add("com/tencent/tinker/loader/AbstractTinkerLoader.class");
		array.add("com/tencent/mm/plugin/accountsync/Plugin.class");
		array.add("com/tencent/tinker/loader/AndroidNClassLoader.class");
		array.add("com/tencent/mm/svg/SVGPreload.class");
		array.add("com/tencent/tinker/loader/TinkerRuntimeException.class");
		array.add("com/tencent/tinker/loader/SystemClassLoaderAdder$V4.class");
		array.add("com/tencent/mm/app/FirstCrashCatcher.class");
		array.add("com/tencent/mm/booter/MMReceivers$BootReceiver.class");
		array.add("com/tencent/tinker/loader/SystemClassLoaderAdder$V14.class");
		array.add("com/tencent/mm/sdk/platformtools/CrashMonitorForJni.class");
		array.add("com/tencent/mm/plugin/base/stub/WXCommProvider.class");
		array.add("com/tencent/mm/app/PatchProfile.class");
		array.add("com/tencent/tinker/loader/TinkerDexLoader.class");
		array.add("com/tencent/mm/booter/BluetoothReceiver.class");
		array.add("com/tencent/mm/plugin/ext/provider/ExtControlProviderSNS.class");
		array.add("com/tencent/mm/plugin/base/stub/WXEntryActivity.class");
		array.add("com/tencent/mm/plugin/sandbox/Plugin.class");
		array.add("com/tencent/tinker/loader/IncrementalClassLoader.class");
		array.add("com/tencent/mm/app/WorkerProfile.class");
		array.add("com/tencent/mm/plugin/base/stub/WXPayEntryActivity.class");
		array.add("com/tencent/tinker/loader/a/b.class");
		array.add("com/tencent/mm/ui/LauncherUI.class");
		array.add("com/tencent/mm/booter/MountReceiver.class");
		array.add("com/tencent/mm/app/ToolsProfile.class");
		array.add("com/tencent/tinker/loader/app/ApplicationLike.class");
		array.add("com/tencent/tinker/loader/SystemClassLoaderAdder$V23.class");
		array.add("com/tencent/mm/app/Application.class");
		array.add("com/tencent/mm/svg/SVGResourceRegister.class");
		array.add("com/tencent/mm/booter/InstallReceiver.class");
		array.add("com/tencent/mm/jni/utils/UtilsJni.class");
		array.add("com/tencent/mm/pluginsdk/model/downloader/FileDownloadReceiver.class");
		array.add("com/tencent/mm/jni/C2JavaExDevice.class");
		array.add("com/tencent/mm/plugin/sandbox/SubCoreSandBox.class");
		array.add("com/tencent/mm/plugin/base/stub/WXEntryActivity$EntryReceiver.class");
		array.add("com/tencent/mm/plugin/wear/model/service/WearDataLayerService.class");
		array.add("com/tencent/mm/jni/platformcomm/PlatformCommBridge.class");
		array.add("com/tencent/tinker/loader/TinkerSoLoader.class");
		array.add("com/tencent/tinker/loader/a/c.class");
		array.add("com/tencent/tinker/loader/app/TinkerApplication.class");
		array.add("com/tencent/mm/booter/MMReceivers$ExdeviceProcessReceiver.class");
		array.add("com/tencent/mm/svg/WeChatSVGRenderC2Java.class");
		array.add("com/tencent/mm/lan_cs/Client.class");
		array.add("com/tencent/mm/plugin/ext/provider/ExtControlProviderAccountSync.class");
		array.add("com/tencent/tinker/loader/app/ApplicationLifeCycle.class");
		array.add("com/tencent/tinker/loader/IncrementalClassLoader$DelegateClassLoader.class");
		array.add("com/tencent/tinker/loader/a/f.class");
		array.add("com/tencent/mm/app/MMApplicationLifeCycle.class");
	}

	public static void main1(String[] args) throws Exception {
		for (String classFile : array) {
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
			rootClassNameSet.add(classFile);
			AsmUtil.findAllDependClassNameMap(rootClassNameSet, classDescriptionMap, referencedClassDescriptionListMap, allClassNameMap, true);
		}
	}
}
