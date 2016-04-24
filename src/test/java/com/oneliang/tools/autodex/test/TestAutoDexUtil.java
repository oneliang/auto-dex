package com.oneliang.tools.autodex.test;

import java.util.Arrays;

import com.oneliang.Constant;
import com.oneliang.tools.autodex.AutoDexUtil;

public class TestAutoDexUtil {

	private static final String allClassesJar="/D:/allClasses.jar";
	private static final String androidManifestFullFilename="/D:/AndroidManifest.xml";
	private static final boolean isMultiDexLoadInAttachBaseContext=true;
	private static final String mainDexOtherClasses="com.sun.tools.javac.Main";
	private static final String resourceDirectorys="/D:/a,/D:/b";
	private static final String outputDirectory="/D:/split";

	public static void main(String[] args) throws Exception{
		AutoDexUtil.autoDex(allClassesJar, androidManifestFullFilename, isMultiDexLoadInAttachBaseContext, Arrays.asList(mainDexOtherClasses.split(Constant.Symbol.COMMA)), Arrays.asList(resourceDirectorys.split(Constant.Symbol.COMMA)), outputDirectory);
	}
	
}
