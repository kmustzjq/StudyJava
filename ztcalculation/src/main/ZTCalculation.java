package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import model.CaculationRsltVO;
import model.DataDetailVO;
import model.MiddleresultVO;
/**
 * 前瞻性ZT系数拟合算法
 * @author guort
 * @version 
 * v0.00 20170923 by guort 根据KP需求新建
 * v0.10 20171030 by guort 根据KP第二版需求算法修改建模规则
 */
public class ZTCalculation {
	private static final String[] indexnamearray = { "GDPvalue", "gdzctzvalue", "fdctzvalue", "wlwjvalue",
			"xfzxxzsvalue", "sylvalue", "incomevalue", "CPIvalue", "PPIvalue", "new70HPricevalue", "es70HPriceGDPvalue",
			"gfjqzsvalue", "RPIvalue", "xzloanvalue", "shrzgmvalue", "cgclvalue", "fdlclvalue", "xfzxxjmvalue",
			"PMIPvalue", "ssvalue", "shlszevalue", "M0value", "M1value", "M2value", "depositbalancevalue",
			"loanbalancevalue" };// 所有指标名称集合
	private double[] curztarray = null; 
	private static double rsquareneedvalue = 0.8;// 设置RSquare必须达到的阈值
	private int maxParamNum = 0;// 最大拟合变量数，如果入模变量达到该值，就算一直达不到拟合优度也直接返回拟合结果
	private boolean logprintind = true;// 日志打印标志，技术调试使用
	private Map<Integer, MiddleresultVO> intoCalList1 = new HashMap<Integer, MiddleresultVO>();// 模型1的已入模
	private Map<Integer, MiddleresultVO> intoCalList2 = new HashMap<Integer, MiddleresultVO>();// 模型2的已入模
	private Map<Integer, MiddleresultVO> intoCalList3 = new HashMap<Integer, MiddleresultVO>();// 模型3的已入模 
	private double curmaxrsquare1 = 0;
	private double curmaxrsquare2 = 0;
	private double curmaxrsquare3 = 0;
	/**
	 * 计算zt值主方法
	 * 
	 * @param dataDetailVOList
	 * @param CT 平均违约率CT值
	 * @param exposureType 敞口类别
	 * @param resquarepara 需达到的R方阈值
	 * @param maxParamNum
	 *            最大拟合变量数
	 * @return
	 */
	public List<CaculationRsltVO> calztestaminate(List<DataDetailVO> dataDetailVOList, double CT,
			String exposureType,double resquarepara,int maxParamNum) {
		List<CaculationRsltVO> rsltList = new ArrayList<CaculationRsltVO>();
		// R方阈值预留为参数，没有传入的时候取默认值
		if (resquarepara > 0.00000001) {
			rsquareneedvalue = resquarepara;
		}
		// 最大拟合变量数预处理
		if (maxParamNum == 0) {
			this.maxParamNum = 6;// 传入为空时默认为6个
		} else if (maxParamNum < 3) {
			this.maxParamNum = 3;// 至少需要拟合3个指标
		} else if (maxParamNum > 10) {
			this.maxParamNum = 10;// 最多拟合10个指标
		} else {
			this.maxParamNum = maxParamNum;
		}
		// 1、数据预处理
		// 1.1 根据实际违约率反推ZT值
		dataDetailVOList = this.calhiszt(dataDetailVOList,CT,exposureType);
		// 1.2 把数据根据报告期重新排序
		dataDetailVOList = this.sortimputlist(dataDetailVOList);
		// 1.3根据敞口对指标进行筛选
		String[] indexnameList=this.getselectindexname(exposureType);
		if(indexnameList.length<26) {
			dataDetailVOList=this.selectindexlist(dataDetailVOList, indexnameList);
		}
		if (dataDetailVOList != null && dataDetailVOList.size() > 4) {
			// 1.4获取ZT值的数组
			curztarray = this.getIndexValueArraybylag(dataDetailVOList, "zt", 0);
			//2.0 根据与GDP的拟合结果判断进入不同的建模逻辑
			//2.1 获取GDP与其滞后数组分别与curzt进行拟合，获取最大ADJ_R方值及对应数组
			MiddleresultVO gdprsltVO=new MiddleresultVO();
			gdprsltVO.setIndexName(indexnameList[0]);
			gdprsltVO.setRsquare(-1.0);
			for(int i=0;i<5;i++){//2.1.1 对GDP数据及其滞后数组进行拟合
				//2.1.2 获取滞后数组
				double[] temparray=this.getIndexValueArraybylag(dataDetailVOList, indexnameList[0], i);
				OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
				//2.1.3 OLS为多元拟合算法，需转换x为二维数组
				double[][] dualArray = changeindextodualarray(temparray,null);
				//2.1.4 y值数组与x值 矩阵建立样本库
				regression.newSampleData(curztarray, dualArray);
				//对模型进行检验，不符合要求的模型和变量组放弃入模
				if(!this.simplecheck(regression, curztarray.length-2,indexnameList[0])) {
					if(logprintind){
						System.out.println("gdp lag"+i+" selectcheck reject!");
					}
					continue;
				}
								
				//2.1.5 获取当前拟合的adjust-r square的值
				double adjr = regression.calculateAdjustedRSquared();
				
				if(gdprsltVO.getRsquare()<=-1.00000001||adjr>gdprsltVO.getRsquare()){//2.1.6 获取adjr较大的值和对应的数组
					gdprsltVO.setRsquare(adjr);
					gdprsltVO.setSelectArray(temparray);
					gdprsltVO.setProessstep("LAG" + i);
				}
				
				
			}
			if(logprintind) {
				System.out.println("after GDP regression the adjr="+gdprsltVO.getRsquare() +" and chooselag=" +gdprsltVO.getProessstep());
			}
			//2.1.6 补充其他指标的滞后期数据
			Map<String,List<double[]>> tobeselindexarrayMap=new HashMap<String,List<double[]>>();
			List<double[]> indexarrayList=null;
			for(int i=0;i<indexnameList.length;i++) {
				indexarrayList=new ArrayList<double[]>();
				for(int j=0;j<5;j++) {
					double[] temparray=this.getIndexValueArraybylag(dataDetailVOList, indexnameList[i], j);
					indexarrayList.add(temparray);
				}
				tobeselindexarrayMap.put(indexnameList[i], indexarrayList);
			}
			//2.2 判断GDP的adj-R的值
			if(gdprsltVO.getRsquare()>=-1.0 && gdprsltVO.getRsquare()<0.35){//2.2.2 情形一：若0≤R<0.35, 则只考虑不强制性添加GDP的模型(3个);				
				//2.2.2.2 第一个入模指标挑选，需要对26个指标的所有滞后期数据分别与zt值进行一元线性拟合,每个指标取出表现最好的一期数据作为当前一元拟合结果数据
				Map<Integer, MiddleresultVO> simpleRsltMap = new HashMap<Integer, MiddleresultVO>();
				for(int i=0;i<indexnameList.length;i++) {
					indexarrayList=new ArrayList<double[]>();
					indexarrayList=tobeselindexarrayMap.get(indexnameList[i]);
					MiddleresultVO rsltVO=new MiddleresultVO();
					rsltVO.setIndexName(indexnameList[i]);
					rsltVO.setRsquare(-1.0);
					for(int j=0;j<indexarrayList.size();j++) {//每个指标取出表现最好的一期数据作为当前一元拟合结果数据
						OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
						double[][] dualArray = changeindextodualarray(indexarrayList.get(j),null);
						regression.newSampleData(curztarray, dualArray);
						//对模型进行检验，不符合要求的模型和变量组放弃入模
						if(!this.simplecheck(regression, curztarray.length-2,rsltVO.getIndexName())) {
							if(logprintind){
								System.out.println("情形一下一元回归 当前指标 "+rsltVO.getIndexName()+" lag"+j+" selectcheck reject!");
							}
							continue;
						}
						double adjr = regression.calculateAdjustedRSquared();
						if(rsltVO.getRsquare()<=-1.000000001 || adjr>rsltVO.getRsquare()){// 获取adjr较大的值和对应的数组
							rsltVO.setRsquare(adjr);
							rsltVO.setProessstep("LAG" + j);
							rsltVO.setSelectArray(indexarrayList.get(j));
						}
					}
					simpleRsltMap.put(i, rsltVO);
				}
				//2.2.2.3 对指标根据R方值重新排序
				simpleRsltMap=this.sortResultListbyRsquare(simpleRsltMap);
				if(simpleRsltMap.size()>0){
					if(logprintind) {
						System.out.println("情形1下一元线性拟合结束，拟合结果如下");
						for(int i=0;i<simpleRsltMap.size();i++) {
							System.out.println("排名第"+i+"名的元素为："+simpleRsltMap.get(i).getIndexName()+" and ADJR="+simpleRsltMap.get(i).getRsquare());
						}
					}
					//2.2.3 进入3个模型计算分支
					//2.2.3.1 选取第一个入模指标
					this.curmaxrsquare1=simpleRsltMap.get(0).getRsquare();
					this.curmaxrsquare2=simpleRsltMap.get(1).getRsquare();
					this.curmaxrsquare3=simpleRsltMap.get(2).getRsquare();
					this.intoCalList1.put(0, simpleRsltMap.get(0));
					this.intoCalList2.put(0, simpleRsltMap.get(1));
					this.intoCalList3.put(0, simpleRsltMap.get(2));
					rsltList.add(this.calztbymodeltype("1", tobeselindexarrayMap,indexnameList));
					rsltList.add(this.calztbymodeltype("2", tobeselindexarrayMap,indexnameList));
					rsltList.add(this.calztbymodeltype("3", tobeselindexarrayMap,indexnameList));	
				}else{
					CaculationRsltVO calrsltvo = new CaculationRsltVO();
					calrsltvo.setModelType("1");
					calrsltvo.setZtValue(-1.0);
					calrsltvo.setCalcufunctiondesc("all indexdata cannot pass the testcheck!");
					rsltList.add(calrsltvo);
				}
			}else if(gdprsltVO.getRsquare()>=0.6 && gdprsltVO.getRsquare()<=1){//2.2.3 情形二：若0.6≤R<1，则只考虑强制性添加GDP模型(3个);
				//2.2.3.1 先将GDP入模
				this.intoCalList1.put(0, gdprsltVO);
				this.intoCalList2.put(0, gdprsltVO);
				this.intoCalList3.put(0, gdprsltVO);
				//2.2.3.2 剩余的25个变量与GDP一起进行二元线性回归
				Map<Integer, MiddleresultVO> simpleRsltMap = new HashMap<Integer, MiddleresultVO>();
				for(int i=1;i<indexnameList.length;i++) {
					indexarrayList=new ArrayList<double[]>();
					indexarrayList=tobeselindexarrayMap.get(indexnameList[i]);
					MiddleresultVO rsltVO=new MiddleresultVO();
					rsltVO.setIndexName(indexnameList[i]);
					rsltVO.setRsquare(-1.0);
					for(int j=0;j<indexarrayList.size();j++) {//每个指标取出表现最好的一期数据作为当前一元拟合结果数据
						OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
						double[][] dualArray = changeindextodualarray(indexarrayList.get(j),intoCalList1);
						regression.newSampleData(curztarray, dualArray);
						if(!this.muticheck(regression, 2,dualArray,rsltVO.getIndexName())) {
							if(logprintind){
								System.out.println("情形二下二元回归 当前指标 "+rsltVO.getIndexName()+" lag"+j+" selectcheck reject!");
							}
							continue;
						}
						double adjr = regression.calculateAdjustedRSquared();
						if(rsltVO.getRsquare()<=-1.000000001 || adjr>rsltVO.getRsquare()){// 获取adjr较大的值和对应的数组
							rsltVO.setRsquare(adjr);
							rsltVO.setProessstep("LAG" + j);
							rsltVO.setSelectArray(indexarrayList.get(j));
						}
					}
					simpleRsltMap.put(i-1, rsltVO);
				}
				//2.2.3.3 对指标根据R方值重新排序
				simpleRsltMap=this.sortResultListbyRsquare(simpleRsltMap);
				if(simpleRsltMap.size()>0){
					if(logprintind) {
						System.out.println("情形2下二元线性拟合结束，拟合结果如下");
						for(int i=0;i<simpleRsltMap.size();i++) {
							System.out.println("排名第"+i+"名的元素为："+simpleRsltMap.get(i).getIndexName()+" and ADJR="+simpleRsltMap.get(i).getRsquare());
						}
					}
					//2.2.3.4进入3个模型计算分支
					this.curmaxrsquare1=simpleRsltMap.get(0).getRsquare();
					this.curmaxrsquare2=simpleRsltMap.get(1).getRsquare();
					this.curmaxrsquare3=simpleRsltMap.get(2).getRsquare();
					this.intoCalList1.put(1, simpleRsltMap.get(0));
					this.intoCalList2.put(1, simpleRsltMap.get(1));
					this.intoCalList3.put(1, simpleRsltMap.get(2));
					rsltList.add(this.calztbymodeltype("1", tobeselindexarrayMap,indexnameList));
					rsltList.add(this.calztbymodeltype("2", tobeselindexarrayMap,indexnameList));
					rsltList.add(this.calztbymodeltype("3", tobeselindexarrayMap,indexnameList));	
				}else{
					CaculationRsltVO calrsltvo = new CaculationRsltVO();
					calrsltvo.setModelType("1");
					calrsltvo.setZtValue(-1.0);
					calrsltvo.setCalcufunctiondesc("all indexdata cannot pass the testcheck!");
					rsltList.add(calrsltvo);
				}
			}else {	//2.2.4情形三：若0.35≤R<0.6, 则既考虑不强制性添加GDP的模型(3个)，也考虑强制性添加GDP的模型(3个) .
				//2.2.4.1 先考虑强制添加的逻辑
				//2.2.4.1.1 先将GDP入模
				this.intoCalList1.put(0, gdprsltVO);
				this.intoCalList2.put(0, gdprsltVO);
				this.intoCalList3.put(0, gdprsltVO);
				//2.2.4.1.2 剩余的25个变量与GDP一起进行二元线性回归
				Map<Integer, MiddleresultVO> simpleRsltMap = new HashMap<Integer, MiddleresultVO>();
				for(int i=1;i<indexnameList.length;i++) {
					indexarrayList=new ArrayList<double[]>();
					indexarrayList=tobeselindexarrayMap.get(indexnameList[i]);
					MiddleresultVO rsltVO=new MiddleresultVO();
					rsltVO.setIndexName(indexnameList[i]);
					rsltVO.setRsquare(-1.0);
					for(int j=0;j<indexarrayList.size();j++) {//每个指标取出表现最好的一期数据作为当前一元拟合结果数据
						OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
						double[][] dualArray = changeindextodualarray(indexarrayList.get(j),intoCalList1);
						regression.newSampleData(curztarray, dualArray);
						if(!this.muticheck(regression, curztarray.length-3,dualArray,rsltVO.getIndexName())) {
							if(logprintind){
								System.out.println("情形三下二元回归 当前指标 "+rsltVO.getIndexName()+" lag"+j+" selectcheck reject!");
							}
							continue;
						}
						double adjr = regression.calculateAdjustedRSquared();
						if(rsltVO.getRsquare()<=-1.000000001 || adjr>rsltVO.getRsquare()){// 获取adjr较大的值和对应的数组
							rsltVO.setRsquare(adjr);
							rsltVO.setProessstep("LAG" + j);
							rsltVO.setSelectArray(indexarrayList.get(j));
						}
					}
					simpleRsltMap.put(i-1, rsltVO);
				}
				//2.2.4.1.3 对指标根据R方值重新排序
				
				simpleRsltMap=this.sortResultListbyRsquare(simpleRsltMap);
				if(simpleRsltMap.size()>0){
					if(logprintind) {
						System.out.println("情形3下二元线性拟合结束，拟合结果如下+simpleRsltMap.size()="+simpleRsltMap.size());
						for(int i=0;i<simpleRsltMap.size();i++) {
							System.out.println("排名第"+i+"名的元素为："+simpleRsltMap.get(i).getIndexName()+" and ADJR="+simpleRsltMap.get(i).getRsquare());
						}
					}
					
					//2.2.4.1.4进入3个模型计算分支
					this.curmaxrsquare1=simpleRsltMap.get(0).getRsquare();
					this.curmaxrsquare2=simpleRsltMap.get(1).getRsquare();
					this.curmaxrsquare3=simpleRsltMap.get(2).getRsquare();
					this.intoCalList1.put(1, simpleRsltMap.get(0));
					this.intoCalList2.put(1, simpleRsltMap.get(1));
					this.intoCalList3.put(1, simpleRsltMap.get(2));
					rsltList.add(this.calztbymodeltype("1", tobeselindexarrayMap,indexnameList));
					rsltList.add(this.calztbymodeltype("2", tobeselindexarrayMap,indexnameList));
					rsltList.add(this.calztbymodeltype("3", tobeselindexarrayMap,indexnameList));	
				}
				//2.2.4.2 进入不考虑GDP强制入模的逻辑
				//2.2.4.2.1 将入选列表先清空
				intoCalList1 = new HashMap<Integer, MiddleresultVO>();
				intoCalList2 = new HashMap<Integer, MiddleresultVO>();
				intoCalList3 = new HashMap<Integer, MiddleresultVO>();
				curmaxrsquare1 = 0;
				curmaxrsquare2 = 0;
				curmaxrsquare3 = 0;
				simpleRsltMap = new HashMap<Integer, MiddleresultVO>();
				for(int i=0;i<indexnameList.length;i++) {
					indexarrayList=new ArrayList<double[]>();
					indexarrayList=tobeselindexarrayMap.get(indexnameList[i]);
					MiddleresultVO rsltVO=new MiddleresultVO();
					rsltVO.setIndexName(indexnameList[i]);
					rsltVO.setRsquare(-1.0);
					for(int j=0;j<indexarrayList.size();j++) {//每个指标取出表现最好的一期数据作为当前一元拟合结果数据
						OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
						double[][] dualArray = changeindextodualarray(indexarrayList.get(j),null);
						regression.newSampleData(curztarray, dualArray);
						//对模型进行检验，不符合要求的模型和变量组放弃入模
						if(!this.simplecheck(regression, curztarray.length-2,rsltVO.getIndexName())) {
							if(logprintind){
								System.out.println("情形三下一元回归 当前指标 "+rsltVO.getIndexName()+" lag"+j+" selectcheck reject!");
							}
							continue;
						}
						double adjr = regression.calculateAdjustedRSquared();
						if(rsltVO.getRsquare()<=-1.000000001 || adjr>rsltVO.getRsquare()){// 获取adjr较大的值和对应的数组
							rsltVO.setRsquare(adjr);
							rsltVO.setProessstep("LAG" + j);
							rsltVO.setSelectArray(indexarrayList.get(j));
						}
					}
					simpleRsltMap.put(i, rsltVO);
				}
				//2.2.4.2.2 对指标根据R方值重新排序
				simpleRsltMap=this.sortResultListbyRsquare(simpleRsltMap);
				if(simpleRsltMap.size()>0){
					if(logprintind) {
						System.out.println("情形3下一元线性拟合结束，拟合结果如下");
						for(int i=0;i<simpleRsltMap.size();i++) {
							System.out.println("排名第"+i+"名的元素为："+simpleRsltMap.get(i).getIndexName()+" and ADJR="+simpleRsltMap.get(i).getRsquare());
						}
					}
					//2.2.4.2.3 进入3个模型计算分支,如果第一个指标为GDP，则需要对模型去重
					if(!simpleRsltMap.get(0).getIndexName().equals("GDPvalue")){
						this.curmaxrsquare1=simpleRsltMap.get(0).getRsquare();
						this.intoCalList1.put(0, simpleRsltMap.get(0));
						rsltList.add(this.calztbymodeltype("1", tobeselindexarrayMap,indexnameList));
					}
					if(!simpleRsltMap.get(1).getIndexName().equals("GDPvalue")){
						this.curmaxrsquare2=simpleRsltMap.get(1).getRsquare();
						this.intoCalList2.put(0, simpleRsltMap.get(1));
						rsltList.add(this.calztbymodeltype("2", tobeselindexarrayMap,indexnameList));
					}
					if(!simpleRsltMap.get(2).getIndexName().equals("GDPvalue")){
						this.curmaxrsquare3=simpleRsltMap.get(2).getRsquare();				
						this.intoCalList3.put(0, simpleRsltMap.get(2));
						rsltList.add(this.calztbymodeltype("3", tobeselindexarrayMap,indexnameList));	
					}
				}
			}
			if(rsltList==null || rsltList.size()==0){
				CaculationRsltVO calrsltvo = new CaculationRsltVO();
				calrsltvo.setModelType("1");
				calrsltvo.setZtValue(-1.0);
				calrsltvo.setCalcufunctiondesc("all indexdata cannot pass the testcheck!");
				rsltList.add(calrsltvo);
			}
		} else {
			CaculationRsltVO calrsltvo = new CaculationRsltVO();
			calrsltvo.setModelType("1");
			calrsltvo.setZtValue(-1.0);
			calrsltvo.setCalcufunctiondesc("No enough data!");
			rsltList.add(calrsltvo);
		}
		return rsltList;
	}
	
	/**
	 * 模型场景下拟合
	 * @param modelType
	 * @param tobeselindexarrayMap
	 * @return CaculationRsltVO
	 */
	public CaculationRsltVO calztbymodeltype(String modelType,Map<String,List<double[]>> tobeselindexarrayMap,String[] indexnameList) {
		CaculationRsltVO calrsltvo = new CaculationRsltVO();
		Map<Integer, MiddleresultVO> intoCalList = new HashMap<Integer, MiddleresultVO>();
		double adjr=-1.0;
		if(modelType.equals("1")) {
			intoCalList=this.intoCalList1;
			adjr=this.curmaxrsquare1;
		}else if(modelType.equals("2")) {
			intoCalList=this.intoCalList2;
			adjr=this.curmaxrsquare2;
		}else if(modelType.equals("3")) {
			intoCalList=this.intoCalList3;
			adjr=this.curmaxrsquare3;
		}
		double[] beta=null;
		//去除tobeselindexarrayMap中已选的指标
		Map<String,List<double[]>> newtobeselindexarrayMap=new HashMap<String,List<double[]>>();
		List<String> selectindexname=new ArrayList<String>();
		List<String> tobeselindexname=new ArrayList<String>();
		for(int i=0;i<intoCalList.size();i++){
			selectindexname.add(intoCalList.get(i).getIndexName());
		}
		for(int i=0;i<indexnameList.length;i++){
			if(!selectindexname.contains(indexnameList[i])){
				newtobeselindexarrayMap.put(indexnameList[i], tobeselindexarrayMap.get(indexnameList[i]));
				tobeselindexname.add(indexnameList[i]);
			}
		}
		//对未入模的变量进行拟合
		int times=tobeselindexname.size();
		double curmaxr=-1.0;
		for(int n=0;n<times;n++){//第一层循环，设置拟合次数	
			Map<Integer, MiddleresultVO> mutiRsltMap = new HashMap<Integer, MiddleresultVO>();
			for(int i=0;i<tobeselindexname.size();i++){//第二层循环，进入多元回归
				List<double[]> indexarrayList=newtobeselindexarrayMap.get(tobeselindexname.get(i));
				MiddleresultVO rsltVO=new MiddleresultVO();
				rsltVO.setIndexName(tobeselindexname.get(i));
				rsltVO.setRsquare(-1.0);
				for(int j=0;j<indexarrayList.size();j++){
					OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
					double[][] dualArray = changeindextodualarray(indexarrayList.get(j),intoCalList);
					regression.newSampleData(curztarray, dualArray);
					if(!this.muticheck(regression, intoCalList.size()+1,dualArray,rsltVO.getIndexName())) {
						if(logprintind){
							System.out.println("模型"+modelType+n+" "+i+"元回归 当前指标 "+rsltVO.getIndexName()+" lag"+j+" selectcheck reject!");
						}
						continue;
					}
					double tmpadjr = regression.calculateAdjustedRSquared();
					if(rsltVO.getRsquare()<=-1.000000001 || tmpadjr>rsltVO.getRsquare()){// 获取adjr较大的值和对应的数组
						rsltVO.setRsquare(tmpadjr);
						rsltVO.setProessstep("LAG" + j);
						rsltVO.setSelectArray(indexarrayList.get(j));
					}
					if(tmpadjr>curmaxr){//此处判断为获取系数，如果R方达标系数值要用于计算最终结果
						curmaxr=tmpadjr;
						beta=regression.estimateRegressionParameters();
					}
				}
				mutiRsltMap.put(i, rsltVO);		
			}
			//对结果进行排序
			mutiRsltMap=this.sortResultListbyRsquare(mutiRsltMap);
			if(mutiRsltMap.size()>0) {
				intoCalList.put(intoCalList.size(), mutiRsltMap.get(0));
				adjr=mutiRsltMap.get(0).getRsquare();
				tobeselindexname.remove(mutiRsltMap.get(0).getIndexName());
			}else {
				if(logprintind) System.out.println("模型"+modelType+"第"+n+"次多元线性拟合结束,无入模指标");
				calrsltvo.setModelType(modelType);
				calrsltvo.setZtValue(-1.0);
				calrsltvo.setCalcufunctiondesc("all indexdata cannot pass the testcheck!");
				return calrsltvo;
			}
			if(logprintind) {
				System.out.println("模型"+modelType+"第"+n+"次多元线性拟合结束：");
				for(int i=0;i<mutiRsltMap.size();i++) {
					System.out.println("排名第"+i+"名的元素为："+mutiRsltMap.get(i).getIndexName()+" and ADJR="+mutiRsltMap.get(i).getRsquare());
				}
				System.out.println("当前入模指标数"+intoCalList.size()+" R方值为："+adjr);
			}
			if((intoCalList.size()>=3 && adjr>=this.rsquareneedvalue)||intoCalList.size()>=this.maxParamNum) {
				calrsltvo.setModelType(modelType);
				if(beta.length==0) {
					calrsltvo.setZtValue(-1.0);
					calrsltvo.setCalcufunctiondesc("build model faided becuase of cannot get beta value");
				}else {
					StringBuffer rsltdesc=new StringBuffer();
					rsltdesc.append("current model adjust-RSquare="+adjr);
					rsltdesc.append(" and zt="+beta[0]);	
					double ztvalue=beta[0];
					for(int i=0;i<Math.min(beta.length, intoCalList.size());i++) {
						MiddleresultVO rsltvo=intoCalList.get(i);
	 					rsltdesc.append("+(");
						rsltdesc.append(beta[i+1]);
						rsltdesc.append(")*(");
						rsltdesc.append(this.getindexdscbyname(rsltvo.getIndexName()));
						rsltdesc.append("_");
						rsltdesc.append(rsltvo.getProessstep());
						rsltdesc.append("[");
						rsltdesc.append(rsltvo.getSelectArray()[rsltvo.getSelectArray().length-1]);
						rsltdesc.append("])");
						ztvalue=ztvalue+beta[i+1]*rsltvo.getSelectArray()[rsltvo.getSelectArray().length-1];
					}
					calrsltvo.setZtValue(ztvalue);
					calrsltvo.setCalcufunctiondesc(rsltdesc.toString());
				}
				return calrsltvo;
			}
		}
		
		return calrsltvo;
	}

	/**
	 * 根据输入的实际违约概率反推实际ZT值
	 * 
	 * @param dataDetailVOList
	 * @param CT 平均违约率CT值
	 * @param exposureType 敞口类别
	 * @return
	 */
	public List<DataDetailVO> calhiszt(List<DataDetailVO> dataDetailVOList,double CT,String exposureType) {

		
		
		if (dataDetailVOList != null && dataDetailVOList.size() > 0) {
//			if(CT<=0) {
//			// CT值缺失的情况下根据历史数据获取CT值
//				int count = 0;
//				for (int i = 0; i < dataDetailVOList.size() - 4; i++) {
//					DataDetailVO dataDetailVO = dataDetailVOList.get(i);
//					if (dataDetailVO.getReport() != null && dataDetailVO.getReport().length() >= 6
//						&& dataDetailVO.getReport().substring(5, 6).equals("4")) {
//						CT = CT + dataDetailVO.getTruedefault();
//						count = count + 1;
//					}
//
//				}
//				if (count > 0) {
//					CT = CT / count;
//				}
//			}
			// 根据每期的实际违约率等计算R、ZT
			for (int i = 0; i < dataDetailVOList.size() - 4; i++) {
				DataDetailVO dataDetailVO = dataDetailVOList.get(i);
				if (CT > 0) {
					NormalDistribution normalDistributioin = new NormalDistribution(0, 1);
					double R=0;
					if(exposureType!=null) {
						if(exposureType.equals("C1")  ||
								exposureType.equals("C2") ||
								exposureType.equals("C3") ||
								exposureType.equals("C4") ||
								exposureType.equals("C5") ||
								exposureType.equals("C6") ||
								exposureType.equals("C7") ||
								exposureType.equals("C8") ||
								exposureType.equals("C9") ||
								exposureType.equals("C10") ||
								exposureType.equals("C11") ||
								exposureType.equals("C12") ||
								exposureType.equals("C13") ||
								exposureType.equals("C14") ) {
							// 敞口为对公敞口(C1,C2,C3,C4,C5,C6,C7,C8,C9,C10,C11,C12,C13,C14)时
							R = 0.12 * (1 - 1 / Math.exp(50 * CT)) / (1 - 1 / Math.exp(50))
									+ 0.24 * (1 - (1 - 1 / Math.exp(50 * CT)) / (1 - 1 / Math.exp(50)));
						}else if(exposureType.equals("P1") ){// 敞口为个人住房贷款(P1)时
							R=0.15;
						}else if(exposureType.equals("P5")||exposureType.equals("P7")||exposureType.equals("P8")){
							//敞口为信用卡业务时(P5,P7,P8)时
							R=0.04;
						}else {
							R = 0.03 * (1 - 1 / Math.exp(35 * CT)) / (1 - 1 / Math.exp(35))
									+ 0.16 * (1 - (1 - 1 / Math.exp(35 * CT)) / (1 - 1 / Math.exp(35)));
						}
					}else {
						dataDetailVO.setTrueZT(-1.00);
					}
					double histzt = (normalDistributioin.inverseCumulativeProbability(CT) - Math.sqrt(1 - R)
							* normalDistributioin.inverseCumulativeProbability(dataDetailVO.getTruedefault()))
							/ Math.sqrt(R);
					dataDetailVO.setCT(CT);
					dataDetailVO.setR(R);
					dataDetailVO.setTrueZT(histzt);
					

					// NORM.S.INV(CT)-SQRT(1-R)*NORM.S.INV(Default))/SQRT(R)
					
//					if(logprintind){			
//						System.out.println("calhiszt report="+dataDetailVO.getReport()+" and r="+R + " and histzt=" +histzt);
//					}
				} else {
					dataDetailVO.setTrueZT(-1.00);
				}

			}

		}
		return dataDetailVOList;
	}


	

	/**
	 * 转换指定指标数据为数组
	 * 
	 * @param dataDetailVOList
	 * @param indexname
	 * @param lagno
	 *            0-表示不平移，1-表示平移一期，2-表示平移2期，3-表示平移三期，4-表示平移4期
	 * @return
	 */
	private double[] getIndexValueArraybylag(List<DataDetailVO> dataDetailVOList, String indexname, int lagno) {
		double[] indexarray = null;
		if (dataDetailVOList != null && dataDetailVOList.size() > 4 && indexname != null) {// 因为包含了预测数据，所以数据列表必须大于4期
			indexarray = new double[dataDetailVOList.size() - 4];
			if ("zt".equals(indexname)) {
				lagno = 0;// 获取zt值时不进行平移
			}
			for (int i = lagno; i < dataDetailVOList.size() + lagno - 4; i++) {
				DataDetailVO dataDetailVO = dataDetailVOList.get(i);
				if ("zt".equals(indexname)) {
					if (dataDetailVO.getTrueZT() != null) {
						indexarray[i - lagno] = dataDetailVO.getTrueZT();
					} else {
						indexarray[i - lagno] = -1;
					}
					// System.out.println("into cal
					// truezt["+i+"]="+indexarray[i-lagno]);
				} else {
					indexarray[i - lagno] = dataDetailVO.getIndexMap().get(indexname);
				}
			}
		}
		return indexarray;
	}

	/**
	 * 为指定对象转换对象值为二元数组
	 * 
	 * @param indexarray1
	 * @param indexarray2
	 * @return
	 */
	private double[][] changeindextodualarray(double[] indexarray1, Map<Integer, MiddleresultVO> intoCalList) {
		double[][] duallinearray = null;
		if (indexarray1 != null && indexarray1.length > 0 && intoCalList != null && intoCalList.size()> 0) {
			duallinearray = new double[indexarray1.length][intoCalList.size()+1];
			for (int i = 0; i < indexarray1.length; i++) {//;TODO
				for(int j=0;j<intoCalList.size()+1;j++){
					if(j<intoCalList.size()){
						duallinearray[i][j] = intoCalList.get(j).getSelectArray()[i];
						//System.out.println("duallinearray["+i+"]["+j+"]="+duallinearray[i][j]);
//						System.out.println("i="+i+" and intoCalList.get("+j+").size="+intoCalList.get(j).getSelectArray().length);
						
					}else{
						duallinearray[i][j] = indexarray1[i];
					}
				}
			}
		}else if (indexarray1 != null && indexarray1.length > 0 ) {
			int length = indexarray1.length;
			duallinearray = new double[length][1];
			for (int i = 0; i < length; i++) {
				duallinearray[i][0] = indexarray1[i];
			}
		}
//		for(int i=0;i<duallinearray.length;i++){
//			System.out.println("changeindextodualarray[" + i +"]="+duallinearray[i]);
//		}
		return duallinearray;
	}



	/**
	 * 根据报告期对列表进行排序
	 * 
	 * @param dataDetailVOList
	 * @return
	 */
	private List<DataDetailVO> sortimputlist(List<DataDetailVO> dataDetailVOList) {
		DataDetailVO tempvo1 = null;
		DataDetailVO tempvo2 = null;
		if (dataDetailVOList != null && dataDetailVOList.size() > 0) {
			for (int i = 0; i < dataDetailVOList.size()-1; i++) {
				for(int j=0;j<dataDetailVOList.size()-1-i;j++) {
					tempvo1 = dataDetailVOList.get(j);
					tempvo2 = dataDetailVOList.get(j + 1);
					if (Double.parseDouble(tempvo1.getReport()) > Double.parseDouble(tempvo2.getReport())) {
						dataDetailVOList.set(j, tempvo2);
						dataDetailVOList.set(j + 1, tempvo1);
					}
				}

			}
		}
		return dataDetailVOList;
	}

	/**
	 * 对结果列表根据R方值进行排序
	 * 
	 * @param resultList
	 * @return
	 */
	private Map<Integer, MiddleresultVO> sortResultListbyRsquare(Map<Integer, MiddleresultVO> resultMap) {
		Map<Integer, MiddleresultVO> newresultMap=new HashMap<Integer, MiddleresultVO>();
		if (resultMap != null && resultMap.size() > 0) {
			
			// 将R方较大的值放到列表前
			for (int i = 0; i < resultMap.size()-1; i++) {
				for(int j=0;j<resultMap.size()-1-i;j++) {
					MiddleresultVO tempvo1 = resultMap.get(j);
					MiddleresultVO tempvo2 = resultMap.get(j+1);
					if (tempvo2.getRsquare() > tempvo1.getRsquare()) {
						resultMap.put(j, tempvo2);
						resultMap.put((j+1), tempvo1);
					}
				}
			}
			
			//去除R方为默认值的数据
			for(int i = 0; i < resultMap.size(); i++){
				MiddleresultVO tempvo = resultMap.get(i);
				if(tempvo.getRsquare()>-0.999999){
					//System.out.println("newresultMap.size="+newresultMap.size()+" and tempvo.getRsquare()="+tempvo.getRsquare());
					newresultMap.put(newresultMap.size(), tempvo);
				}
			}
		}
		return newresultMap;
	}
	/**
	 * 一元回归线性分析检验结果
	 * @param regression 回归样本库
	 * @param degreesOfFreedom 自由度
	 * @return 检查结果
	 */
	private boolean simplecheck(OLSMultipleLinearRegression regression,int degreesOfFreedom,String indexName) {
		boolean checkresult=true;
		//对系数进行校验
		double[] beta=regression.estimateRegressionParameters();
		if(beta.length<2 || (!indexName.equals("sylvalue") && beta[1]<=0) 
				|| (indexName.equals("sylvalue") && beta[1]==0)) {//变量系数为0时该数组不适用,并且只有V6系数可以为负
			checkresult=false;
			return checkresult;
		}
		//获取其他关键数据
		double RSquare=regression.calculateRSquared();		
		double SSe=regression.calculateResidualSumOfSquares();//残差平方和		
		double SSt=regression.calculateTotalSumOfSquares();//总平方和
		double SSr=SSt-SSe;//回归平方和
		//对单变量结果进行校验,单变量检验使用t检验
		double t=Math.sqrt(RSquare)/Math.sqrt((1-RSquare)/(degreesOfFreedom));
		TDistribution tdis=new TDistribution(degreesOfFreedom);//当前拟合得到的t值
		double tp=tdis.cumulativeProbability(0.2);//查询t分布表，找到对应alpha=0.2时该自由度下的t值
		
		if(t<=tp) {//关系不显著时检查不通过
			checkresult=false;
			return checkresult;
		}
		//对模型结果进行校验，使用F校验
		double F=SSr/(SSe/degreesOfFreedom);
		FDistribution fdis=new FDistribution(1,degreesOfFreedom);//当前拟合得到的t值 
		double fd=fdis.cumulativeProbability(0.2);
		if(F<=fd) {//关系不显著时检查不通过
			checkresult=false;
			return checkresult;
		}
//		if(logprintind) {
//			System.out.println("simpleregression the  t="+t+" and tp="+tp+" and F="+F+" and fd="+fd);			
//		}
		return checkresult;
	}
	
	/**
	 * 多元回归线性分析检验结果
	 * @param regression 回归样本库
	 * @param valuenum 变量个数
	 * @return
	 */
	private boolean muticheck(OLSMultipleLinearRegression regression,int valuenum,double[][] x,String indexName) {
		boolean checkresult=true;
		//获取样本数：
		double samplenum=this.curztarray.length;
		//获取自由度
		double degreesOfFreedom=samplenum-valuenum-1;
		//对系数进行校验
		double[] beta=regression.estimateRegressionParameters();
		if(beta.length<valuenum+1 || (!indexName.equals("sylvalue") && beta[valuenum]<=0) 
				|| (indexName.equals("sylvalue") && beta[valuenum]==0)) {//最后入模变量系数为0时该数组不适用,,并且只有V6系数可以为负
			checkresult=false;
			return checkresult;
		}
		//获取相关参数
		double RSquare=regression.calculateRSquared();		
		double SSe=regression.calculateResidualSumOfSquares();//残差平方和		
		double SSt=regression.calculateTotalSumOfSquares();//总平方和
		double SSr=SSt-SSe;//回归平方和
		//对单变量系数进行t检验
		double[][] matrixdata = regression.estimateRegressionParametersVariance();//获取(XT*X)的逆矩阵
		double cii = matrixdata[matrixdata.length - 1][matrixdata.length - 1];
		double t = beta[valuenum] / Math.sqrt(cii * regression.calculateResidualSumOfSquares() / (x.length - valuenum-1));
		TDistribution tdis=new TDistribution(degreesOfFreedom);//当前拟合得到的t值
		double tp=tdis.cumulativeProbability(0.2);//查询t分布表，找到对应alpha=0.2时该自由度下的t值		
		if(t<=tp) {//关系不显著时检查不通过
			checkresult=false;
			return checkresult;
		}
		//对模型进行F检验
		double F=(SSr/valuenum)/(SSe/degreesOfFreedom);
		FDistribution fdis=new FDistribution(valuenum,degreesOfFreedom);//当前拟合得到的t值 
		double fd=fdis.cumulativeProbability(0.2);
		if(F<=fd) {//关系不显著时检查不通过
			checkresult=false;
			return checkresult;
		}
//		if(logprintind) {
//			System.out.println("mutiregression the  t="+t+" and tp="+tp+" and F="+F+" and fd="+fd);			
//		}
		return checkresult;
	}
	
	/**
	 * 
	 * @param exposureType
	 * @return
	 */
	private String[] getselectindexname(String exposureType) {//暂时不做敞口指标筛选，后续再放开
//		if(exposureType==null || "".equals(exposureType)) {
			return indexnamearray;
//		}else {
//			if(exposureType.equals("C2")) {
//				String[] indexnameList= {"GDPvalue","gdzctzvalue","CPIvalue","PPIvalue","xzloanvalue","shrzgmvalue","PMIPvalue","ssvalue",
//						"M0value","M1value","M2value","loanbalancevalue"};
//				return indexnameList;
//			}else if(exposureType.equals("C3")) {
//				String[] indexnameList= {"GDPvalue","gdzctzvalue","fdctzvalue","CPIvalue","PPIvalue","xzloanvalue","shrzgmvalue","cgclvalue",
//						"fdlclvalue","PMIPvalue","ssvalue","M0value","M1value","M2value","loanbalancevalue"};
//				return indexnameList;
//			}else if(exposureType.equals("C4")) {
//				String[] indexnameList= {"GDPvalue","gdzctzvalue","CPIvalue","PPIvalue","xzloanvalue","shrzgmvalue","fdlclvalue","PMIPvalue",
//						"ssvalue","M0value","M1value","M2value","loanbalancevalue"};
//				return indexnameList;
//			}else if(exposureType.equals("C5")) {
//				String[] indexnameList= {"GDPvalue","gdzctzvalue","CPIvalue","PPIvalue","xzloanvalue","shrzgmvalue","cgclvalue","fdlclvalue",
//						"PMIPvalue","ssvalue","shlszevalue","M0value","M1value","M2value","loanbalancevalue"};
//				return indexnameList;
//			}else if(exposureType.equals("C6")) {
//				String[] indexnameList= {"GDPvalue","gdzctzvalue","CPIvalue","PPIvalue","xzloanvalue","shrzgmvalue","cgclvalue","fdlclvalue",
//						"PMIPvalue","ssvalue","shlszevalue","M0value","M1value","M2value","loanbalancevalue"};
//				return indexnameList;
//			}else if(exposureType.equals("C7")) {
//				String[] indexnameList= {"GDPvalue","wlwjvalue","CPIvalue","PPIvalue","xzloanvalue","shrzgmvalue","PMIPvalue","ssvalue",
//						"shlszevalue","M0value","M1value","M2value","loanbalancevalue"};
//				return indexnameList;
//			}else if(exposureType.equals("C8")) {
//				String[] indexnameList= {"GDPvalue","wlwjvalue","CPIvalue","PPIvalue","xzloanvalue","shrzgmvalue","PMIPvalue","ssvalue",
//						"shlszevalue","M0value","M1value","M2value","loanbalancevalue"};
//				return indexnameList;
//			}else if(exposureType.equals("C9")) {
//				String[] indexnameList= {"GDPvalue","gdzctzvalue","fdctzvalue","CPIvalue","PPIvalue","new70HPricevalue","es70HPriceGDPvalue",
//						"gfjqzsvalue","xzloanvalue","shrzgmvalue","cgclvalue","fdlclvalue","PMIPvalue","ssvalue","M0value","M1value","M2value",
//						"loanbalancevalue"};
//				return indexnameList;
//			}else if(exposureType.equals("C10")) {
//				String[] indexnameList= {"GDPvalue","gdzctzvalue","fdctzvalue","CPIvalue","PPIvalue","new70HPricevalue","es70HPriceGDPvalue",
//						"gfjqzsvalue","xzloanvalue","shrzgmvalue","cgclvalue","fdlclvalue","PMIPvalue","ssvalue","M0value","M1value","M2value",
//						"loanbalancevalue"};
//				return indexnameList;
//			}else if(exposureType.equals("C11")) {
//				String[] indexnameList= {"GDPvalue","gdzctzvalue","fdctzvalue","CPIvalue","PPIvalue","xzloanvalue","shrzgmvalue","cgclvalue",
//						"fdlclvalue","PMIPvalue","ssvalue","M0value","M1value","M2value","loanbalancevalue"};
//				return indexnameList;
//			}else if(exposureType.equals("C12")) {
//				String[] indexnameList= {"GDPvalue","gdzctzvalue","fdctzvalue","CPIvalue","PPIvalue","xzloanvalue","shrzgmvalue","cgclvalue",
//						"fdlclvalue","PMIPvalue","ssvalue","M0value","M1value","M2value","loanbalancevalue"};
//				return indexnameList;
//			}else if(exposureType.equals("C13")) {
//				String[] indexnameList= {"GDPvalue","gdzctzvalue","wlwjvalue","xfzxxzsvalue","sylvalue","incomevalue","CPIvalue","PPIvalue",
//						"xzloanvalue","shrzgmvalue","PMIPvalue","ssvalue","M0value","M1value","M2value","loanbalancevalue"};
//				return indexnameList;
//				
//			}else if(exposureType.equals("C14")) {
//				String[] indexnameList= {"GDPvalue","gdzctzvalue","wlwjvalue","xfzxxzsvalue","sylvalue","incomevalue","CPIvalue","PPIvalue",
//						"xzloanvalue","shrzgmvalue","PMIPvalue","ssvalue","M0value","M1value","M2value","loanbalancevalue"};
//				return indexnameList;
//			}else{
//				return indexnamearray;
//			}
//		}
		
	}
	
	/**
	 * 
	 * @param dataDetailVOList
	 * @param exposureType
	 * @return dataDetailVOList
	 */
	private List<DataDetailVO> selectindexlist(List<DataDetailVO> dataDetailVOList,String[] indexnameList){
		if(indexnameList.length>0 && dataDetailVOList.size()>0) {
			for(int i=0;i<dataDetailVOList.size();i++) {
				Map<String,Double> indexmap=dataDetailVOList.get(i).getIndexMap();
				Map<String,Double> newindexmap=new HashMap<String,Double>();
				for(int j=0;j<indexnameList.length;j++) {
					String indexname=indexnameList[j];
					newindexmap.put(indexname, indexmap.get(indexname));
				}
				dataDetailVOList.get(i).setIndexMap(newindexmap);
			}
		}
		return dataDetailVOList;
	}

	private String getindexdscbyname(String indexname) {
		String indexdsc = new String();
		if (indexname == "GDPvalue") {
			indexdsc = "GDP:不变价:当季同比";
		}
		if (indexname == "gdzctzvalue") {
			indexdsc = "固定资产投资完成额:实际当季同比";
		}
		if (indexname == "fdctzvalue") {
			indexdsc = "房地产投资完成额:实际当季同比";
		}
		if (indexname == "wlwjvalue") {
			indexdsc = "未来物价预期指数";
		}
		if (indexname == "xfzxxzsvalue") {
			indexdsc = "消费者信心指数(季)";
		}
		if (indexname == "sylvalue") {
			indexdsc = "城镇登记失业率";
		}
		if (indexname == "incomevalue") {
			indexdsc = "城镇居民人均可支配收入:当季同比";
		}
		if (indexname == "CPIvalue") {
			indexdsc = "CPI:季度环比";
		}
		if (indexname == "PPIvalue") {
			indexdsc = "PPI:全部工业品:季度环比";
		}
		if (indexname == "new70HPricevalue") {
			indexdsc = "70个大中城市新建住宅价格指数:季度环比";
		}
		if (indexname == "es70HPriceGDPvalue") {
			indexdsc = "70个大中城市二手住宅价格指数:季度环比";
		}
		if (indexname == "gfjqzsvalue") {
			indexdsc = "国房景气指数:季末值";
		}
		if (indexname == "RPIvalue") {
			indexdsc = "RPI:季度环比";
		}
		if (indexname == "xzloanvalue") {
			indexdsc = "社会融资规模:新增人民币贷款:当季同比";
		}
		if (indexname == "shrzgmvalue") {
			indexdsc = "社会融资规模:当季同比";
		}
		if (indexname == "cgclvalue") {
			indexdsc = "产量:粗钢:当季同比";
		}
		if (indexname == "fdlclvalue") {
			indexdsc = "产量:发电量:当季同比";
		}
		if (indexname == "xfzxxjmvalue") {
			indexdsc = "消费者信心指数:季末值";
		}
		if (indexname == "PMIPvalue") {
			indexdsc = "PMI:季末值";
		}
		if (indexname == "ssvalue") {
			indexdsc = "税收收入:当季同比";
		}
		if (indexname == "shlszevalue") {
			indexdsc = "社会消费零售总额:当季同比";
		}
		if (indexname == "M0value") {
			indexdsc = "M0:同比季末值";
		}
		if (indexname == "M1value") {
			indexdsc = "M1:同比季末值";
		}
		if (indexname == "M2value") {
			indexdsc = "M2:同比季末值";
		}
		if (indexname == "depositbalancevalue") {
			indexdsc = "金融机构:各项存款余额:同比季末值";
		}
		if (indexname == "loanbalancevalue") {
			indexdsc = "金融机构:各项贷款余额:同比季末值";
		}

		return indexdsc;
	}

	public double[] getCurztarray() {
		return curztarray;
	}

	public void setCurztarray(double[] curztarray) {
		this.curztarray = curztarray;
	}

	
	
	public static double getRsquareneedvalue() {
		return rsquareneedvalue;
	}

	public static void setRsquareneedvalue(double rsquareneedvalue) {
		ZTCalculation.rsquareneedvalue = rsquareneedvalue;
	}
	
	public int getMaxParamNum() {
		return maxParamNum;
	}

	public void setMaxParamNum(int maxParamNum) {
		this.maxParamNum = maxParamNum;
	}

	public boolean isLogprintind() {
		return logprintind;
	}

	public void setLogprintind(boolean logprintind) {
		this.logprintind = logprintind;
	}

	public static String[] getIndexnamearray() {
		return indexnamearray;
	}

	

	public double getCurmaxrsquare1() {
		return curmaxrsquare1;
	}

	public void setCurmaxrsquare1(double curmaxrsquare1) {
		this.curmaxrsquare1 = curmaxrsquare1;
	}

	public double getCurmaxrsquare2() {
		return curmaxrsquare2;
	}

	public void setCurmaxrsquare2(double curmaxrsquare2) {
		this.curmaxrsquare2 = curmaxrsquare2;
	}

	public double getCurmaxrsquare3() {
		return curmaxrsquare3;
	}

	public void setCurmaxrsquare3(double curmaxrsquare3) {
		this.curmaxrsquare3 = curmaxrsquare3;
	}

	public Map<Integer, MiddleresultVO> getIntoCalList1() {
		return intoCalList1;
	}

	public void setIntoCalList1(Map<Integer, MiddleresultVO> intoCalList1) {
		this.intoCalList1 = intoCalList1;
	}

	public Map<Integer, MiddleresultVO> getIntoCalList2() {
		return intoCalList2;
	}

	public void setIntoCalList2(Map<Integer, MiddleresultVO> intoCalList2) {
		this.intoCalList2 = intoCalList2;
	}

	public Map<Integer, MiddleresultVO> getIntoCalList3() {
		return intoCalList3;
	}

	public void setIntoCalList3(Map<Integer, MiddleresultVO> intoCalList3) {
		this.intoCalList3 = intoCalList3;
	}

	

	
}
