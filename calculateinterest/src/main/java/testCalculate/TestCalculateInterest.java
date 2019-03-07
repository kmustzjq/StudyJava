package testCalculate;

import calculateInterest.CalculateInterest;

import java.text.NumberFormat;

public class TestCalculateInterest {
    public static void main(String[] args) {
        CalculateInterest c1= new CalculateInterest();
        c1.setMoney(500000);
        c1.setInterestrate(0.059);
        c1.setDate(20);
        NumberFormat nf1 = NumberFormat.getNumberInstance();
        nf1.setMaximumFractionDigits(2);
        double equal_principal_and_interest;//等额本息
        equal_principal_and_interest=c1.Equal_installments_of_principal_and_interest(c1.getMoney(),c1.getInterestrate(),c1.getDate());
        System.out.println("等额本息是："+nf1.format(equal_principal_and_interest));
        double[] equal_principal;//等额本金
        equal_principal=c1.Equivalent_principal(c1.getMoney(),c1.getInterestrate(),c1.getDate());
        System.out.println("等额本金是：");
        for (int i=0;i<(12*c1.getDate());i++){
            System.out.print("第"+(i+1)+"个月要支付的钱为\t"+nf1.format(equal_principal[i]));
            System.out.println();
        }
    }
}
