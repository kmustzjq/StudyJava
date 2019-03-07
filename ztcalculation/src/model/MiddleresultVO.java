package model;

public class MiddleresultVO {
	private String indexName;//指标名称
	private Integer ranking;//排名
	private double rsquare;//R方
	private String proessstep;//使用的滞后期数据
	private double scope;//斜率
	private double interception;//截距
	private double[] selectArray;//选中的数组
	public String getIndexName() {
		return indexName;
	}
	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}
	
	public Integer getRanking() {
		return ranking;
	}
	public void setRanking(Integer ranking) {
		this.ranking = ranking;
	}
	public double getRsquare() {
		return rsquare;
	}
	public void setRsquare(double rsquare) {
		this.rsquare = rsquare;
	}
	public String getProessstep() {
		return proessstep;
	}
	public void setProessstep(String proessstep) {
		this.proessstep = proessstep;
	}
	public double getScope() {
		return scope;
	}
	public void setScope(double scope) {
		this.scope = scope;
	}
	public double getInterception() {
		return interception;
	}
	public void setInterception(double interception) {
		this.interception = interception;
	}
	public double[] getSelectArray() {
		return selectArray;
	}
	public void setSelectArray(double[] selectArray) {
		this.selectArray = selectArray;
	}
}
