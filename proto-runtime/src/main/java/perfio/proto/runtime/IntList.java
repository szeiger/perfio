package perfio.proto.runtime;

import java.util.Arrays;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;

public class IntList {
  public IntList(int[] data, int len) {
    this.data = data;
    this.len = len;
  }

  public IntList() { this(new int[8], 0); }

  int[] data;
  int len;

  public int getInt(int idx) {
    Objects.checkIndex(idx, len);
    return data[idx];
  }

  public void add(int value) {
    if(len >= data.length) data = Arrays.copyOf(data, len*2);
    data[len++] = value;
  }

  public int size() { return len; }

  public boolean isEmpty() { return len == 0; }

  public int[] copyToArray() { return Arrays.copyOf(data, len); }

  public PrimitiveIterator.OfInt iterator() {
    return new PrimitiveIterator.OfInt() {
      private int pos = 0;

      @Override
      public int nextInt() { return getInt(pos++); }

      @Override
      public boolean hasNext() { return pos < size(); }

      @Override
      public void forEachRemaining(IntConsumer action) {
        int l = size();
        for(int i = pos; i < l; i++) action.accept(getInt(i));
      }
    };
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof IntList l) {
      if(len != l.len) return false;
      for(int i=0; i<len; i++)
        if(data[i] != l.data[i]) return false;
      return true;
    } else return false;
  }

  @Override
  public int hashCode() {
    int h = 0;
    for(int i=0; i<len; i++) h += data[i];
    return h;
  }

  public static final IntList EMPTY = new IntList(new int[0], 0) {
    @Override
    public void add(int value) { throw new UnsupportedOperationException(); }
  };
}
