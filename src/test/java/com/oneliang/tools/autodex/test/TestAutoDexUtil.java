package com.oneliang.tools.autodex.test;

import java.util.Arrays;

import com.oneliang.Constant;
import com.oneliang.tools.autodex.AutoDexUtil;
import com.oneliang.util.file.FileUtil;

public class TestAutoDexUtil {

	private static final String allClassesJar="/D:/allClasses.jar";
	private static final String androidManifestFullFilename="/D:/wechat_base/app/public/AndroidManifest.xml";
	private static final String mainDexOtherClasses="com.tencent.mm.app.MMApplication";
	private static final String outputDirectory="/D:/split";
	private static final boolean debug=true;

	public static void main(String[] args) throws Exception{
		FileUtil.deleteAllFile(outputDirectory);
		AutoDexUtil.autoDex(allClassesJar, androidManifestFullFilename, Arrays.asList(mainDexOtherClasses.split(Constant.Symbol.COMMA)), outputDirectory, debug);
	}
	
}
