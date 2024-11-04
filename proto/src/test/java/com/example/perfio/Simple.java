// Generated by perfio-proto. Do not edit!
// source: proto/src/test/proto/simple.proto

package com.example.perfio;

public final class Simple {
  private Simple() {}

  public static final class Nested {

    private int _a;
    public int getA() { return _a; }
    public com.example.perfio.Simple.Nested setA(int value) { this._a = value; return this; }
    public boolean hasA() { return _a != 0; }

    private int _b;
    public int getB() { return _b; }
    public com.example.perfio.Simple.Nested setB(int value) { this._b = value; return this; }
    public boolean hasB() { return _b != 0; }

    public static com.example.perfio.Simple.Nested parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new com.example.perfio.Simple.Nested();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, com.example.perfio.Simple.Nested base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 8 -> base.setA(perfio.proto.runtime.Runtime.parseInt32(in));
          case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setA(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          case 16 -> base.setB(perfio.proto.runtime.Runtime.parseInt32(in));
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setB(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 2 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasA()) { perfio.proto.runtime.Runtime.writeInt32(out, 8); perfio.proto.runtime.Runtime.writeInt32(out, this._a); }
      if(this.hasB()) { perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeInt32(out, this._b); }
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof com.example.perfio.Simple.Nested m) {
        if(this._a != m._a) return false;
        if(this._b != m._b) return false;
        return true;
      } else return false;
    }
  }

  public static final class SimpleMessage {
    public enum E {
      V1(0),
      V2(1),
      V3(2),
      UNRECOGNIZED(-1);
      public final int number;
      E(int number) { this.number = number; }
      public static E valueOf(int number) {
        return switch(number) {
          case 0 -> V1;
          case 1 -> V2;
          case 2 -> V3;
          default -> UNRECOGNIZED;
        };
      }
    }

    public static final class File {

      private int flags0;

      private java.lang.String _name = "";
      public java.lang.String getName() { return _name; }
      public com.example.perfio.Simple.SimpleMessage.File setName(java.lang.String value) { this._name = value; this.flags0 |= 1; return this; }
      public boolean hasName() { return (this.flags0 & 1) != 0; }

      private java.lang.String _insertionPoint = "";
      public java.lang.String getInsertionPoint() { return _insertionPoint; }
      public com.example.perfio.Simple.SimpleMessage.File setInsertionPoint(java.lang.String value) { this._insertionPoint = value; this.flags0 |= 2; return this; }
      public boolean hasInsertionPoint() { return (this.flags0 & 2) != 0; }

      private java.lang.String _content = "";
      public java.lang.String getContent() { return _content; }
      public com.example.perfio.Simple.SimpleMessage.File setContent(java.lang.String value) { this._content = value; this.flags0 |= 4; return this; }
      public boolean hasContent() { return (this.flags0 & 4) != 0; }

      public static com.example.perfio.Simple.SimpleMessage.File parseFrom(perfio.BufferedInput in) throws java.io.IOException {
        var m = new com.example.perfio.Simple.SimpleMessage.File();
        parseFrom(in, m);
        return m;
      }
      public static void parseFrom(perfio.BufferedInput in, com.example.perfio.Simple.SimpleMessage.File base) throws java.io.IOException {
        while(in.hasMore()) {
          int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
          switch(tag) {
            case 10 -> base.setName(perfio.proto.runtime.Runtime.parseString(in));
            case 18 -> base.setInsertionPoint(perfio.proto.runtime.Runtime.parseString(in));
            case 122 -> base.setContent(perfio.proto.runtime.Runtime.parseString(in));
            default -> parseOther(in, tag);
          }
        }
      }
      private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
        int wt = tag & 7;
        int field = tag >>> 3;
        switch(field) {
          case 1, 2, 15 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
          default -> perfio.proto.runtime.Runtime.skip(in, wt);
        }
      }

      public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
        if(this.hasName()) { perfio.proto.runtime.Runtime.writeInt32(out, 10); perfio.proto.runtime.Runtime.writeString(out, this._name); }
        if(this.hasInsertionPoint()) { perfio.proto.runtime.Runtime.writeInt32(out, 18); perfio.proto.runtime.Runtime.writeString(out, this._insertionPoint); }
        if(this.hasContent()) { perfio.proto.runtime.Runtime.writeInt32(out, 122); perfio.proto.runtime.Runtime.writeString(out, this._content); }
      }

      public boolean equals(java.lang.Object o) {
        if(o == this) return true;
        else if(o instanceof com.example.perfio.Simple.SimpleMessage.File m) {
          if(this.flags0 != m.flags0) return false;
          if(this.hasName() && !this._name.equals(m._name)) return false;
          if(this.hasInsertionPoint() && !this._insertionPoint.equals(m._insertionPoint)) return false;
          if(this.hasContent() && !this._content.equals(m._content)) return false;
          return true;
        } else return false;
      }
    }

    private int flags0;
    private int oneof_testOneof;

    private int _i32;
    public int getI32() { return _i32; }
    public com.example.perfio.Simple.SimpleMessage setI32(int value) { this._i32 = value; return this; }
    public boolean hasI32() { return _i32 != 0; }

    private long _i64;
    public long getI64() { return _i64; }
    public com.example.perfio.Simple.SimpleMessage setI64(long value) { this._i64 = value; return this; }
    public boolean hasI64() { return _i64 != 0; }

    private com.example.perfio.Simple.SimpleMessage.E _e = com.example.perfio.Simple.SimpleMessage.E.V1;
    public com.example.perfio.Simple.SimpleMessage.E getE() { return _e; }
    public com.example.perfio.Simple.SimpleMessage setE(com.example.perfio.Simple.SimpleMessage.E value) { this._e = value; return this; }
    public boolean hasE() { return _e.number != 0; }

    private com.example.perfio.Simple.Nested _nested;
    public com.example.perfio.Simple.Nested getNested() {
      if(_nested == null) _nested = new com.example.perfio.Simple.Nested();
      return _nested;
    }
    public com.example.perfio.Simple.SimpleMessage setNested(com.example.perfio.Simple.Nested value) { this._nested = value; this.flags0 |= 1; return this; }
    public boolean hasNested() { return (this.flags0 & 1) != 0; }

    private java.util.List<java.lang.Integer> _ints = java.util.List.of();
    public java.util.List<java.lang.Integer> getIntsList() { return _ints; }
    public void setIntsList(java.util.List<java.lang.Integer> value) { this._ints = value; }
    public void addInts(int value) {
      if(this._ints == null || (java.util.List)this._ints == java.util.List.of()) this.setIntsList(new java.util.ArrayList<>());
      this._ints.add(value);
    }
    public boolean hasInts() { return !_ints.isEmpty(); }

    private java.util.List<com.example.perfio.Simple.Nested> _rNested = java.util.List.of();
    public java.util.List<com.example.perfio.Simple.Nested> getRNestedList() { return _rNested; }
    public void setRNestedList(java.util.List<com.example.perfio.Simple.Nested> value) { this._rNested = value; }
    public void addRNested(com.example.perfio.Simple.Nested value) {
      if(this._rNested == null || (java.util.List)this._rNested == java.util.List.of()) this.setRNestedList(new java.util.ArrayList<>());
      this._rNested.add(value);
    }
    public boolean hasRNested() { return !_rNested.isEmpty(); }

    private java.lang.String _oneofString = "";
    public java.lang.String getOneofString() { return _oneofString; }
    public com.example.perfio.Simple.SimpleMessage setOneofString(java.lang.String value) { this._oneofString = value; this.oneof_testOneof = 1;
      this._oneofInt = 0;
      return this;
    }
    public boolean hasOneofString() { return this.oneof_testOneof == 1; }

    private int _oneofInt;
    public int getOneofInt() { return _oneofInt; }
    public com.example.perfio.Simple.SimpleMessage setOneofInt(int value) { this._oneofInt = value; this.oneof_testOneof = 2;
      this._oneofString = "";
      return this;
    }
    public boolean hasOneofInt() { return this.oneof_testOneof == 2; }

    private java.util.List<com.example.perfio.Simple.SimpleMessage.File> _file = java.util.List.of();
    public java.util.List<com.example.perfio.Simple.SimpleMessage.File> getFileList() { return _file; }
    public void setFileList(java.util.List<com.example.perfio.Simple.SimpleMessage.File> value) { this._file = value; }
    public void addFile(com.example.perfio.Simple.SimpleMessage.File value) {
      if(this._file == null || (java.util.List)this._file == java.util.List.of()) this.setFileList(new java.util.ArrayList<>());
      this._file.add(value);
    }
    public boolean hasFile() { return !_file.isEmpty(); }

    public static com.example.perfio.Simple.SimpleMessage parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new com.example.perfio.Simple.SimpleMessage();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, com.example.perfio.Simple.SimpleMessage base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 8 -> base.setI32(perfio.proto.runtime.Runtime.parseInt32(in));
          case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setI32(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          case 16 -> base.setI64(perfio.proto.runtime.Runtime.parseInt64(in));
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setI64(perfio.proto.runtime.Runtime.parseInt64(in2));; in2.close(); }
          case 24 -> base.setE(com.example.perfio.Simple.SimpleMessage.E.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 34 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getNested(); com.example.perfio.Simple.Nested.parseFrom(in2, m); in2.close(); base.setNested(m); }
          case 40 -> base.addInts(perfio.proto.runtime.Runtime.parseInt32(in));
          case 42 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.addInts(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          case 66 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addRNested(com.example.perfio.Simple.Nested.parseFrom(in2)); in2.close(); }
          case 50 -> base.setOneofString(perfio.proto.runtime.Runtime.parseString(in));
          case 56 -> base.setOneofInt(perfio.proto.runtime.Runtime.parseInt32(in));
          case 58 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setOneofInt(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          case 122 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addFile(com.example.perfio.Simple.SimpleMessage.File.parseFrom(in2)); in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 2, 3, 4, 5, 8, 6, 7, 15 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasI32()) { perfio.proto.runtime.Runtime.writeInt32(out, 8); perfio.proto.runtime.Runtime.writeInt32(out, this._i32); }
      if(this.hasI64()) { perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeInt64(out, this._i64); }
      if(this.hasE()) { perfio.proto.runtime.Runtime.writeInt32(out, 24); perfio.proto.runtime.Runtime.writeInt32(out, this._e.number); }
      if(this.hasNested()) { perfio.proto.runtime.Runtime.writeInt32(out, 34); var out2 = out.defer(); this._nested.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      { var it = this._ints.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 40); perfio.proto.runtime.Runtime.writeInt32(out, v); }}
      { var it = this._rNested.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 66); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      if(this.hasOneofString()) { perfio.proto.runtime.Runtime.writeInt32(out, 50); perfio.proto.runtime.Runtime.writeString(out, this._oneofString); }
      if(this.hasOneofInt()) { perfio.proto.runtime.Runtime.writeInt32(out, 56); perfio.proto.runtime.Runtime.writeInt32(out, this._oneofInt); }
      { var it = this._file.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 122); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof com.example.perfio.Simple.SimpleMessage m) {
        if(this.flags0 != m.flags0) return false;
        if(this.oneof_testOneof != m.oneof_testOneof) return false;
        if(this._i32 != m._i32) return false;
        if(this._i64 != m._i64) return false;
        if(this._e != m._e) return false;
        if(this.hasNested() && !this._nested.equals(m._nested)) return false;
        if(!this._ints.equals(m._ints)) return false;
        if(!this._rNested.equals(m._rNested)) return false;
        if(this.hasOneofString() && !this._oneofString.equals(m._oneofString)) return false;
        if(this._oneofInt != m._oneofInt) return false;
        if(!this._file.equals(m._file)) return false;
        return true;
      } else return false;
    }
  }
}
