package com.oneliang.tools.autodex.test;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import com.oneliang.Constant;
import com.oneliang.thirdparty.asm.util.AsmUtil;
import com.oneliang.tools.autodex.AutoDexUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.logging.BaseLogger;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public class TestAutoDexUtil {

	private static final String allClassesJar="/D:/allClasses.jar";
	private static final String androidManifestFullFilename="/D:/wechat_base/app/public/AndroidManifest.xml";
	private static final String mainDexOtherClasses=".ui.NoRomSpaceDexUI,.svg.SVGPreload,.svg.SVGBuildConfig,.plugin.exdevice.jni.C2JavaExDevice,.svg.graphics.SVGCodeDrawable,com.tenpay.cert.CertUtil,.svg.WeChatSVGRenderC2Java,com.tencent.kingkong.database.SQLiteDoneException,.crash.CrashUploaderService,.app.MMApplicationWrapper,.app.WorkerProfile,.app.PusherProfile,.app.ToolsProfile,.app.SandBoxProfile,.app.ExDeviceProfile,.plugin.sandbox.SubCoreSandBox,.sdk.platformtools.CrashMonitorForJni,.jni.utils.UtilsJni,.plugin.accountsync.Plugin,.plugin.sandbox.Plugin,.ui.base.preference.PreferenceScreen,.lan_cs.Client,.lan_cs.Server,.svg.SVGResourceRegister,.jni.platformcomm.PlatformCommBridge";
	private static final String outputDirectory="/D:/split";
	private static final boolean debug=false;

	public static void main(String[] args) throws Exception{
		LoggerManager.registerLogger(AutoDexUtil.class, new BaseLogger(Logger.Level.INFO));
		LoggerManager.registerLogger(AsmUtil.class, new BaseLogger(Logger.Level.VERBOSE));
		System.setOut(new PrintStream(new FileOutputStream("/D:/mainDex.txt")));
		FileUtil.deleteAllFile(outputDirectory);
		AutoDexUtil.autoDex(allClassesJar, androidManifestFullFilename, Arrays.asList(mainDexOtherClasses.split(Constant.Symbol.COMMA)), outputDirectory, debug);
	}
	
}
