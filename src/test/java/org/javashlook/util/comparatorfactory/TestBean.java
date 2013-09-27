package org.javashlook.util.comparatorfactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestBean {

    private Integer i;
    private String s;
    private Date d;

    public TestBean() {}

    public TestBean(Integer i, String s, Date d) {
        this.i = i;
        this.s = s;
        this.d = d;
    }

    public TestBean(Integer i, String s, String d) {
        this(i, s, parseDate(d));
    }

    private static Date parseDate(String d) {
        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yyyy");
        try {
            return f.parse(d);
        }
        catch (ParseException e) {
            return null;
        }
    }

    protected Date getD() {
        return d;
    }

    protected void setD(Date d) {
        this.d = d;
    }

    protected Integer getI() {
        return i;
    }

    protected void setI(Integer i) {
        this.i = i;
    }

    protected String getS() {
        return s;
    }

    protected void setS(String s) {
        this.s = s;
    }

}
