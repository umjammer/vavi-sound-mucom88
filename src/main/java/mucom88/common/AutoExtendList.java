package mucom88.common;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;


public class AutoExtendList<T> {

    /** @see "https://stackoverflow.com/a/530289" */
    class GenSet<E> {

        private E[] a;

        public GenSet(Class<E> c, int s) {
            // Use Array native method to create array
            // of a type only known at run time
            @SuppressWarnings("unchecked")
            final E[] a = (E[]) Array.newInstance(c, s);
            this.a = a;
        }

        void addAll(List<E> c) {
            IntStream.range(0, a.length).forEach(i -> {a[i] = c.get(i);});
        }

        E[] getRaw() {
            return a;
        }

        E get(int i) {
            return a[i];
        }
    }

    private List<T> buf;

    public int getCount() {
        return buf == null ? 0 : buf.size();
    }

    public AutoExtendList() {
        buf = new ArrayList<>();
    }

    public void Set(int adr, T d) {
        if (adr >= buf.size()) {
            int size = adr + 1;
            for (int i = buf.size(); i < size; i++)
                buf.add(null);
        }
        buf.set(adr, d);
    }

    public T Get(int adr) {
        if (adr >= buf.size()) return null;
        return buf.get(adr);
    }

    public T[] GetByteArray(Class<T> c) {
        GenSet<T> s = new GenSet<T>(c, buf.size());
        s.addAll(buf);
        return s.getRaw();
    }

    public void Clear() {
        buf = new ArrayList<T>();
    }
}
