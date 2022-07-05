package mucom88.common;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.IntStream;

import vavi.util.Debug;


/**
 * not meet the specifications of {@link List}.
 */
public class AutoExtendList<T> implements List<T> {

    private List<T> buf = new ArrayList<>();

    Class<T> clazz;
    public AutoExtendList(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T set(int adr, T d) {
        if (adr >= buf.size()) {
            int size = adr + 1;
            for (int i = buf.size(); i < size; i++) {
//Debug.println("add: " + d.getClass().getName());
                if (clazz == Integer.TYPE) {
                    buf.add((T) Integer.valueOf(0));
                } else if (clazz == Byte.TYPE) {
                    buf.add((T) Byte.valueOf((byte) 0));
                } else {
                    buf.add(null);
                }
            }
        }
        return buf.set(adr, d);
    }

    @Override
    public void add(int index, T element) {
        buf.add(index, element);
    }

    @Override
    public T remove(int index) {
        return buf.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return buf.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return buf.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return buf.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return buf.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return buf.subList(fromIndex, toIndex);
    }

    @Override
    public T get(int adr) {
        if (adr >= buf.size()) {
            if (clazz == Integer.TYPE || clazz == Integer.class) {
                return (T) Integer.valueOf(0);
            } else if (clazz == Byte.TYPE || clazz == Byte.class) {
                return (T) Byte.valueOf((byte) 0);
            } else {
                return null;
            }
        }
        return buf.get(adr);
    }

    @Override
    public int size() {
        return buf.size();
    }

    @Override
    public boolean isEmpty() {
        return buf.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return buf.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return buf.iterator();
    }

    @Override
    public Object[] toArray() {
        return buf.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return buf.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return buf.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return buf.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return buf.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return buf.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return buf.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return buf.retainAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return buf.retainAll(c);
    }

    @Override
    public void clear() {
        buf.clear();
    }
}
