package model;

import java.util.Map;

public class DataDetailVO {
	private String report;//������-��+�µ�6λ�������
	private Double truedefault;//����ʵ��ΥԼ��
	private Double CT;//CTֵ-ϵͳ�Զ�����
	private Double R;//Rֵ-ϵͳ�Զ�����
	private Double trueZT;//trueZTֵ-ϵͳ�Զ�����
	private Map<String,Double> indexMap;//26����۾���ָ��ֵMap
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
