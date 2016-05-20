package com.oneliang.tools.autodex.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oneliang.Constant;
import com.oneliang.thirdparty.asm.util.AsmUtil;
import com.oneliang.tools.autodex.AutoDexUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.logging.AbstractLogger;
import com.oneliang.util.logging.BaseLogger;
import com.oneliang.util.logging.ComplexLogger;
import com.oneliang.util.logging.FileLogger;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public class TestAutoDexUtil {

	private static final String allClassesJar="/D:/allClasses.jar";
	private static final String androidManifestFullFilename="/D:/wechat_base/app/public/AndroidManifest.xml";
	private static final String mainDexOtherClasses=".ui.LauncherUI";
	private static final String outputDirectory="/D:/split";
	private static final boolean debug=true;
	private static final boolean attachBaseContext=debug;
	private static final boolean autoByPackage=false;

	public static void main(String[] args) throws Exception{
		List<AbstractLogger> loggerList=new ArrayList<AbstractLogger>();
		loggerList.add(new BaseLogger(Logger.Level.INFO));
		loggerList.add(new FileLogger(Logger.Level.VERBOSE,new File("/D:/a.txt")));
		Logger logger=new ComplexLogger(Logger.Level.VERBOSE, loggerList);
		LoggerManager.registerLogger(AutoDexUtil.class, logger);
		LoggerManager.registerLogger(AsmUtil.class, logger);
//		System.setOut(new PrintStream(new FileOutputStream("/D:/mainDex.txt")));
		FileUtil.deleteAllFile(outputDirectory);
		AutoDexUtil.autoDex(allClassesJar, androidManifestFullFilename, attachBaseContext, Arrays.asList(mainDexOtherClasses.split(Constant.Symbol.COMMA)), outputDirectory, debug, autoByPackage);
	}
	
}
