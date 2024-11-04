package perfio.protoapi;

public final class DescriptorProtos {
  private DescriptorProtos() {}

  public enum Edition {
    EDITION_UNKNOWN(0),
    EDITION_LEGACY(900),
    EDITION_PROTO2(998),
    EDITION_PROTO3(999),
    EDITION_2023(1000),
    EDITION_2024(1001),
    EDITION_1_TEST_ONLY(1),
    EDITION_2_TEST_ONLY(2),
    EDITION_99997_TEST_ONLY(99997),
    EDITION_99998_TEST_ONLY(99998),
    EDITION_99999_TEST_ONLY(99999),
    EDITION_MAX(2147483647),
    UNRECOGNIZED(-1);
    public final int number;
    Edition(int number) { this.number = number; }
    public static Edition valueOf(int number) {
      return switch(number) {
        case 0 -> EDITION_UNKNOWN;
        case 900 -> EDITION_LEGACY;
        case 998 -> EDITION_PROTO2;
        case 999 -> EDITION_PROTO3;
        case 1000 -> EDITION_2023;
        case 1001 -> EDITION_2024;
        case 1 -> EDITION_1_TEST_ONLY;
        case 2 -> EDITION_2_TEST_ONLY;
        case 99997 -> EDITION_99997_TEST_ONLY;
        case 99998 -> EDITION_99998_TEST_ONLY;
        case 99999 -> EDITION_99999_TEST_ONLY;
        case 2147483647 -> EDITION_MAX;
        default -> UNRECOGNIZED;
      };
    }
  }

  public static final class FileDescriptorSet {

    private java.util.List<perfio.protoapi.DescriptorProtos.FileDescriptorProto> _file = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.FileDescriptorProto> getFileList() { return _file; }
    public void setFileList(java.util.List<perfio.protoapi.DescriptorProtos.FileDescriptorProto> value) { this._file = value; }
    public void addFile(perfio.protoapi.DescriptorProtos.FileDescriptorProto value) {
      if(this._file == null || (java.util.List)this._file == java.util.List.of()) this.setFileList(new java.util.ArrayList<>());
      this._file.add(value);
    }
    public boolean hasFile() { return !_file.isEmpty(); }

    public static perfio.protoapi.DescriptorProtos.FileDescriptorSet parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.FileDescriptorSet();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.FileDescriptorSet base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addFile(perfio.protoapi.DescriptorProtos.FileDescriptorProto.parseFrom(in2)); in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      { var it = this._file.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 10); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.FileDescriptorSet m) {
        if(!this._file.equals(m._file)) return false;
        return true;
      } else return false;
    }
  }

  public static final class FileDescriptorProto {

    private int flags0;

    private java.lang.String _name = "";
    public java.lang.String getName() { return _name; }
    public perfio.protoapi.DescriptorProtos.FileDescriptorProto setName(java.lang.String value) { this._name = value; this.flags0 |= 1; return this; }
    public boolean hasName() { return (this.flags0 & 1) != 0; }

    private java.lang.String _package = "";
    public java.lang.String getPackage() { return _package; }
    public perfio.protoapi.DescriptorProtos.FileDescriptorProto setPackage(java.lang.String value) { this._package = value; this.flags0 |= 2; return this; }
    public boolean hasPackage() { return (this.flags0 & 2) != 0; }

    private java.util.List<java.lang.String> _dependency = java.util.List.of();
    public java.util.List<java.lang.String> getDependencyList() { return _dependency; }
    public void setDependencyList(java.util.List<java.lang.String> value) { this._dependency = value; }
    public void addDependency(java.lang.String value) {
      if(this._dependency == null || (java.util.List)this._dependency == java.util.List.of()) this.setDependencyList(new java.util.ArrayList<>());
      this._dependency.add(value);
    }
    public boolean hasDependency() { return !_dependency.isEmpty(); }

    private java.util.List<java.lang.Integer> _publicDependency = java.util.List.of();
    public java.util.List<java.lang.Integer> getPublicDependencyList() { return _publicDependency; }
    public void setPublicDependencyList(java.util.List<java.lang.Integer> value) { this._publicDependency = value; }
    public void addPublicDependency(int value) {
      if(this._publicDependency == null || (java.util.List)this._publicDependency == java.util.List.of()) this.setPublicDependencyList(new java.util.ArrayList<>());
      this._publicDependency.add(value);
    }
    public boolean hasPublicDependency() { return !_publicDependency.isEmpty(); }

    private java.util.List<java.lang.Integer> _weakDependency = java.util.List.of();
    public java.util.List<java.lang.Integer> getWeakDependencyList() { return _weakDependency; }
    public void setWeakDependencyList(java.util.List<java.lang.Integer> value) { this._weakDependency = value; }
    public void addWeakDependency(int value) {
      if(this._weakDependency == null || (java.util.List)this._weakDependency == java.util.List.of()) this.setWeakDependencyList(new java.util.ArrayList<>());
      this._weakDependency.add(value);
    }
    public boolean hasWeakDependency() { return !_weakDependency.isEmpty(); }

    private java.util.List<perfio.protoapi.DescriptorProtos.DescriptorProto> _messageType = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.DescriptorProto> getMessageTypeList() { return _messageType; }
    public void setMessageTypeList(java.util.List<perfio.protoapi.DescriptorProtos.DescriptorProto> value) { this._messageType = value; }
    public void addMessageType(perfio.protoapi.DescriptorProtos.DescriptorProto value) {
      if(this._messageType == null || (java.util.List)this._messageType == java.util.List.of()) this.setMessageTypeList(new java.util.ArrayList<>());
      this._messageType.add(value);
    }
    public boolean hasMessageType() { return !_messageType.isEmpty(); }

    private java.util.List<perfio.protoapi.DescriptorProtos.EnumDescriptorProto> _enumType = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.EnumDescriptorProto> getEnumTypeList() { return _enumType; }
    public void setEnumTypeList(java.util.List<perfio.protoapi.DescriptorProtos.EnumDescriptorProto> value) { this._enumType = value; }
    public void addEnumType(perfio.protoapi.DescriptorProtos.EnumDescriptorProto value) {
      if(this._enumType == null || (java.util.List)this._enumType == java.util.List.of()) this.setEnumTypeList(new java.util.ArrayList<>());
      this._enumType.add(value);
    }
    public boolean hasEnumType() { return !_enumType.isEmpty(); }

    private java.util.List<perfio.protoapi.DescriptorProtos.ServiceDescriptorProto> _service = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.ServiceDescriptorProto> getServiceList() { return _service; }
    public void setServiceList(java.util.List<perfio.protoapi.DescriptorProtos.ServiceDescriptorProto> value) { this._service = value; }
    public void addService(perfio.protoapi.DescriptorProtos.ServiceDescriptorProto value) {
      if(this._service == null || (java.util.List)this._service == java.util.List.of()) this.setServiceList(new java.util.ArrayList<>());
      this._service.add(value);
    }
    public boolean hasService() { return !_service.isEmpty(); }

    private java.util.List<perfio.protoapi.DescriptorProtos.FieldDescriptorProto> _extension = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.FieldDescriptorProto> getExtensionList() { return _extension; }
    public void setExtensionList(java.util.List<perfio.protoapi.DescriptorProtos.FieldDescriptorProto> value) { this._extension = value; }
    public void addExtension(perfio.protoapi.DescriptorProtos.FieldDescriptorProto value) {
      if(this._extension == null || (java.util.List)this._extension == java.util.List.of()) this.setExtensionList(new java.util.ArrayList<>());
      this._extension.add(value);
    }
    public boolean hasExtension() { return !_extension.isEmpty(); }

    private perfio.protoapi.DescriptorProtos.FileOptions _options;
    public perfio.protoapi.DescriptorProtos.FileOptions getOptions() {
      if(_options == null) _options = new perfio.protoapi.DescriptorProtos.FileOptions();
      return _options;
    }
    public perfio.protoapi.DescriptorProtos.FileDescriptorProto setOptions(perfio.protoapi.DescriptorProtos.FileOptions value) { this._options = value; this.flags0 |= 4; return this; }
    public boolean hasOptions() { return (this.flags0 & 4) != 0; }

    private perfio.protoapi.DescriptorProtos.SourceCodeInfo _sourceCodeInfo;
    public perfio.protoapi.DescriptorProtos.SourceCodeInfo getSourceCodeInfo() {
      if(_sourceCodeInfo == null) _sourceCodeInfo = new perfio.protoapi.DescriptorProtos.SourceCodeInfo();
      return _sourceCodeInfo;
    }
    public perfio.protoapi.DescriptorProtos.FileDescriptorProto setSourceCodeInfo(perfio.protoapi.DescriptorProtos.SourceCodeInfo value) { this._sourceCodeInfo = value; this.flags0 |= 8; return this; }
    public boolean hasSourceCodeInfo() { return (this.flags0 & 8) != 0; }

    private java.lang.String _syntax = "";
    public java.lang.String getSyntax() { return _syntax; }
    public perfio.protoapi.DescriptorProtos.FileDescriptorProto setSyntax(java.lang.String value) { this._syntax = value; this.flags0 |= 16; return this; }
    public boolean hasSyntax() { return (this.flags0 & 16) != 0; }

    private perfio.protoapi.DescriptorProtos.Edition _edition = perfio.protoapi.DescriptorProtos.Edition.EDITION_UNKNOWN;
    public perfio.protoapi.DescriptorProtos.Edition getEdition() { return _edition; }
    public perfio.protoapi.DescriptorProtos.FileDescriptorProto setEdition(perfio.protoapi.DescriptorProtos.Edition value) { this._edition = value; this.flags0 |= 32; return this; }
    public boolean hasEdition() { return (this.flags0 & 32) != 0; }

    public static perfio.protoapi.DescriptorProtos.FileDescriptorProto parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.FileDescriptorProto();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.FileDescriptorProto base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> base.setName(perfio.proto.runtime.Runtime.parseString(in));
          case 18 -> base.setPackage(perfio.proto.runtime.Runtime.parseString(in));
          case 26 -> base.addDependency(perfio.proto.runtime.Runtime.parseString(in));
          case 80 -> base.addPublicDependency(perfio.proto.runtime.Runtime.parseInt32(in));
          case 82 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.addPublicDependency(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          case 88 -> base.addWeakDependency(perfio.proto.runtime.Runtime.parseInt32(in));
          case 90 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.addWeakDependency(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          case 34 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addMessageType(perfio.protoapi.DescriptorProtos.DescriptorProto.parseFrom(in2)); in2.close(); }
          case 42 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addEnumType(perfio.protoapi.DescriptorProtos.EnumDescriptorProto.parseFrom(in2)); in2.close(); }
          case 50 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addService(perfio.protoapi.DescriptorProtos.ServiceDescriptorProto.parseFrom(in2)); in2.close(); }
          case 58 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addExtension(perfio.protoapi.DescriptorProtos.FieldDescriptorProto.parseFrom(in2)); in2.close(); }
          case 66 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getOptions(); perfio.protoapi.DescriptorProtos.FileOptions.parseFrom(in2, m); in2.close(); base.setOptions(m); }
          case 74 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getSourceCodeInfo(); perfio.protoapi.DescriptorProtos.SourceCodeInfo.parseFrom(in2, m); in2.close(); base.setSourceCodeInfo(m); }
          case 98 -> base.setSyntax(perfio.proto.runtime.Runtime.parseString(in));
          case 112 -> base.setEdition(perfio.protoapi.DescriptorProtos.Edition.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 2, 3, 10, 11, 4, 5, 6, 7, 8, 9, 12, 14 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasName()) { perfio.proto.runtime.Runtime.writeInt32(out, 10); perfio.proto.runtime.Runtime.writeString(out, this._name); }
      if(this.hasPackage()) { perfio.proto.runtime.Runtime.writeInt32(out, 18); perfio.proto.runtime.Runtime.writeString(out, this._package); }
      { var it = this._dependency.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 26); perfio.proto.runtime.Runtime.writeString(out, v); }}
      { var it = this._publicDependency.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 80); perfio.proto.runtime.Runtime.writeInt32(out, v); }}
      { var it = this._weakDependency.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 88); perfio.proto.runtime.Runtime.writeInt32(out, v); }}
      { var it = this._messageType.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 34); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      { var it = this._enumType.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 42); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      { var it = this._service.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 50); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      { var it = this._extension.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 58); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      if(this.hasOptions()) { perfio.proto.runtime.Runtime.writeInt32(out, 66); var out2 = out.defer(); this._options.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      if(this.hasSourceCodeInfo()) { perfio.proto.runtime.Runtime.writeInt32(out, 74); var out2 = out.defer(); this._sourceCodeInfo.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      if(this.hasSyntax()) { perfio.proto.runtime.Runtime.writeInt32(out, 98); perfio.proto.runtime.Runtime.writeString(out, this._syntax); }
      if(this.hasEdition()) { perfio.proto.runtime.Runtime.writeInt32(out, 112); perfio.proto.runtime.Runtime.writeInt32(out, this._edition.number); }
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.FileDescriptorProto m) {
        if(this.flags0 != m.flags0) return false;
        if(this.hasName() && !this._name.equals(m._name)) return false;
        if(this.hasPackage() && !this._package.equals(m._package)) return false;
        if(!this._dependency.equals(m._dependency)) return false;
        if(!this._publicDependency.equals(m._publicDependency)) return false;
        if(!this._weakDependency.equals(m._weakDependency)) return false;
        if(!this._messageType.equals(m._messageType)) return false;
        if(!this._enumType.equals(m._enumType)) return false;
        if(!this._service.equals(m._service)) return false;
        if(!this._extension.equals(m._extension)) return false;
        if(this.hasOptions() && !this._options.equals(m._options)) return false;
        if(this.hasSourceCodeInfo() && !this._sourceCodeInfo.equals(m._sourceCodeInfo)) return false;
        if(this.hasSyntax() && !this._syntax.equals(m._syntax)) return false;
        if(this._edition != m._edition) return false;
        return true;
      } else return false;
    }
  }

  public static final class DescriptorProto {

    public static final class ExtensionRange {

      private int flags0;

      private int _start;
      public int getStart() { return _start; }
      public perfio.protoapi.DescriptorProtos.DescriptorProto.ExtensionRange setStart(int value) { this._start = value; this.flags0 |= 1; return this; }
      public boolean hasStart() { return (this.flags0 & 1) != 0; }

      private int _end;
      public int getEnd() { return _end; }
      public perfio.protoapi.DescriptorProtos.DescriptorProto.ExtensionRange setEnd(int value) { this._end = value; this.flags0 |= 2; return this; }
      public boolean hasEnd() { return (this.flags0 & 2) != 0; }

      private perfio.protoapi.DescriptorProtos.ExtensionRangeOptions _options;
      public perfio.protoapi.DescriptorProtos.ExtensionRangeOptions getOptions() {
        if(_options == null) _options = new perfio.protoapi.DescriptorProtos.ExtensionRangeOptions();
        return _options;
      }
      public perfio.protoapi.DescriptorProtos.DescriptorProto.ExtensionRange setOptions(perfio.protoapi.DescriptorProtos.ExtensionRangeOptions value) { this._options = value; this.flags0 |= 4; return this; }
      public boolean hasOptions() { return (this.flags0 & 4) != 0; }

      public static perfio.protoapi.DescriptorProtos.DescriptorProto.ExtensionRange parseFrom(perfio.BufferedInput in) throws java.io.IOException {
        var m = new perfio.protoapi.DescriptorProtos.DescriptorProto.ExtensionRange();
        parseFrom(in, m);
        return m;
      }
      public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.DescriptorProto.ExtensionRange base) throws java.io.IOException {
        while(in.hasMore()) {
          int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
          switch(tag) {
            case 8 -> base.setStart(perfio.proto.runtime.Runtime.parseInt32(in));
            case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setStart(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
            case 16 -> base.setEnd(perfio.proto.runtime.Runtime.parseInt32(in));
            case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setEnd(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
            case 26 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getOptions(); perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.parseFrom(in2, m); in2.close(); base.setOptions(m); }
            default -> parseOther(in, tag);
          }
        }
      }
      private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
        int wt = tag & 7;
        int field = tag >>> 3;
        switch(field) {
          case 1, 2, 3 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
          default -> perfio.proto.runtime.Runtime.skip(in, wt);
        }
      }

      public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
        if(this.hasStart()) { perfio.proto.runtime.Runtime.writeInt32(out, 8); perfio.proto.runtime.Runtime.writeInt32(out, this._start); }
        if(this.hasEnd()) { perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeInt32(out, this._end); }
        if(this.hasOptions()) { perfio.proto.runtime.Runtime.writeInt32(out, 26); var out2 = out.defer(); this._options.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      }

      public boolean equals(java.lang.Object o) {
        if(o == this) return true;
        else if(o instanceof perfio.protoapi.DescriptorProtos.DescriptorProto.ExtensionRange m) {
          if(this.flags0 != m.flags0) return false;
          if(this._start != m._start) return false;
          if(this._end != m._end) return false;
          if(this.hasOptions() && !this._options.equals(m._options)) return false;
          return true;
        } else return false;
      }
    }

    public static final class ReservedRange {

      private int flags0;

      private int _start;
      public int getStart() { return _start; }
      public perfio.protoapi.DescriptorProtos.DescriptorProto.ReservedRange setStart(int value) { this._start = value; this.flags0 |= 1; return this; }
      public boolean hasStart() { return (this.flags0 & 1) != 0; }

      private int _end;
      public int getEnd() { return _end; }
      public perfio.protoapi.DescriptorProtos.DescriptorProto.ReservedRange setEnd(int value) { this._end = value; this.flags0 |= 2; return this; }
      public boolean hasEnd() { return (this.flags0 & 2) != 0; }

      public static perfio.protoapi.DescriptorProtos.DescriptorProto.ReservedRange parseFrom(perfio.BufferedInput in) throws java.io.IOException {
        var m = new perfio.protoapi.DescriptorProtos.DescriptorProto.ReservedRange();
        parseFrom(in, m);
        return m;
      }
      public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.DescriptorProto.ReservedRange base) throws java.io.IOException {
        while(in.hasMore()) {
          int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
          switch(tag) {
            case 8 -> base.setStart(perfio.proto.runtime.Runtime.parseInt32(in));
            case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setStart(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
            case 16 -> base.setEnd(perfio.proto.runtime.Runtime.parseInt32(in));
            case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setEnd(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
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
        if(this.hasStart()) { perfio.proto.runtime.Runtime.writeInt32(out, 8); perfio.proto.runtime.Runtime.writeInt32(out, this._start); }
        if(this.hasEnd()) { perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeInt32(out, this._end); }
      }

      public boolean equals(java.lang.Object o) {
        if(o == this) return true;
        else if(o instanceof perfio.protoapi.DescriptorProtos.DescriptorProto.ReservedRange m) {
          if(this.flags0 != m.flags0) return false;
          if(this._start != m._start) return false;
          if(this._end != m._end) return false;
          return true;
        } else return false;
      }
    }

    private int flags0;

    private java.lang.String _name = "";
    public java.lang.String getName() { return _name; }
    public perfio.protoapi.DescriptorProtos.DescriptorProto setName(java.lang.String value) { this._name = value; this.flags0 |= 1; return this; }
    public boolean hasName() { return (this.flags0 & 1) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.FieldDescriptorProto> _field = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.FieldDescriptorProto> getFieldList() { return _field; }
    public void setFieldList(java.util.List<perfio.protoapi.DescriptorProtos.FieldDescriptorProto> value) { this._field = value; }
    public void addField(perfio.protoapi.DescriptorProtos.FieldDescriptorProto value) {
      if(this._field == null || (java.util.List)this._field == java.util.List.of()) this.setFieldList(new java.util.ArrayList<>());
      this._field.add(value);
    }
    public boolean hasField() { return !_field.isEmpty(); }

    private java.util.List<perfio.protoapi.DescriptorProtos.FieldDescriptorProto> _extension = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.FieldDescriptorProto> getExtensionList() { return _extension; }
    public void setExtensionList(java.util.List<perfio.protoapi.DescriptorProtos.FieldDescriptorProto> value) { this._extension = value; }
    public void addExtension(perfio.protoapi.DescriptorProtos.FieldDescriptorProto value) {
      if(this._extension == null || (java.util.List)this._extension == java.util.List.of()) this.setExtensionList(new java.util.ArrayList<>());
      this._extension.add(value);
    }
    public boolean hasExtension() { return !_extension.isEmpty(); }

    private java.util.List<perfio.protoapi.DescriptorProtos.DescriptorProto> _nestedType = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.DescriptorProto> getNestedTypeList() { return _nestedType; }
    public void setNestedTypeList(java.util.List<perfio.protoapi.DescriptorProtos.DescriptorProto> value) { this._nestedType = value; }
    public void addNestedType(perfio.protoapi.DescriptorProtos.DescriptorProto value) {
      if(this._nestedType == null || (java.util.List)this._nestedType == java.util.List.of()) this.setNestedTypeList(new java.util.ArrayList<>());
      this._nestedType.add(value);
    }
    public boolean hasNestedType() { return !_nestedType.isEmpty(); }

    private java.util.List<perfio.protoapi.DescriptorProtos.EnumDescriptorProto> _enumType = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.EnumDescriptorProto> getEnumTypeList() { return _enumType; }
    public void setEnumTypeList(java.util.List<perfio.protoapi.DescriptorProtos.EnumDescriptorProto> value) { this._enumType = value; }
    public void addEnumType(perfio.protoapi.DescriptorProtos.EnumDescriptorProto value) {
      if(this._enumType == null || (java.util.List)this._enumType == java.util.List.of()) this.setEnumTypeList(new java.util.ArrayList<>());
      this._enumType.add(value);
    }
    public boolean hasEnumType() { return !_enumType.isEmpty(); }

    private java.util.List<perfio.protoapi.DescriptorProtos.DescriptorProto.ExtensionRange> _extensionRange = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.DescriptorProto.ExtensionRange> getExtensionRangeList() { return _extensionRange; }
    public void setExtensionRangeList(java.util.List<perfio.protoapi.DescriptorProtos.DescriptorProto.ExtensionRange> value) { this._extensionRange = value; }
    public void addExtensionRange(perfio.protoapi.DescriptorProtos.DescriptorProto.ExtensionRange value) {
      if(this._extensionRange == null || (java.util.List)this._extensionRange == java.util.List.of()) this.setExtensionRangeList(new java.util.ArrayList<>());
      this._extensionRange.add(value);
    }
    public boolean hasExtensionRange() { return !_extensionRange.isEmpty(); }

    private java.util.List<perfio.protoapi.DescriptorProtos.OneofDescriptorProto> _oneofDecl = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.OneofDescriptorProto> getOneofDeclList() { return _oneofDecl; }
    public void setOneofDeclList(java.util.List<perfio.protoapi.DescriptorProtos.OneofDescriptorProto> value) { this._oneofDecl = value; }
    public void addOneofDecl(perfio.protoapi.DescriptorProtos.OneofDescriptorProto value) {
      if(this._oneofDecl == null || (java.util.List)this._oneofDecl == java.util.List.of()) this.setOneofDeclList(new java.util.ArrayList<>());
      this._oneofDecl.add(value);
    }
    public boolean hasOneofDecl() { return !_oneofDecl.isEmpty(); }

    private perfio.protoapi.DescriptorProtos.MessageOptions _options;
    public perfio.protoapi.DescriptorProtos.MessageOptions getOptions() {
      if(_options == null) _options = new perfio.protoapi.DescriptorProtos.MessageOptions();
      return _options;
    }
    public perfio.protoapi.DescriptorProtos.DescriptorProto setOptions(perfio.protoapi.DescriptorProtos.MessageOptions value) { this._options = value; this.flags0 |= 2; return this; }
    public boolean hasOptions() { return (this.flags0 & 2) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.DescriptorProto.ReservedRange> _reservedRange = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.DescriptorProto.ReservedRange> getReservedRangeList() { return _reservedRange; }
    public void setReservedRangeList(java.util.List<perfio.protoapi.DescriptorProtos.DescriptorProto.ReservedRange> value) { this._reservedRange = value; }
    public void addReservedRange(perfio.protoapi.DescriptorProtos.DescriptorProto.ReservedRange value) {
      if(this._reservedRange == null || (java.util.List)this._reservedRange == java.util.List.of()) this.setReservedRangeList(new java.util.ArrayList<>());
      this._reservedRange.add(value);
    }
    public boolean hasReservedRange() { return !_reservedRange.isEmpty(); }

    private java.util.List<java.lang.String> _reservedName = java.util.List.of();
    public java.util.List<java.lang.String> getReservedNameList() { return _reservedName; }
    public void setReservedNameList(java.util.List<java.lang.String> value) { this._reservedName = value; }
    public void addReservedName(java.lang.String value) {
      if(this._reservedName == null || (java.util.List)this._reservedName == java.util.List.of()) this.setReservedNameList(new java.util.ArrayList<>());
      this._reservedName.add(value);
    }
    public boolean hasReservedName() { return !_reservedName.isEmpty(); }

    public static perfio.protoapi.DescriptorProtos.DescriptorProto parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.DescriptorProto();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.DescriptorProto base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> base.setName(perfio.proto.runtime.Runtime.parseString(in));
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addField(perfio.protoapi.DescriptorProtos.FieldDescriptorProto.parseFrom(in2)); in2.close(); }
          case 50 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addExtension(perfio.protoapi.DescriptorProtos.FieldDescriptorProto.parseFrom(in2)); in2.close(); }
          case 26 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addNestedType(perfio.protoapi.DescriptorProtos.DescriptorProto.parseFrom(in2)); in2.close(); }
          case 34 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addEnumType(perfio.protoapi.DescriptorProtos.EnumDescriptorProto.parseFrom(in2)); in2.close(); }
          case 42 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addExtensionRange(perfio.protoapi.DescriptorProtos.DescriptorProto.ExtensionRange.parseFrom(in2)); in2.close(); }
          case 66 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addOneofDecl(perfio.protoapi.DescriptorProtos.OneofDescriptorProto.parseFrom(in2)); in2.close(); }
          case 58 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getOptions(); perfio.protoapi.DescriptorProtos.MessageOptions.parseFrom(in2, m); in2.close(); base.setOptions(m); }
          case 74 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addReservedRange(perfio.protoapi.DescriptorProtos.DescriptorProto.ReservedRange.parseFrom(in2)); in2.close(); }
          case 82 -> base.addReservedName(perfio.proto.runtime.Runtime.parseString(in));
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 2, 6, 3, 4, 5, 8, 7, 9, 10 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasName()) { perfio.proto.runtime.Runtime.writeInt32(out, 10); perfio.proto.runtime.Runtime.writeString(out, this._name); }
      { var it = this._field.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 18); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      { var it = this._extension.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 50); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      { var it = this._nestedType.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 26); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      { var it = this._enumType.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 34); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      { var it = this._extensionRange.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 42); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      { var it = this._oneofDecl.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 66); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      if(this.hasOptions()) { perfio.proto.runtime.Runtime.writeInt32(out, 58); var out2 = out.defer(); this._options.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      { var it = this._reservedRange.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 74); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      { var it = this._reservedName.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 82); perfio.proto.runtime.Runtime.writeString(out, v); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.DescriptorProto m) {
        if(this.flags0 != m.flags0) return false;
        if(this.hasName() && !this._name.equals(m._name)) return false;
        if(!this._field.equals(m._field)) return false;
        if(!this._extension.equals(m._extension)) return false;
        if(!this._nestedType.equals(m._nestedType)) return false;
        if(!this._enumType.equals(m._enumType)) return false;
        if(!this._extensionRange.equals(m._extensionRange)) return false;
        if(!this._oneofDecl.equals(m._oneofDecl)) return false;
        if(this.hasOptions() && !this._options.equals(m._options)) return false;
        if(!this._reservedRange.equals(m._reservedRange)) return false;
        if(!this._reservedName.equals(m._reservedName)) return false;
        return true;
      } else return false;
    }
  }

  public static final class ExtensionRangeOptions {
    public enum VerificationState {
      DECLARATION(0),
      UNVERIFIED(1),
      UNRECOGNIZED(-1);
      public final int number;
      VerificationState(int number) { this.number = number; }
      public static VerificationState valueOf(int number) {
        return switch(number) {
          case 0 -> DECLARATION;
          case 1 -> UNVERIFIED;
          default -> UNRECOGNIZED;
        };
      }
    }

    public static final class Declaration {

      private int flags0;

      private int _number;
      public int getNumber() { return _number; }
      public perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.Declaration setNumber(int value) { this._number = value; this.flags0 |= 1; return this; }
      public boolean hasNumber() { return (this.flags0 & 1) != 0; }

      private java.lang.String _fullName = "";
      public java.lang.String getFullName() { return _fullName; }
      public perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.Declaration setFullName(java.lang.String value) { this._fullName = value; this.flags0 |= 2; return this; }
      public boolean hasFullName() { return (this.flags0 & 2) != 0; }

      private java.lang.String _type = "";
      public java.lang.String getType() { return _type; }
      public perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.Declaration setType(java.lang.String value) { this._type = value; this.flags0 |= 4; return this; }
      public boolean hasType() { return (this.flags0 & 4) != 0; }

      private boolean _reserved;
      public boolean getReserved() { return _reserved; }
      public perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.Declaration setReserved(boolean value) { this._reserved = value; this.flags0 |= 8; return this; }
      public boolean hasReserved() { return (this.flags0 & 8) != 0; }

      private boolean _repeated;
      public boolean getRepeated() { return _repeated; }
      public perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.Declaration setRepeated(boolean value) { this._repeated = value; this.flags0 |= 16; return this; }
      public boolean hasRepeated() { return (this.flags0 & 16) != 0; }

      public static perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.Declaration parseFrom(perfio.BufferedInput in) throws java.io.IOException {
        var m = new perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.Declaration();
        parseFrom(in, m);
        return m;
      }
      public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.Declaration base) throws java.io.IOException {
        while(in.hasMore()) {
          int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
          switch(tag) {
            case 8 -> base.setNumber(perfio.proto.runtime.Runtime.parseInt32(in));
            case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setNumber(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
            case 18 -> base.setFullName(perfio.proto.runtime.Runtime.parseString(in));
            case 26 -> base.setType(perfio.proto.runtime.Runtime.parseString(in));
            case 40 -> base.setReserved(perfio.proto.runtime.Runtime.parseBoolean(in));
            case 42 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setReserved(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
            case 48 -> base.setRepeated(perfio.proto.runtime.Runtime.parseBoolean(in));
            case 50 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setRepeated(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
            default -> parseOther(in, tag);
          }
        }
      }
      private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
        int wt = tag & 7;
        int field = tag >>> 3;
        switch(field) {
          case 1, 2, 3, 5, 6 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
          default -> perfio.proto.runtime.Runtime.skip(in, wt);
        }
      }

      public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
        if(this.hasNumber()) { perfio.proto.runtime.Runtime.writeInt32(out, 8); perfio.proto.runtime.Runtime.writeInt32(out, this._number); }
        if(this.hasFullName()) { perfio.proto.runtime.Runtime.writeInt32(out, 18); perfio.proto.runtime.Runtime.writeString(out, this._fullName); }
        if(this.hasType()) { perfio.proto.runtime.Runtime.writeInt32(out, 26); perfio.proto.runtime.Runtime.writeString(out, this._type); }
        if(this.hasReserved()) { perfio.proto.runtime.Runtime.writeInt32(out, 40); perfio.proto.runtime.Runtime.writeBoolean(out, this._reserved); }
        if(this.hasRepeated()) { perfio.proto.runtime.Runtime.writeInt32(out, 48); perfio.proto.runtime.Runtime.writeBoolean(out, this._repeated); }
      }

      public boolean equals(java.lang.Object o) {
        if(o == this) return true;
        else if(o instanceof perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.Declaration m) {
          if(this.flags0 != m.flags0) return false;
          if(this._number != m._number) return false;
          if(this.hasFullName() && !this._fullName.equals(m._fullName)) return false;
          if(this.hasType() && !this._type.equals(m._type)) return false;
          if(this._reserved != m._reserved) return false;
          if(this._repeated != m._repeated) return false;
          return true;
        } else return false;
      }
    }

    private int flags0;

    private java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> _uninterpretedOption = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> getUninterpretedOptionList() { return _uninterpretedOption; }
    public void setUninterpretedOptionList(java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> value) { this._uninterpretedOption = value; }
    public void addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption value) {
      if(this._uninterpretedOption == null || (java.util.List)this._uninterpretedOption == java.util.List.of()) this.setUninterpretedOptionList(new java.util.ArrayList<>());
      this._uninterpretedOption.add(value);
    }
    public boolean hasUninterpretedOption() { return !_uninterpretedOption.isEmpty(); }

    private java.util.List<perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.Declaration> _declaration = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.Declaration> getDeclarationList() { return _declaration; }
    public void setDeclarationList(java.util.List<perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.Declaration> value) { this._declaration = value; }
    public void addDeclaration(perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.Declaration value) {
      if(this._declaration == null || (java.util.List)this._declaration == java.util.List.of()) this.setDeclarationList(new java.util.ArrayList<>());
      this._declaration.add(value);
    }
    public boolean hasDeclaration() { return !_declaration.isEmpty(); }

    private perfio.protoapi.DescriptorProtos.FeatureSet _features;
    public perfio.protoapi.DescriptorProtos.FeatureSet getFeatures() {
      if(_features == null) _features = new perfio.protoapi.DescriptorProtos.FeatureSet();
      return _features;
    }
    public perfio.protoapi.DescriptorProtos.ExtensionRangeOptions setFeatures(perfio.protoapi.DescriptorProtos.FeatureSet value) { this._features = value; this.flags0 |= 1; return this; }
    public boolean hasFeatures() { return (this.flags0 & 1) != 0; }

    private perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.VerificationState _verification = perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.VerificationState.DECLARATION;
    public perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.VerificationState getVerification() { return _verification; }
    public perfio.protoapi.DescriptorProtos.ExtensionRangeOptions setVerification(perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.VerificationState value) { this._verification = value; this.flags0 |= 2; return this; }
    public boolean hasVerification() { return (this.flags0 & 2) != 0; }

    public static perfio.protoapi.DescriptorProtos.ExtensionRangeOptions parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.ExtensionRangeOptions();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.ExtensionRangeOptions base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 7994 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption.parseFrom(in2)); in2.close(); }
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addDeclaration(perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.Declaration.parseFrom(in2)); in2.close(); }
          case 402 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getFeatures(); perfio.protoapi.DescriptorProtos.FeatureSet.parseFrom(in2, m); in2.close(); base.setFeatures(m); }
          case 24 -> base.setVerification(perfio.protoapi.DescriptorProtos.ExtensionRangeOptions.VerificationState.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 999, 2, 50, 3 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      { var it = this._uninterpretedOption.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 7994); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      { var it = this._declaration.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 18); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      if(this.hasFeatures()) { perfio.proto.runtime.Runtime.writeInt32(out, 402); var out2 = out.defer(); this._features.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      if(this.hasVerification()) { perfio.proto.runtime.Runtime.writeInt32(out, 24); perfio.proto.runtime.Runtime.writeInt32(out, this._verification.number); }
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.ExtensionRangeOptions m) {
        if(this.flags0 != m.flags0) return false;
        if(!this._uninterpretedOption.equals(m._uninterpretedOption)) return false;
        if(!this._declaration.equals(m._declaration)) return false;
        if(this.hasFeatures() && !this._features.equals(m._features)) return false;
        if(this._verification != m._verification) return false;
        return true;
      } else return false;
    }
  }

  public static final class FieldDescriptorProto {
    public enum Type {
      TYPE_DOUBLE(1),
      TYPE_FLOAT(2),
      TYPE_INT64(3),
      TYPE_UINT64(4),
      TYPE_INT32(5),
      TYPE_FIXED64(6),
      TYPE_FIXED32(7),
      TYPE_BOOL(8),
      TYPE_STRING(9),
      TYPE_GROUP(10),
      TYPE_MESSAGE(11),
      TYPE_BYTES(12),
      TYPE_UINT32(13),
      TYPE_ENUM(14),
      TYPE_SFIXED32(15),
      TYPE_SFIXED64(16),
      TYPE_SINT32(17),
      TYPE_SINT64(18),
      UNRECOGNIZED(-1);
      public final int number;
      Type(int number) { this.number = number; }
      public static Type valueOf(int number) {
        return switch(number) {
          case 1 -> TYPE_DOUBLE;
          case 2 -> TYPE_FLOAT;
          case 3 -> TYPE_INT64;
          case 4 -> TYPE_UINT64;
          case 5 -> TYPE_INT32;
          case 6 -> TYPE_FIXED64;
          case 7 -> TYPE_FIXED32;
          case 8 -> TYPE_BOOL;
          case 9 -> TYPE_STRING;
          case 10 -> TYPE_GROUP;
          case 11 -> TYPE_MESSAGE;
          case 12 -> TYPE_BYTES;
          case 13 -> TYPE_UINT32;
          case 14 -> TYPE_ENUM;
          case 15 -> TYPE_SFIXED32;
          case 16 -> TYPE_SFIXED64;
          case 17 -> TYPE_SINT32;
          case 18 -> TYPE_SINT64;
          default -> UNRECOGNIZED;
        };
      }
    }
    public enum Label {
      LABEL_OPTIONAL(1),
      LABEL_REPEATED(3),
      LABEL_REQUIRED(2),
      UNRECOGNIZED(-1);
      public final int number;
      Label(int number) { this.number = number; }
      public static Label valueOf(int number) {
        return switch(number) {
          case 1 -> LABEL_OPTIONAL;
          case 3 -> LABEL_REPEATED;
          case 2 -> LABEL_REQUIRED;
          default -> UNRECOGNIZED;
        };
      }
    }

    private int flags0;

    private java.lang.String _name = "";
    public java.lang.String getName() { return _name; }
    public perfio.protoapi.DescriptorProtos.FieldDescriptorProto setName(java.lang.String value) { this._name = value; this.flags0 |= 1; return this; }
    public boolean hasName() { return (this.flags0 & 1) != 0; }

    private int _number;
    public int getNumber() { return _number; }
    public perfio.protoapi.DescriptorProtos.FieldDescriptorProto setNumber(int value) { this._number = value; this.flags0 |= 2; return this; }
    public boolean hasNumber() { return (this.flags0 & 2) != 0; }

    private perfio.protoapi.DescriptorProtos.FieldDescriptorProto.Label _label = perfio.protoapi.DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL;
    public perfio.protoapi.DescriptorProtos.FieldDescriptorProto.Label getLabel() { return _label; }
    public perfio.protoapi.DescriptorProtos.FieldDescriptorProto setLabel(perfio.protoapi.DescriptorProtos.FieldDescriptorProto.Label value) { this._label = value; this.flags0 |= 4; return this; }
    public boolean hasLabel() { return (this.flags0 & 4) != 0; }

    private perfio.protoapi.DescriptorProtos.FieldDescriptorProto.Type _type = perfio.protoapi.DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE;
    public perfio.protoapi.DescriptorProtos.FieldDescriptorProto.Type getType() { return _type; }
    public perfio.protoapi.DescriptorProtos.FieldDescriptorProto setType(perfio.protoapi.DescriptorProtos.FieldDescriptorProto.Type value) { this._type = value; this.flags0 |= 8; return this; }
    public boolean hasType() { return (this.flags0 & 8) != 0; }

    private java.lang.String _typeName = "";
    public java.lang.String getTypeName() { return _typeName; }
    public perfio.protoapi.DescriptorProtos.FieldDescriptorProto setTypeName(java.lang.String value) { this._typeName = value; this.flags0 |= 16; return this; }
    public boolean hasTypeName() { return (this.flags0 & 16) != 0; }

    private java.lang.String _extendee = "";
    public java.lang.String getExtendee() { return _extendee; }
    public perfio.protoapi.DescriptorProtos.FieldDescriptorProto setExtendee(java.lang.String value) { this._extendee = value; this.flags0 |= 32; return this; }
    public boolean hasExtendee() { return (this.flags0 & 32) != 0; }

    private java.lang.String _defaultValue = "";
    public java.lang.String getDefaultValue() { return _defaultValue; }
    public perfio.protoapi.DescriptorProtos.FieldDescriptorProto setDefaultValue(java.lang.String value) { this._defaultValue = value; this.flags0 |= 64; return this; }
    public boolean hasDefaultValue() { return (this.flags0 & 64) != 0; }

    private int _oneofIndex;
    public int getOneofIndex() { return _oneofIndex; }
    public perfio.protoapi.DescriptorProtos.FieldDescriptorProto setOneofIndex(int value) { this._oneofIndex = value; this.flags0 |= 128; return this; }
    public boolean hasOneofIndex() { return (this.flags0 & 128) != 0; }

    private java.lang.String _jsonName = "";
    public java.lang.String getJsonName() { return _jsonName; }
    public perfio.protoapi.DescriptorProtos.FieldDescriptorProto setJsonName(java.lang.String value) { this._jsonName = value; this.flags0 |= 256; return this; }
    public boolean hasJsonName() { return (this.flags0 & 256) != 0; }

    private perfio.protoapi.DescriptorProtos.FieldOptions _options;
    public perfio.protoapi.DescriptorProtos.FieldOptions getOptions() {
      if(_options == null) _options = new perfio.protoapi.DescriptorProtos.FieldOptions();
      return _options;
    }
    public perfio.protoapi.DescriptorProtos.FieldDescriptorProto setOptions(perfio.protoapi.DescriptorProtos.FieldOptions value) { this._options = value; this.flags0 |= 512; return this; }
    public boolean hasOptions() { return (this.flags0 & 512) != 0; }

    private boolean _proto3Optional;
    public boolean getProto3Optional() { return _proto3Optional; }
    public perfio.protoapi.DescriptorProtos.FieldDescriptorProto setProto3Optional(boolean value) { this._proto3Optional = value; this.flags0 |= 1024; return this; }
    public boolean hasProto3Optional() { return (this.flags0 & 1024) != 0; }

    public static perfio.protoapi.DescriptorProtos.FieldDescriptorProto parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.FieldDescriptorProto();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.FieldDescriptorProto base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> base.setName(perfio.proto.runtime.Runtime.parseString(in));
          case 24 -> base.setNumber(perfio.proto.runtime.Runtime.parseInt32(in));
          case 26 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setNumber(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          case 32 -> base.setLabel(perfio.protoapi.DescriptorProtos.FieldDescriptorProto.Label.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 40 -> base.setType(perfio.protoapi.DescriptorProtos.FieldDescriptorProto.Type.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 50 -> base.setTypeName(perfio.proto.runtime.Runtime.parseString(in));
          case 18 -> base.setExtendee(perfio.proto.runtime.Runtime.parseString(in));
          case 58 -> base.setDefaultValue(perfio.proto.runtime.Runtime.parseString(in));
          case 72 -> base.setOneofIndex(perfio.proto.runtime.Runtime.parseInt32(in));
          case 74 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setOneofIndex(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          case 82 -> base.setJsonName(perfio.proto.runtime.Runtime.parseString(in));
          case 66 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getOptions(); perfio.protoapi.DescriptorProtos.FieldOptions.parseFrom(in2, m); in2.close(); base.setOptions(m); }
          case 136 -> base.setProto3Optional(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 138 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setProto3Optional(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 3, 4, 5, 6, 2, 7, 9, 10, 8, 17 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasName()) { perfio.proto.runtime.Runtime.writeInt32(out, 10); perfio.proto.runtime.Runtime.writeString(out, this._name); }
      if(this.hasNumber()) { perfio.proto.runtime.Runtime.writeInt32(out, 24); perfio.proto.runtime.Runtime.writeInt32(out, this._number); }
      if(this.hasLabel()) { perfio.proto.runtime.Runtime.writeInt32(out, 32); perfio.proto.runtime.Runtime.writeInt32(out, this._label.number); }
      if(this.hasType()) { perfio.proto.runtime.Runtime.writeInt32(out, 40); perfio.proto.runtime.Runtime.writeInt32(out, this._type.number); }
      if(this.hasTypeName()) { perfio.proto.runtime.Runtime.writeInt32(out, 50); perfio.proto.runtime.Runtime.writeString(out, this._typeName); }
      if(this.hasExtendee()) { perfio.proto.runtime.Runtime.writeInt32(out, 18); perfio.proto.runtime.Runtime.writeString(out, this._extendee); }
      if(this.hasDefaultValue()) { perfio.proto.runtime.Runtime.writeInt32(out, 58); perfio.proto.runtime.Runtime.writeString(out, this._defaultValue); }
      if(this.hasOneofIndex()) { perfio.proto.runtime.Runtime.writeInt32(out, 72); perfio.proto.runtime.Runtime.writeInt32(out, this._oneofIndex); }
      if(this.hasJsonName()) { perfio.proto.runtime.Runtime.writeInt32(out, 82); perfio.proto.runtime.Runtime.writeString(out, this._jsonName); }
      if(this.hasOptions()) { perfio.proto.runtime.Runtime.writeInt32(out, 66); var out2 = out.defer(); this._options.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      if(this.hasProto3Optional()) { perfio.proto.runtime.Runtime.writeInt32(out, 136); perfio.proto.runtime.Runtime.writeBoolean(out, this._proto3Optional); }
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.FieldDescriptorProto m) {
        if(this.flags0 != m.flags0) return false;
        if(this.hasName() && !this._name.equals(m._name)) return false;
        if(this._number != m._number) return false;
        if(this._label != m._label) return false;
        if(this._type != m._type) return false;
        if(this.hasTypeName() && !this._typeName.equals(m._typeName)) return false;
        if(this.hasExtendee() && !this._extendee.equals(m._extendee)) return false;
        if(this.hasDefaultValue() && !this._defaultValue.equals(m._defaultValue)) return false;
        if(this._oneofIndex != m._oneofIndex) return false;
        if(this.hasJsonName() && !this._jsonName.equals(m._jsonName)) return false;
        if(this.hasOptions() && !this._options.equals(m._options)) return false;
        if(this._proto3Optional != m._proto3Optional) return false;
        return true;
      } else return false;
    }
  }

  public static final class OneofDescriptorProto {

    private int flags0;

    private java.lang.String _name = "";
    public java.lang.String getName() { return _name; }
    public perfio.protoapi.DescriptorProtos.OneofDescriptorProto setName(java.lang.String value) { this._name = value; this.flags0 |= 1; return this; }
    public boolean hasName() { return (this.flags0 & 1) != 0; }

    private perfio.protoapi.DescriptorProtos.OneofOptions _options;
    public perfio.protoapi.DescriptorProtos.OneofOptions getOptions() {
      if(_options == null) _options = new perfio.protoapi.DescriptorProtos.OneofOptions();
      return _options;
    }
    public perfio.protoapi.DescriptorProtos.OneofDescriptorProto setOptions(perfio.protoapi.DescriptorProtos.OneofOptions value) { this._options = value; this.flags0 |= 2; return this; }
    public boolean hasOptions() { return (this.flags0 & 2) != 0; }

    public static perfio.protoapi.DescriptorProtos.OneofDescriptorProto parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.OneofDescriptorProto();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.OneofDescriptorProto base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> base.setName(perfio.proto.runtime.Runtime.parseString(in));
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getOptions(); perfio.protoapi.DescriptorProtos.OneofOptions.parseFrom(in2, m); in2.close(); base.setOptions(m); }
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
      if(this.hasName()) { perfio.proto.runtime.Runtime.writeInt32(out, 10); perfio.proto.runtime.Runtime.writeString(out, this._name); }
      if(this.hasOptions()) { perfio.proto.runtime.Runtime.writeInt32(out, 18); var out2 = out.defer(); this._options.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.OneofDescriptorProto m) {
        if(this.flags0 != m.flags0) return false;
        if(this.hasName() && !this._name.equals(m._name)) return false;
        if(this.hasOptions() && !this._options.equals(m._options)) return false;
        return true;
      } else return false;
    }
  }

  public static final class EnumDescriptorProto {

    public static final class EnumReservedRange {

      private int flags0;

      private int _start;
      public int getStart() { return _start; }
      public perfio.protoapi.DescriptorProtos.EnumDescriptorProto.EnumReservedRange setStart(int value) { this._start = value; this.flags0 |= 1; return this; }
      public boolean hasStart() { return (this.flags0 & 1) != 0; }

      private int _end;
      public int getEnd() { return _end; }
      public perfio.protoapi.DescriptorProtos.EnumDescriptorProto.EnumReservedRange setEnd(int value) { this._end = value; this.flags0 |= 2; return this; }
      public boolean hasEnd() { return (this.flags0 & 2) != 0; }

      public static perfio.protoapi.DescriptorProtos.EnumDescriptorProto.EnumReservedRange parseFrom(perfio.BufferedInput in) throws java.io.IOException {
        var m = new perfio.protoapi.DescriptorProtos.EnumDescriptorProto.EnumReservedRange();
        parseFrom(in, m);
        return m;
      }
      public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.EnumDescriptorProto.EnumReservedRange base) throws java.io.IOException {
        while(in.hasMore()) {
          int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
          switch(tag) {
            case 8 -> base.setStart(perfio.proto.runtime.Runtime.parseInt32(in));
            case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setStart(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
            case 16 -> base.setEnd(perfio.proto.runtime.Runtime.parseInt32(in));
            case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setEnd(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
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
        if(this.hasStart()) { perfio.proto.runtime.Runtime.writeInt32(out, 8); perfio.proto.runtime.Runtime.writeInt32(out, this._start); }
        if(this.hasEnd()) { perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeInt32(out, this._end); }
      }

      public boolean equals(java.lang.Object o) {
        if(o == this) return true;
        else if(o instanceof perfio.protoapi.DescriptorProtos.EnumDescriptorProto.EnumReservedRange m) {
          if(this.flags0 != m.flags0) return false;
          if(this._start != m._start) return false;
          if(this._end != m._end) return false;
          return true;
        } else return false;
      }
    }

    private int flags0;

    private java.lang.String _name = "";
    public java.lang.String getName() { return _name; }
    public perfio.protoapi.DescriptorProtos.EnumDescriptorProto setName(java.lang.String value) { this._name = value; this.flags0 |= 1; return this; }
    public boolean hasName() { return (this.flags0 & 1) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.EnumValueDescriptorProto> _value = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.EnumValueDescriptorProto> getValueList() { return _value; }
    public void setValueList(java.util.List<perfio.protoapi.DescriptorProtos.EnumValueDescriptorProto> value) { this._value = value; }
    public void addValue(perfio.protoapi.DescriptorProtos.EnumValueDescriptorProto value) {
      if(this._value == null || (java.util.List)this._value == java.util.List.of()) this.setValueList(new java.util.ArrayList<>());
      this._value.add(value);
    }
    public boolean hasValue() { return !_value.isEmpty(); }

    private perfio.protoapi.DescriptorProtos.EnumOptions _options;
    public perfio.protoapi.DescriptorProtos.EnumOptions getOptions() {
      if(_options == null) _options = new perfio.protoapi.DescriptorProtos.EnumOptions();
      return _options;
    }
    public perfio.protoapi.DescriptorProtos.EnumDescriptorProto setOptions(perfio.protoapi.DescriptorProtos.EnumOptions value) { this._options = value; this.flags0 |= 2; return this; }
    public boolean hasOptions() { return (this.flags0 & 2) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.EnumDescriptorProto.EnumReservedRange> _reservedRange = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.EnumDescriptorProto.EnumReservedRange> getReservedRangeList() { return _reservedRange; }
    public void setReservedRangeList(java.util.List<perfio.protoapi.DescriptorProtos.EnumDescriptorProto.EnumReservedRange> value) { this._reservedRange = value; }
    public void addReservedRange(perfio.protoapi.DescriptorProtos.EnumDescriptorProto.EnumReservedRange value) {
      if(this._reservedRange == null || (java.util.List)this._reservedRange == java.util.List.of()) this.setReservedRangeList(new java.util.ArrayList<>());
      this._reservedRange.add(value);
    }
    public boolean hasReservedRange() { return !_reservedRange.isEmpty(); }

    private java.util.List<java.lang.String> _reservedName = java.util.List.of();
    public java.util.List<java.lang.String> getReservedNameList() { return _reservedName; }
    public void setReservedNameList(java.util.List<java.lang.String> value) { this._reservedName = value; }
    public void addReservedName(java.lang.String value) {
      if(this._reservedName == null || (java.util.List)this._reservedName == java.util.List.of()) this.setReservedNameList(new java.util.ArrayList<>());
      this._reservedName.add(value);
    }
    public boolean hasReservedName() { return !_reservedName.isEmpty(); }

    public static perfio.protoapi.DescriptorProtos.EnumDescriptorProto parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.EnumDescriptorProto();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.EnumDescriptorProto base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> base.setName(perfio.proto.runtime.Runtime.parseString(in));
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addValue(perfio.protoapi.DescriptorProtos.EnumValueDescriptorProto.parseFrom(in2)); in2.close(); }
          case 26 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getOptions(); perfio.protoapi.DescriptorProtos.EnumOptions.parseFrom(in2, m); in2.close(); base.setOptions(m); }
          case 34 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addReservedRange(perfio.protoapi.DescriptorProtos.EnumDescriptorProto.EnumReservedRange.parseFrom(in2)); in2.close(); }
          case 42 -> base.addReservedName(perfio.proto.runtime.Runtime.parseString(in));
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 2, 3, 4, 5 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasName()) { perfio.proto.runtime.Runtime.writeInt32(out, 10); perfio.proto.runtime.Runtime.writeString(out, this._name); }
      { var it = this._value.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 18); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      if(this.hasOptions()) { perfio.proto.runtime.Runtime.writeInt32(out, 26); var out2 = out.defer(); this._options.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      { var it = this._reservedRange.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 34); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      { var it = this._reservedName.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 42); perfio.proto.runtime.Runtime.writeString(out, v); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.EnumDescriptorProto m) {
        if(this.flags0 != m.flags0) return false;
        if(this.hasName() && !this._name.equals(m._name)) return false;
        if(!this._value.equals(m._value)) return false;
        if(this.hasOptions() && !this._options.equals(m._options)) return false;
        if(!this._reservedRange.equals(m._reservedRange)) return false;
        if(!this._reservedName.equals(m._reservedName)) return false;
        return true;
      } else return false;
    }
  }

  public static final class EnumValueDescriptorProto {

    private int flags0;

    private java.lang.String _name = "";
    public java.lang.String getName() { return _name; }
    public perfio.protoapi.DescriptorProtos.EnumValueDescriptorProto setName(java.lang.String value) { this._name = value; this.flags0 |= 1; return this; }
    public boolean hasName() { return (this.flags0 & 1) != 0; }

    private int _number;
    public int getNumber() { return _number; }
    public perfio.protoapi.DescriptorProtos.EnumValueDescriptorProto setNumber(int value) { this._number = value; this.flags0 |= 2; return this; }
    public boolean hasNumber() { return (this.flags0 & 2) != 0; }

    private perfio.protoapi.DescriptorProtos.EnumValueOptions _options;
    public perfio.protoapi.DescriptorProtos.EnumValueOptions getOptions() {
      if(_options == null) _options = new perfio.protoapi.DescriptorProtos.EnumValueOptions();
      return _options;
    }
    public perfio.protoapi.DescriptorProtos.EnumValueDescriptorProto setOptions(perfio.protoapi.DescriptorProtos.EnumValueOptions value) { this._options = value; this.flags0 |= 4; return this; }
    public boolean hasOptions() { return (this.flags0 & 4) != 0; }

    public static perfio.protoapi.DescriptorProtos.EnumValueDescriptorProto parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.EnumValueDescriptorProto();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.EnumValueDescriptorProto base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> base.setName(perfio.proto.runtime.Runtime.parseString(in));
          case 16 -> base.setNumber(perfio.proto.runtime.Runtime.parseInt32(in));
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setNumber(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          case 26 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getOptions(); perfio.protoapi.DescriptorProtos.EnumValueOptions.parseFrom(in2, m); in2.close(); base.setOptions(m); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 2, 3 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasName()) { perfio.proto.runtime.Runtime.writeInt32(out, 10); perfio.proto.runtime.Runtime.writeString(out, this._name); }
      if(this.hasNumber()) { perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeInt32(out, this._number); }
      if(this.hasOptions()) { perfio.proto.runtime.Runtime.writeInt32(out, 26); var out2 = out.defer(); this._options.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.EnumValueDescriptorProto m) {
        if(this.flags0 != m.flags0) return false;
        if(this.hasName() && !this._name.equals(m._name)) return false;
        if(this._number != m._number) return false;
        if(this.hasOptions() && !this._options.equals(m._options)) return false;
        return true;
      } else return false;
    }
  }

  public static final class ServiceDescriptorProto {

    private int flags0;

    private java.lang.String _name = "";
    public java.lang.String getName() { return _name; }
    public perfio.protoapi.DescriptorProtos.ServiceDescriptorProto setName(java.lang.String value) { this._name = value; this.flags0 |= 1; return this; }
    public boolean hasName() { return (this.flags0 & 1) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.MethodDescriptorProto> _method = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.MethodDescriptorProto> getMethodList() { return _method; }
    public void setMethodList(java.util.List<perfio.protoapi.DescriptorProtos.MethodDescriptorProto> value) { this._method = value; }
    public void addMethod(perfio.protoapi.DescriptorProtos.MethodDescriptorProto value) {
      if(this._method == null || (java.util.List)this._method == java.util.List.of()) this.setMethodList(new java.util.ArrayList<>());
      this._method.add(value);
    }
    public boolean hasMethod() { return !_method.isEmpty(); }

    private perfio.protoapi.DescriptorProtos.ServiceOptions _options;
    public perfio.protoapi.DescriptorProtos.ServiceOptions getOptions() {
      if(_options == null) _options = new perfio.protoapi.DescriptorProtos.ServiceOptions();
      return _options;
    }
    public perfio.protoapi.DescriptorProtos.ServiceDescriptorProto setOptions(perfio.protoapi.DescriptorProtos.ServiceOptions value) { this._options = value; this.flags0 |= 2; return this; }
    public boolean hasOptions() { return (this.flags0 & 2) != 0; }

    public static perfio.protoapi.DescriptorProtos.ServiceDescriptorProto parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.ServiceDescriptorProto();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.ServiceDescriptorProto base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> base.setName(perfio.proto.runtime.Runtime.parseString(in));
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addMethod(perfio.protoapi.DescriptorProtos.MethodDescriptorProto.parseFrom(in2)); in2.close(); }
          case 26 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getOptions(); perfio.protoapi.DescriptorProtos.ServiceOptions.parseFrom(in2, m); in2.close(); base.setOptions(m); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 2, 3 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasName()) { perfio.proto.runtime.Runtime.writeInt32(out, 10); perfio.proto.runtime.Runtime.writeString(out, this._name); }
      { var it = this._method.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 18); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      if(this.hasOptions()) { perfio.proto.runtime.Runtime.writeInt32(out, 26); var out2 = out.defer(); this._options.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.ServiceDescriptorProto m) {
        if(this.flags0 != m.flags0) return false;
        if(this.hasName() && !this._name.equals(m._name)) return false;
        if(!this._method.equals(m._method)) return false;
        if(this.hasOptions() && !this._options.equals(m._options)) return false;
        return true;
      } else return false;
    }
  }

  public static final class MethodDescriptorProto {

    private int flags0;

    private java.lang.String _name = "";
    public java.lang.String getName() { return _name; }
    public perfio.protoapi.DescriptorProtos.MethodDescriptorProto setName(java.lang.String value) { this._name = value; this.flags0 |= 1; return this; }
    public boolean hasName() { return (this.flags0 & 1) != 0; }

    private java.lang.String _inputType = "";
    public java.lang.String getInputType() { return _inputType; }
    public perfio.protoapi.DescriptorProtos.MethodDescriptorProto setInputType(java.lang.String value) { this._inputType = value; this.flags0 |= 2; return this; }
    public boolean hasInputType() { return (this.flags0 & 2) != 0; }

    private java.lang.String _outputType = "";
    public java.lang.String getOutputType() { return _outputType; }
    public perfio.protoapi.DescriptorProtos.MethodDescriptorProto setOutputType(java.lang.String value) { this._outputType = value; this.flags0 |= 4; return this; }
    public boolean hasOutputType() { return (this.flags0 & 4) != 0; }

    private perfio.protoapi.DescriptorProtos.MethodOptions _options;
    public perfio.protoapi.DescriptorProtos.MethodOptions getOptions() {
      if(_options == null) _options = new perfio.protoapi.DescriptorProtos.MethodOptions();
      return _options;
    }
    public perfio.protoapi.DescriptorProtos.MethodDescriptorProto setOptions(perfio.protoapi.DescriptorProtos.MethodOptions value) { this._options = value; this.flags0 |= 8; return this; }
    public boolean hasOptions() { return (this.flags0 & 8) != 0; }

    private boolean _clientStreaming;
    public boolean getClientStreaming() { return _clientStreaming; }
    public perfio.protoapi.DescriptorProtos.MethodDescriptorProto setClientStreaming(boolean value) { this._clientStreaming = value; this.flags0 |= 16; return this; }
    public boolean hasClientStreaming() { return (this.flags0 & 16) != 0; }

    private boolean _serverStreaming;
    public boolean getServerStreaming() { return _serverStreaming; }
    public perfio.protoapi.DescriptorProtos.MethodDescriptorProto setServerStreaming(boolean value) { this._serverStreaming = value; this.flags0 |= 32; return this; }
    public boolean hasServerStreaming() { return (this.flags0 & 32) != 0; }

    public static perfio.protoapi.DescriptorProtos.MethodDescriptorProto parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.MethodDescriptorProto();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.MethodDescriptorProto base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> base.setName(perfio.proto.runtime.Runtime.parseString(in));
          case 18 -> base.setInputType(perfio.proto.runtime.Runtime.parseString(in));
          case 26 -> base.setOutputType(perfio.proto.runtime.Runtime.parseString(in));
          case 34 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getOptions(); perfio.protoapi.DescriptorProtos.MethodOptions.parseFrom(in2, m); in2.close(); base.setOptions(m); }
          case 40 -> base.setClientStreaming(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 42 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setClientStreaming(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 48 -> base.setServerStreaming(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 50 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setServerStreaming(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 2, 3, 4, 5, 6 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasName()) { perfio.proto.runtime.Runtime.writeInt32(out, 10); perfio.proto.runtime.Runtime.writeString(out, this._name); }
      if(this.hasInputType()) { perfio.proto.runtime.Runtime.writeInt32(out, 18); perfio.proto.runtime.Runtime.writeString(out, this._inputType); }
      if(this.hasOutputType()) { perfio.proto.runtime.Runtime.writeInt32(out, 26); perfio.proto.runtime.Runtime.writeString(out, this._outputType); }
      if(this.hasOptions()) { perfio.proto.runtime.Runtime.writeInt32(out, 34); var out2 = out.defer(); this._options.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      if(this.hasClientStreaming()) { perfio.proto.runtime.Runtime.writeInt32(out, 40); perfio.proto.runtime.Runtime.writeBoolean(out, this._clientStreaming); }
      if(this.hasServerStreaming()) { perfio.proto.runtime.Runtime.writeInt32(out, 48); perfio.proto.runtime.Runtime.writeBoolean(out, this._serverStreaming); }
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.MethodDescriptorProto m) {
        if(this.flags0 != m.flags0) return false;
        if(this.hasName() && !this._name.equals(m._name)) return false;
        if(this.hasInputType() && !this._inputType.equals(m._inputType)) return false;
        if(this.hasOutputType() && !this._outputType.equals(m._outputType)) return false;
        if(this.hasOptions() && !this._options.equals(m._options)) return false;
        if(this._clientStreaming != m._clientStreaming) return false;
        if(this._serverStreaming != m._serverStreaming) return false;
        return true;
      } else return false;
    }
  }

  public static final class FileOptions {
    public enum OptimizeMode {
      SPEED(1),
      CODE_SIZE(2),
      LITE_RUNTIME(3),
      UNRECOGNIZED(-1);
      public final int number;
      OptimizeMode(int number) { this.number = number; }
      public static OptimizeMode valueOf(int number) {
        return switch(number) {
          case 1 -> SPEED;
          case 2 -> CODE_SIZE;
          case 3 -> LITE_RUNTIME;
          default -> UNRECOGNIZED;
        };
      }
    }

    private int flags0;

    private java.lang.String _javaPackage = "";
    public java.lang.String getJavaPackage() { return _javaPackage; }
    public perfio.protoapi.DescriptorProtos.FileOptions setJavaPackage(java.lang.String value) { this._javaPackage = value; this.flags0 |= 1; return this; }
    public boolean hasJavaPackage() { return (this.flags0 & 1) != 0; }

    private java.lang.String _javaOuterClassname = "";
    public java.lang.String getJavaOuterClassname() { return _javaOuterClassname; }
    public perfio.protoapi.DescriptorProtos.FileOptions setJavaOuterClassname(java.lang.String value) { this._javaOuterClassname = value; this.flags0 |= 2; return this; }
    public boolean hasJavaOuterClassname() { return (this.flags0 & 2) != 0; }

    private boolean _javaMultipleFiles;
    public boolean getJavaMultipleFiles() { return _javaMultipleFiles; }
    public perfio.protoapi.DescriptorProtos.FileOptions setJavaMultipleFiles(boolean value) { this._javaMultipleFiles = value; this.flags0 |= 4; return this; }
    public boolean hasJavaMultipleFiles() { return (this.flags0 & 4) != 0; }

    private boolean _javaGenerateEqualsAndHash;
    public boolean getJavaGenerateEqualsAndHash() { return _javaGenerateEqualsAndHash; }
    public perfio.protoapi.DescriptorProtos.FileOptions setJavaGenerateEqualsAndHash(boolean value) { this._javaGenerateEqualsAndHash = value; this.flags0 |= 8; return this; }
    public boolean hasJavaGenerateEqualsAndHash() { return (this.flags0 & 8) != 0; }

    private boolean _javaStringCheckUtf8;
    public boolean getJavaStringCheckUtf8() { return _javaStringCheckUtf8; }
    public perfio.protoapi.DescriptorProtos.FileOptions setJavaStringCheckUtf8(boolean value) { this._javaStringCheckUtf8 = value; this.flags0 |= 16; return this; }
    public boolean hasJavaStringCheckUtf8() { return (this.flags0 & 16) != 0; }

    private perfio.protoapi.DescriptorProtos.FileOptions.OptimizeMode _optimizeFor = perfio.protoapi.DescriptorProtos.FileOptions.OptimizeMode.SPEED;
    public perfio.protoapi.DescriptorProtos.FileOptions.OptimizeMode getOptimizeFor() { return _optimizeFor; }
    public perfio.protoapi.DescriptorProtos.FileOptions setOptimizeFor(perfio.protoapi.DescriptorProtos.FileOptions.OptimizeMode value) { this._optimizeFor = value; this.flags0 |= 32; return this; }
    public boolean hasOptimizeFor() { return (this.flags0 & 32) != 0; }

    private java.lang.String _goPackage = "";
    public java.lang.String getGoPackage() { return _goPackage; }
    public perfio.protoapi.DescriptorProtos.FileOptions setGoPackage(java.lang.String value) { this._goPackage = value; this.flags0 |= 64; return this; }
    public boolean hasGoPackage() { return (this.flags0 & 64) != 0; }

    private boolean _ccGenericServices;
    public boolean getCcGenericServices() { return _ccGenericServices; }
    public perfio.protoapi.DescriptorProtos.FileOptions setCcGenericServices(boolean value) { this._ccGenericServices = value; this.flags0 |= 128; return this; }
    public boolean hasCcGenericServices() { return (this.flags0 & 128) != 0; }

    private boolean _javaGenericServices;
    public boolean getJavaGenericServices() { return _javaGenericServices; }
    public perfio.protoapi.DescriptorProtos.FileOptions setJavaGenericServices(boolean value) { this._javaGenericServices = value; this.flags0 |= 256; return this; }
    public boolean hasJavaGenericServices() { return (this.flags0 & 256) != 0; }

    private boolean _pyGenericServices;
    public boolean getPyGenericServices() { return _pyGenericServices; }
    public perfio.protoapi.DescriptorProtos.FileOptions setPyGenericServices(boolean value) { this._pyGenericServices = value; this.flags0 |= 512; return this; }
    public boolean hasPyGenericServices() { return (this.flags0 & 512) != 0; }

    private boolean _deprecated;
    public boolean getDeprecated() { return _deprecated; }
    public perfio.protoapi.DescriptorProtos.FileOptions setDeprecated(boolean value) { this._deprecated = value; this.flags0 |= 1024; return this; }
    public boolean hasDeprecated() { return (this.flags0 & 1024) != 0; }

    private boolean _ccEnableArenas;
    public boolean getCcEnableArenas() { return _ccEnableArenas; }
    public perfio.protoapi.DescriptorProtos.FileOptions setCcEnableArenas(boolean value) { this._ccEnableArenas = value; this.flags0 |= 2048; return this; }
    public boolean hasCcEnableArenas() { return (this.flags0 & 2048) != 0; }

    private java.lang.String _objcClassPrefix = "";
    public java.lang.String getObjcClassPrefix() { return _objcClassPrefix; }
    public perfio.protoapi.DescriptorProtos.FileOptions setObjcClassPrefix(java.lang.String value) { this._objcClassPrefix = value; this.flags0 |= 4096; return this; }
    public boolean hasObjcClassPrefix() { return (this.flags0 & 4096) != 0; }

    private java.lang.String _csharpNamespace = "";
    public java.lang.String getCsharpNamespace() { return _csharpNamespace; }
    public perfio.protoapi.DescriptorProtos.FileOptions setCsharpNamespace(java.lang.String value) { this._csharpNamespace = value; this.flags0 |= 8192; return this; }
    public boolean hasCsharpNamespace() { return (this.flags0 & 8192) != 0; }

    private java.lang.String _swiftPrefix = "";
    public java.lang.String getSwiftPrefix() { return _swiftPrefix; }
    public perfio.protoapi.DescriptorProtos.FileOptions setSwiftPrefix(java.lang.String value) { this._swiftPrefix = value; this.flags0 |= 16384; return this; }
    public boolean hasSwiftPrefix() { return (this.flags0 & 16384) != 0; }

    private java.lang.String _phpClassPrefix = "";
    public java.lang.String getPhpClassPrefix() { return _phpClassPrefix; }
    public perfio.protoapi.DescriptorProtos.FileOptions setPhpClassPrefix(java.lang.String value) { this._phpClassPrefix = value; this.flags0 |= 32768; return this; }
    public boolean hasPhpClassPrefix() { return (this.flags0 & 32768) != 0; }

    private java.lang.String _phpNamespace = "";
    public java.lang.String getPhpNamespace() { return _phpNamespace; }
    public perfio.protoapi.DescriptorProtos.FileOptions setPhpNamespace(java.lang.String value) { this._phpNamespace = value; this.flags0 |= 65536; return this; }
    public boolean hasPhpNamespace() { return (this.flags0 & 65536) != 0; }

    private java.lang.String _phpMetadataNamespace = "";
    public java.lang.String getPhpMetadataNamespace() { return _phpMetadataNamespace; }
    public perfio.protoapi.DescriptorProtos.FileOptions setPhpMetadataNamespace(java.lang.String value) { this._phpMetadataNamespace = value; this.flags0 |= 131072; return this; }
    public boolean hasPhpMetadataNamespace() { return (this.flags0 & 131072) != 0; }

    private java.lang.String _rubyPackage = "";
    public java.lang.String getRubyPackage() { return _rubyPackage; }
    public perfio.protoapi.DescriptorProtos.FileOptions setRubyPackage(java.lang.String value) { this._rubyPackage = value; this.flags0 |= 262144; return this; }
    public boolean hasRubyPackage() { return (this.flags0 & 262144) != 0; }

    private perfio.protoapi.DescriptorProtos.FeatureSet _features;
    public perfio.protoapi.DescriptorProtos.FeatureSet getFeatures() {
      if(_features == null) _features = new perfio.protoapi.DescriptorProtos.FeatureSet();
      return _features;
    }
    public perfio.protoapi.DescriptorProtos.FileOptions setFeatures(perfio.protoapi.DescriptorProtos.FeatureSet value) { this._features = value; this.flags0 |= 524288; return this; }
    public boolean hasFeatures() { return (this.flags0 & 524288) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> _uninterpretedOption = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> getUninterpretedOptionList() { return _uninterpretedOption; }
    public void setUninterpretedOptionList(java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> value) { this._uninterpretedOption = value; }
    public void addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption value) {
      if(this._uninterpretedOption == null || (java.util.List)this._uninterpretedOption == java.util.List.of()) this.setUninterpretedOptionList(new java.util.ArrayList<>());
      this._uninterpretedOption.add(value);
    }
    public boolean hasUninterpretedOption() { return !_uninterpretedOption.isEmpty(); }

    public static perfio.protoapi.DescriptorProtos.FileOptions parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.FileOptions();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.FileOptions base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> base.setJavaPackage(perfio.proto.runtime.Runtime.parseString(in));
          case 66 -> base.setJavaOuterClassname(perfio.proto.runtime.Runtime.parseString(in));
          case 80 -> base.setJavaMultipleFiles(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 82 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setJavaMultipleFiles(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 160 -> base.setJavaGenerateEqualsAndHash(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 162 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setJavaGenerateEqualsAndHash(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 216 -> base.setJavaStringCheckUtf8(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 218 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setJavaStringCheckUtf8(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 72 -> base.setOptimizeFor(perfio.protoapi.DescriptorProtos.FileOptions.OptimizeMode.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 90 -> base.setGoPackage(perfio.proto.runtime.Runtime.parseString(in));
          case 128 -> base.setCcGenericServices(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 130 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setCcGenericServices(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 136 -> base.setJavaGenericServices(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 138 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setJavaGenericServices(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 144 -> base.setPyGenericServices(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 146 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setPyGenericServices(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 184 -> base.setDeprecated(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 186 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setDeprecated(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 248 -> base.setCcEnableArenas(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 250 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setCcEnableArenas(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 290 -> base.setObjcClassPrefix(perfio.proto.runtime.Runtime.parseString(in));
          case 298 -> base.setCsharpNamespace(perfio.proto.runtime.Runtime.parseString(in));
          case 314 -> base.setSwiftPrefix(perfio.proto.runtime.Runtime.parseString(in));
          case 322 -> base.setPhpClassPrefix(perfio.proto.runtime.Runtime.parseString(in));
          case 330 -> base.setPhpNamespace(perfio.proto.runtime.Runtime.parseString(in));
          case 354 -> base.setPhpMetadataNamespace(perfio.proto.runtime.Runtime.parseString(in));
          case 362 -> base.setRubyPackage(perfio.proto.runtime.Runtime.parseString(in));
          case 402 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getFeatures(); perfio.protoapi.DescriptorProtos.FeatureSet.parseFrom(in2, m); in2.close(); base.setFeatures(m); }
          case 7994 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption.parseFrom(in2)); in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 8, 10, 20, 27, 9, 11, 16, 17, 18, 23, 31, 36, 37, 39, 40, 41, 44, 45, 50, 999 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasJavaPackage()) { perfio.proto.runtime.Runtime.writeInt32(out, 10); perfio.proto.runtime.Runtime.writeString(out, this._javaPackage); }
      if(this.hasJavaOuterClassname()) { perfio.proto.runtime.Runtime.writeInt32(out, 66); perfio.proto.runtime.Runtime.writeString(out, this._javaOuterClassname); }
      if(this.hasJavaMultipleFiles()) { perfio.proto.runtime.Runtime.writeInt32(out, 80); perfio.proto.runtime.Runtime.writeBoolean(out, this._javaMultipleFiles); }
      if(this.hasJavaGenerateEqualsAndHash()) { perfio.proto.runtime.Runtime.writeInt32(out, 160); perfio.proto.runtime.Runtime.writeBoolean(out, this._javaGenerateEqualsAndHash); }
      if(this.hasJavaStringCheckUtf8()) { perfio.proto.runtime.Runtime.writeInt32(out, 216); perfio.proto.runtime.Runtime.writeBoolean(out, this._javaStringCheckUtf8); }
      if(this.hasOptimizeFor()) { perfio.proto.runtime.Runtime.writeInt32(out, 72); perfio.proto.runtime.Runtime.writeInt32(out, this._optimizeFor.number); }
      if(this.hasGoPackage()) { perfio.proto.runtime.Runtime.writeInt32(out, 90); perfio.proto.runtime.Runtime.writeString(out, this._goPackage); }
      if(this.hasCcGenericServices()) { perfio.proto.runtime.Runtime.writeInt32(out, 128); perfio.proto.runtime.Runtime.writeBoolean(out, this._ccGenericServices); }
      if(this.hasJavaGenericServices()) { perfio.proto.runtime.Runtime.writeInt32(out, 136); perfio.proto.runtime.Runtime.writeBoolean(out, this._javaGenericServices); }
      if(this.hasPyGenericServices()) { perfio.proto.runtime.Runtime.writeInt32(out, 144); perfio.proto.runtime.Runtime.writeBoolean(out, this._pyGenericServices); }
      if(this.hasDeprecated()) { perfio.proto.runtime.Runtime.writeInt32(out, 184); perfio.proto.runtime.Runtime.writeBoolean(out, this._deprecated); }
      if(this.hasCcEnableArenas()) { perfio.proto.runtime.Runtime.writeInt32(out, 248); perfio.proto.runtime.Runtime.writeBoolean(out, this._ccEnableArenas); }
      if(this.hasObjcClassPrefix()) { perfio.proto.runtime.Runtime.writeInt32(out, 290); perfio.proto.runtime.Runtime.writeString(out, this._objcClassPrefix); }
      if(this.hasCsharpNamespace()) { perfio.proto.runtime.Runtime.writeInt32(out, 298); perfio.proto.runtime.Runtime.writeString(out, this._csharpNamespace); }
      if(this.hasSwiftPrefix()) { perfio.proto.runtime.Runtime.writeInt32(out, 314); perfio.proto.runtime.Runtime.writeString(out, this._swiftPrefix); }
      if(this.hasPhpClassPrefix()) { perfio.proto.runtime.Runtime.writeInt32(out, 322); perfio.proto.runtime.Runtime.writeString(out, this._phpClassPrefix); }
      if(this.hasPhpNamespace()) { perfio.proto.runtime.Runtime.writeInt32(out, 330); perfio.proto.runtime.Runtime.writeString(out, this._phpNamespace); }
      if(this.hasPhpMetadataNamespace()) { perfio.proto.runtime.Runtime.writeInt32(out, 354); perfio.proto.runtime.Runtime.writeString(out, this._phpMetadataNamespace); }
      if(this.hasRubyPackage()) { perfio.proto.runtime.Runtime.writeInt32(out, 362); perfio.proto.runtime.Runtime.writeString(out, this._rubyPackage); }
      if(this.hasFeatures()) { perfio.proto.runtime.Runtime.writeInt32(out, 402); var out2 = out.defer(); this._features.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      { var it = this._uninterpretedOption.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 7994); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.FileOptions m) {
        if(this.flags0 != m.flags0) return false;
        if(this.hasJavaPackage() && !this._javaPackage.equals(m._javaPackage)) return false;
        if(this.hasJavaOuterClassname() && !this._javaOuterClassname.equals(m._javaOuterClassname)) return false;
        if(this._javaMultipleFiles != m._javaMultipleFiles) return false;
        if(this._javaGenerateEqualsAndHash != m._javaGenerateEqualsAndHash) return false;
        if(this._javaStringCheckUtf8 != m._javaStringCheckUtf8) return false;
        if(this._optimizeFor != m._optimizeFor) return false;
        if(this.hasGoPackage() && !this._goPackage.equals(m._goPackage)) return false;
        if(this._ccGenericServices != m._ccGenericServices) return false;
        if(this._javaGenericServices != m._javaGenericServices) return false;
        if(this._pyGenericServices != m._pyGenericServices) return false;
        if(this._deprecated != m._deprecated) return false;
        if(this._ccEnableArenas != m._ccEnableArenas) return false;
        if(this.hasObjcClassPrefix() && !this._objcClassPrefix.equals(m._objcClassPrefix)) return false;
        if(this.hasCsharpNamespace() && !this._csharpNamespace.equals(m._csharpNamespace)) return false;
        if(this.hasSwiftPrefix() && !this._swiftPrefix.equals(m._swiftPrefix)) return false;
        if(this.hasPhpClassPrefix() && !this._phpClassPrefix.equals(m._phpClassPrefix)) return false;
        if(this.hasPhpNamespace() && !this._phpNamespace.equals(m._phpNamespace)) return false;
        if(this.hasPhpMetadataNamespace() && !this._phpMetadataNamespace.equals(m._phpMetadataNamespace)) return false;
        if(this.hasRubyPackage() && !this._rubyPackage.equals(m._rubyPackage)) return false;
        if(this.hasFeatures() && !this._features.equals(m._features)) return false;
        if(!this._uninterpretedOption.equals(m._uninterpretedOption)) return false;
        return true;
      } else return false;
    }
  }

  public static final class MessageOptions {

    private int flags0;

    private boolean _messageSetWireFormat;
    public boolean getMessageSetWireFormat() { return _messageSetWireFormat; }
    public perfio.protoapi.DescriptorProtos.MessageOptions setMessageSetWireFormat(boolean value) { this._messageSetWireFormat = value; this.flags0 |= 1; return this; }
    public boolean hasMessageSetWireFormat() { return (this.flags0 & 1) != 0; }

    private boolean _noStandardDescriptorAccessor;
    public boolean getNoStandardDescriptorAccessor() { return _noStandardDescriptorAccessor; }
    public perfio.protoapi.DescriptorProtos.MessageOptions setNoStandardDescriptorAccessor(boolean value) { this._noStandardDescriptorAccessor = value; this.flags0 |= 2; return this; }
    public boolean hasNoStandardDescriptorAccessor() { return (this.flags0 & 2) != 0; }

    private boolean _deprecated;
    public boolean getDeprecated() { return _deprecated; }
    public perfio.protoapi.DescriptorProtos.MessageOptions setDeprecated(boolean value) { this._deprecated = value; this.flags0 |= 4; return this; }
    public boolean hasDeprecated() { return (this.flags0 & 4) != 0; }

    private boolean _mapEntry;
    public boolean getMapEntry() { return _mapEntry; }
    public perfio.protoapi.DescriptorProtos.MessageOptions setMapEntry(boolean value) { this._mapEntry = value; this.flags0 |= 8; return this; }
    public boolean hasMapEntry() { return (this.flags0 & 8) != 0; }

    private boolean _deprecatedLegacyJsonFieldConflicts;
    public boolean getDeprecatedLegacyJsonFieldConflicts() { return _deprecatedLegacyJsonFieldConflicts; }
    public perfio.protoapi.DescriptorProtos.MessageOptions setDeprecatedLegacyJsonFieldConflicts(boolean value) { this._deprecatedLegacyJsonFieldConflicts = value; this.flags0 |= 16; return this; }
    public boolean hasDeprecatedLegacyJsonFieldConflicts() { return (this.flags0 & 16) != 0; }

    private perfio.protoapi.DescriptorProtos.FeatureSet _features;
    public perfio.protoapi.DescriptorProtos.FeatureSet getFeatures() {
      if(_features == null) _features = new perfio.protoapi.DescriptorProtos.FeatureSet();
      return _features;
    }
    public perfio.protoapi.DescriptorProtos.MessageOptions setFeatures(perfio.protoapi.DescriptorProtos.FeatureSet value) { this._features = value; this.flags0 |= 32; return this; }
    public boolean hasFeatures() { return (this.flags0 & 32) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> _uninterpretedOption = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> getUninterpretedOptionList() { return _uninterpretedOption; }
    public void setUninterpretedOptionList(java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> value) { this._uninterpretedOption = value; }
    public void addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption value) {
      if(this._uninterpretedOption == null || (java.util.List)this._uninterpretedOption == java.util.List.of()) this.setUninterpretedOptionList(new java.util.ArrayList<>());
      this._uninterpretedOption.add(value);
    }
    public boolean hasUninterpretedOption() { return !_uninterpretedOption.isEmpty(); }

    public static perfio.protoapi.DescriptorProtos.MessageOptions parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.MessageOptions();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.MessageOptions base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 8 -> base.setMessageSetWireFormat(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setMessageSetWireFormat(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 16 -> base.setNoStandardDescriptorAccessor(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setNoStandardDescriptorAccessor(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 24 -> base.setDeprecated(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 26 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setDeprecated(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 56 -> base.setMapEntry(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 58 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setMapEntry(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 88 -> base.setDeprecatedLegacyJsonFieldConflicts(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 90 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setDeprecatedLegacyJsonFieldConflicts(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 98 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getFeatures(); perfio.protoapi.DescriptorProtos.FeatureSet.parseFrom(in2, m); in2.close(); base.setFeatures(m); }
          case 7994 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption.parseFrom(in2)); in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 2, 3, 7, 11, 12, 999 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasMessageSetWireFormat()) { perfio.proto.runtime.Runtime.writeInt32(out, 8); perfio.proto.runtime.Runtime.writeBoolean(out, this._messageSetWireFormat); }
      if(this.hasNoStandardDescriptorAccessor()) { perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeBoolean(out, this._noStandardDescriptorAccessor); }
      if(this.hasDeprecated()) { perfio.proto.runtime.Runtime.writeInt32(out, 24); perfio.proto.runtime.Runtime.writeBoolean(out, this._deprecated); }
      if(this.hasMapEntry()) { perfio.proto.runtime.Runtime.writeInt32(out, 56); perfio.proto.runtime.Runtime.writeBoolean(out, this._mapEntry); }
      if(this.hasDeprecatedLegacyJsonFieldConflicts()) { perfio.proto.runtime.Runtime.writeInt32(out, 88); perfio.proto.runtime.Runtime.writeBoolean(out, this._deprecatedLegacyJsonFieldConflicts); }
      if(this.hasFeatures()) { perfio.proto.runtime.Runtime.writeInt32(out, 98); var out2 = out.defer(); this._features.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      { var it = this._uninterpretedOption.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 7994); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.MessageOptions m) {
        if(this.flags0 != m.flags0) return false;
        if(this._messageSetWireFormat != m._messageSetWireFormat) return false;
        if(this._noStandardDescriptorAccessor != m._noStandardDescriptorAccessor) return false;
        if(this._deprecated != m._deprecated) return false;
        if(this._mapEntry != m._mapEntry) return false;
        if(this._deprecatedLegacyJsonFieldConflicts != m._deprecatedLegacyJsonFieldConflicts) return false;
        if(this.hasFeatures() && !this._features.equals(m._features)) return false;
        if(!this._uninterpretedOption.equals(m._uninterpretedOption)) return false;
        return true;
      } else return false;
    }
  }

  public static final class FieldOptions {
    public enum CType {
      STRING(0),
      CORD(1),
      STRING_PIECE(2),
      UNRECOGNIZED(-1);
      public final int number;
      CType(int number) { this.number = number; }
      public static CType valueOf(int number) {
        return switch(number) {
          case 0 -> STRING;
          case 1 -> CORD;
          case 2 -> STRING_PIECE;
          default -> UNRECOGNIZED;
        };
      }
    }
    public enum JSType {
      JS_NORMAL(0),
      JS_STRING(1),
      JS_NUMBER(2),
      UNRECOGNIZED(-1);
      public final int number;
      JSType(int number) { this.number = number; }
      public static JSType valueOf(int number) {
        return switch(number) {
          case 0 -> JS_NORMAL;
          case 1 -> JS_STRING;
          case 2 -> JS_NUMBER;
          default -> UNRECOGNIZED;
        };
      }
    }
    public enum OptionRetention {
      RETENTION_UNKNOWN(0),
      RETENTION_RUNTIME(1),
      RETENTION_SOURCE(2),
      UNRECOGNIZED(-1);
      public final int number;
      OptionRetention(int number) { this.number = number; }
      public static OptionRetention valueOf(int number) {
        return switch(number) {
          case 0 -> RETENTION_UNKNOWN;
          case 1 -> RETENTION_RUNTIME;
          case 2 -> RETENTION_SOURCE;
          default -> UNRECOGNIZED;
        };
      }
    }
    public enum OptionTargetType {
      TARGET_TYPE_UNKNOWN(0),
      TARGET_TYPE_FILE(1),
      TARGET_TYPE_EXTENSION_RANGE(2),
      TARGET_TYPE_MESSAGE(3),
      TARGET_TYPE_FIELD(4),
      TARGET_TYPE_ONEOF(5),
      TARGET_TYPE_ENUM(6),
      TARGET_TYPE_ENUM_ENTRY(7),
      TARGET_TYPE_SERVICE(8),
      TARGET_TYPE_METHOD(9),
      UNRECOGNIZED(-1);
      public final int number;
      OptionTargetType(int number) { this.number = number; }
      public static OptionTargetType valueOf(int number) {
        return switch(number) {
          case 0 -> TARGET_TYPE_UNKNOWN;
          case 1 -> TARGET_TYPE_FILE;
          case 2 -> TARGET_TYPE_EXTENSION_RANGE;
          case 3 -> TARGET_TYPE_MESSAGE;
          case 4 -> TARGET_TYPE_FIELD;
          case 5 -> TARGET_TYPE_ONEOF;
          case 6 -> TARGET_TYPE_ENUM;
          case 7 -> TARGET_TYPE_ENUM_ENTRY;
          case 8 -> TARGET_TYPE_SERVICE;
          case 9 -> TARGET_TYPE_METHOD;
          default -> UNRECOGNIZED;
        };
      }
    }

    public static final class EditionDefault {

      private int flags0;

      private perfio.protoapi.DescriptorProtos.Edition _edition = perfio.protoapi.DescriptorProtos.Edition.EDITION_UNKNOWN;
      public perfio.protoapi.DescriptorProtos.Edition getEdition() { return _edition; }
      public perfio.protoapi.DescriptorProtos.FieldOptions.EditionDefault setEdition(perfio.protoapi.DescriptorProtos.Edition value) { this._edition = value; this.flags0 |= 1; return this; }
      public boolean hasEdition() { return (this.flags0 & 1) != 0; }

      private java.lang.String _value = "";
      public java.lang.String getValue() { return _value; }
      public perfio.protoapi.DescriptorProtos.FieldOptions.EditionDefault setValue(java.lang.String value) { this._value = value; this.flags0 |= 2; return this; }
      public boolean hasValue() { return (this.flags0 & 2) != 0; }

      public static perfio.protoapi.DescriptorProtos.FieldOptions.EditionDefault parseFrom(perfio.BufferedInput in) throws java.io.IOException {
        var m = new perfio.protoapi.DescriptorProtos.FieldOptions.EditionDefault();
        parseFrom(in, m);
        return m;
      }
      public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.FieldOptions.EditionDefault base) throws java.io.IOException {
        while(in.hasMore()) {
          int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
          switch(tag) {
            case 24 -> base.setEdition(perfio.protoapi.DescriptorProtos.Edition.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
            case 18 -> base.setValue(perfio.proto.runtime.Runtime.parseString(in));
            default -> parseOther(in, tag);
          }
        }
      }
      private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
        int wt = tag & 7;
        int field = tag >>> 3;
        switch(field) {
          case 3, 2 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
          default -> perfio.proto.runtime.Runtime.skip(in, wt);
        }
      }

      public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
        if(this.hasEdition()) { perfio.proto.runtime.Runtime.writeInt32(out, 24); perfio.proto.runtime.Runtime.writeInt32(out, this._edition.number); }
        if(this.hasValue()) { perfio.proto.runtime.Runtime.writeInt32(out, 18); perfio.proto.runtime.Runtime.writeString(out, this._value); }
      }

      public boolean equals(java.lang.Object o) {
        if(o == this) return true;
        else if(o instanceof perfio.protoapi.DescriptorProtos.FieldOptions.EditionDefault m) {
          if(this.flags0 != m.flags0) return false;
          if(this._edition != m._edition) return false;
          if(this.hasValue() && !this._value.equals(m._value)) return false;
          return true;
        } else return false;
      }
    }

    public static final class FeatureSupport {

      private int flags0;

      private perfio.protoapi.DescriptorProtos.Edition _editionIntroduced = perfio.protoapi.DescriptorProtos.Edition.EDITION_UNKNOWN;
      public perfio.protoapi.DescriptorProtos.Edition getEditionIntroduced() { return _editionIntroduced; }
      public perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport setEditionIntroduced(perfio.protoapi.DescriptorProtos.Edition value) { this._editionIntroduced = value; this.flags0 |= 1; return this; }
      public boolean hasEditionIntroduced() { return (this.flags0 & 1) != 0; }

      private perfio.protoapi.DescriptorProtos.Edition _editionDeprecated = perfio.protoapi.DescriptorProtos.Edition.EDITION_UNKNOWN;
      public perfio.protoapi.DescriptorProtos.Edition getEditionDeprecated() { return _editionDeprecated; }
      public perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport setEditionDeprecated(perfio.protoapi.DescriptorProtos.Edition value) { this._editionDeprecated = value; this.flags0 |= 2; return this; }
      public boolean hasEditionDeprecated() { return (this.flags0 & 2) != 0; }

      private java.lang.String _deprecationWarning = "";
      public java.lang.String getDeprecationWarning() { return _deprecationWarning; }
      public perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport setDeprecationWarning(java.lang.String value) { this._deprecationWarning = value; this.flags0 |= 4; return this; }
      public boolean hasDeprecationWarning() { return (this.flags0 & 4) != 0; }

      private perfio.protoapi.DescriptorProtos.Edition _editionRemoved = perfio.protoapi.DescriptorProtos.Edition.EDITION_UNKNOWN;
      public perfio.protoapi.DescriptorProtos.Edition getEditionRemoved() { return _editionRemoved; }
      public perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport setEditionRemoved(perfio.protoapi.DescriptorProtos.Edition value) { this._editionRemoved = value; this.flags0 |= 8; return this; }
      public boolean hasEditionRemoved() { return (this.flags0 & 8) != 0; }

      public static perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport parseFrom(perfio.BufferedInput in) throws java.io.IOException {
        var m = new perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport();
        parseFrom(in, m);
        return m;
      }
      public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport base) throws java.io.IOException {
        while(in.hasMore()) {
          int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
          switch(tag) {
            case 8 -> base.setEditionIntroduced(perfio.protoapi.DescriptorProtos.Edition.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
            case 16 -> base.setEditionDeprecated(perfio.protoapi.DescriptorProtos.Edition.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
            case 26 -> base.setDeprecationWarning(perfio.proto.runtime.Runtime.parseString(in));
            case 32 -> base.setEditionRemoved(perfio.protoapi.DescriptorProtos.Edition.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
            default -> parseOther(in, tag);
          }
        }
      }
      private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
        int wt = tag & 7;
        int field = tag >>> 3;
        switch(field) {
          case 1, 2, 3, 4 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
          default -> perfio.proto.runtime.Runtime.skip(in, wt);
        }
      }

      public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
        if(this.hasEditionIntroduced()) { perfio.proto.runtime.Runtime.writeInt32(out, 8); perfio.proto.runtime.Runtime.writeInt32(out, this._editionIntroduced.number); }
        if(this.hasEditionDeprecated()) { perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeInt32(out, this._editionDeprecated.number); }
        if(this.hasDeprecationWarning()) { perfio.proto.runtime.Runtime.writeInt32(out, 26); perfio.proto.runtime.Runtime.writeString(out, this._deprecationWarning); }
        if(this.hasEditionRemoved()) { perfio.proto.runtime.Runtime.writeInt32(out, 32); perfio.proto.runtime.Runtime.writeInt32(out, this._editionRemoved.number); }
      }

      public boolean equals(java.lang.Object o) {
        if(o == this) return true;
        else if(o instanceof perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport m) {
          if(this.flags0 != m.flags0) return false;
          if(this._editionIntroduced != m._editionIntroduced) return false;
          if(this._editionDeprecated != m._editionDeprecated) return false;
          if(this.hasDeprecationWarning() && !this._deprecationWarning.equals(m._deprecationWarning)) return false;
          if(this._editionRemoved != m._editionRemoved) return false;
          return true;
        } else return false;
      }
    }

    private int flags0;

    private perfio.protoapi.DescriptorProtos.FieldOptions.CType _ctype = perfio.protoapi.DescriptorProtos.FieldOptions.CType.STRING;
    public perfio.protoapi.DescriptorProtos.FieldOptions.CType getCtype() { return _ctype; }
    public perfio.protoapi.DescriptorProtos.FieldOptions setCtype(perfio.protoapi.DescriptorProtos.FieldOptions.CType value) { this._ctype = value; this.flags0 |= 1; return this; }
    public boolean hasCtype() { return (this.flags0 & 1) != 0; }

    private boolean _packed;
    public boolean getPacked() { return _packed; }
    public perfio.protoapi.DescriptorProtos.FieldOptions setPacked(boolean value) { this._packed = value; this.flags0 |= 2; return this; }
    public boolean hasPacked() { return (this.flags0 & 2) != 0; }

    private perfio.protoapi.DescriptorProtos.FieldOptions.JSType _jstype = perfio.protoapi.DescriptorProtos.FieldOptions.JSType.JS_NORMAL;
    public perfio.protoapi.DescriptorProtos.FieldOptions.JSType getJstype() { return _jstype; }
    public perfio.protoapi.DescriptorProtos.FieldOptions setJstype(perfio.protoapi.DescriptorProtos.FieldOptions.JSType value) { this._jstype = value; this.flags0 |= 4; return this; }
    public boolean hasJstype() { return (this.flags0 & 4) != 0; }

    private boolean _lazy;
    public boolean getLazy() { return _lazy; }
    public perfio.protoapi.DescriptorProtos.FieldOptions setLazy(boolean value) { this._lazy = value; this.flags0 |= 8; return this; }
    public boolean hasLazy() { return (this.flags0 & 8) != 0; }

    private boolean _unverifiedLazy;
    public boolean getUnverifiedLazy() { return _unverifiedLazy; }
    public perfio.protoapi.DescriptorProtos.FieldOptions setUnverifiedLazy(boolean value) { this._unverifiedLazy = value; this.flags0 |= 16; return this; }
    public boolean hasUnverifiedLazy() { return (this.flags0 & 16) != 0; }

    private boolean _deprecated;
    public boolean getDeprecated() { return _deprecated; }
    public perfio.protoapi.DescriptorProtos.FieldOptions setDeprecated(boolean value) { this._deprecated = value; this.flags0 |= 32; return this; }
    public boolean hasDeprecated() { return (this.flags0 & 32) != 0; }

    private boolean _weak;
    public boolean getWeak() { return _weak; }
    public perfio.protoapi.DescriptorProtos.FieldOptions setWeak(boolean value) { this._weak = value; this.flags0 |= 64; return this; }
    public boolean hasWeak() { return (this.flags0 & 64) != 0; }

    private boolean _debugRedact;
    public boolean getDebugRedact() { return _debugRedact; }
    public perfio.protoapi.DescriptorProtos.FieldOptions setDebugRedact(boolean value) { this._debugRedact = value; this.flags0 |= 128; return this; }
    public boolean hasDebugRedact() { return (this.flags0 & 128) != 0; }

    private perfio.protoapi.DescriptorProtos.FieldOptions.OptionRetention _retention = perfio.protoapi.DescriptorProtos.FieldOptions.OptionRetention.RETENTION_UNKNOWN;
    public perfio.protoapi.DescriptorProtos.FieldOptions.OptionRetention getRetention() { return _retention; }
    public perfio.protoapi.DescriptorProtos.FieldOptions setRetention(perfio.protoapi.DescriptorProtos.FieldOptions.OptionRetention value) { this._retention = value; this.flags0 |= 256; return this; }
    public boolean hasRetention() { return (this.flags0 & 256) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.FieldOptions.OptionTargetType> _targets = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.FieldOptions.OptionTargetType> getTargetsList() { return _targets; }
    public void setTargetsList(java.util.List<perfio.protoapi.DescriptorProtos.FieldOptions.OptionTargetType> value) { this._targets = value; }
    public void addTargets(perfio.protoapi.DescriptorProtos.FieldOptions.OptionTargetType value) {
      if(this._targets == null || (java.util.List)this._targets == java.util.List.of()) this.setTargetsList(new java.util.ArrayList<>());
      this._targets.add(value);
    }
    public boolean hasTargets() { return !_targets.isEmpty(); }

    private java.util.List<perfio.protoapi.DescriptorProtos.FieldOptions.EditionDefault> _editionDefaults = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.FieldOptions.EditionDefault> getEditionDefaultsList() { return _editionDefaults; }
    public void setEditionDefaultsList(java.util.List<perfio.protoapi.DescriptorProtos.FieldOptions.EditionDefault> value) { this._editionDefaults = value; }
    public void addEditionDefaults(perfio.protoapi.DescriptorProtos.FieldOptions.EditionDefault value) {
      if(this._editionDefaults == null || (java.util.List)this._editionDefaults == java.util.List.of()) this.setEditionDefaultsList(new java.util.ArrayList<>());
      this._editionDefaults.add(value);
    }
    public boolean hasEditionDefaults() { return !_editionDefaults.isEmpty(); }

    private perfio.protoapi.DescriptorProtos.FeatureSet _features;
    public perfio.protoapi.DescriptorProtos.FeatureSet getFeatures() {
      if(_features == null) _features = new perfio.protoapi.DescriptorProtos.FeatureSet();
      return _features;
    }
    public perfio.protoapi.DescriptorProtos.FieldOptions setFeatures(perfio.protoapi.DescriptorProtos.FeatureSet value) { this._features = value; this.flags0 |= 512; return this; }
    public boolean hasFeatures() { return (this.flags0 & 512) != 0; }

    private perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport _featureSupport;
    public perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport getFeatureSupport() {
      if(_featureSupport == null) _featureSupport = new perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport();
      return _featureSupport;
    }
    public perfio.protoapi.DescriptorProtos.FieldOptions setFeatureSupport(perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport value) { this._featureSupport = value; this.flags0 |= 1024; return this; }
    public boolean hasFeatureSupport() { return (this.flags0 & 1024) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> _uninterpretedOption = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> getUninterpretedOptionList() { return _uninterpretedOption; }
    public void setUninterpretedOptionList(java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> value) { this._uninterpretedOption = value; }
    public void addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption value) {
      if(this._uninterpretedOption == null || (java.util.List)this._uninterpretedOption == java.util.List.of()) this.setUninterpretedOptionList(new java.util.ArrayList<>());
      this._uninterpretedOption.add(value);
    }
    public boolean hasUninterpretedOption() { return !_uninterpretedOption.isEmpty(); }

    public static perfio.protoapi.DescriptorProtos.FieldOptions parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.FieldOptions();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.FieldOptions base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 8 -> base.setCtype(perfio.protoapi.DescriptorProtos.FieldOptions.CType.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 16 -> base.setPacked(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setPacked(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 48 -> base.setJstype(perfio.protoapi.DescriptorProtos.FieldOptions.JSType.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 40 -> base.setLazy(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 42 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setLazy(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 120 -> base.setUnverifiedLazy(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 122 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setUnverifiedLazy(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 24 -> base.setDeprecated(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 26 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setDeprecated(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 80 -> base.setWeak(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 82 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setWeak(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 128 -> base.setDebugRedact(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 130 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setDebugRedact(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 136 -> base.setRetention(perfio.protoapi.DescriptorProtos.FieldOptions.OptionRetention.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 152 -> base.addTargets(perfio.protoapi.DescriptorProtos.FieldOptions.OptionTargetType.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 162 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addEditionDefaults(perfio.protoapi.DescriptorProtos.FieldOptions.EditionDefault.parseFrom(in2)); in2.close(); }
          case 170 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getFeatures(); perfio.protoapi.DescriptorProtos.FeatureSet.parseFrom(in2, m); in2.close(); base.setFeatures(m); }
          case 178 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getFeatureSupport(); perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport.parseFrom(in2, m); in2.close(); base.setFeatureSupport(m); }
          case 7994 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption.parseFrom(in2)); in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 2, 6, 5, 15, 3, 10, 16, 17, 19, 20, 21, 22, 999 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasCtype()) { perfio.proto.runtime.Runtime.writeInt32(out, 8); perfio.proto.runtime.Runtime.writeInt32(out, this._ctype.number); }
      if(this.hasPacked()) { perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeBoolean(out, this._packed); }
      if(this.hasJstype()) { perfio.proto.runtime.Runtime.writeInt32(out, 48); perfio.proto.runtime.Runtime.writeInt32(out, this._jstype.number); }
      if(this.hasLazy()) { perfio.proto.runtime.Runtime.writeInt32(out, 40); perfio.proto.runtime.Runtime.writeBoolean(out, this._lazy); }
      if(this.hasUnverifiedLazy()) { perfio.proto.runtime.Runtime.writeInt32(out, 120); perfio.proto.runtime.Runtime.writeBoolean(out, this._unverifiedLazy); }
      if(this.hasDeprecated()) { perfio.proto.runtime.Runtime.writeInt32(out, 24); perfio.proto.runtime.Runtime.writeBoolean(out, this._deprecated); }
      if(this.hasWeak()) { perfio.proto.runtime.Runtime.writeInt32(out, 80); perfio.proto.runtime.Runtime.writeBoolean(out, this._weak); }
      if(this.hasDebugRedact()) { perfio.proto.runtime.Runtime.writeInt32(out, 128); perfio.proto.runtime.Runtime.writeBoolean(out, this._debugRedact); }
      if(this.hasRetention()) { perfio.proto.runtime.Runtime.writeInt32(out, 136); perfio.proto.runtime.Runtime.writeInt32(out, this._retention.number); }
      { var it = this._targets.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 152); perfio.proto.runtime.Runtime.writeInt32(out, v.number); }}
      { var it = this._editionDefaults.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 162); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      if(this.hasFeatures()) { perfio.proto.runtime.Runtime.writeInt32(out, 170); var out2 = out.defer(); this._features.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      if(this.hasFeatureSupport()) { perfio.proto.runtime.Runtime.writeInt32(out, 178); var out2 = out.defer(); this._featureSupport.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      { var it = this._uninterpretedOption.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 7994); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.FieldOptions m) {
        if(this.flags0 != m.flags0) return false;
        if(this._ctype != m._ctype) return false;
        if(this._packed != m._packed) return false;
        if(this._jstype != m._jstype) return false;
        if(this._lazy != m._lazy) return false;
        if(this._unverifiedLazy != m._unverifiedLazy) return false;
        if(this._deprecated != m._deprecated) return false;
        if(this._weak != m._weak) return false;
        if(this._debugRedact != m._debugRedact) return false;
        if(this._retention != m._retention) return false;
        if(!this._targets.equals(m._targets)) return false;
        if(!this._editionDefaults.equals(m._editionDefaults)) return false;
        if(this.hasFeatures() && !this._features.equals(m._features)) return false;
        if(this.hasFeatureSupport() && !this._featureSupport.equals(m._featureSupport)) return false;
        if(!this._uninterpretedOption.equals(m._uninterpretedOption)) return false;
        return true;
      } else return false;
    }
  }

  public static final class OneofOptions {

    private int flags0;

    private perfio.protoapi.DescriptorProtos.FeatureSet _features;
    public perfio.protoapi.DescriptorProtos.FeatureSet getFeatures() {
      if(_features == null) _features = new perfio.protoapi.DescriptorProtos.FeatureSet();
      return _features;
    }
    public perfio.protoapi.DescriptorProtos.OneofOptions setFeatures(perfio.protoapi.DescriptorProtos.FeatureSet value) { this._features = value; this.flags0 |= 1; return this; }
    public boolean hasFeatures() { return (this.flags0 & 1) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> _uninterpretedOption = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> getUninterpretedOptionList() { return _uninterpretedOption; }
    public void setUninterpretedOptionList(java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> value) { this._uninterpretedOption = value; }
    public void addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption value) {
      if(this._uninterpretedOption == null || (java.util.List)this._uninterpretedOption == java.util.List.of()) this.setUninterpretedOptionList(new java.util.ArrayList<>());
      this._uninterpretedOption.add(value);
    }
    public boolean hasUninterpretedOption() { return !_uninterpretedOption.isEmpty(); }

    public static perfio.protoapi.DescriptorProtos.OneofOptions parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.OneofOptions();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.OneofOptions base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getFeatures(); perfio.protoapi.DescriptorProtos.FeatureSet.parseFrom(in2, m); in2.close(); base.setFeatures(m); }
          case 7994 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption.parseFrom(in2)); in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 999 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasFeatures()) { perfio.proto.runtime.Runtime.writeInt32(out, 10); var out2 = out.defer(); this._features.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      { var it = this._uninterpretedOption.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 7994); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.OneofOptions m) {
        if(this.flags0 != m.flags0) return false;
        if(this.hasFeatures() && !this._features.equals(m._features)) return false;
        if(!this._uninterpretedOption.equals(m._uninterpretedOption)) return false;
        return true;
      } else return false;
    }
  }

  public static final class EnumOptions {

    private int flags0;

    private boolean _allowAlias;
    public boolean getAllowAlias() { return _allowAlias; }
    public perfio.protoapi.DescriptorProtos.EnumOptions setAllowAlias(boolean value) { this._allowAlias = value; this.flags0 |= 1; return this; }
    public boolean hasAllowAlias() { return (this.flags0 & 1) != 0; }

    private boolean _deprecated;
    public boolean getDeprecated() { return _deprecated; }
    public perfio.protoapi.DescriptorProtos.EnumOptions setDeprecated(boolean value) { this._deprecated = value; this.flags0 |= 2; return this; }
    public boolean hasDeprecated() { return (this.flags0 & 2) != 0; }

    private boolean _deprecatedLegacyJsonFieldConflicts;
    public boolean getDeprecatedLegacyJsonFieldConflicts() { return _deprecatedLegacyJsonFieldConflicts; }
    public perfio.protoapi.DescriptorProtos.EnumOptions setDeprecatedLegacyJsonFieldConflicts(boolean value) { this._deprecatedLegacyJsonFieldConflicts = value; this.flags0 |= 4; return this; }
    public boolean hasDeprecatedLegacyJsonFieldConflicts() { return (this.flags0 & 4) != 0; }

    private perfio.protoapi.DescriptorProtos.FeatureSet _features;
    public perfio.protoapi.DescriptorProtos.FeatureSet getFeatures() {
      if(_features == null) _features = new perfio.protoapi.DescriptorProtos.FeatureSet();
      return _features;
    }
    public perfio.protoapi.DescriptorProtos.EnumOptions setFeatures(perfio.protoapi.DescriptorProtos.FeatureSet value) { this._features = value; this.flags0 |= 8; return this; }
    public boolean hasFeatures() { return (this.flags0 & 8) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> _uninterpretedOption = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> getUninterpretedOptionList() { return _uninterpretedOption; }
    public void setUninterpretedOptionList(java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> value) { this._uninterpretedOption = value; }
    public void addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption value) {
      if(this._uninterpretedOption == null || (java.util.List)this._uninterpretedOption == java.util.List.of()) this.setUninterpretedOptionList(new java.util.ArrayList<>());
      this._uninterpretedOption.add(value);
    }
    public boolean hasUninterpretedOption() { return !_uninterpretedOption.isEmpty(); }

    public static perfio.protoapi.DescriptorProtos.EnumOptions parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.EnumOptions();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.EnumOptions base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 16 -> base.setAllowAlias(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setAllowAlias(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 24 -> base.setDeprecated(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 26 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setDeprecated(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 48 -> base.setDeprecatedLegacyJsonFieldConflicts(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 50 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setDeprecatedLegacyJsonFieldConflicts(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 58 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getFeatures(); perfio.protoapi.DescriptorProtos.FeatureSet.parseFrom(in2, m); in2.close(); base.setFeatures(m); }
          case 7994 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption.parseFrom(in2)); in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 2, 3, 6, 7, 999 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasAllowAlias()) { perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeBoolean(out, this._allowAlias); }
      if(this.hasDeprecated()) { perfio.proto.runtime.Runtime.writeInt32(out, 24); perfio.proto.runtime.Runtime.writeBoolean(out, this._deprecated); }
      if(this.hasDeprecatedLegacyJsonFieldConflicts()) { perfio.proto.runtime.Runtime.writeInt32(out, 48); perfio.proto.runtime.Runtime.writeBoolean(out, this._deprecatedLegacyJsonFieldConflicts); }
      if(this.hasFeatures()) { perfio.proto.runtime.Runtime.writeInt32(out, 58); var out2 = out.defer(); this._features.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      { var it = this._uninterpretedOption.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 7994); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.EnumOptions m) {
        if(this.flags0 != m.flags0) return false;
        if(this._allowAlias != m._allowAlias) return false;
        if(this._deprecated != m._deprecated) return false;
        if(this._deprecatedLegacyJsonFieldConflicts != m._deprecatedLegacyJsonFieldConflicts) return false;
        if(this.hasFeatures() && !this._features.equals(m._features)) return false;
        if(!this._uninterpretedOption.equals(m._uninterpretedOption)) return false;
        return true;
      } else return false;
    }
  }

  public static final class EnumValueOptions {

    private int flags0;

    private boolean _deprecated;
    public boolean getDeprecated() { return _deprecated; }
    public perfio.protoapi.DescriptorProtos.EnumValueOptions setDeprecated(boolean value) { this._deprecated = value; this.flags0 |= 1; return this; }
    public boolean hasDeprecated() { return (this.flags0 & 1) != 0; }

    private perfio.protoapi.DescriptorProtos.FeatureSet _features;
    public perfio.protoapi.DescriptorProtos.FeatureSet getFeatures() {
      if(_features == null) _features = new perfio.protoapi.DescriptorProtos.FeatureSet();
      return _features;
    }
    public perfio.protoapi.DescriptorProtos.EnumValueOptions setFeatures(perfio.protoapi.DescriptorProtos.FeatureSet value) { this._features = value; this.flags0 |= 2; return this; }
    public boolean hasFeatures() { return (this.flags0 & 2) != 0; }

    private boolean _debugRedact;
    public boolean getDebugRedact() { return _debugRedact; }
    public perfio.protoapi.DescriptorProtos.EnumValueOptions setDebugRedact(boolean value) { this._debugRedact = value; this.flags0 |= 4; return this; }
    public boolean hasDebugRedact() { return (this.flags0 & 4) != 0; }

    private perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport _featureSupport;
    public perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport getFeatureSupport() {
      if(_featureSupport == null) _featureSupport = new perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport();
      return _featureSupport;
    }
    public perfio.protoapi.DescriptorProtos.EnumValueOptions setFeatureSupport(perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport value) { this._featureSupport = value; this.flags0 |= 8; return this; }
    public boolean hasFeatureSupport() { return (this.flags0 & 8) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> _uninterpretedOption = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> getUninterpretedOptionList() { return _uninterpretedOption; }
    public void setUninterpretedOptionList(java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> value) { this._uninterpretedOption = value; }
    public void addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption value) {
      if(this._uninterpretedOption == null || (java.util.List)this._uninterpretedOption == java.util.List.of()) this.setUninterpretedOptionList(new java.util.ArrayList<>());
      this._uninterpretedOption.add(value);
    }
    public boolean hasUninterpretedOption() { return !_uninterpretedOption.isEmpty(); }

    public static perfio.protoapi.DescriptorProtos.EnumValueOptions parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.EnumValueOptions();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.EnumValueOptions base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 8 -> base.setDeprecated(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setDeprecated(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getFeatures(); perfio.protoapi.DescriptorProtos.FeatureSet.parseFrom(in2, m); in2.close(); base.setFeatures(m); }
          case 24 -> base.setDebugRedact(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 26 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setDebugRedact(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 34 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getFeatureSupport(); perfio.protoapi.DescriptorProtos.FieldOptions.FeatureSupport.parseFrom(in2, m); in2.close(); base.setFeatureSupport(m); }
          case 7994 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption.parseFrom(in2)); in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 2, 3, 4, 999 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasDeprecated()) { perfio.proto.runtime.Runtime.writeInt32(out, 8); perfio.proto.runtime.Runtime.writeBoolean(out, this._deprecated); }
      if(this.hasFeatures()) { perfio.proto.runtime.Runtime.writeInt32(out, 18); var out2 = out.defer(); this._features.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      if(this.hasDebugRedact()) { perfio.proto.runtime.Runtime.writeInt32(out, 24); perfio.proto.runtime.Runtime.writeBoolean(out, this._debugRedact); }
      if(this.hasFeatureSupport()) { perfio.proto.runtime.Runtime.writeInt32(out, 34); var out2 = out.defer(); this._featureSupport.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      { var it = this._uninterpretedOption.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 7994); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.EnumValueOptions m) {
        if(this.flags0 != m.flags0) return false;
        if(this._deprecated != m._deprecated) return false;
        if(this.hasFeatures() && !this._features.equals(m._features)) return false;
        if(this._debugRedact != m._debugRedact) return false;
        if(this.hasFeatureSupport() && !this._featureSupport.equals(m._featureSupport)) return false;
        if(!this._uninterpretedOption.equals(m._uninterpretedOption)) return false;
        return true;
      } else return false;
    }
  }

  public static final class ServiceOptions {

    private int flags0;

    private perfio.protoapi.DescriptorProtos.FeatureSet _features;
    public perfio.protoapi.DescriptorProtos.FeatureSet getFeatures() {
      if(_features == null) _features = new perfio.protoapi.DescriptorProtos.FeatureSet();
      return _features;
    }
    public perfio.protoapi.DescriptorProtos.ServiceOptions setFeatures(perfio.protoapi.DescriptorProtos.FeatureSet value) { this._features = value; this.flags0 |= 1; return this; }
    public boolean hasFeatures() { return (this.flags0 & 1) != 0; }

    private boolean _deprecated;
    public boolean getDeprecated() { return _deprecated; }
    public perfio.protoapi.DescriptorProtos.ServiceOptions setDeprecated(boolean value) { this._deprecated = value; this.flags0 |= 2; return this; }
    public boolean hasDeprecated() { return (this.flags0 & 2) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> _uninterpretedOption = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> getUninterpretedOptionList() { return _uninterpretedOption; }
    public void setUninterpretedOptionList(java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> value) { this._uninterpretedOption = value; }
    public void addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption value) {
      if(this._uninterpretedOption == null || (java.util.List)this._uninterpretedOption == java.util.List.of()) this.setUninterpretedOptionList(new java.util.ArrayList<>());
      this._uninterpretedOption.add(value);
    }
    public boolean hasUninterpretedOption() { return !_uninterpretedOption.isEmpty(); }

    public static perfio.protoapi.DescriptorProtos.ServiceOptions parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.ServiceOptions();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.ServiceOptions base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 274 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getFeatures(); perfio.protoapi.DescriptorProtos.FeatureSet.parseFrom(in2, m); in2.close(); base.setFeatures(m); }
          case 264 -> base.setDeprecated(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 266 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setDeprecated(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 7994 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption.parseFrom(in2)); in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 34, 33, 999 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasFeatures()) { perfio.proto.runtime.Runtime.writeInt32(out, 274); var out2 = out.defer(); this._features.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      if(this.hasDeprecated()) { perfio.proto.runtime.Runtime.writeInt32(out, 264); perfio.proto.runtime.Runtime.writeBoolean(out, this._deprecated); }
      { var it = this._uninterpretedOption.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 7994); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.ServiceOptions m) {
        if(this.flags0 != m.flags0) return false;
        if(this.hasFeatures() && !this._features.equals(m._features)) return false;
        if(this._deprecated != m._deprecated) return false;
        if(!this._uninterpretedOption.equals(m._uninterpretedOption)) return false;
        return true;
      } else return false;
    }
  }

  public static final class MethodOptions {
    public enum IdempotencyLevel {
      IDEMPOTENCY_UNKNOWN(0),
      NO_SIDE_EFFECTS(1),
      IDEMPOTENT(2),
      UNRECOGNIZED(-1);
      public final int number;
      IdempotencyLevel(int number) { this.number = number; }
      public static IdempotencyLevel valueOf(int number) {
        return switch(number) {
          case 0 -> IDEMPOTENCY_UNKNOWN;
          case 1 -> NO_SIDE_EFFECTS;
          case 2 -> IDEMPOTENT;
          default -> UNRECOGNIZED;
        };
      }
    }

    private int flags0;

    private boolean _deprecated;
    public boolean getDeprecated() { return _deprecated; }
    public perfio.protoapi.DescriptorProtos.MethodOptions setDeprecated(boolean value) { this._deprecated = value; this.flags0 |= 1; return this; }
    public boolean hasDeprecated() { return (this.flags0 & 1) != 0; }

    private perfio.protoapi.DescriptorProtos.MethodOptions.IdempotencyLevel _idempotencyLevel = perfio.protoapi.DescriptorProtos.MethodOptions.IdempotencyLevel.IDEMPOTENCY_UNKNOWN;
    public perfio.protoapi.DescriptorProtos.MethodOptions.IdempotencyLevel getIdempotencyLevel() { return _idempotencyLevel; }
    public perfio.protoapi.DescriptorProtos.MethodOptions setIdempotencyLevel(perfio.protoapi.DescriptorProtos.MethodOptions.IdempotencyLevel value) { this._idempotencyLevel = value; this.flags0 |= 2; return this; }
    public boolean hasIdempotencyLevel() { return (this.flags0 & 2) != 0; }

    private perfio.protoapi.DescriptorProtos.FeatureSet _features;
    public perfio.protoapi.DescriptorProtos.FeatureSet getFeatures() {
      if(_features == null) _features = new perfio.protoapi.DescriptorProtos.FeatureSet();
      return _features;
    }
    public perfio.protoapi.DescriptorProtos.MethodOptions setFeatures(perfio.protoapi.DescriptorProtos.FeatureSet value) { this._features = value; this.flags0 |= 4; return this; }
    public boolean hasFeatures() { return (this.flags0 & 4) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> _uninterpretedOption = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> getUninterpretedOptionList() { return _uninterpretedOption; }
    public void setUninterpretedOptionList(java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption> value) { this._uninterpretedOption = value; }
    public void addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption value) {
      if(this._uninterpretedOption == null || (java.util.List)this._uninterpretedOption == java.util.List.of()) this.setUninterpretedOptionList(new java.util.ArrayList<>());
      this._uninterpretedOption.add(value);
    }
    public boolean hasUninterpretedOption() { return !_uninterpretedOption.isEmpty(); }

    public static perfio.protoapi.DescriptorProtos.MethodOptions parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.MethodOptions();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.MethodOptions base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 264 -> base.setDeprecated(perfio.proto.runtime.Runtime.parseBoolean(in));
          case 266 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setDeprecated(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
          case 272 -> base.setIdempotencyLevel(perfio.protoapi.DescriptorProtos.MethodOptions.IdempotencyLevel.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 282 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getFeatures(); perfio.protoapi.DescriptorProtos.FeatureSet.parseFrom(in2, m); in2.close(); base.setFeatures(m); }
          case 7994 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addUninterpretedOption(perfio.protoapi.DescriptorProtos.UninterpretedOption.parseFrom(in2)); in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 33, 34, 35, 999 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasDeprecated()) { perfio.proto.runtime.Runtime.writeInt32(out, 264); perfio.proto.runtime.Runtime.writeBoolean(out, this._deprecated); }
      if(this.hasIdempotencyLevel()) { perfio.proto.runtime.Runtime.writeInt32(out, 272); perfio.proto.runtime.Runtime.writeInt32(out, this._idempotencyLevel.number); }
      if(this.hasFeatures()) { perfio.proto.runtime.Runtime.writeInt32(out, 282); var out2 = out.defer(); this._features.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      { var it = this._uninterpretedOption.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 7994); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.MethodOptions m) {
        if(this.flags0 != m.flags0) return false;
        if(this._deprecated != m._deprecated) return false;
        if(this._idempotencyLevel != m._idempotencyLevel) return false;
        if(this.hasFeatures() && !this._features.equals(m._features)) return false;
        if(!this._uninterpretedOption.equals(m._uninterpretedOption)) return false;
        return true;
      } else return false;
    }
  }

  public static final class UninterpretedOption {

    public static final class NamePart {

      private java.lang.String _namePart = "";
      public java.lang.String getNamePart() { return _namePart; }
      public perfio.protoapi.DescriptorProtos.UninterpretedOption.NamePart setNamePart(java.lang.String value) { this._namePart = value; return this; }
      public boolean hasNamePart() { return !_namePart.isEmpty(); }

      private boolean _isExtension;
      public boolean getIsExtension() { return _isExtension; }
      public perfio.protoapi.DescriptorProtos.UninterpretedOption.NamePart setIsExtension(boolean value) { this._isExtension = value; return this; }
      public boolean hasIsExtension() { return _isExtension; }

      public static perfio.protoapi.DescriptorProtos.UninterpretedOption.NamePart parseFrom(perfio.BufferedInput in) throws java.io.IOException {
        var m = new perfio.protoapi.DescriptorProtos.UninterpretedOption.NamePart();
        parseFrom(in, m);
        return m;
      }
      public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.UninterpretedOption.NamePart base) throws java.io.IOException {
        while(in.hasMore()) {
          int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
          switch(tag) {
            case 10 -> base.setNamePart(perfio.proto.runtime.Runtime.parseString(in));
            case 16 -> base.setIsExtension(perfio.proto.runtime.Runtime.parseBoolean(in));
            case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setIsExtension(perfio.proto.runtime.Runtime.parseBoolean(in2));; in2.close(); }
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
        if(this.hasNamePart()) { perfio.proto.runtime.Runtime.writeInt32(out, 10); perfio.proto.runtime.Runtime.writeString(out, this._namePart); }
        if(this.hasIsExtension()) { perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeBoolean(out, this._isExtension); }
      }

      public boolean equals(java.lang.Object o) {
        if(o == this) return true;
        else if(o instanceof perfio.protoapi.DescriptorProtos.UninterpretedOption.NamePart m) {
          if(!this._namePart.equals(m._namePart)) return false;
          if(this._isExtension != m._isExtension) return false;
          return true;
        } else return false;
      }
    }

    private int flags0;

    private java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption.NamePart> _name = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption.NamePart> getNameList() { return _name; }
    public void setNameList(java.util.List<perfio.protoapi.DescriptorProtos.UninterpretedOption.NamePart> value) { this._name = value; }
    public void addName(perfio.protoapi.DescriptorProtos.UninterpretedOption.NamePart value) {
      if(this._name == null || (java.util.List)this._name == java.util.List.of()) this.setNameList(new java.util.ArrayList<>());
      this._name.add(value);
    }
    public boolean hasName() { return !_name.isEmpty(); }

    private java.lang.String _identifierValue = "";
    public java.lang.String getIdentifierValue() { return _identifierValue; }
    public perfio.protoapi.DescriptorProtos.UninterpretedOption setIdentifierValue(java.lang.String value) { this._identifierValue = value; this.flags0 |= 1; return this; }
    public boolean hasIdentifierValue() { return (this.flags0 & 1) != 0; }

    private long _positiveIntValue;
    public long getPositiveIntValue() { return _positiveIntValue; }
    public perfio.protoapi.DescriptorProtos.UninterpretedOption setPositiveIntValue(long value) { this._positiveIntValue = value; this.flags0 |= 2; return this; }
    public boolean hasPositiveIntValue() { return (this.flags0 & 2) != 0; }

    private long _negativeIntValue;
    public long getNegativeIntValue() { return _negativeIntValue; }
    public perfio.protoapi.DescriptorProtos.UninterpretedOption setNegativeIntValue(long value) { this._negativeIntValue = value; this.flags0 |= 4; return this; }
    public boolean hasNegativeIntValue() { return (this.flags0 & 4) != 0; }

    private double _doubleValue;
    public double getDoubleValue() { return _doubleValue; }
    public perfio.protoapi.DescriptorProtos.UninterpretedOption setDoubleValue(double value) { this._doubleValue = value; this.flags0 |= 8; return this; }
    public boolean hasDoubleValue() { return (this.flags0 & 8) != 0; }

    private byte[] _stringValue = new byte[0];
    public byte[] getStringValue() { return _stringValue; }
    public perfio.protoapi.DescriptorProtos.UninterpretedOption setStringValue(byte[] value) { this._stringValue = value; this.flags0 |= 16; return this; }
    public boolean hasStringValue() { return (this.flags0 & 16) != 0; }

    private java.lang.String _aggregateValue = "";
    public java.lang.String getAggregateValue() { return _aggregateValue; }
    public perfio.protoapi.DescriptorProtos.UninterpretedOption setAggregateValue(java.lang.String value) { this._aggregateValue = value; this.flags0 |= 32; return this; }
    public boolean hasAggregateValue() { return (this.flags0 & 32) != 0; }

    public static perfio.protoapi.DescriptorProtos.UninterpretedOption parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.UninterpretedOption();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.UninterpretedOption base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addName(perfio.protoapi.DescriptorProtos.UninterpretedOption.NamePart.parseFrom(in2)); in2.close(); }
          case 26 -> base.setIdentifierValue(perfio.proto.runtime.Runtime.parseString(in));
          case 32 -> base.setPositiveIntValue(perfio.proto.runtime.Runtime.parseInt64(in));
          case 34 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setPositiveIntValue(perfio.proto.runtime.Runtime.parseInt64(in2));; in2.close(); }
          case 40 -> base.setNegativeIntValue(perfio.proto.runtime.Runtime.parseInt64(in));
          case 42 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setNegativeIntValue(perfio.proto.runtime.Runtime.parseInt64(in2));; in2.close(); }
          case 49 -> base.setDoubleValue(perfio.proto.runtime.Runtime.parseDouble(in));
          case 50 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setDoubleValue(perfio.proto.runtime.Runtime.parseDouble(in2));; in2.close(); }
          case 58 -> base.setStringValue(perfio.proto.runtime.Runtime.parseBytes(in));
          case 66 -> base.setAggregateValue(perfio.proto.runtime.Runtime.parseString(in));
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 2, 3, 4, 5, 6, 7, 8 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      { var it = this._name.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 18); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      if(this.hasIdentifierValue()) { perfio.proto.runtime.Runtime.writeInt32(out, 26); perfio.proto.runtime.Runtime.writeString(out, this._identifierValue); }
      if(this.hasPositiveIntValue()) { perfio.proto.runtime.Runtime.writeInt32(out, 32); perfio.proto.runtime.Runtime.writeInt64(out, this._positiveIntValue); }
      if(this.hasNegativeIntValue()) { perfio.proto.runtime.Runtime.writeInt32(out, 40); perfio.proto.runtime.Runtime.writeInt64(out, this._negativeIntValue); }
      if(this.hasDoubleValue()) { perfio.proto.runtime.Runtime.writeInt32(out, 49); perfio.proto.runtime.Runtime.writeDouble(out, this._doubleValue); }
      if(this.hasStringValue()) { perfio.proto.runtime.Runtime.writeInt32(out, 58); perfio.proto.runtime.Runtime.writeBytes(out, this._stringValue); }
      if(this.hasAggregateValue()) { perfio.proto.runtime.Runtime.writeInt32(out, 66); perfio.proto.runtime.Runtime.writeString(out, this._aggregateValue); }
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.UninterpretedOption m) {
        if(this.flags0 != m.flags0) return false;
        if(!this._name.equals(m._name)) return false;
        if(this.hasIdentifierValue() && !this._identifierValue.equals(m._identifierValue)) return false;
        if(this._positiveIntValue != m._positiveIntValue) return false;
        if(this._negativeIntValue != m._negativeIntValue) return false;
        if(this._doubleValue != m._doubleValue) return false;
        if(this.hasStringValue() && !this._stringValue.equals(m._stringValue)) return false;
        if(this.hasAggregateValue() && !this._aggregateValue.equals(m._aggregateValue)) return false;
        return true;
      } else return false;
    }
  }

  public static final class FeatureSet {
    public enum FieldPresence {
      FIELD_PRESENCE_UNKNOWN(0),
      EXPLICIT(1),
      IMPLICIT(2),
      LEGACY_REQUIRED(3),
      UNRECOGNIZED(-1);
      public final int number;
      FieldPresence(int number) { this.number = number; }
      public static FieldPresence valueOf(int number) {
        return switch(number) {
          case 0 -> FIELD_PRESENCE_UNKNOWN;
          case 1 -> EXPLICIT;
          case 2 -> IMPLICIT;
          case 3 -> LEGACY_REQUIRED;
          default -> UNRECOGNIZED;
        };
      }
    }
    public enum EnumType {
      ENUM_TYPE_UNKNOWN(0),
      OPEN(1),
      CLOSED(2),
      UNRECOGNIZED(-1);
      public final int number;
      EnumType(int number) { this.number = number; }
      public static EnumType valueOf(int number) {
        return switch(number) {
          case 0 -> ENUM_TYPE_UNKNOWN;
          case 1 -> OPEN;
          case 2 -> CLOSED;
          default -> UNRECOGNIZED;
        };
      }
    }
    public enum RepeatedFieldEncoding {
      REPEATED_FIELD_ENCODING_UNKNOWN(0),
      PACKED(1),
      EXPANDED(2),
      UNRECOGNIZED(-1);
      public final int number;
      RepeatedFieldEncoding(int number) { this.number = number; }
      public static RepeatedFieldEncoding valueOf(int number) {
        return switch(number) {
          case 0 -> REPEATED_FIELD_ENCODING_UNKNOWN;
          case 1 -> PACKED;
          case 2 -> EXPANDED;
          default -> UNRECOGNIZED;
        };
      }
    }
    public enum Utf8Validation {
      UTF8_VALIDATION_UNKNOWN(0),
      VERIFY(2),
      NONE(3),
      UNRECOGNIZED(-1);
      public final int number;
      Utf8Validation(int number) { this.number = number; }
      public static Utf8Validation valueOf(int number) {
        return switch(number) {
          case 0 -> UTF8_VALIDATION_UNKNOWN;
          case 2 -> VERIFY;
          case 3 -> NONE;
          default -> UNRECOGNIZED;
        };
      }
    }
    public enum MessageEncoding {
      MESSAGE_ENCODING_UNKNOWN(0),
      LENGTH_PREFIXED(1),
      DELIMITED(2),
      UNRECOGNIZED(-1);
      public final int number;
      MessageEncoding(int number) { this.number = number; }
      public static MessageEncoding valueOf(int number) {
        return switch(number) {
          case 0 -> MESSAGE_ENCODING_UNKNOWN;
          case 1 -> LENGTH_PREFIXED;
          case 2 -> DELIMITED;
          default -> UNRECOGNIZED;
        };
      }
    }
    public enum JsonFormat {
      JSON_FORMAT_UNKNOWN(0),
      ALLOW(1),
      LEGACY_BEST_EFFORT(2),
      UNRECOGNIZED(-1);
      public final int number;
      JsonFormat(int number) { this.number = number; }
      public static JsonFormat valueOf(int number) {
        return switch(number) {
          case 0 -> JSON_FORMAT_UNKNOWN;
          case 1 -> ALLOW;
          case 2 -> LEGACY_BEST_EFFORT;
          default -> UNRECOGNIZED;
        };
      }
    }

    private int flags0;

    private perfio.protoapi.DescriptorProtos.FeatureSet.FieldPresence _fieldPresence = perfio.protoapi.DescriptorProtos.FeatureSet.FieldPresence.FIELD_PRESENCE_UNKNOWN;
    public perfio.protoapi.DescriptorProtos.FeatureSet.FieldPresence getFieldPresence() { return _fieldPresence; }
    public perfio.protoapi.DescriptorProtos.FeatureSet setFieldPresence(perfio.protoapi.DescriptorProtos.FeatureSet.FieldPresence value) { this._fieldPresence = value; this.flags0 |= 1; return this; }
    public boolean hasFieldPresence() { return (this.flags0 & 1) != 0; }

    private perfio.protoapi.DescriptorProtos.FeatureSet.EnumType _enumType = perfio.protoapi.DescriptorProtos.FeatureSet.EnumType.ENUM_TYPE_UNKNOWN;
    public perfio.protoapi.DescriptorProtos.FeatureSet.EnumType getEnumType() { return _enumType; }
    public perfio.protoapi.DescriptorProtos.FeatureSet setEnumType(perfio.protoapi.DescriptorProtos.FeatureSet.EnumType value) { this._enumType = value; this.flags0 |= 2; return this; }
    public boolean hasEnumType() { return (this.flags0 & 2) != 0; }

    private perfio.protoapi.DescriptorProtos.FeatureSet.RepeatedFieldEncoding _repeatedFieldEncoding = perfio.protoapi.DescriptorProtos.FeatureSet.RepeatedFieldEncoding.REPEATED_FIELD_ENCODING_UNKNOWN;
    public perfio.protoapi.DescriptorProtos.FeatureSet.RepeatedFieldEncoding getRepeatedFieldEncoding() { return _repeatedFieldEncoding; }
    public perfio.protoapi.DescriptorProtos.FeatureSet setRepeatedFieldEncoding(perfio.protoapi.DescriptorProtos.FeatureSet.RepeatedFieldEncoding value) { this._repeatedFieldEncoding = value; this.flags0 |= 4; return this; }
    public boolean hasRepeatedFieldEncoding() { return (this.flags0 & 4) != 0; }

    private perfio.protoapi.DescriptorProtos.FeatureSet.Utf8Validation _utf8Validation = perfio.protoapi.DescriptorProtos.FeatureSet.Utf8Validation.UTF8_VALIDATION_UNKNOWN;
    public perfio.protoapi.DescriptorProtos.FeatureSet.Utf8Validation getUtf8Validation() { return _utf8Validation; }
    public perfio.protoapi.DescriptorProtos.FeatureSet setUtf8Validation(perfio.protoapi.DescriptorProtos.FeatureSet.Utf8Validation value) { this._utf8Validation = value; this.flags0 |= 8; return this; }
    public boolean hasUtf8Validation() { return (this.flags0 & 8) != 0; }

    private perfio.protoapi.DescriptorProtos.FeatureSet.MessageEncoding _messageEncoding = perfio.protoapi.DescriptorProtos.FeatureSet.MessageEncoding.MESSAGE_ENCODING_UNKNOWN;
    public perfio.protoapi.DescriptorProtos.FeatureSet.MessageEncoding getMessageEncoding() { return _messageEncoding; }
    public perfio.protoapi.DescriptorProtos.FeatureSet setMessageEncoding(perfio.protoapi.DescriptorProtos.FeatureSet.MessageEncoding value) { this._messageEncoding = value; this.flags0 |= 16; return this; }
    public boolean hasMessageEncoding() { return (this.flags0 & 16) != 0; }

    private perfio.protoapi.DescriptorProtos.FeatureSet.JsonFormat _jsonFormat = perfio.protoapi.DescriptorProtos.FeatureSet.JsonFormat.JSON_FORMAT_UNKNOWN;
    public perfio.protoapi.DescriptorProtos.FeatureSet.JsonFormat getJsonFormat() { return _jsonFormat; }
    public perfio.protoapi.DescriptorProtos.FeatureSet setJsonFormat(perfio.protoapi.DescriptorProtos.FeatureSet.JsonFormat value) { this._jsonFormat = value; this.flags0 |= 32; return this; }
    public boolean hasJsonFormat() { return (this.flags0 & 32) != 0; }

    public static perfio.protoapi.DescriptorProtos.FeatureSet parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.FeatureSet();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.FeatureSet base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 8 -> base.setFieldPresence(perfio.protoapi.DescriptorProtos.FeatureSet.FieldPresence.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 16 -> base.setEnumType(perfio.protoapi.DescriptorProtos.FeatureSet.EnumType.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 24 -> base.setRepeatedFieldEncoding(perfio.protoapi.DescriptorProtos.FeatureSet.RepeatedFieldEncoding.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 32 -> base.setUtf8Validation(perfio.protoapi.DescriptorProtos.FeatureSet.Utf8Validation.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 40 -> base.setMessageEncoding(perfio.protoapi.DescriptorProtos.FeatureSet.MessageEncoding.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 48 -> base.setJsonFormat(perfio.protoapi.DescriptorProtos.FeatureSet.JsonFormat.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 2, 3, 4, 5, 6 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasFieldPresence()) { perfio.proto.runtime.Runtime.writeInt32(out, 8); perfio.proto.runtime.Runtime.writeInt32(out, this._fieldPresence.number); }
      if(this.hasEnumType()) { perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeInt32(out, this._enumType.number); }
      if(this.hasRepeatedFieldEncoding()) { perfio.proto.runtime.Runtime.writeInt32(out, 24); perfio.proto.runtime.Runtime.writeInt32(out, this._repeatedFieldEncoding.number); }
      if(this.hasUtf8Validation()) { perfio.proto.runtime.Runtime.writeInt32(out, 32); perfio.proto.runtime.Runtime.writeInt32(out, this._utf8Validation.number); }
      if(this.hasMessageEncoding()) { perfio.proto.runtime.Runtime.writeInt32(out, 40); perfio.proto.runtime.Runtime.writeInt32(out, this._messageEncoding.number); }
      if(this.hasJsonFormat()) { perfio.proto.runtime.Runtime.writeInt32(out, 48); perfio.proto.runtime.Runtime.writeInt32(out, this._jsonFormat.number); }
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.FeatureSet m) {
        if(this.flags0 != m.flags0) return false;
        if(this._fieldPresence != m._fieldPresence) return false;
        if(this._enumType != m._enumType) return false;
        if(this._repeatedFieldEncoding != m._repeatedFieldEncoding) return false;
        if(this._utf8Validation != m._utf8Validation) return false;
        if(this._messageEncoding != m._messageEncoding) return false;
        if(this._jsonFormat != m._jsonFormat) return false;
        return true;
      } else return false;
    }
  }

  public static final class FeatureSetDefaults {

    public static final class FeatureSetEditionDefault {

      private int flags0;

      private perfio.protoapi.DescriptorProtos.Edition _edition = perfio.protoapi.DescriptorProtos.Edition.EDITION_UNKNOWN;
      public perfio.protoapi.DescriptorProtos.Edition getEdition() { return _edition; }
      public perfio.protoapi.DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault setEdition(perfio.protoapi.DescriptorProtos.Edition value) { this._edition = value; this.flags0 |= 1; return this; }
      public boolean hasEdition() { return (this.flags0 & 1) != 0; }

      private perfio.protoapi.DescriptorProtos.FeatureSet _overridableFeatures;
      public perfio.protoapi.DescriptorProtos.FeatureSet getOverridableFeatures() {
        if(_overridableFeatures == null) _overridableFeatures = new perfio.protoapi.DescriptorProtos.FeatureSet();
        return _overridableFeatures;
      }
      public perfio.protoapi.DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault setOverridableFeatures(perfio.protoapi.DescriptorProtos.FeatureSet value) { this._overridableFeatures = value; this.flags0 |= 2; return this; }
      public boolean hasOverridableFeatures() { return (this.flags0 & 2) != 0; }

      private perfio.protoapi.DescriptorProtos.FeatureSet _fixedFeatures;
      public perfio.protoapi.DescriptorProtos.FeatureSet getFixedFeatures() {
        if(_fixedFeatures == null) _fixedFeatures = new perfio.protoapi.DescriptorProtos.FeatureSet();
        return _fixedFeatures;
      }
      public perfio.protoapi.DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault setFixedFeatures(perfio.protoapi.DescriptorProtos.FeatureSet value) { this._fixedFeatures = value; this.flags0 |= 4; return this; }
      public boolean hasFixedFeatures() { return (this.flags0 & 4) != 0; }

      public static perfio.protoapi.DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault parseFrom(perfio.BufferedInput in) throws java.io.IOException {
        var m = new perfio.protoapi.DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault();
        parseFrom(in, m);
        return m;
      }
      public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault base) throws java.io.IOException {
        while(in.hasMore()) {
          int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
          switch(tag) {
            case 24 -> base.setEdition(perfio.protoapi.DescriptorProtos.Edition.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
            case 34 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getOverridableFeatures(); perfio.protoapi.DescriptorProtos.FeatureSet.parseFrom(in2, m); in2.close(); base.setOverridableFeatures(m); }
            case 42 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getFixedFeatures(); perfio.protoapi.DescriptorProtos.FeatureSet.parseFrom(in2, m); in2.close(); base.setFixedFeatures(m); }
            default -> parseOther(in, tag);
          }
        }
      }
      private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
        int wt = tag & 7;
        int field = tag >>> 3;
        switch(field) {
          case 3, 4, 5 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
          default -> perfio.proto.runtime.Runtime.skip(in, wt);
        }
      }

      public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
        if(this.hasEdition()) { perfio.proto.runtime.Runtime.writeInt32(out, 24); perfio.proto.runtime.Runtime.writeInt32(out, this._edition.number); }
        if(this.hasOverridableFeatures()) { perfio.proto.runtime.Runtime.writeInt32(out, 34); var out2 = out.defer(); this._overridableFeatures.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
        if(this.hasFixedFeatures()) { perfio.proto.runtime.Runtime.writeInt32(out, 42); var out2 = out.defer(); this._fixedFeatures.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      }

      public boolean equals(java.lang.Object o) {
        if(o == this) return true;
        else if(o instanceof perfio.protoapi.DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault m) {
          if(this.flags0 != m.flags0) return false;
          if(this._edition != m._edition) return false;
          if(this.hasOverridableFeatures() && !this._overridableFeatures.equals(m._overridableFeatures)) return false;
          if(this.hasFixedFeatures() && !this._fixedFeatures.equals(m._fixedFeatures)) return false;
          return true;
        } else return false;
      }
    }

    private int flags0;

    private java.util.List<perfio.protoapi.DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault> _defaults = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault> getDefaultsList() { return _defaults; }
    public void setDefaultsList(java.util.List<perfio.protoapi.DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault> value) { this._defaults = value; }
    public void addDefaults(perfio.protoapi.DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault value) {
      if(this._defaults == null || (java.util.List)this._defaults == java.util.List.of()) this.setDefaultsList(new java.util.ArrayList<>());
      this._defaults.add(value);
    }
    public boolean hasDefaults() { return !_defaults.isEmpty(); }

    private perfio.protoapi.DescriptorProtos.Edition _minimumEdition = perfio.protoapi.DescriptorProtos.Edition.EDITION_UNKNOWN;
    public perfio.protoapi.DescriptorProtos.Edition getMinimumEdition() { return _minimumEdition; }
    public perfio.protoapi.DescriptorProtos.FeatureSetDefaults setMinimumEdition(perfio.protoapi.DescriptorProtos.Edition value) { this._minimumEdition = value; this.flags0 |= 1; return this; }
    public boolean hasMinimumEdition() { return (this.flags0 & 1) != 0; }

    private perfio.protoapi.DescriptorProtos.Edition _maximumEdition = perfio.protoapi.DescriptorProtos.Edition.EDITION_UNKNOWN;
    public perfio.protoapi.DescriptorProtos.Edition getMaximumEdition() { return _maximumEdition; }
    public perfio.protoapi.DescriptorProtos.FeatureSetDefaults setMaximumEdition(perfio.protoapi.DescriptorProtos.Edition value) { this._maximumEdition = value; this.flags0 |= 2; return this; }
    public boolean hasMaximumEdition() { return (this.flags0 & 2) != 0; }

    public static perfio.protoapi.DescriptorProtos.FeatureSetDefaults parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.FeatureSetDefaults();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.FeatureSetDefaults base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addDefaults(perfio.protoapi.DescriptorProtos.FeatureSetDefaults.FeatureSetEditionDefault.parseFrom(in2)); in2.close(); }
          case 32 -> base.setMinimumEdition(perfio.protoapi.DescriptorProtos.Edition.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          case 40 -> base.setMaximumEdition(perfio.protoapi.DescriptorProtos.Edition.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 4, 5 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      { var it = this._defaults.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 10); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      if(this.hasMinimumEdition()) { perfio.proto.runtime.Runtime.writeInt32(out, 32); perfio.proto.runtime.Runtime.writeInt32(out, this._minimumEdition.number); }
      if(this.hasMaximumEdition()) { perfio.proto.runtime.Runtime.writeInt32(out, 40); perfio.proto.runtime.Runtime.writeInt32(out, this._maximumEdition.number); }
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.FeatureSetDefaults m) {
        if(this.flags0 != m.flags0) return false;
        if(!this._defaults.equals(m._defaults)) return false;
        if(this._minimumEdition != m._minimumEdition) return false;
        if(this._maximumEdition != m._maximumEdition) return false;
        return true;
      } else return false;
    }
  }

  public static final class SourceCodeInfo {

    public static final class Location {

      private int flags0;

      private java.util.List<java.lang.Integer> _path = java.util.List.of();
      public java.util.List<java.lang.Integer> getPathList() { return _path; }
      public void setPathList(java.util.List<java.lang.Integer> value) { this._path = value; }
      public void addPath(int value) {
        if(this._path == null || (java.util.List)this._path == java.util.List.of()) this.setPathList(new java.util.ArrayList<>());
        this._path.add(value);
      }
      public boolean hasPath() { return !_path.isEmpty(); }

      private java.util.List<java.lang.Integer> _span = java.util.List.of();
      public java.util.List<java.lang.Integer> getSpanList() { return _span; }
      public void setSpanList(java.util.List<java.lang.Integer> value) { this._span = value; }
      public void addSpan(int value) {
        if(this._span == null || (java.util.List)this._span == java.util.List.of()) this.setSpanList(new java.util.ArrayList<>());
        this._span.add(value);
      }
      public boolean hasSpan() { return !_span.isEmpty(); }

      private java.lang.String _leadingComments = "";
      public java.lang.String getLeadingComments() { return _leadingComments; }
      public perfio.protoapi.DescriptorProtos.SourceCodeInfo.Location setLeadingComments(java.lang.String value) { this._leadingComments = value; this.flags0 |= 1; return this; }
      public boolean hasLeadingComments() { return (this.flags0 & 1) != 0; }

      private java.lang.String _trailingComments = "";
      public java.lang.String getTrailingComments() { return _trailingComments; }
      public perfio.protoapi.DescriptorProtos.SourceCodeInfo.Location setTrailingComments(java.lang.String value) { this._trailingComments = value; this.flags0 |= 2; return this; }
      public boolean hasTrailingComments() { return (this.flags0 & 2) != 0; }

      private java.util.List<java.lang.String> _leadingDetachedComments = java.util.List.of();
      public java.util.List<java.lang.String> getLeadingDetachedCommentsList() { return _leadingDetachedComments; }
      public void setLeadingDetachedCommentsList(java.util.List<java.lang.String> value) { this._leadingDetachedComments = value; }
      public void addLeadingDetachedComments(java.lang.String value) {
        if(this._leadingDetachedComments == null || (java.util.List)this._leadingDetachedComments == java.util.List.of()) this.setLeadingDetachedCommentsList(new java.util.ArrayList<>());
        this._leadingDetachedComments.add(value);
      }
      public boolean hasLeadingDetachedComments() { return !_leadingDetachedComments.isEmpty(); }

      public static perfio.protoapi.DescriptorProtos.SourceCodeInfo.Location parseFrom(perfio.BufferedInput in) throws java.io.IOException {
        var m = new perfio.protoapi.DescriptorProtos.SourceCodeInfo.Location();
        parseFrom(in, m);
        return m;
      }
      public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.SourceCodeInfo.Location base) throws java.io.IOException {
        while(in.hasMore()) {
          int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
          switch(tag) {
            case 8 -> base.addPath(perfio.proto.runtime.Runtime.parseInt32(in));
            case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.addPath(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
            case 16 -> base.addSpan(perfio.proto.runtime.Runtime.parseInt32(in));
            case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.addSpan(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
            case 26 -> base.setLeadingComments(perfio.proto.runtime.Runtime.parseString(in));
            case 34 -> base.setTrailingComments(perfio.proto.runtime.Runtime.parseString(in));
            case 50 -> base.addLeadingDetachedComments(perfio.proto.runtime.Runtime.parseString(in));
            default -> parseOther(in, tag);
          }
        }
      }
      private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
        int wt = tag & 7;
        int field = tag >>> 3;
        switch(field) {
          case 1, 2, 3, 4, 6 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
          default -> perfio.proto.runtime.Runtime.skip(in, wt);
        }
      }

      public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
        { var it = this._path.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 8); perfio.proto.runtime.Runtime.writeInt32(out, v); }}
        { var it = this._span.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeInt32(out, v); }}
        if(this.hasLeadingComments()) { perfio.proto.runtime.Runtime.writeInt32(out, 26); perfio.proto.runtime.Runtime.writeString(out, this._leadingComments); }
        if(this.hasTrailingComments()) { perfio.proto.runtime.Runtime.writeInt32(out, 34); perfio.proto.runtime.Runtime.writeString(out, this._trailingComments); }
        { var it = this._leadingDetachedComments.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 50); perfio.proto.runtime.Runtime.writeString(out, v); }}
      }

      public boolean equals(java.lang.Object o) {
        if(o == this) return true;
        else if(o instanceof perfio.protoapi.DescriptorProtos.SourceCodeInfo.Location m) {
          if(this.flags0 != m.flags0) return false;
          if(!this._path.equals(m._path)) return false;
          if(!this._span.equals(m._span)) return false;
          if(this.hasLeadingComments() && !this._leadingComments.equals(m._leadingComments)) return false;
          if(this.hasTrailingComments() && !this._trailingComments.equals(m._trailingComments)) return false;
          if(!this._leadingDetachedComments.equals(m._leadingDetachedComments)) return false;
          return true;
        } else return false;
      }
    }

    private java.util.List<perfio.protoapi.DescriptorProtos.SourceCodeInfo.Location> _location = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.SourceCodeInfo.Location> getLocationList() { return _location; }
    public void setLocationList(java.util.List<perfio.protoapi.DescriptorProtos.SourceCodeInfo.Location> value) { this._location = value; }
    public void addLocation(perfio.protoapi.DescriptorProtos.SourceCodeInfo.Location value) {
      if(this._location == null || (java.util.List)this._location == java.util.List.of()) this.setLocationList(new java.util.ArrayList<>());
      this._location.add(value);
    }
    public boolean hasLocation() { return !_location.isEmpty(); }

    public static perfio.protoapi.DescriptorProtos.SourceCodeInfo parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.SourceCodeInfo();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.SourceCodeInfo base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addLocation(perfio.protoapi.DescriptorProtos.SourceCodeInfo.Location.parseFrom(in2)); in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      { var it = this._location.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 10); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.SourceCodeInfo m) {
        if(!this._location.equals(m._location)) return false;
        return true;
      } else return false;
    }
  }

  public static final class GeneratedCodeInfo {

    public static final class Annotation {
      public enum Semantic {
        NONE(0),
        SET(1),
        ALIAS(2),
        UNRECOGNIZED(-1);
        public final int number;
        Semantic(int number) { this.number = number; }
        public static Semantic valueOf(int number) {
          return switch(number) {
            case 0 -> NONE;
            case 1 -> SET;
            case 2 -> ALIAS;
            default -> UNRECOGNIZED;
          };
        }
      }

      private int flags0;

      private java.util.List<java.lang.Integer> _path = java.util.List.of();
      public java.util.List<java.lang.Integer> getPathList() { return _path; }
      public void setPathList(java.util.List<java.lang.Integer> value) { this._path = value; }
      public void addPath(int value) {
        if(this._path == null || (java.util.List)this._path == java.util.List.of()) this.setPathList(new java.util.ArrayList<>());
        this._path.add(value);
      }
      public boolean hasPath() { return !_path.isEmpty(); }

      private java.lang.String _sourceFile = "";
      public java.lang.String getSourceFile() { return _sourceFile; }
      public perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation setSourceFile(java.lang.String value) { this._sourceFile = value; this.flags0 |= 1; return this; }
      public boolean hasSourceFile() { return (this.flags0 & 1) != 0; }

      private int _begin;
      public int getBegin() { return _begin; }
      public perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation setBegin(int value) { this._begin = value; this.flags0 |= 2; return this; }
      public boolean hasBegin() { return (this.flags0 & 2) != 0; }

      private int _end;
      public int getEnd() { return _end; }
      public perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation setEnd(int value) { this._end = value; this.flags0 |= 4; return this; }
      public boolean hasEnd() { return (this.flags0 & 4) != 0; }

      private perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation.Semantic _semantic = perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation.Semantic.NONE;
      public perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation.Semantic getSemantic() { return _semantic; }
      public perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation setSemantic(perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation.Semantic value) { this._semantic = value; this.flags0 |= 8; return this; }
      public boolean hasSemantic() { return (this.flags0 & 8) != 0; }

      public static perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation parseFrom(perfio.BufferedInput in) throws java.io.IOException {
        var m = new perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation();
        parseFrom(in, m);
        return m;
      }
      public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation base) throws java.io.IOException {
        while(in.hasMore()) {
          int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
          switch(tag) {
            case 8 -> base.addPath(perfio.proto.runtime.Runtime.parseInt32(in));
            case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.addPath(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
            case 18 -> base.setSourceFile(perfio.proto.runtime.Runtime.parseString(in));
            case 24 -> base.setBegin(perfio.proto.runtime.Runtime.parseInt32(in));
            case 26 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setBegin(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
            case 32 -> base.setEnd(perfio.proto.runtime.Runtime.parseInt32(in));
            case 34 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setEnd(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
            case 40 -> base.setSemantic(perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation.Semantic.valueOf(perfio.proto.runtime.Runtime.parseInt32(in)));
            default -> parseOther(in, tag);
          }
        }
      }
      private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
        int wt = tag & 7;
        int field = tag >>> 3;
        switch(field) {
          case 1, 2, 3, 4, 5 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
          default -> perfio.proto.runtime.Runtime.skip(in, wt);
        }
      }

      public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
        { var it = this._path.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 8); perfio.proto.runtime.Runtime.writeInt32(out, v); }}
        if(this.hasSourceFile()) { perfio.proto.runtime.Runtime.writeInt32(out, 18); perfio.proto.runtime.Runtime.writeString(out, this._sourceFile); }
        if(this.hasBegin()) { perfio.proto.runtime.Runtime.writeInt32(out, 24); perfio.proto.runtime.Runtime.writeInt32(out, this._begin); }
        if(this.hasEnd()) { perfio.proto.runtime.Runtime.writeInt32(out, 32); perfio.proto.runtime.Runtime.writeInt32(out, this._end); }
        if(this.hasSemantic()) { perfio.proto.runtime.Runtime.writeInt32(out, 40); perfio.proto.runtime.Runtime.writeInt32(out, this._semantic.number); }
      }

      public boolean equals(java.lang.Object o) {
        if(o == this) return true;
        else if(o instanceof perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation m) {
          if(this.flags0 != m.flags0) return false;
          if(!this._path.equals(m._path)) return false;
          if(this.hasSourceFile() && !this._sourceFile.equals(m._sourceFile)) return false;
          if(this._begin != m._begin) return false;
          if(this._end != m._end) return false;
          if(this._semantic != m._semantic) return false;
          return true;
        } else return false;
      }
    }

    private java.util.List<perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation> _annotation = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation> getAnnotationList() { return _annotation; }
    public void setAnnotationList(java.util.List<perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation> value) { this._annotation = value; }
    public void addAnnotation(perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation value) {
      if(this._annotation == null || (java.util.List)this._annotation == java.util.List.of()) this.setAnnotationList(new java.util.ArrayList<>());
      this._annotation.add(value);
    }
    public boolean hasAnnotation() { return !_annotation.isEmpty(); }

    public static perfio.protoapi.DescriptorProtos.GeneratedCodeInfo parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.DescriptorProtos.GeneratedCodeInfo();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.DescriptorProtos.GeneratedCodeInfo base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addAnnotation(perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.Annotation.parseFrom(in2)); in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      { var it = this._annotation.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 10); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.DescriptorProtos.GeneratedCodeInfo m) {
        if(!this._annotation.equals(m._annotation)) return false;
        return true;
      } else return false;
    }
  }
}
