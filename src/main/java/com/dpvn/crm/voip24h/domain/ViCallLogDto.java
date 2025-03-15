package com.dpvn.crm.voip24h.domain;

import com.dpvn.reportcrudservice.domain.entity.voip24h.ViCallLog;
import com.dpvn.shared.domain.BaseDto;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class ViCallLogDto extends BaseDto<ViCallLog> {
  private String uuid;
  private String callDate;
  private String caller;
  private String callee;
  private String did;
  private String extension;
  private String type;
  private String status;
  private String callId;
  private Long duration;

  @JsonAlias("billsec")
  private Long billSec;

  private String note;
  private String recordingFile;

  public ViCallLogDto() {
    super(ViCallLog.class);
  }

  // Override the `id` field to ignore it during deserialization
  @Override
  @JsonIgnore // This annotation applies only to TsOrderDto
  public Long getId() {
    return super.getId();
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getCallDate() {
    return callDate;
  }

  public void setCallDate(String callDate) {
    this.callDate = callDate;
  }

  public String getCaller() {
    return caller;
  }

  public void setCaller(String caller) {
    this.caller = caller;
  }

  public String getCallee() {
    return callee;
  }

  public void setCallee(String callee) {
    this.callee = callee;
  }

  public String getDid() {
    return did;
  }

  public void setDid(String did) {
    this.did = did;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String getStatus() {
    return status;
  }

  @Override
  public void setStatus(String status) {
    this.status = status;
  }

  public String getCallId() {
    return callId;
  }

  public void setCallId(String callId) {
    this.callId = callId;
  }

  public Long getDuration() {
    return duration;
  }

  public void setDuration(Long duration) {
    this.duration = duration;
  }

  public Long getBillSec() {
    return billSec;
  }

  public void setBillSec(Long billSec) {
    this.billSec = billSec;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getRecordingFile() {
    return recordingFile;
  }

  public void setRecordingFile(String recordingFile) {
    this.recordingFile = recordingFile;
  }
}
