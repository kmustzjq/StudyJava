package model;

public class CaculationRsltVO {
	private String modelType;//模型名称
	private double ztValue;//ztֵ
	private String calcufunctiondesc;//计算公式补充描述
	
	public String getModelType() {
		return modelType;
	}
	public void setModelType(String modelType) {
		this.modelType = modelType;
	}
	public double getZtValue() {
		return ztValue;
	}
	public void setZtValue(double ztValue) {
		this.ztValue = ztValue;
	}
	public String getCalcufunctiondesc() {
		return calcufunctiondesc;
	}
	public void setCalcufunctiondesc(String calcufunctiondesc) {
		this.calcufunctiondesc = calcufunctiondesc;
	}
}
