# comparator-factory

Creating comparators with code generation.

## Example

### Bean

```java
public class TestBean {

    private Integer i;
    private String s;
    private Date d;

	// getters are required, yet need not be required

	protected Date getD() {
        return d;
    }

    protected Integer getI() {
        return i;
    }

    protected String getS() {
        return s;
    }
    
    // ...
}
```

### Using factory

```java
Comparator<TestBean> c = ComparatorFactory.forClass(TestBean.class)
		.addProperty("i", Integer.class, false)
		.addProperty("s", String.class, true)
		.addProperty("d", Date.class, false)
		.generate();
```

This will generate a class on-the-fly, with logic equivalent to:

```java
class TestBeanComparator_$i_s_d implements Comparator<TestBean> {

	public int compare(TestBean b1, TestBean b2) {
		int c;
		
		c = -1 * ComparatorHelper.compare(b1.getI(), b2.getI());
		if (c != null) {
			return c;
		}	
		
		c = ComparatorHelper.compare(b1.getS(), b2.getS());
		if (c != null) {
			return c;
		}	
	
		c = -1 * ComparatorHelper.compare(b1.getD(), b2.getD());
		return c;
	}
}
```

## TODO

* Checks if properties themselves are ``Comparable``?
* Nested properties and/or comparators.
