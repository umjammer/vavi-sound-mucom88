﻿import System;
import System.Collections.Generic;
import System.Text;

package mucomDotNET.Compiler;
    public class AutoExtendList_<T>
    {
        private List<T> buf;

        public int Count
        {
            get
            {
                return buf == null ? 0 : buf.Count;
            }
        }

        public AutoExtendList_()
        {
            buf = new ArrayList<T>();
        }

        public void Set(int adr, T d)
        {
            if (adr >= buf.Count)
            {
                int size = adr + 1;
                for (int i = buf.Count; i < size; i++)
                    buf.Add(default(T));
            }
            buf[adr] = d;
        }

        public T Get(int adr)
        {
            if (adr >= buf.Count) return default(T);
            return buf[adr];
        }

        public T[] GetByteArray()
        {
            return buf.ToArray();
        }

        public void Clear()
        {
            buf = new ArrayList<T>();
        }
    }
}