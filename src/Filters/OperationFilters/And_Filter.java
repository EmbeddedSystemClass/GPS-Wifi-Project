package Filters.OperationFilters;

import Filters.Base.AbstractFilter;

public class And_Filter extends AbstractFilter {
    private AbstractFilter filter1, filter2;

    public And_Filter(AbstractFilter filter1, AbstractFilter filter2) {
        this.filter1 = filter1;
        this.filter2 = filter2;
    }

    @Override
    public boolean test(Object o) {
        return filter1.test(o) && filter2.test(o);
    }
}
