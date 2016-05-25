package com.oneliang.tools.autodex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.oneliang.Constant;
import com.oneliang.thirdparty.asm.util.AsmUtil;
import com.oneliang.thirdparty.asm.util.AsmUtil.FieldProcessor;
import com.oneliang.thirdparty.asm.util.ClassDescription;
import com.oneliang.tools.dex.DexUtil;
import com.oneliang.tools.linearalloc.AllocClassVisitor.MethodReference;
import com.oneliang.tools.linearalloc.LinearAllocUtil;
import com.oneliang.tools.linearalloc.LinearAllocUtil.AllocStat;
import com.oneliang.util.common.Generator;
import com.oneliang.util.common.JavaXmlUtil;
import com.oneliang.util.common.ObjectUtil;
import com.oneliang.util.common.StringUtil;
import com.oneliang.util.file.FileUtil;
import com.oneliang.util.logging.Logger;
import com.oneliang.util.logging.LoggerManager;

public final class AutoDexUtil {

	private static final Logger logger = LoggerManager.getLogger(AutoDexUtil.class);

	private static final String CLASSES="classes";
	private static final String DEX="dex";
	private static final String AUTO_DEX_DEX_CLASSES_PREFIX="dexClasses";

	/**
	 * find main class list from android manifest
	 * @return List<String>
	 */
	public static List<String> findMainDexClassListFromAndroidManifest(String androidManifestFullFilename,boolean attachBaseContextMultiDex){
		List<String> mainDexClassList=new ArrayList<String>();
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		try{
			Document document = JavaXmlUtil.parse(new FileInputStream(androidManifestFullFilename));
			String applicationName=findApplication(xPath, document);
			if(StringUtil.isNotBlank(applicationName)){
				mainDexClassList.add(applicationName);
			}
			if(!attachBaseContextMultiDex){
				mainDexClassList.addAll(findActivity(xPath, document));
				mainDexClassList.addAll(findProvider(xPath, document));
				mainDexClassList.addAll(findReceiver(xPath, document));
				mainDexClassList.addAll(findService(xPath, document));
			}
		}catch(Exception e){
			throw new AutoDexUtilException(e);
		}
		return mainDexClassList;
	}

	/**
	 * find application
	 * @param xPath
	 * @param document
	 * @return String
	 * @throws Exception
	 */
	private static String findApplication(XPath xPath,Document document) throws Exception{
		String applicationName=null;
		//application
		NodeList nodeList = (NodeList) xPath.evaluate("/manifest/application[@*]", document, XPathConstants.NODESET);
		if(nodeList!=null){
			for(int i=0;i<nodeList.getLength();i++){
				Node node=nodeList.item(i);
				Node nameNode=node.getAttributes().getNamedItem("android:name");
				if(nameNode!=null){
					applicationName=nameNode.getTextContent();
					logger.verbose(applicationName);
				}
			}
		}
		return applicationName;
	}

	/**
	 * find activity
	 * @param xPath
	 * @param document
	 * @return List<String>
	 * @throws Exception
	 */
	private static List<String> findActivity(XPath xPath,Document document) throws Exception{
		List<String> mainActivityList=new ArrayList<String>();
		NodeList nodeList = (NodeList) xPath.evaluate("/manifest/application/activity", document, XPathConstants.NODESET);
		if(nodeList!=null){
			for(int i=0;i<nodeList.getLength();i++){
				Node node=nodeList.item(i);
				String activityName=node.getAttributes().getNamedItem("android:name").getTextContent();
				Node activityExportedNode=node.getAttributes().getNamedItem("android:exported");
				if(activityExportedNode!=null){
					boolean exported=Boolean.parseBoolean(activityExportedNode.getTextContent());
					if(exported){
						logger.verbose(activityName);
						mainActivityList.add(activityName);
					}
				}else{
					Element element=(Element)node;
					NodeList actionNodeList=element.getElementsByTagName("action");
					if(actionNodeList.getLength()>0){
//						logger.verbose(activityName);
//						mainActivityList.add(activityName);
					}
					for(int j=0;j<actionNodeList.getLength();j++){
						Node activityActionNode=actionNodeList.item(j).getAttributes().getNamedItem("android:name");
						if(activityActionNode!=null){
							String activityActionName=activityActionNode.getTextContent();
							if(activityActionName.equals("android.intent.action.MAIN")){
								logger.verbose(activityName);
								mainActivityList.add(activityName);
							}
						}
					}
				}
			}
		}
		return mainActivityList;
	}

	/**
	 * find provider
	 * @param xPath
	 * @param document
	 * @return List<String>
	 * @throws Exception
	 */
	private static List<String> findProvider(XPath xPath,Document document) throws Exception{
		List<String> providerList=new ArrayList<String>();
		NodeList nodeList = (NodeList) xPath.evaluate("/manifest/application/provider", document, XPathConstants.NODESET);
		if(nodeList!=null){
			for(int i=0;i<nodeList.getLength();i++){
				Node node=nodeList.item(i);
				Node nameNode=node.getAttributes().getNamedItem("android:name");
				if(nameNode!=null){
//					Node providerExportedNode=node.getAttributes().getNamedItem("android:exported");
//					if(providerExportedNode!=null){
//						boolean exported=Boolean.parseBoolean(providerExportedNode.getTextContent());
//						if(exported){
							String providerName=nameNode.getTextContent();
							logger.verbose(providerName);
							providerList.add(providerName);
//						}
//					}
				}
			}
		}
		return providerList;
	}

	/**
	 * find receiver
	 * @param xPath
	 * @param document
	 * @return List<String>
	 * @throws Exception
	 */
	private static List<String> findReceiver(XPath xPath,Document document) throws Exception{
		List<String> receiverList=new ArrayList<String>();
		NodeList nodeList = (NodeList) xPath.evaluate("/manifest/application/receiver", document, XPathConstants.NODESET);
		if(nodeList!=null){
			for(int i=0;i<nodeList.getLength();i++){
				Node node=nodeList.item(i);
				Node nameNode=node.getAttributes().getNamedItem("android:name");
				if(nameNode!=null){
					Node receiverExportedNode=node.getAttributes().getNamedItem("android:exported");
					String receiverName=nameNode.getTextContent();
					boolean needToCheckAgain=false;
					if(receiverExportedNode!=null){
						boolean exported=Boolean.parseBoolean(receiverExportedNode.getTextContent());
						if(exported){
							logger.verbose(receiverName);
							receiverList.add(receiverName);
						}else{
							needToCheckAgain=true;
						}
					}else{
						needToCheckAgain=true;
					}
					if(needToCheckAgain){
						Element element=(Element)node;
						NodeList actionNodeList=element.getElementsByTagName("action");
						if(actionNodeList.getLength()>0){
							logger.verbose(receiverName);
							receiverList.add(receiverName);
						}
					}
				}
			}
		}
		return receiverList;
	}

	/**
	 * find service
	 * @param xPath
	 * @param document
	 * @return List<String>
	 * @throws Exception
	 */
	private static List<String> findService(XPath xPath,Document document) throws Exception{
		List<String> serviceList=new ArrayList<String>();
		NodeList nodeList = (NodeList) xPath.evaluate("/manifest/application/service", document, XPathConstants.NODESET);
		if(nodeList!=null){
			for(int i=0;i<nodeList.getLength();i++){
				Node node=nodeList.item(i);
				Node nameNode=node.getAttributes().getNamedItem("android:name");
				if(nameNode!=null){
					String serviceName=nameNode.getTextContent();
					Node serviceExportedNode=node.getAttributes().getNamedItem("android:exported");
					boolean needToCheckAgain=false;
					if(serviceExportedNode!=null){
						boolean exported=Boolean.parseBoolean(serviceExportedNode.getTextContent());
						if(exported){
							logger.verbose(serviceName);
							serviceList.add(serviceName);
						}else{
							needToCheckAgain=true;
						}
					}else{
						needToCheckAgain=true;
					}
					if(needToCheckAgain){
						Element element=(Element)node;
						NodeList actionNodeList=element.getElementsByTagName("action");
						if(actionNodeList.getLength()>0){
							logger.verbose(serviceName);
							serviceList.add(serviceName);
						}
					}
				}
			}
		}
		return serviceList;
	}

	/**
	 * auto dex
	 * @param option
	 */
	public static void autoDex(Option option) {
		String outputDirectory=new File(option.outputDirectory).getAbsolutePath();
		FileUtil.createDirectory(outputDirectory);
		long outerBegin=System.currentTimeMillis();
		long innerBegin=outerBegin;
		List<String> classNameList=new ArrayList<String>();
		//parse android manifest and package name
		String packageName=null;
		if(option.androidManifestFullFilename!=null&&FileUtil.isExist(option.androidManifestFullFilename)){
			classNameList.addAll(findMainDexClassListFromAndroidManifest(option.androidManifestFullFilename,option.attachBaseContext));
			packageName=parsePackageName(option.androidManifestFullFilename);
		}
		//read all combined class
		String cacheFullFilename=outputDirectory+Constant.Symbol.SLASH_LEFT+"cache.txt";
		Cache cache=readAllCombinedClass(option.combinedClassList, cacheFullFilename);
		//find main root class
		if(option.mainDexOtherClassList!=null){
			classNameList.addAll(option.mainDexOtherClassList);
		}
		Set<String> mainDexRootClassNameSet=new HashSet<String>();
		if(option.combinedClassList!=null){
			mainDexRootClassNameSet.addAll(findMainRootClassSet(cache.classNameByteArrayMap.keySet(), packageName, classNameList));
		}
		for(String className:mainDexRootClassNameSet){
			logger.verbose("Main root class:"+className);
		}
		Map<Integer,Map<String,String>> dexIdClassNameMap=null;
		//find all dex class
		if(cache.dexIdClassNameMap!=null&&!cache.dexIdClassNameMap.isEmpty()){
			dexIdClassNameMap=cache.dexIdClassNameMap;
			String incrementalDirectory=outputDirectory+Constant.Symbol.SLASH_LEFT+"incremental";
			FileUtil.deleteAllFile(incrementalDirectory);
			FileUtil.createDirectory(incrementalDirectory);
			Set<Integer> incrementalDexIdSet=new HashSet<Integer>();
			Map<Integer, Map<String, String>> changedDexIdClassNameMap=new HashMap<Integer, Map<String,String>>();
			if(cache.incrementalClassNameByteArrayMap!=null&&!cache.incrementalClassNameByteArrayMap.isEmpty()){
				Iterator<Entry<String,byte[]>> incrementalClassNameIterator=cache.incrementalClassNameByteArrayMap.entrySet().iterator();
				int dexId=0;
				while(incrementalClassNameIterator.hasNext()){
					Entry<String,byte[]> incrementalEntry=incrementalClassNameIterator.next();
					String className=incrementalEntry.getKey();
					FileUtil.writeFile(incrementalDirectory+Constant.Symbol.SLASH_LEFT+dexId+Constant.Symbol.SLASH_LEFT+className, incrementalEntry.getValue());
					incrementalDexIdSet.add(dexId);

					Map<String, String> changedClassNameMap=null;
					if(changedDexIdClassNameMap.containsKey(dexId)){
						changedClassNameMap=changedDexIdClassNameMap.get(dexId);
					}else{
						changedClassNameMap=new HashMap<String,String>();
						changedDexIdClassNameMap.put(dexId, changedClassNameMap);
					}
					changedClassNameMap.put(className, className);
				}
			}
			if(cache.modifiedClassNameByteArrayMap!=null&&!cache.modifiedClassNameByteArrayMap.isEmpty()){
				Iterator<Entry<String,byte[]>> modifiedClassNameIterator=cache.modifiedClassNameByteArrayMap.entrySet().iterator();
				while(modifiedClassNameIterator.hasNext()){
					Entry<String,byte[]> modifiedEntry=modifiedClassNameIterator.next();
					String className=modifiedEntry.getKey();
					Iterator<Entry<Integer, Map<String, String>>> dexIdClassNameIterator=cache.dexIdClassNameMap.entrySet().iterator();
					while(dexIdClassNameIterator.hasNext()){
						Entry<Integer, Map<String, String>> dexIdClassNameEntry=dexIdClassNameIterator.next();
						int dexId=dexIdClassNameEntry.getKey();
						Map<String, String> classNameMap=dexIdClassNameEntry.getValue();
						if(classNameMap.containsKey(className)){
							FileUtil.writeFile(incrementalDirectory+Constant.Symbol.SLASH_LEFT+dexId+Constant.Symbol.SLASH_LEFT+modifiedEntry.getKey(), modifiedEntry.getValue());
							incrementalDexIdSet.add(dexId);

							Map<String, String> changedClassNameMap=null;
							if(changedDexIdClassNameMap.containsKey(dexId)){
								changedClassNameMap=changedDexIdClassNameMap.get(dexId);
							}else{
								changedClassNameMap=new HashMap<String,String>();
								changedDexIdClassNameMap.put(dexId, changedClassNameMap);
							}
							changedClassNameMap.put(className, className);
							break;
						}
					}
				}
			}
			//dx
			for(int dexId:incrementalDexIdSet){
				String incrementalDexFullFilename=incrementalDirectory+Constant.Symbol.SLASH_LEFT+CLASSES+(dexId==0?StringUtil.BLANK:(dexId+1))+Constant.Symbol.DOT+DEX;
				DexUtil.androidDx(incrementalDexFullFilename, Arrays.asList(incrementalDirectory+Constant.Symbol.SLASH_LEFT+dexId), option.debug);
				String dexFullFilename=outputDirectory+Constant.Symbol.SLASH_LEFT+CLASSES+(dexId==0?StringUtil.BLANK:(dexId+1))+Constant.Symbol.DOT+DEX;
				DexUtil.androidMergeDex(dexFullFilename, Arrays.asList(incrementalDexFullFilename, dexFullFilename));
			}
			//update cache
			cache.dexIdClassNameMap.putAll(changedDexIdClassNameMap);
			if(cache.incrementalClassNameByteArrayMap!=null){
				cache.classNameByteArrayMap.putAll(cache.incrementalClassNameByteArrayMap);
			}
			if(cache.modifiedClassNameByteArrayMap!=null){
				cache.classNameByteArrayMap.putAll(cache.modifiedClassNameByteArrayMap);
			}
		}else{
			dexIdClassNameMap=autoDex(option, cache.classNameByteArrayMap, mainDexRootClassNameSet, null);
			cache.dexIdClassNameMap.putAll(dexIdClassNameMap);
			logger.info("Caculate total cost:"+(System.currentTimeMillis()-innerBegin));
			try{
				String splitAndDxTempDirectory=outputDirectory+Constant.Symbol.SLASH_LEFT+"temp";
				final Map<Integer,List<String>> subDexListMap=splitAndDx(cache.classNameByteArrayMap, splitAndDxTempDirectory, dexIdClassNameMap, option.debug);
				//concurrent merge dex
				innerBegin=System.currentTimeMillis();
				final CountDownLatch countDownLatch=new CountDownLatch(subDexListMap.size());
				Set<Integer> dexIdSet=subDexListMap.keySet();
				for(final int dexId:dexIdSet){
					String dexOutputDirectory=outputDirectory;
					String dexFullFilename=null;
					if(dexId==0){
						dexFullFilename=dexOutputDirectory+"/"+CLASSES+Constant.Symbol.DOT+DEX;
					}else{
						dexFullFilename=dexOutputDirectory+"/"+CLASSES+(dexId+1)+Constant.Symbol.DOT+DEX;
					}
					final String finalDexFullFilename=dexFullFilename;
					Thread thread=new Thread(new Runnable(){
						public void run() {
							try{
								DexUtil.androidMergeDex(finalDexFullFilename, subDexListMap.get(dexId));
							}catch(Exception e){
								logger.error(Constant.Base.EXCEPTION+",dexId:"+dexId+","+e.getMessage(), e);
							}
							countDownLatch.countDown();
						}
					});
					thread.start();
				}
				countDownLatch.await();
				logger.info("Merge dex cost:"+(System.currentTimeMillis()-innerBegin));
				FileUtil.deleteAllFile(splitAndDxTempDirectory);
			}catch(Exception e){
				throw new AutoDexUtilException(Constant.Base.EXCEPTION, e);
			}
		}
		try {
			ObjectUtil.writeObject(cache, new FileOutputStream(cacheFullFilename));
		} catch (Exception e) {
			logger.error("Write cache exception.", e);
		}
		logger.info("Auto dex cost:"+(System.currentTimeMillis()-outerBegin));
	}

	/**
	 * auto dex
	 * @param option
	 * @param classNameByteArrayMap
	 * @param mainDexRootClassNameSet
	 * @param fieldProcessor
	 * @return Map<Integer, Map<String,String>>, <dexId,classNameMap>
	 */
	private static Map<Integer,Map<String,String>> autoDex(Option option, Map<String, byte[]> classNameByteArrayMap, Set<String> mainDexRootClassNameSet, final FieldProcessor fieldProcessor){
		final Map<Integer,Map<String,String>> dexIdClassNameMap=new HashMap<Integer, Map<String,String>>();
		try{
			long begin=System.currentTimeMillis();
			//all class description
			Map<String,List<ClassDescription>> referencedClassDescriptionListMap=new HashMap<String,List<ClassDescription>>();
			Map<String,ClassDescription> classDescriptionMap=new HashMap<String,ClassDescription>();
			if(classNameByteArrayMap!=null){
				classDescriptionMap.putAll(AsmUtil.findClassDescriptionMap(classNameByteArrayMap, referencedClassDescriptionListMap));
			}
			logger.info("classDescriptionMap:"+classDescriptionMap.size()+",referencedClassDescriptionListMap:"+referencedClassDescriptionListMap.size());
			//all class map
			Map<String,String> allClassNameMap=new HashMap<String,String>();
			Set<String> classNameKeySet=classDescriptionMap.keySet();
			for(String className:classNameKeySet){
				allClassNameMap.put(className, className);
			}
			logger.info("Find all class description cost:"+(System.currentTimeMillis()-begin));
			Map<String, List<String>> samePackageClassNameListMap=null;
			if(option.autoByPackage){
				samePackageClassNameListMap=findAllSamePackageClassNameListMap(classDescriptionMap);
			}
			//main dex
			begin=System.currentTimeMillis();
			if(mainDexRootClassNameSet!=null){
				begin=System.currentTimeMillis();
				Map<Integer,Set<String>> dexClassRootSetMap=new HashMap<Integer,Set<String>>();
				dexClassRootSetMap.put(0, mainDexRootClassNameSet);
				Queue<Integer> dexQueue=new ConcurrentLinkedQueue<Integer>();
				dexQueue.add(0);
				final Map<Integer,AllocStat> dexAllocStatMap=new HashMap<Integer,AllocStat>();
				int autoDexId=0;
				boolean mustMainDex=true;
				while(!dexQueue.isEmpty()){
					Integer dexId=dexQueue.poll();
					Set<String> rootClassNameSet=dexClassRootSetMap.get(dexId);
					Map<String,String> dependClassNameMap=null;
					if(option.autoByPackage){
						dependClassNameMap=findAllSamePackageClassNameMap(rootClassNameSet, samePackageClassNameListMap);
					}else{
						if(mustMainDex){
							mustMainDex=false;
							dependClassNameMap=AsmUtil.findAllDependClassNameMap(rootClassNameSet, classDescriptionMap, referencedClassDescriptionListMap, allClassNameMap, true);
						}else{
							dependClassNameMap=AsmUtil.findAllDependClassNameMap(rootClassNameSet, classDescriptionMap, referencedClassDescriptionListMap, allClassNameMap, !option.debug);
						}
					}
					//先算这一垞有多少个方法数和linear
					AllocStat thisTimeAllocStat=new AllocStat();
					thisTimeAllocStat.setMethodReferenceMap(new HashMap<String,String>());
					thisTimeAllocStat.setFieldReferenceMap(new HashMap<String,String>());
					Set<String> keySet=dependClassNameMap.keySet();
					for(String key:keySet){
						AllocStat allocStat = LinearAllocUtil.estimateClass(new ByteArrayInputStream(classNameByteArrayMap.get(key)));
						thisTimeAllocStat.setTotalAlloc(thisTimeAllocStat.getTotalAlloc()+allocStat.getTotalAlloc());
						List<MethodReference> methodReferenceList=allocStat.getMethodReferenceList();
						if(methodReferenceList!=null){
							for(MethodReference methodReference:methodReferenceList){
								thisTimeAllocStat.getMethodReferenceMap().put(methodReference.toString(), methodReference.toString());
							}
						}
						//field reference map
						ClassDescription classDescription=classDescriptionMap.get(key);
						thisTimeAllocStat.getFieldReferenceMap().putAll(classDescription.referenceFieldNameMap);
						for(String fieldName:classDescription.fieldNameList){
							thisTimeAllocStat.getFieldReferenceMap().put(fieldName, fieldName);
						}
						allClassNameMap.remove(key);
					}
					//然后加上原来dex已经统计的方法数,如果是dexId=0就记一次就好了
					AllocStat dexTotalAllocStat=null;
					if(dexAllocStatMap.containsKey(dexId)){
						dexTotalAllocStat=dexAllocStatMap.get(dexId);
					}else{
						dexTotalAllocStat=new AllocStat();
						dexTotalAllocStat.setMethodReferenceMap(new HashMap<String,String>());
						dexTotalAllocStat.setFieldReferenceMap(new HashMap<String,String>());
						dexAllocStatMap.put(dexId, dexTotalAllocStat);
					}
					//因为dexId=0只会循环一次，所以如果还有类没分完，而且当前是dexId=0的话就开始第二个dex,此注释已过期,现在优先把主dex撑满
					int tempFieldLimit=option.fieldLimit;
					int tempMethodLimit=option.methodLimit;
					int tempLinearAllocLimit=option.linearAllocLimit;
					if(dexId==0){
						int thisTimeFieldLimit=thisTimeAllocStat.getFieldReferenceMap().size();
						int thisTimeMethodLimit=thisTimeAllocStat.getMethodReferenceMap().size();
						int thisTimeTotalAlloc=dexTotalAllocStat.getTotalAlloc()+thisTimeAllocStat.getTotalAlloc();
						if(thisTimeFieldLimit>tempFieldLimit){
							tempFieldLimit=thisTimeFieldLimit;
						}
						if(thisTimeMethodLimit>tempMethodLimit){
							tempMethodLimit=thisTimeMethodLimit;
						}
						if(thisTimeTotalAlloc>tempLinearAllocLimit){
							tempLinearAllocLimit=thisTimeTotalAlloc;
						}
					}
					boolean normalCaculate=false;
					if(option.minMainDex){
						if(dexId==0){
							dexTotalAllocStat.setTotalAlloc(dexTotalAllocStat.getTotalAlloc()+thisTimeAllocStat.getTotalAlloc());
							dexTotalAllocStat.getMethodReferenceMap().putAll(thisTimeAllocStat.getMethodReferenceMap());
							//add to current dex class name map
							if(!dexIdClassNameMap.containsKey(dexId)){
								dexIdClassNameMap.put(dexId, dependClassNameMap);
							}
							//and put the this time alloc stat to dex all stat map
							dexAllocStatMap.put(dexId, thisTimeAllocStat);
							autoDexId++;
						}else{
							normalCaculate=true;
						}
					}else{
						normalCaculate=true;
					}
					if(normalCaculate){//不是主dex的时候才要考虑合并计算
						//先clone原有的map，然后合并估算一下
						Map<String,String> oldFieldReferenceMap=dexTotalAllocStat.getFieldReferenceMap();
						Map<String,String> oldMethodReferenceMap=dexTotalAllocStat.getMethodReferenceMap();
						Map<String,String> tempFieldReferenceMap=(Map<String,String>)((HashMap<String,String>)oldFieldReferenceMap).clone();
						Map<String,String> tempMethodReferenceMap=(Map<String,String>)((HashMap<String,String>)oldMethodReferenceMap).clone();
						tempFieldReferenceMap.putAll(thisTimeAllocStat.getFieldReferenceMap());
						tempMethodReferenceMap.putAll(thisTimeAllocStat.getMethodReferenceMap());
						int tempTotalAlloc=dexTotalAllocStat.getTotalAlloc()+thisTimeAllocStat.getTotalAlloc();
						//如果没有超过method limit就不增加autoDexId
						if(tempFieldReferenceMap.size()<=tempFieldLimit&&tempMethodReferenceMap.size()<=tempMethodLimit&&tempTotalAlloc<=tempLinearAllocLimit){
							dexTotalAllocStat.setTotalAlloc(dexTotalAllocStat.getTotalAlloc()+thisTimeAllocStat.getTotalAlloc());
							dexTotalAllocStat.getMethodReferenceMap().putAll(thisTimeAllocStat.getMethodReferenceMap());
							dexTotalAllocStat.getFieldReferenceMap().putAll(thisTimeAllocStat.getFieldReferenceMap());
							if(!dexIdClassNameMap.containsKey(dexId)){
								dexIdClassNameMap.put(dexId, dependClassNameMap);
							}else{
								dexIdClassNameMap.get(dexId).putAll(dependClassNameMap);
							}
						}else{
							//this dex is full then next one.
							autoDexId++;
							//add to new dex class name map
							if(!dexIdClassNameMap.containsKey(autoDexId)){
								dexIdClassNameMap.put(autoDexId, dependClassNameMap);
							}
							//and put the this time alloc stat to dex all stat map
							dexAllocStatMap.put(autoDexId, thisTimeAllocStat);
						}
					}
					//autoDexId不变的时候还要继续当前dex
					Set<String> remainKeySet=allClassNameMap.keySet();
					for(String key:remainKeySet){
						dexQueue.add(autoDexId);
						Set<String> set=new HashSet<String>();
						set.add(key);
						dexClassRootSetMap.put(autoDexId, set);
						break;
					}
				}
				logger.info("Caculate class dependency cost:"+(System.currentTimeMillis()-begin));
				logger.info("remain classes:"+allClassNameMap.size());
				Iterator<Entry<Integer,AllocStat>> iterator=dexAllocStatMap.entrySet().iterator();
				while(iterator.hasNext()){
					Entry<Integer,AllocStat> entry=iterator.next();
					logger.info("\tdexId:"+entry.getKey()+"\tlinearAlloc:"+entry.getValue().getTotalAlloc()+"\tfield:"+entry.getValue().getFieldReferenceMap().size()+"\tmethod:"+entry.getValue().getMethodReferenceMap().size());
				}
			}
		}catch(Exception e){
			throw new AutoDexUtilException(e);
		}
		return dexIdClassNameMap;
	}

	/**
	 * split and dx
	 * @param classNameByteArrayMap
	 * @param outputDirectory
	 * @param dexIdClassNameMap
	 * @param apkDebug
	 */
	public static Map<Integer,List<String>> splitAndDx(final Map<String,byte[]> classNameByteArrayMap,final String outputDirectory,final Map<Integer,Map<String,String>> dexIdClassNameMap,final boolean apkDebug){
		final Map<Integer,List<String>> subDexListMap=new HashMap<Integer,List<String>>();
		long begin=System.currentTimeMillis();
		try{
			final String parentOutputDirectory=new File(outputDirectory).getParent();
			try{
				//copy all classes
				final CountDownLatch splitJarCountDownLatch=new CountDownLatch(dexIdClassNameMap.size());
				Set<Integer> dexIdSet=dexIdClassNameMap.keySet();
				final int fileCountPerJar=500;
				//concurrent split jar
				for(final int dexId:dexIdSet){
					final Set<String> classNameSet=dexIdClassNameMap.get(dexId).keySet();
					Thread thread=new Thread(new Runnable(){
						public void run() {
							int total=classNameSet.size();
							int subDexCount=0,count=0;
							ZipOutputStream dexJarOutputStream=null;
							String classesJar=null;
							String classNameTxt=null;
							String jarSubDexNameTxt=null;
							OutputStream classNameTxtOutputStream=null;
							OutputStream jarSubDexNameTxtOutputStream=null;
							try{
								classNameTxt=parentOutputDirectory+"/"+dexId+Constant.Symbol.DOT+Constant.File.TXT;
								jarSubDexNameTxt=outputDirectory+"/"+dexId+Constant.File.JAR+Constant.Symbol.DOT+Constant.File.TXT;
								FileUtil.createFile(classNameTxt);
								FileUtil.createFile(jarSubDexNameTxt);
								Properties classNameProperties=new Properties();
								Properties jarSubDexNameProperties=new Properties();
								for(String className:classNameSet){
									if(count%fileCountPerJar==0){
										classesJar=outputDirectory+"/"+AUTO_DEX_DEX_CLASSES_PREFIX+dexId+Constant.Symbol.UNDERLINE+subDexCount+Constant.Symbol.DOT+Constant.File.JAR;
										classesJar=new File(classesJar).getAbsolutePath();
										FileUtil.createFile(classesJar);
										dexJarOutputStream=new ZipOutputStream(new FileOutputStream(classesJar));
									}
									ZipEntry zipEntry=new ZipEntry(className);
									byte[] byteArray=classNameByteArrayMap.get(className);
									FileUtil.addZipEntry(dexJarOutputStream, zipEntry, new ByteArrayInputStream(byteArray));
									count++;
									classNameProperties.put(className, classesJar);

									if(count%fileCountPerJar==0||count==total){
										if(dexJarOutputStream!=null){
											dexJarOutputStream.flush();
											dexJarOutputStream.close();
										}
										String classesDex=outputDirectory+"/"+AUTO_DEX_DEX_CLASSES_PREFIX+dexId+Constant.Symbol.UNDERLINE+subDexCount+Constant.Symbol.DOT+Constant.File.DEX;
										classesDex=new File(classesDex).getAbsolutePath();
										if(classesJar!=null){
											DexUtil.androidDx(classesDex, Arrays.asList(classesJar), apkDebug);
											if(subDexListMap.containsKey(dexId)){
												subDexListMap.get(dexId).add(classesDex);
											}else{
												List<String> subDexList=new ArrayList<String>();
												subDexList.add(classesDex);
												subDexListMap.put(dexId, subDexList);
											}
										}
										jarSubDexNameProperties.put(classesJar, classesDex);
										subDexCount++;
									}
								}
								classNameTxtOutputStream=new FileOutputStream(classNameTxt);
								classNameProperties.store(classNameTxtOutputStream, null);
								jarSubDexNameTxtOutputStream=new FileOutputStream(jarSubDexNameTxt);
								jarSubDexNameProperties.store(jarSubDexNameTxtOutputStream, null);
							}catch (Exception e) {
								throw new AutoDexUtilException(classesJar,e);
							}finally{
								if(dexJarOutputStream!=null){
									try {
										dexJarOutputStream.flush();
										dexJarOutputStream.close();
									} catch (Exception e) {
										throw new AutoDexUtilException(classesJar,e);
									}
								}
								if(classNameTxtOutputStream!=null){
									try {
										classNameTxtOutputStream.flush();
										classNameTxtOutputStream.close();
									} catch (Exception e) {
										throw new AutoDexUtilException(classNameTxt,e);
									}
								}
								if(jarSubDexNameTxtOutputStream!=null){
									try {
										jarSubDexNameTxtOutputStream.flush();
										jarSubDexNameTxtOutputStream.close();
									} catch (Exception e) {
										throw new AutoDexUtilException(jarSubDexNameTxt,e);
									}
								}
							}
							splitJarCountDownLatch.countDown();
						}
					});
					thread.start();
				}
				splitJarCountDownLatch.await();
				logger.info("Split multi jar and dx,file count per jar:"+fileCountPerJar+",cost:"+(System.currentTimeMillis()-begin));
			}finally{
			}
		}catch (Exception e) {
			throw new AutoDexUtilException(e);
		}
		return subDexListMap;
	}

	/**
	 * find all same package class name list map
	 * @param classDescriptionMap
	 * @return Map<String,List<String>>
	 */
	private static Map<String,List<String>> findAllSamePackageClassNameListMap(Map<String,ClassDescription> classDescriptionMap){
		Map<String, List<String>> samePackageClassNameListMap=new HashMap<String, List<String>>();
		if(classDescriptionMap!=null){
			Set<String> classNameSet=classDescriptionMap.keySet();
			for(String className:classNameSet){
				String packageName=className.substring(0, className.lastIndexOf(Constant.Symbol.SLASH_LEFT));
				List<String> classNameList=null;
				if(samePackageClassNameListMap.containsKey(packageName)){
					classNameList=samePackageClassNameListMap.get(packageName);
				}else{
					classNameList=new ArrayList<String>();
					samePackageClassNameListMap.put(packageName, classNameList);
				}
				classNameList.add(className);
			}
		}
		return samePackageClassNameListMap;
	}

	/**
	 * find all same package class name map
	 * @param rootClassNameSet
	 * @param samePackageClassNameListMap
	 * @return Map<String,String>
	 */
	private static Map<String,String> findAllSamePackageClassNameMap(Set<String> rootClassNameSet,Map<String,List<String>> samePackageClassNameListMap){
		Map<String,String> classNameMap=new HashMap<String,String>();
		if(rootClassNameSet!=null&&samePackageClassNameListMap!=null){
			for(String rootClassName:rootClassNameSet){
				String packageName=rootClassName.substring(0,rootClassName.lastIndexOf(Constant.Symbol.SLASH_LEFT));
				List<String> samePackageClassNameList=samePackageClassNameListMap.get(packageName);
				if(samePackageClassNameList!=null){
					for(String className:samePackageClassNameList){
						classNameMap.put(className, className);
					}
				}else{
					logger.error("package:"+packageName+" is not exist.", null);
				}
			}
		}
		return classNameMap;
	}

	/**
	 * parse package name
	 * @param androidManifestFullFilename
	 * @return String
	 */
	private static String parsePackageName(String androidManifestFullFilename){
		String packageName=null;
		if(FileUtil.isExist(androidManifestFullFilename)){
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			Document document = JavaXmlUtil.parse(androidManifestFullFilename);
			try{
				XPathExpression expression = xpath.compile("/manifest[@package]");
				NodeList nodeList = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
				if(nodeList!=null&&nodeList.getLength()>0){
					Node node=nodeList.item(0);
					packageName=node.getAttributes().getNamedItem("package").getTextContent();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return packageName;
	}

	/**
	 * read all combined class
	 * @param combinedClassList
	 * @return AutoDexCache
	 */
	private static Cache readAllCombinedClass(List<String> combinedClassList){
		Map<String, byte[]> classNameByteArrayMap=new HashMap<String,byte[]>();
		if(combinedClassList!=null){
			for(String combinedClassFullFilename:combinedClassList){
				File combinedClassFile=new File(combinedClassFullFilename);
				combinedClassFullFilename=combinedClassFile.getAbsolutePath();
				if(combinedClassFile.isFile()&&combinedClassFile.getName().endsWith(Constant.Symbol.DOT+Constant.File.JAR)){
					logger.debug("Reading jar combined class:"+combinedClassFullFilename);
					ZipFile zipFile = null;
					try{
						zipFile = new ZipFile(combinedClassFile.getAbsolutePath());
						Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
						while (enumeration.hasMoreElements()) {
							ZipEntry zipEntry = enumeration.nextElement();
							String zipEntryName = zipEntry.getName();
							if(zipEntryName.endsWith(Constant.Symbol.DOT+Constant.File.CLASS)){
								InputStream inputStream=null;
								try{
									inputStream=zipFile.getInputStream(zipEntry);
									ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
									FileUtil.copyStream(inputStream, byteArrayOutputStream);
									logger.verbose(zipEntryName+","+byteArrayOutputStream.toByteArray().length);
									classNameByteArrayMap.put(zipEntryName, byteArrayOutputStream.toByteArray());
								}catch(Exception e){
									logger.error("Exception:"+zipEntryName, e);
								}finally{
									if(inputStream!=null){
										try{
											inputStream.close();
										}catch (Exception e) {
											logger.error("Close exception:"+zipEntryName, e);
										}
									}
								}
							}
						}
					}catch(Exception e){
						throw new AutoDexUtilException(e);
					}finally{
						if(zipFile!=null){
							try {
								zipFile.close();
							} catch (Exception e) {
								throw new AutoDexUtilException(e);
							}
						}
					}
				}else if(combinedClassFile.isDirectory()){
					logger.debug("Reading directory combined class:"+combinedClassFullFilename);
					String combiledClassRootPath=combinedClassFile.getAbsolutePath();
					List<String> allClassFullFilenameList=FileUtil.findMatchFile(combiledClassRootPath, Constant.Symbol.DOT+Constant.File.CLASS);
					if(allClassFullFilenameList!=null){
						combiledClassRootPath=new File(combiledClassRootPath).getAbsolutePath();
						for(String classFullFilename:allClassFullFilenameList){
							classFullFilename=new File(classFullFilename).getAbsolutePath();
							String relativeClassFilename=classFullFilename.substring(combiledClassRootPath.length()+1);
							relativeClassFilename=relativeClassFilename.replace(Constant.Symbol.SLASH_RIGHT, Constant.Symbol.SLASH_LEFT);
							byte[] byteArray=FileUtil.readFile(classFullFilename);
							classNameByteArrayMap.put(relativeClassFilename, byteArray);
						}
					}
				}
			}
		}
		return new Cache(classNameByteArrayMap);
	}

	/**
	 * read all combined class
	 * @param combinedClassList
	 * @param cacheFullFilename
	 */
	private static Cache readAllCombinedClass(List<String> combinedClassList, String cacheFullFilename){
		long begin=System.currentTimeMillis();
		Cache cache=null;
		if(FileUtil.isExist(cacheFullFilename)){
			try{
				cache=(Cache)ObjectUtil.readObject(new FileInputStream(cacheFullFilename));
			}catch(Exception e){
				logger.error("Read cache exception.", e);
			}
		}
		if(cache==null){
			cache=readAllCombinedClass(combinedClassList);
		}else{//has cache
			//need to update cache
			Map<String, byte[]> incrementalClassNameByteArrayMap=new HashMap<String,byte[]>();
			Map<String, byte[]> modifiedClassNameByteArrayMap=new HashMap<String,byte[]>();
			Cache newAutoDexCache=readAllCombinedClass(combinedClassList);
			Iterator<Entry<String,byte[]>> newIterator=newAutoDexCache.classNameByteArrayMap.entrySet().iterator();
			while(newIterator.hasNext()){
				Entry<String, byte[]> newEntry=newIterator.next();
				String newClassName=newEntry.getKey();
				byte[] newByteArray=newEntry.getValue();
				String newClassFileMd5=Generator.MD5(new ByteArrayInputStream(newByteArray));
				if(cache.classNameByteArrayMap.containsKey(newClassName)){
					byte[] byteArray=cache.classNameByteArrayMap.get(newClassName);
					String oldClassFileMd5=Generator.MD5(new ByteArrayInputStream(byteArray));
					if(!newClassFileMd5.equals(oldClassFileMd5)){
						logger.debug("It is a modify class:"+newClassName);
						modifiedClassNameByteArrayMap.put(newClassName, newByteArray);
					}else{
						logger.verbose("It is a same class:"+newClassName);
					}
				}else{
					logger.debug("It is a new class:"+newClassName);
					incrementalClassNameByteArrayMap.put(newClassName, newByteArray);
				}
			}
			cache.incrementalClassNameByteArrayMap=incrementalClassNameByteArrayMap;
			cache.modifiedClassNameByteArrayMap=modifiedClassNameByteArrayMap;
			logger.info("Incremental class size:"+cache.incrementalClassNameByteArrayMap.size());
			logger.info("Modified class size:"+cache.modifiedClassNameByteArrayMap.size());
		}
		logger.info("Read all class file cost:"+(System.currentTimeMillis()-begin));
		logger.info("Cache dex size:"+cache.dexIdClassNameMap.size());
		return cache;
	}

	/**
	 * find main root class set
	 * @param combinedClassNameSet
	 * @param classNameList
	 * @return List<String>
	 */
	private static Set<String> findMainRootClassSet(Set<String> combinedClassNameSet, String packageName, List<String> classNameList){
		List<String> regexList=new ArrayList<String>();
		Set<String> allClassSet=new HashSet<String>();
		if(classNameList!=null){
			for(String className:classNameList){
				className=className.trim();
				if(StringUtil.isNotBlank(className)){
					if(className.startsWith(Constant.Symbol.DOT)){
						className=packageName+className;
					}
					className=className.replace(Constant.Symbol.DOT, Constant.Symbol.SLASH_LEFT);
					if(className.indexOf(Constant.Symbol.WILDCARD)>-1||className.indexOf(Constant.Symbol.WILDCARD+Constant.Symbol.WILDCARD)>-1){
						String regex=Constant.Symbol.XOR+className.replace(Constant.Symbol.WILDCARD+Constant.Symbol.WILDCARD, "[\\S]+").replace(Constant.Symbol.WILDCARD, "[^/\\s]+")+Constant.Symbol.DOT+Constant.File.CLASS+Constant.Symbol.DOLLAR;
						regexList.add(regex);
					}else{
						className=className+Constant.Symbol.DOT+Constant.File.CLASS;
						allClassSet.add(className);
					}
				}
			}
		}
		if(combinedClassNameSet!=null){
			for(String combinedClassName:combinedClassNameSet){
				for(String regex:regexList){
					if(StringUtil.isMatchRegex(combinedClassName, regex)){
						logger.verbose("match:"+combinedClassName);
						allClassSet.add(combinedClassName);
					}
				}
			}
		}
		return allClassSet;
	}

	public static final class Option{
		public static final int DEFAULT_FIELD_LIMIT=0xFFD0;//dex field must less than 65536,but field stat always less then in
		public static final int DEFAULT_METHOD_LIMIT=0xFFFF;//dex must less than 65536,55000 is more safer then 65535
		public static final int DEFAULT_LINEAR_ALLOC_LIMIT=Integer.MAX_VALUE;
		public List<String> combinedClassList=null;
		public String androidManifestFullFilename=null;
		public List<String> mainDexOtherClassList=null;
		public String outputDirectory=null;
		public boolean debug=true;
		public boolean attachBaseContext=true;
		public boolean autoByPackage=false;
		public boolean minMainDex=true;
		public int fieldLimit=DEFAULT_FIELD_LIMIT;
		public int methodLimit=DEFAULT_METHOD_LIMIT;
		public int linearAllocLimit=DEFAULT_LINEAR_ALLOC_LIMIT;
		public Option(List<String> combinedClassList, String androidManifestFullFilename, String outputDirectory, boolean debug) {
			this.combinedClassList=combinedClassList;
			this.androidManifestFullFilename=androidManifestFullFilename;
			this.outputDirectory=outputDirectory;
			this.debug=debug;
			this.fieldLimit=this.debug?(DEFAULT_FIELD_LIMIT-0x200):DEFAULT_FIELD_LIMIT;
			this.methodLimit=this.debug?(DEFAULT_METHOD_LIMIT-0x200):DEFAULT_METHOD_LIMIT;
		}
	}

	private static final class Cache implements Serializable{
		private static final long serialVersionUID = 5668038330717176798L;
		private final Map<String,byte[]> classNameByteArrayMap;
		private final Map<Integer,Map<String,String>> dexIdClassNameMap=new HashMap<Integer, Map<String,String>>();
		public volatile Map<String,byte[]> incrementalClassNameByteArrayMap=null;
		public volatile Map<String,byte[]> modifiedClassNameByteArrayMap=null;
		public Cache(Map<String,byte[]> classNameByteArrayMap) {
			this.classNameByteArrayMap=classNameByteArrayMap;
		}
	}

	private static class AutoDexUtilException extends RuntimeException{
		private static final long serialVersionUID = -6167451596208904365L;
		public AutoDexUtilException(String message) {
			super(message);
		}

		public AutoDexUtilException(Throwable cause) {
			super(cause);
		}

		public AutoDexUtilException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
