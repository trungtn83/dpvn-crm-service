package com.dpvn.crm.report.domain;

import com.dpvn.crmcrudservice.domain.entity.report.CustomerBySeller;
import com.dpvn.crmcrudservice.domain.entity.report.InteractionBySeller;
import com.dpvn.crmcrudservice.domain.entity.report.TaskBySeller;
import com.dpvn.reportcrudservice.domain.report.CallLogBySeller;
import com.dpvn.wmscrudservice.domain.entity.report.InvoiceBySeller;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PerformanceBySellerDetail {
  private LocalDate day;
  private List<InvoiceBySeller> invoices = new ArrayList<>();
  private List<CustomerBySeller> customers = new ArrayList<>();
  private List<InteractionBySeller> interactions = new ArrayList<>();
  private List<TaskBySeller> tasks = new ArrayList<>();
  private List<CallLogBySeller> callLogs = new ArrayList<>();

  public LocalDate getDay() {
    return day;
  }

  public void setDay(LocalDate day) {
    this.day = day;
  }

  public List<InvoiceBySeller> getInvoices() {
    return invoices;
  }

  public void setInvoices(List<InvoiceBySeller> invoices) {
    this.invoices = invoices;
  }

  public List<CustomerBySeller> getCustomers() {
    return customers;
  }

  public void setCustomers(List<CustomerBySeller> customers) {
    this.customers = customers;
  }

  public List<InteractionBySeller> getInteractions() {
    return interactions;
  }

  public void setInteractions(List<InteractionBySeller> interactions) {
    this.interactions = interactions;
  }

  public List<TaskBySeller> getTasks() {
    return tasks;
  }

  public void setTasks(List<TaskBySeller> tasks) {
    this.tasks = tasks;
  }

  public List<CallLogBySeller> getCallLogs() {
    return callLogs;
  }

  public void setCallLogs(List<CallLogBySeller> callLogs) {
    this.callLogs = callLogs;
  }
}
