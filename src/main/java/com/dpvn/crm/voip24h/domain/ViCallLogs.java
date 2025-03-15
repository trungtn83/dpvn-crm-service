package com.dpvn.crm.voip24h.domain;

public class ViCallLogs {
  private ViCallLogs() {}

  public static class Type {
    private Type() {}

    public static final String INBOUND = "inbound";
    public static final String OUTBOUND = "outbound";
    public static final String LOCAL = "local";
  }

  public static class Status {
    private Status() {}

    public static final String ANSWERED = "ANSWERED";
    public static final String NO_ANSWER = "NO ANSWER";
    public static final String MISSED = "MISSED";
    public static final String FAILED = "FAILED";
    public static final String BUSY = "BUSY";
  }
}
