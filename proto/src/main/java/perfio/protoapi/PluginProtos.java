package perfio.protoapi;

public final class PluginProtos {
  private PluginProtos() {}

  public static final class Version {

    private int flags0;

    private int _major;
    public int getMajor() { return _major; }
    public perfio.protoapi.PluginProtos.Version setMajor(int value) { this._major = value; this.flags0 |= 1; return this; }
    public boolean hasMajor() { return (this.flags0 & 1) != 0; }

    private int _minor;
    public int getMinor() { return _minor; }
    public perfio.protoapi.PluginProtos.Version setMinor(int value) { this._minor = value; this.flags0 |= 2; return this; }
    public boolean hasMinor() { return (this.flags0 & 2) != 0; }

    private int _patch;
    public int getPatch() { return _patch; }
    public perfio.protoapi.PluginProtos.Version setPatch(int value) { this._patch = value; this.flags0 |= 4; return this; }
    public boolean hasPatch() { return (this.flags0 & 4) != 0; }

    private java.lang.String _suffix = "";
    public java.lang.String getSuffix() { return _suffix; }
    public perfio.protoapi.PluginProtos.Version setSuffix(java.lang.String value) { this._suffix = value; this.flags0 |= 8; return this; }
    public boolean hasSuffix() { return (this.flags0 & 8) != 0; }

    public static perfio.protoapi.PluginProtos.Version parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.PluginProtos.Version();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.PluginProtos.Version base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 8 -> base.setMajor(perfio.proto.runtime.Runtime.parseInt32(in));
          case 10 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setMajor(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          case 16 -> base.setMinor(perfio.proto.runtime.Runtime.parseInt32(in));
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setMinor(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          case 24 -> base.setPatch(perfio.proto.runtime.Runtime.parseInt32(in));
          case 26 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setPatch(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          case 34 -> base.setSuffix(perfio.proto.runtime.Runtime.parseString(in));
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
      if(this.hasMajor()) { perfio.proto.runtime.Runtime.writeInt32(out, 8); perfio.proto.runtime.Runtime.writeInt32(out, this._major); }
      if(this.hasMinor()) { perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeInt32(out, this._minor); }
      if(this.hasPatch()) { perfio.proto.runtime.Runtime.writeInt32(out, 24); perfio.proto.runtime.Runtime.writeInt32(out, this._patch); }
      if(this.hasSuffix()) { perfio.proto.runtime.Runtime.writeInt32(out, 34); perfio.proto.runtime.Runtime.writeString(out, this._suffix); }
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.PluginProtos.Version m) {
        if(this.flags0 != m.flags0) return false;
        if(this._major != m._major) return false;
        if(this._minor != m._minor) return false;
        if(this._patch != m._patch) return false;
        if(this.hasSuffix() && !this._suffix.equals(m._suffix)) return false;
        return true;
      } else return false;
    }
  }

  public static final class CodeGeneratorRequest {

    private int flags0;

    private java.util.List<java.lang.String> _fileToGenerate = java.util.List.of();
    public java.util.List<java.lang.String> getFileToGenerateList() { return _fileToGenerate; }
    public void setFileToGenerateList(java.util.List<java.lang.String> value) { this._fileToGenerate = value; }
    public void addFileToGenerate(java.lang.String value) {
      if(this._fileToGenerate == null || (java.util.List)this._fileToGenerate == java.util.List.of()) this.setFileToGenerateList(new java.util.ArrayList<>());
      this._fileToGenerate.add(value);
    }
    public boolean hasFileToGenerate() { return !_fileToGenerate.isEmpty(); }

    private java.lang.String _parameter = "";
    public java.lang.String getParameter() { return _parameter; }
    public perfio.protoapi.PluginProtos.CodeGeneratorRequest setParameter(java.lang.String value) { this._parameter = value; this.flags0 |= 1; return this; }
    public boolean hasParameter() { return (this.flags0 & 1) != 0; }

    private java.util.List<perfio.protoapi.DescriptorProtos.FileDescriptorProto> _protoFile = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.FileDescriptorProto> getProtoFileList() { return _protoFile; }
    public void setProtoFileList(java.util.List<perfio.protoapi.DescriptorProtos.FileDescriptorProto> value) { this._protoFile = value; }
    public void addProtoFile(perfio.protoapi.DescriptorProtos.FileDescriptorProto value) {
      if(this._protoFile == null || (java.util.List)this._protoFile == java.util.List.of()) this.setProtoFileList(new java.util.ArrayList<>());
      this._protoFile.add(value);
    }
    public boolean hasProtoFile() { return !_protoFile.isEmpty(); }

    private java.util.List<perfio.protoapi.DescriptorProtos.FileDescriptorProto> _sourceFileDescriptors = java.util.List.of();
    public java.util.List<perfio.protoapi.DescriptorProtos.FileDescriptorProto> getSourceFileDescriptorsList() { return _sourceFileDescriptors; }
    public void setSourceFileDescriptorsList(java.util.List<perfio.protoapi.DescriptorProtos.FileDescriptorProto> value) { this._sourceFileDescriptors = value; }
    public void addSourceFileDescriptors(perfio.protoapi.DescriptorProtos.FileDescriptorProto value) {
      if(this._sourceFileDescriptors == null || (java.util.List)this._sourceFileDescriptors == java.util.List.of()) this.setSourceFileDescriptorsList(new java.util.ArrayList<>());
      this._sourceFileDescriptors.add(value);
    }
    public boolean hasSourceFileDescriptors() { return !_sourceFileDescriptors.isEmpty(); }

    private perfio.protoapi.PluginProtos.Version _compilerVersion;
    public perfio.protoapi.PluginProtos.Version getCompilerVersion() {
      if(_compilerVersion == null) _compilerVersion = new perfio.protoapi.PluginProtos.Version();
      return _compilerVersion;
    }
    public perfio.protoapi.PluginProtos.CodeGeneratorRequest setCompilerVersion(perfio.protoapi.PluginProtos.Version value) { this._compilerVersion = value; this.flags0 |= 2; return this; }
    public boolean hasCompilerVersion() { return (this.flags0 & 2) != 0; }

    public static perfio.protoapi.PluginProtos.CodeGeneratorRequest parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.PluginProtos.CodeGeneratorRequest();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.PluginProtos.CodeGeneratorRequest base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> base.addFileToGenerate(perfio.proto.runtime.Runtime.parseString(in));
          case 18 -> base.setParameter(perfio.proto.runtime.Runtime.parseString(in));
          case 122 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addProtoFile(perfio.protoapi.DescriptorProtos.FileDescriptorProto.parseFrom(in2)); in2.close(); }
          case 138 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addSourceFileDescriptors(perfio.protoapi.DescriptorProtos.FileDescriptorProto.parseFrom(in2)); in2.close(); }
          case 26 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getCompilerVersion(); perfio.protoapi.PluginProtos.Version.parseFrom(in2, m); in2.close(); base.setCompilerVersion(m); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 2, 15, 17, 3 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      { var it = this._fileToGenerate.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 10); perfio.proto.runtime.Runtime.writeString(out, v); }}
      if(this.hasParameter()) { perfio.proto.runtime.Runtime.writeInt32(out, 18); perfio.proto.runtime.Runtime.writeString(out, this._parameter); }
      { var it = this._protoFile.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 122); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      { var it = this._sourceFileDescriptors.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 138); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
      if(this.hasCompilerVersion()) { perfio.proto.runtime.Runtime.writeInt32(out, 26); var out2 = out.defer(); this._compilerVersion.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.PluginProtos.CodeGeneratorRequest m) {
        if(this.flags0 != m.flags0) return false;
        if(!this._fileToGenerate.equals(m._fileToGenerate)) return false;
        if(this.hasParameter() && !this._parameter.equals(m._parameter)) return false;
        if(!this._protoFile.equals(m._protoFile)) return false;
        if(!this._sourceFileDescriptors.equals(m._sourceFileDescriptors)) return false;
        if(this.hasCompilerVersion() && !this._compilerVersion.equals(m._compilerVersion)) return false;
        return true;
      } else return false;
    }
  }

  public static final class CodeGeneratorResponse {
    public enum Feature {
      FEATURE_NONE(0),
      FEATURE_PROTO3_OPTIONAL(1),
      FEATURE_SUPPORTS_EDITIONS(2),
      UNRECOGNIZED(-1);
      public final int number;
      Feature(int number) { this.number = number; }
      public static Feature valueOf(int number) {
        return switch(number) {
          case 0 -> FEATURE_NONE;
          case 1 -> FEATURE_PROTO3_OPTIONAL;
          case 2 -> FEATURE_SUPPORTS_EDITIONS;
          default -> UNRECOGNIZED;
        };
      }
    }

    public static final class File {

      private int flags0;

      private java.lang.String _name = "";
      public java.lang.String getName() { return _name; }
      public perfio.protoapi.PluginProtos.CodeGeneratorResponse.File setName(java.lang.String value) { this._name = value; this.flags0 |= 1; return this; }
      public boolean hasName() { return (this.flags0 & 1) != 0; }

      private java.lang.String _insertionPoint = "";
      public java.lang.String getInsertionPoint() { return _insertionPoint; }
      public perfio.protoapi.PluginProtos.CodeGeneratorResponse.File setInsertionPoint(java.lang.String value) { this._insertionPoint = value; this.flags0 |= 2; return this; }
      public boolean hasInsertionPoint() { return (this.flags0 & 2) != 0; }

      private java.lang.String _content = "";
      public java.lang.String getContent() { return _content; }
      public perfio.protoapi.PluginProtos.CodeGeneratorResponse.File setContent(java.lang.String value) { this._content = value; this.flags0 |= 4; return this; }
      public boolean hasContent() { return (this.flags0 & 4) != 0; }

      private perfio.protoapi.DescriptorProtos.GeneratedCodeInfo _generatedCodeInfo;
      public perfio.protoapi.DescriptorProtos.GeneratedCodeInfo getGeneratedCodeInfo() {
        if(_generatedCodeInfo == null) _generatedCodeInfo = new perfio.protoapi.DescriptorProtos.GeneratedCodeInfo();
        return _generatedCodeInfo;
      }
      public perfio.protoapi.PluginProtos.CodeGeneratorResponse.File setGeneratedCodeInfo(perfio.protoapi.DescriptorProtos.GeneratedCodeInfo value) { this._generatedCodeInfo = value; this.flags0 |= 8; return this; }
      public boolean hasGeneratedCodeInfo() { return (this.flags0 & 8) != 0; }

      public static perfio.protoapi.PluginProtos.CodeGeneratorResponse.File parseFrom(perfio.BufferedInput in) throws java.io.IOException {
        var m = new perfio.protoapi.PluginProtos.CodeGeneratorResponse.File();
        parseFrom(in, m);
        return m;
      }
      public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.PluginProtos.CodeGeneratorResponse.File base) throws java.io.IOException {
        while(in.hasMore()) {
          int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
          switch(tag) {
            case 10 -> base.setName(perfio.proto.runtime.Runtime.parseString(in));
            case 18 -> base.setInsertionPoint(perfio.proto.runtime.Runtime.parseString(in));
            case 122 -> base.setContent(perfio.proto.runtime.Runtime.parseString(in));
            case 130 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); var m = base.getGeneratedCodeInfo(); perfio.protoapi.DescriptorProtos.GeneratedCodeInfo.parseFrom(in2, m); in2.close(); base.setGeneratedCodeInfo(m); }
            default -> parseOther(in, tag);
          }
        }
      }
      private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
        int wt = tag & 7;
        int field = tag >>> 3;
        switch(field) {
          case 1, 2, 15, 16 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
          default -> perfio.proto.runtime.Runtime.skip(in, wt);
        }
      }

      public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
        if(this.hasName()) { perfio.proto.runtime.Runtime.writeInt32(out, 10); perfio.proto.runtime.Runtime.writeString(out, this._name); }
        if(this.hasInsertionPoint()) { perfio.proto.runtime.Runtime.writeInt32(out, 18); perfio.proto.runtime.Runtime.writeString(out, this._insertionPoint); }
        if(this.hasContent()) { perfio.proto.runtime.Runtime.writeInt32(out, 122); perfio.proto.runtime.Runtime.writeString(out, this._content); }
        if(this.hasGeneratedCodeInfo()) { perfio.proto.runtime.Runtime.writeInt32(out, 130); var out2 = out.defer(); this._generatedCodeInfo.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }
      }

      public boolean equals(java.lang.Object o) {
        if(o == this) return true;
        else if(o instanceof perfio.protoapi.PluginProtos.CodeGeneratorResponse.File m) {
          if(this.flags0 != m.flags0) return false;
          if(this.hasName() && !this._name.equals(m._name)) return false;
          if(this.hasInsertionPoint() && !this._insertionPoint.equals(m._insertionPoint)) return false;
          if(this.hasContent() && !this._content.equals(m._content)) return false;
          if(this.hasGeneratedCodeInfo() && !this._generatedCodeInfo.equals(m._generatedCodeInfo)) return false;
          return true;
        } else return false;
      }
    }

    private int flags0;

    private java.lang.String _error = "";
    public java.lang.String getError() { return _error; }
    public perfio.protoapi.PluginProtos.CodeGeneratorResponse setError(java.lang.String value) { this._error = value; this.flags0 |= 1; return this; }
    public boolean hasError() { return (this.flags0 & 1) != 0; }

    private long _supportedFeatures;
    public long getSupportedFeatures() { return _supportedFeatures; }
    public perfio.protoapi.PluginProtos.CodeGeneratorResponse setSupportedFeatures(long value) { this._supportedFeatures = value; this.flags0 |= 2; return this; }
    public boolean hasSupportedFeatures() { return (this.flags0 & 2) != 0; }

    private int _minimumEdition;
    public int getMinimumEdition() { return _minimumEdition; }
    public perfio.protoapi.PluginProtos.CodeGeneratorResponse setMinimumEdition(int value) { this._minimumEdition = value; this.flags0 |= 4; return this; }
    public boolean hasMinimumEdition() { return (this.flags0 & 4) != 0; }

    private int _maximumEdition;
    public int getMaximumEdition() { return _maximumEdition; }
    public perfio.protoapi.PluginProtos.CodeGeneratorResponse setMaximumEdition(int value) { this._maximumEdition = value; this.flags0 |= 8; return this; }
    public boolean hasMaximumEdition() { return (this.flags0 & 8) != 0; }

    private java.util.List<perfio.protoapi.PluginProtos.CodeGeneratorResponse.File> _file = java.util.List.of();
    public java.util.List<perfio.protoapi.PluginProtos.CodeGeneratorResponse.File> getFileList() { return _file; }
    public void setFileList(java.util.List<perfio.protoapi.PluginProtos.CodeGeneratorResponse.File> value) { this._file = value; }
    public void addFile(perfio.protoapi.PluginProtos.CodeGeneratorResponse.File value) {
      if(this._file == null || (java.util.List)this._file == java.util.List.of()) this.setFileList(new java.util.ArrayList<>());
      this._file.add(value);
    }
    public boolean hasFile() { return !_file.isEmpty(); }

    public static perfio.protoapi.PluginProtos.CodeGeneratorResponse parseFrom(perfio.BufferedInput in) throws java.io.IOException {
      var m = new perfio.protoapi.PluginProtos.CodeGeneratorResponse();
      parseFrom(in, m);
      return m;
    }
    public static void parseFrom(perfio.BufferedInput in, perfio.protoapi.PluginProtos.CodeGeneratorResponse base) throws java.io.IOException {
      while(in.hasMore()) {
        int tag = (int)perfio.proto.runtime.Runtime.parseVarint(in);
        switch(tag) {
          case 10 -> base.setError(perfio.proto.runtime.Runtime.parseString(in));
          case 16 -> base.setSupportedFeatures(perfio.proto.runtime.Runtime.parseInt64(in));
          case 18 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setSupportedFeatures(perfio.proto.runtime.Runtime.parseInt64(in2));; in2.close(); }
          case 24 -> base.setMinimumEdition(perfio.proto.runtime.Runtime.parseInt32(in));
          case 26 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setMinimumEdition(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          case 32 -> base.setMaximumEdition(perfio.proto.runtime.Runtime.parseInt32(in));
          case 34 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); while(in2.hasMore()) base.setMaximumEdition(perfio.proto.runtime.Runtime.parseInt32(in2));; in2.close(); }
          case 122 -> { var in2 = in.delimitedView(perfio.proto.runtime.Runtime.parseLen(in)); base.addFile(perfio.protoapi.PluginProtos.CodeGeneratorResponse.File.parseFrom(in2)); in2.close(); }
          default -> parseOther(in, tag);
        }
      }
    }
    private static void parseOther(perfio.BufferedInput in, int tag) throws java.io.IOException {
      int wt = tag & 7;
      int field = tag >>> 3;
      switch(field) {
        case 1, 2, 3, 4, 15 -> throw perfio.proto.runtime.Runtime.invalidWireType(wt, field);
        default -> perfio.proto.runtime.Runtime.skip(in, wt);
      }
    }

    public void writeTo(perfio.BufferedOutput out) throws java.io.IOException {
      if(this.hasError()) { perfio.proto.runtime.Runtime.writeInt32(out, 10); perfio.proto.runtime.Runtime.writeString(out, this._error); }
      if(this.hasSupportedFeatures()) { perfio.proto.runtime.Runtime.writeInt32(out, 16); perfio.proto.runtime.Runtime.writeInt64(out, this._supportedFeatures); }
      if(this.hasMinimumEdition()) { perfio.proto.runtime.Runtime.writeInt32(out, 24); perfio.proto.runtime.Runtime.writeInt32(out, this._minimumEdition); }
      if(this.hasMaximumEdition()) { perfio.proto.runtime.Runtime.writeInt32(out, 32); perfio.proto.runtime.Runtime.writeInt32(out, this._maximumEdition); }
      { var it = this._file.iterator(); while(it.hasNext()) { var v = it.next(); perfio.proto.runtime.Runtime.writeInt32(out, 122); var out2 = out.defer(); v.writeTo(out2); perfio.proto.runtime.Runtime.writeLen(out, out2.totalBytesWritten()); out2.close(); }}
    }

    public boolean equals(java.lang.Object o) {
      if(o == this) return true;
      else if(o instanceof perfio.protoapi.PluginProtos.CodeGeneratorResponse m) {
        if(this.flags0 != m.flags0) return false;
        if(this.hasError() && !this._error.equals(m._error)) return false;
        if(this._supportedFeatures != m._supportedFeatures) return false;
        if(this._minimumEdition != m._minimumEdition) return false;
        if(this._maximumEdition != m._maximumEdition) return false;
        if(!this._file.equals(m._file)) return false;
        return true;
      } else return false;
    }
  }
}
