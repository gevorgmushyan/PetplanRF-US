package Petplan;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static Petplan.PolicyPlan.*;

public class CRF {
    public Policy policy;
    public MyExcel excel;
    //public String filePath = System.getProperty("user.dir") + "\\src\\main\\resources\\Blueprint (Rates) 20190423 0525 pm.xlsx";
    public String filePath = System.getProperty("user.dir") + "\\src\\main\\resources\\Blueprint - US (Rates) 20190703 0850 am.xlsx";

    public CRF() {
        Thread thread = new Thread("New Thread") {
            public void run() {
                excel = new MyExcel(filePath);
            }
        };
        thread.start();
        ScanData scan = new ScanData();
        policy = scan.scanAndGetPolicy();
    }

    private int getBaseRateColunm() {
        for (int i = 0; i < baseRates.length; i++) {
            if (baseRates[i].toLowerCase().equals(policy.getPolicyScheme()))
                if (policy.getPetType().equals(PetType.CAT))
                    return i + 2;
                else
                    return i + baseRates.length + 2;
        }
        return -1;
    }

    private int getBaseRateRow() {
        excel.openWorkSheet("Base Rate");
        //return excel.finedInColumn(0, policy.getState(), 4, 54);
        for (int i = 0; i < states.length; i++) {
            if (states[i].toLowerCase().equals(policy.getState()))
                return i + 3;
        }
        return -1;
    }

    private void showBaseRate() {
        excel.openWorkSheet("Base Rate");
        int i = getBaseRateRow();
        if (i == -1) {
            System.out.println("Cannot fined state in sheet.");
            return;
        }
        int j = getBaseRateColunm();
        if (j == -1) {
            System.out.println("Cannot fined scheme for pet in sheet.");
            return;
        }

        System.out.println("Base Rate: " + excel.readCell(i, j));
    }

    private void showAgeFactor() {
        excel.openWorkSheet("Breed to Breed Grp Mapping 2019");
        String breedVal = this.policy.getPetType().equals(PetType.DOG) ? "ppdog001" : "ppcat001";
        int i = excel.finedInColumn(1, policy.getBreed(), 2, breedVal, 1, 626);
        if (i == -1) {
            System.out.println("Cannot fined breed in sheet.");
            return;
        }
        String groupId = excel.readCell(i, 7);
        System.out.println("Pet group Id is: " + groupId);

        excel.openWorkSheet("Combined Breed Grp-Age Factors ");

        int baseLine = excel.finedInColumn(0, policy.getPetType().toString(), 1, groupId, 10, 1067);
        if (baseLine == -1) {
            System.out.println("Cannot fined group Id for pet in sheet.");
            return;
        }

        System.out.println("Age Factor: " + excel.readCell(baseLine + policy.getAge(), 3));
    }

    private void showAreaLookup() {
        excel.openWorkSheet("Zip Code Look Up");
        int i = excel.finedInColumn(0, policy.getZipCode(), 3, 38250);
        if (i == -1) {
            System.out.println("Cannot fined zip in sheet.");
            return;
        }
        String areaLookup = excel.readCell(i, 4);
        System.out.println("Pet area lookup is: " + areaLookup);

        excel.openWorkSheet("Area 2019");
        int num;
        try {
            num = (int) Double.parseDouble(areaLookup.trim());
        } catch (NumberFormatException e) {
            System.out.println("Bad format for area lookup.");
            return;
        }
        int deep = policy.getPetType().equals(PetType.CAT) ? 0 : 7;
        System.out.println("Pet Area Factor is: " +
                excel.readCell(2 + num + deep, 3));
    }

    private void showDeductibleFactor() {
        excel.openWorkSheet("Deductible");
        int i = excel.finedInColumn(0, policy.getState(), 3, 56);
        if (i == -1) {
            System.out.println("Cannot fined state in sheet.");
            return;
        }
        int k = -1;
        for (int j = 0; j < deductibles.length; j++) {
            if (deductibles[j].toLowerCase().equals(policy.getDeductible())) {
                k = j;
                break;
            }
        }
        if (k == -1) {
            System.out.println("Bad deductible.");
            return;
        }
        int j = (policy.getPetType().equals(PetType.DOG) ? deductibles.length : 0) + k + 1;
        String deductible = excel.readCell(i, j);
        System.out.println("Pet deductible factor is: " + deductible);
    }

    private void showCopayFactor() {
        excel.openWorkSheet("Copay");
        int i = excel.finedInColumn(0, policy.getState(), 3, 56);
        if (i == -1) {
            System.out.println("Cannot fined state in sheet.");
            return;
        }
        int k = -1;
        for (int j = 0; j < copays.length; j++) {
            if (copays[j].toLowerCase().equals(policy.getCopay())) {
                k = j;
                break;
            }
        }
        if (k == -1) {
            System.out.println("Bad copay.");
            return;
        }
        int j = (policy.getPetType().equals(PetType.DOG) ? copays.length : 0) + k + 1;
        String deductible = excel.readCell(i, j);
        System.out.println("Pet copay factor is: " + deductible);
    }

    private void showAnnualDeductible() {
        excel.openWorkSheet("Annual Deductible");
        int i = excel.finedInColumn(0, policy.getState(), 1, policy.getPetType().toString(), 3, 108);
        if (i == -1) {
            System.out.println("Cannot fined state or pet in sheet.");
            return;
        }
        String deductible = excel.readCell(i, 2);
        System.out.println("Pet annual deductible is: " + deductible);
    }

    private void showAgeAtInceptionFactor() {
        excel.openWorkSheet("Age-At-Inception");
        int i = excel.finedInColumn(0, policy.getState(), 1, policy.getPetType().toString(), 3, 2655);
        if (i == -1) {
            System.out.println("Cannot fined state or pet in sheet.");
            return;
        }
        String inception = excel.readCell(i + policy.getAge(), 4);
        System.out.println("Pet Age At Inception is: " + inception);
    }

    public static void disableWarning() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe u = (Unsafe) theUnsafe.get(null);

            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception e) {
            // ignore
        }
    }

    public static void main(String[] args) {
        disableWarning();
        CRF calculator = new CRF();
        System.out.println();
        calculator.showBaseRate();
        calculator.showAgeFactor();
        calculator.showAreaLookup();
        calculator.showDeductibleFactor();
        calculator.showCopayFactor();
        calculator.showAnnualDeductible();
        calculator.showAgeAtInceptionFactor();
    }
}
//mvn clean compile assembly:single