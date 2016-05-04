package com.oneliang.tools.autodex.test;

import java.util.Arrays;

import com.oneliang.Constant;
import com.oneliang.tools.autodex.AutoDexUtil;
import com.oneliang.util.file.FileUtil;

public class TestAutoDexUtil {

	private static final String allClassesJar="/D:/allClasses.jar";
	private static final String androidManifestFullFilename="/D:/wechat/app/public/AndroidManifest.xml";
	private static final boolean attachBaseContext=true;
	private static final String mainDexOtherClasses="com.tencent.mm.app.MMApplication";
	private static final String resourceDirectorys="/D:/a,/D:/b";
	private static final String outputDirectory="/D:/split";
	private static final boolean debug=false;

	public static void main(String[] args) throws Exception{
		FileUtil.deleteAllFile(outputDirectory);
		AutoDexUtil.autoDex(allClassesJar, androidManifestFullFilename, attachBaseContext, Arrays.asList(mainDexOtherClasses.split(Constant.Symbol.COMMA)), Arrays.asList(resourceDirectorys.split(Constant.Symbol.COMMA)), outputDirectory, debug);
	}
	
}
