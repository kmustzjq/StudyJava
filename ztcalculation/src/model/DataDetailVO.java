package model;

import java.util.Map;

public class DataDetailVO {
	private String report;//报告期-年+月的6位数字组合
	private Double truedefault;//敞口实际违约率
	private Double CT;//CT值-系统自动计算
	private Double R;//R值-系统自动计算
	private Double trueZT;//trueZT值-系统自动计算
	private Map<String,Double> indexMap;//26个宏观经济指标值Map
	public String getReport() {
		return report;
	}
	public void setReport(String report) {
		this.report = report;
	}

	public Double getTruedefault() {
		return truedefault;
	}
	public void setTruedefault(Double truedefault) {
		this.truedefault = truedefault;
	}

	public Double getCT() {
		return CT;
	}
	public void setCT(Double cT) {
		CT = cT;
	}
	public Double getR() {
		return R;
	}
	public void setR(Double r) {
		R = r;
	}
	public Double getTrueZT() {
		return trueZT;
	}
	public void setTrueZT(Double trueZT) {
		this.trueZT = trueZT;
	}
	public Map<String, Double> getIndexMap() {
		return indexMap;
	}
	public void setIndexMap(Map<String, Double> indexMap) {
		this.indexMap = indexMap;
	}
	

}
