package calculateInterest;

public class CalculateInterest {
    //贷款本金
    private double money;
    //利率
    private double interestrate;
    //时间
    private int date;

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
    }

    public double getInterestrate() {
        return interestrate;
    }

    public void setInterestrate(double interestrate) {
        this.interestrate = interestrate;
    }
    //计算等额本息
    public  double Equal_installments_of_principal_and_interest(double a,double b,int c){
        double power=Math.pow((1+b/12),12*c);
        return (a*b/12*power)/(power-1);
    }
    //计算等额本金
    public  double[] Equivalent_principal(double a,double b ,int c){
        double[] interest1=new double[12*c];
        double principal=a/(12*c);
        for (int i=0;i<(12*c);i++){
            interest1[i]=principal+(a-i*principal)*b/12;
        }
        return  interest1;
    }

}
