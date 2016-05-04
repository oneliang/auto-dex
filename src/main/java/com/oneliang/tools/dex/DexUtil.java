package com.oneliang.tools.dex;

import java.util.ArrayList;
import java.util.List;

import com.android.dx.merge.DexMerger;

public class DexUtil {
	/**
	 * execute android dx
	 * @param outputDexFullFilename
	 * @param classesDirectoryListAndLibraryList
	 * @param isDebug
	 */
	public static void androidDx(String outputDexFullFilename,List<String> classesDirectoryListAndLibraryList,boolean isDebug){
		List<String> parameterList=new ArrayList<String>();
		parameterList.add("--dex");
		if(isDebug){
			parameterList.add("--debug");
		}
		parameterList.add("--force-jumbo");
		parameterList.add("--output="+outputDexFullFilename);
		for(String classesDirectoryListAndLibrary:classesDirectoryListAndLibraryList){
			parameterList.add(classesDirectoryListAndLibrary);
		}
		com.android.dx.command.Main.main(parameterList.toArray(new String[]{}));
	}

	/**
	 * merge dex
	 * @param outputDexFullFilename
	 * @param toMergeDexFullFilenameList
	 */
	public static void androidMergeDex(String outputDexFullFilename,List<String> toMergeDexFullFilenameList){
		List<String> parameterList=new ArrayList<String>();
		parameterList.add(outputDexFullFilename);
		if(toMergeDexFullFilenameList!=null){
			if(!toMergeDexFullFilenameList.isEmpty()){
				parameterList.add(toMergeDexFullFilenameList.get(0));
			}
			parameterList.addAll(toMergeDexFullFilenameList);
		}
		try {
			DexMerger.main(parameterList.toArray(new String[]{}));
		} catch (Exception e) {
			throw new DexUtilException(e);
		}
	}

	private static class DexUtilException extends RuntimeException{
		private static final long serialVersionUID = -8907942490256942919L;

		public DexUtilException(String message) {
			super(message);
		}

		public DexUtilException(Throwable cause) {
			super(cause);
		}

		public DexUtilException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
